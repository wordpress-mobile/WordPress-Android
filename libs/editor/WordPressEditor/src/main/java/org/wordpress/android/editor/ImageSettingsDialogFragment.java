package org.wordpress.android.editor;

import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/**
 * A full-screen DialogFragment with image settings.
 *
 * Modifies the action bar - host activity must call {@link ImageSettingsDialogFragment#dismissFragment()}
 * when the fragment is dismissed to restore it.
 */
public class ImageSettingsDialogFragment extends DialogFragment {
    public static final int IMAGE_SETTINGS_DIALOG_REQUEST_CODE = 5;
    public static final String IMAGE_SETTINGS_DIALOG_TAG = "image-settings";

    private JSONObject mImageMeta;
    private int mMaxImageWidth;

    private EditText mTitleText;
    private EditText mCaptionText;
    private EditText mAltText;
    private Spinner mAlignmentSpinner;
    private EditText mLinkTo;
    private EditText mWidthText;
    private CheckBox mFeaturedCheckBox;

    private boolean mIsFeatured;

    private Map<String, String> mHttpHeaders;

    private CharSequence mPreviousActionBarTitle;
    private boolean mPreviousHomeAsUpEnabled;
    private View mPreviousCustomView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            return;
        }

        actionBar.show();

        mPreviousActionBarTitle = actionBar.getTitle();
        mPreviousCustomView = actionBar.getCustomView();

        final int displayOptions = actionBar.getDisplayOptions();
        mPreviousHomeAsUpEnabled = (displayOptions & ActionBar.DISPLAY_HOME_AS_UP) != 0;

        actionBar.setTitle(R.string.image_settings);
        actionBar.setDisplayHomeAsUpEnabled(true);
        if (getResources().getBoolean(R.bool.show_extra_side_padding)) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close_padded);
        } else {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        }

        // Show custom view with padded Save button
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.image_settings_formatbar);

        actionBar.getCustomView().findViewById(R.id.menu_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mImageMeta = extractMetaDataFromFields(mImageMeta);

                String imageRemoteId = "";
                try {
                    imageRemoteId = mImageMeta.getString("attachment_id");
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.EDITOR, "Unable to retrieve featured image id from meta data");
                }

                Intent intent = new Intent();
                intent.putExtra("imageMeta", mImageMeta.toString());

                mIsFeatured = mFeaturedCheckBox.isChecked();
                intent.putExtra("isFeatured", mIsFeatured);

                if (!imageRemoteId.isEmpty()) {
                    intent.putExtra("imageRemoteId", Integer.parseInt(imageRemoteId));
                }

                getTargetFragment().onActivityResult(getTargetRequestCode(), getTargetRequestCode(), intent);

                restorePreviousActionBar();
                getFragmentManager().popBackStack();
                ToastUtils.showToast(getActivity(), R.string.image_settings_save_toast);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_image_options, container, false);

        ImageView thumbnailImage = (ImageView) view.findViewById(R.id.image_thumbnail);
        TextView filenameLabel = (TextView) view.findViewById(R.id.image_filename);
        mTitleText = (EditText) view.findViewById(R.id.image_title);
        mCaptionText = (EditText) view.findViewById(R.id.image_caption);
        mAltText = (EditText) view.findViewById(R.id.image_alt_text);
        mAlignmentSpinner = (Spinner) view.findViewById(R.id.alignment_spinner);
        mLinkTo = (EditText) view.findViewById(R.id.image_link_to);
        SeekBar widthSeekBar = (SeekBar) view.findViewById(R.id.image_width_seekbar);
        mWidthText = (EditText) view.findViewById(R.id.image_width_text);
        mFeaturedCheckBox = (CheckBox) view.findViewById(R.id.featuredImage);

        // Populate the dialog with existing values
        Bundle bundle = getArguments();
        if (bundle != null) {
            try {
                mImageMeta = new JSONObject(bundle.getString("imageMeta"));

                mHttpHeaders = (Map) bundle.getSerializable("headerMap");

                final String imageSrc = mImageMeta.getString("src");
                final String imageFilename = imageSrc.substring(imageSrc.lastIndexOf("/") + 1);

                loadThumbnail(imageSrc, thumbnailImage);
                filenameLabel.setText(imageFilename);

                mTitleText.setText(mImageMeta.getString("title"));
                mCaptionText.setText(mImageMeta.getString("caption"));
                mAltText.setText(mImageMeta.getString("alt"));

                String alignment = mImageMeta.getString("align");

                // Capitalize the alignment value to match the spinner entries
                alignment = alignment.substring(0, 1).toUpperCase(Locale.US) + alignment.substring(1);

                String[] alignmentArray = getResources().getStringArray(R.array.alignment_array);
                mAlignmentSpinner.setSelection(Arrays.asList(alignmentArray).indexOf(alignment));

                mLinkTo.setText(mImageMeta.getString("linkUrl"));

                mMaxImageWidth = MediaUtils.getMaximumImageWidth(mImageMeta.getInt("naturalWidth"),
                        bundle.getString("maxWidth"));

                setupWidthSeekBar(widthSeekBar, mWidthText, mImageMeta.getInt("width"));

                boolean featuredImageSupported = bundle.getBoolean("featuredImageSupported");
                if (featuredImageSupported) {
                    mFeaturedCheckBox.setVisibility(View.VISIBLE);
                    mIsFeatured = bundle.getBoolean("isFeatured", false);
                    mFeaturedCheckBox.setChecked(mIsFeatured);
                }
            } catch (JSONException e1) {
                AppLog.d(AppLog.T.EDITOR, "Missing JSON properties");
            }
        }

        mTitleText.requestFocus();

        return view;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.show();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (menu != null) {
            menu.clear();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            dismissFragment();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private ActionBar getActionBar() {
        if (!isAdded()) {
            return null;
        }

        if (getActivity() instanceof AppCompatActivity) {
            return ((AppCompatActivity) getActivity()).getSupportActionBar();
        } else {
            return null;
        }
    }

    /**
     * To be called when the fragment is being dismissed, either by ActionBar navigation or by pressing back in the
     * navigation bar.
     * Displays a confirmation dialog if there are unsaved changes, otherwise undoes the fragment's modifications to
     * the ActionBar and restores the last visible fragment.
     */
    public void dismissFragment() {
        try {
            JSONObject newImageMeta = extractMetaDataFromFields(new JSONObject());

            for (int i = 0; i < newImageMeta.names().length(); i++) {
                String name = newImageMeta.names().getString(i);
                if (!newImageMeta.getString(name).equals(mImageMeta.getString(name))) {
                    showDiscardChangesDialog();
                    return;
                }
            }

            if (mFeaturedCheckBox.isChecked() != mIsFeatured) {
                // Featured image status has changed
                showDiscardChangesDialog();
                return;
            }
        } catch (JSONException e) {
            AppLog.d(AppLog.T.EDITOR, "Unable to update JSON array");
        }

        getTargetFragment().onActivityResult(getTargetRequestCode(), getTargetRequestCode(), null);
        restorePreviousActionBar();
        getFragmentManager().popBackStack();
    }

    private void restorePreviousActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(mPreviousActionBarTitle);
            actionBar.setHomeAsUpIndicator(null);
            actionBar.setDisplayHomeAsUpEnabled(mPreviousHomeAsUpEnabled);

            actionBar.setCustomView(mPreviousCustomView);
            if (mPreviousCustomView == null) {
                actionBar.setDisplayShowCustomEnabled(false);
            }
        }
    }

    /**
     * Displays a dialog asking the user to confirm that they want to exit, discarding unsaved changes.
     */
    private void showDiscardChangesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.image_settings_dismiss_dialog_title));
        builder.setPositiveButton(getString(R.string.discard), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                getTargetFragment().onActivityResult(getTargetRequestCode(), getTargetRequestCode(), null);
                restorePreviousActionBar();
                getFragmentManager().popBackStack();
            }
        });

        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Extracts the meta data from the dialog fields and updates the entries in the given JSONObject.
     */
    private JSONObject extractMetaDataFromFields(JSONObject metaData) {
        try {
            metaData.put("title", mTitleText.getText().toString());
            metaData.put("caption", mCaptionText.getText().toString());
            metaData.put("alt", mAltText.getText().toString());
            metaData.put("align", mAlignmentSpinner.getSelectedItem().toString().toLowerCase(Locale.US));
            metaData.put("linkUrl", mLinkTo.getText().toString());

            int newWidth = getEditTextIntegerClamped(mWidthText, 10, mMaxImageWidth);
            metaData.put("width", newWidth);
            metaData.put("height", getRelativeHeightFromWidth(newWidth));
        } catch (JSONException e) {
            AppLog.d(AppLog.T.EDITOR, "Unable to build JSON object from new meta data");
        }

        return metaData;
    }

    private void loadThumbnail(final String src, final ImageView thumbnailImage) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (isAdded()) {
                    final Uri localUri = Utils.downloadExternalMedia(getActivity(), Uri.parse(src), mHttpHeaders);

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                thumbnailImage.setImageURI(localUri);
                            }
                        });
                    }
                }
            }
        });

        thread.start();
    }

    /**
     * Initialize the image width SeekBar and accompanying EditText
     */
    private void setupWidthSeekBar(final SeekBar widthSeekBar, final EditText widthText, int imageWidth) {
        widthSeekBar.setMax(mMaxImageWidth / 10);

        if (imageWidth != 0) {
            widthSeekBar.setProgress(imageWidth / 10);
            widthText.setText(String.valueOf(imageWidth) + "px");
        }

        widthSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 0) {
                    progress = 1;
                }
                widthText.setText(progress * 10 + "px");
            }
        });

        widthText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    widthText.setText("");
                }
            }
        });

        widthText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                int width = getEditTextIntegerClamped(widthText, 10, mMaxImageWidth);
                widthSeekBar.setProgress(width / 10);
                widthText.setSelection((String.valueOf(width).length()));

                InputMethodManager imm = (InputMethodManager) getActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(widthText.getWindowToken(),
                        InputMethodManager.RESULT_UNCHANGED_SHOWN);

                return true;
            }
        });
    }

    /**
     * Return the integer value of the width EditText, adjusted to be within the given min and max, and stripped of the
     * 'px' units
     */
    private int getEditTextIntegerClamped(EditText editText, int minWidth, int maxWidth) {
        int width = 10;

        try {
            if (editText.getText() != null)
                width = Integer.parseInt(editText.getText().toString().replace("px", ""));
        } catch (NumberFormatException e) {
            AppLog.e(AppLog.T.EDITOR, e);
        }

        width = Math.min(maxWidth, Math.max(width, minWidth));

        return width;
    }

    /**
     * Given the new width, return the proportionally adjusted height, given the dimensions of the original image
     */
    private int getRelativeHeightFromWidth(int width) {
        int height = 0;

        try {
            int naturalHeight = mImageMeta.getInt("naturalHeight");
            int naturalWidth = mImageMeta.getInt("naturalWidth");

            float ratio = (float) naturalHeight / naturalWidth;
            height = (int) (ratio * width);
        } catch (JSONException e) {
            AppLog.d(AppLog.T.EDITOR, "JSON object missing naturalHeight or naturalWidth property");
        }

        return height;
    }
}
