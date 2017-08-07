package org.wordpress.android.editor;

import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
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
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.MediaUtils;

import java.util.Arrays;

import static org.wordpress.android.editor.EditorFragmentAbstract.ATTR_ALIGN;
import static org.wordpress.android.editor.EditorFragmentAbstract.ATTR_ALT;
import static org.wordpress.android.editor.EditorFragmentAbstract.ATTR_CAPTION;
import static org.wordpress.android.editor.EditorFragmentAbstract.ATTR_DIMEN_HEIGHT;
import static org.wordpress.android.editor.EditorFragmentAbstract.ATTR_DIMEN_WIDTH;
import static org.wordpress.android.editor.EditorFragmentAbstract.ATTR_ID_ATTACHMENT;
import static org.wordpress.android.editor.EditorFragmentAbstract.ATTR_ID_IMAGE_REMOTE;
import static org.wordpress.android.editor.EditorFragmentAbstract.ATTR_TITLE;
import static org.wordpress.android.editor.EditorFragmentAbstract.ATTR_URL_LINK;
import static org.wordpress.android.editor.EditorFragmentAbstract.EXTRA_ENABLED_AZTEC;
import static org.wordpress.android.editor.EditorFragmentAbstract.EXTRA_FEATURED;
import static org.wordpress.android.editor.EditorFragmentAbstract.EXTRA_IMAGE_FEATURED;
import static org.wordpress.android.editor.EditorFragmentAbstract.EXTRA_IMAGE_META;
import static org.wordpress.android.editor.EditorFragmentAbstract.EXTRA_MAX_WIDTH;

/**
 * A full-screen DialogFragment with image settings.
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
    private String[] mAlignmentKeyArray;
    private EditText mLinkTo;
    private EditText mWidthText;
    private CheckBox mFeaturedCheckBox;

    private boolean mIsFeatured;

    private CharSequence mPreviousActionBarTitle;
    private boolean mPreviousHomeAsUpEnabled;

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

        final int displayOptions = actionBar.getDisplayOptions();
        mPreviousHomeAsUpEnabled = (displayOptions & ActionBar.DISPLAY_HOME_AS_UP) != 0;

        actionBar.setTitle(R.string.image_settings);
        actionBar.setDisplayHomeAsUpEnabled(true);
        if (getResources().getBoolean(R.bool.show_extra_side_padding)) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close_padded);
        } else {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        }
    }

    public void saveAndClose() {
        mImageMeta = extractMetaDataFromFields(mImageMeta);

        String imageRemoteId = "";
        try {
            imageRemoteId = mImageMeta.getString(ATTR_ID_ATTACHMENT);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.EDITOR, "Unable to retrieve featured image id from meta data");
        }

        Intent intent = new Intent();
        intent.putExtra(EXTRA_IMAGE_META, mImageMeta.toString());

        mIsFeatured = mFeaturedCheckBox.isChecked();
        intent.putExtra(EXTRA_FEATURED, mIsFeatured);

        if (!imageRemoteId.isEmpty()) {
            intent.putExtra(ATTR_ID_IMAGE_REMOTE, Integer.parseInt(imageRemoteId));
        }

        getTargetFragment().onActivityResult(getTargetRequestCode(), getTargetRequestCode(), intent);

        restorePreviousActionBar();
        getFragmentManager().popBackStack();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_image_options, container, false);

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
                mImageMeta = new JSONObject(bundle.getString(EXTRA_IMAGE_META));

                mTitleText.setText(mImageMeta.getString(ATTR_TITLE));
                mCaptionText.setText(mImageMeta.getString(ATTR_CAPTION));
                mAltText.setText(mImageMeta.getString(ATTR_ALT));

                String alignment = mImageMeta.getString(ATTR_ALIGN);
                mAlignmentKeyArray = getResources().getStringArray(R.array.alignment_key_array);
                int alignmentIndex = Arrays.asList(mAlignmentKeyArray).indexOf(alignment);
                mAlignmentSpinner.setSelection(alignmentIndex == -1 ? 0 : alignmentIndex);

                mLinkTo.setText(mImageMeta.getString(ATTR_URL_LINK));

                mMaxImageWidth = MediaUtils.getMaximumImageWidth(mImageMeta.getInt("naturalWidth"),
                        bundle.getString(EXTRA_MAX_WIDTH));

                setupWidthSeekBar(widthSeekBar, mWidthText, mImageMeta.getInt(ATTR_DIMEN_WIDTH));

                boolean featuredImageSupported = bundle.getBoolean(EXTRA_IMAGE_FEATURED);
                if (featuredImageSupported) {
                    mFeaturedCheckBox.setVisibility(View.VISIBLE);
                    mIsFeatured = bundle.getBoolean(EXTRA_FEATURED, false);
                    mFeaturedCheckBox.setChecked(mIsFeatured);
                }
            } catch (JSONException e1) {
                AppLog.d(AppLog.T.EDITOR, "Missing JSON properties");
            }

            // TODO: Unsupported in Aztec - remove once caption, alignment & link support added
            if (bundle.getBoolean(EXTRA_ENABLED_AZTEC)) {
                mCaptionText.setVisibility(View.GONE);
                View label = view.findViewById(R.id.image_caption_label);
                if (label != null) {
                    label.setVisibility(View.GONE);
                }

                mLinkTo.setVisibility(View.GONE);
                label = view.findViewById(R.id.image_link_to_label);
                if (label != null) {
                    label.setVisibility(View.GONE);
                }

                mAlignmentSpinner.setVisibility(View.GONE);
                label = view.findViewById(R.id.alignment_spinner_label);
                if (label != null) {
                    label.setVisibility(View.GONE);
                }
            }
        }


        mTitleText.requestFocus();

        return view;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
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
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private ActionBar getActionBar() {
        if (getActivity() instanceof AppCompatActivity) {
            return ((AppCompatActivity) getActivity()).getSupportActionBar();
        } else {
            return null;
        }
    }

    private void restorePreviousActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(mPreviousActionBarTitle);
            actionBar.setHomeAsUpIndicator(null);
            actionBar.setDisplayHomeAsUpEnabled(mPreviousHomeAsUpEnabled);
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
            metaData.put(ATTR_TITLE, mTitleText.getText().toString());
            metaData.put(ATTR_CAPTION, mCaptionText.getText().toString());
            metaData.put(ATTR_ALT, mAltText.getText().toString());
            if (mAlignmentSpinner.getSelectedItemPosition() < mAlignmentKeyArray.length) {
                metaData.put(ATTR_ALIGN, mAlignmentKeyArray[mAlignmentSpinner.getSelectedItemPosition()]);
            }
            metaData.put(ATTR_URL_LINK, mLinkTo.getText().toString());

            int newWidth = getEditTextIntegerClamped(mWidthText, 1, Integer.MAX_VALUE);
            metaData.put(ATTR_DIMEN_WIDTH, newWidth);
            metaData.put(ATTR_DIMEN_HEIGHT, getRelativeHeightFromWidth(newWidth));
        } catch (JSONException e) {
            AppLog.d(AppLog.T.EDITOR, "Unable to build JSON object from new meta data");
        }

        return metaData;
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
