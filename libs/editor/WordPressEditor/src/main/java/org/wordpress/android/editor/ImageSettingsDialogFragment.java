package org.wordpress.android.editor;

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
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

import java.util.Arrays;

public class ImageSettingsDialogFragment extends DialogFragment {

    public static final int IMAGE_SETTINGS_DIALOG_REQUEST_CODE = 5;
    public static final String IMAGE_SETTINGS_DIALOG_TAG = "image-settings";

    private static final int DEFAULT_MAX_IMAGE_WIDTH = 1024;

    private JSONObject mImageMeta;
    private int mMaxImageWidth;

    private EditText mTitleText;
    private EditText mCaptionText;
    private EditText mAltText;
    private Spinner mAlignmentSpinner;
    private EditText mLinkTo;
    private EditText mWidthText;
    private CheckBox mFeaturedCheckBox;

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
                String imageRemoteId = "";
                try {
                    imageRemoteId = mImageMeta.getString("attachment_id");

                    mImageMeta.put("title", mTitleText.getText().toString());
                    mImageMeta.put("caption", mCaptionText.getText().toString());
                    mImageMeta.put("alt", mAltText.getText().toString());
                    mImageMeta.put("align", mAlignmentSpinner.getSelectedItem().toString());
                    mImageMeta.put("linkUrl", mLinkTo.getText().toString());

                    int newWidth = getEditTextIntegerClamped(mWidthText, 10, mMaxImageWidth);
                    mImageMeta.put("width", newWidth);
                    mImageMeta.put("height", getRelativeHeightFromWidth(newWidth));
                } catch (JSONException e) {
                    AppLog.d(AppLog.T.EDITOR, "Unable to update JSON array");
                }

                Intent intent = new Intent();
                intent.putExtra("imageMeta", mImageMeta.toString());

                boolean isFeaturedImage = mFeaturedCheckBox.isChecked();
                intent.putExtra("isFeatured", isFeaturedImage);

                if (!imageRemoteId.isEmpty()) {
                    intent.putExtra("imageRemoteId", Integer.parseInt(imageRemoteId));
                }

                getTargetFragment().onActivityResult(getTargetRequestCode(), getTargetRequestCode(), intent);

                restorePreviousActionBar();
                getFragmentManager().popBackStack();
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
        final CheckBox featuredInPostCheckBox = (CheckBox) view.findViewById(R.id.featuredInPost);

        // Populate the dialog with existing values
        Bundle bundle = getArguments();
        if (bundle != null) {
            try {
                mImageMeta = new JSONObject(bundle.getString("imageMeta"));

                final String imageSrc = mImageMeta.getString("src");
                final String imageFilename = imageSrc.substring(imageSrc.lastIndexOf("/") + 1);

                loadThumbnail(imageSrc, thumbnailImage);
                filenameLabel.setText(imageFilename);

                mTitleText.setText(mImageMeta.getString("title"));
                mCaptionText.setText(mImageMeta.getString("caption"));
                mAltText.setText(mImageMeta.getString("alt"));

                String alignment = mImageMeta.getString("align");
                String[] alignmentArray = getResources().getStringArray(R.array.alignment_array);
                mAlignmentSpinner.setSelection(Arrays.asList(alignmentArray).indexOf(alignment));

                mLinkTo.setText(mImageMeta.getString("linkUrl"));

                mMaxImageWidth = getMaximumImageWidth(mImageMeta.getInt("naturalWidth"), bundle.getString("maxWidth"));

                setupWidthSeekBar(widthSeekBar, mWidthText, mImageMeta.getInt("width"));

                // TODO: Featured image handling
                mFeaturedCheckBox.setChecked(bundle.getBoolean("isFeatured", false));

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
            restorePreviousActionBar();
            getFragmentManager().popBackStack();
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

    private void loadThumbnail(final String src, final ImageView thumbnailImage) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (isAdded()) {
                    final Uri localUri = Utils.downloadExternalMedia(getActivity(), Uri.parse(src));

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            thumbnailImage.setImageURI(localUri);
                        }
                    });
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
     * Calculate and return the maximum allowed image width by comparing the width of the image at its full size with
     * the maximum upload width set in the blog settings
     * @param naturalImageWidth the image's natural (full) width
     * @param imageWidthBlogSettingString the maximum upload width set in the blog settings
     * @return
     */
    public static int getMaximumImageWidth(int naturalImageWidth, String imageWidthBlogSettingString) {
        int imageWidthBlogSetting = Integer.MAX_VALUE;

        if (!imageWidthBlogSettingString.equals("Original Size")) {
            try {
                imageWidthBlogSetting = Integer.valueOf(imageWidthBlogSettingString);
            } catch (NumberFormatException e) {
                AppLog.e(AppLog.T.EDITOR, e);
            }
        }

        int imageWidthPictureSetting = naturalImageWidth == 0 ? Integer.MAX_VALUE : naturalImageWidth;

        if (Math.min(imageWidthPictureSetting, imageWidthBlogSetting) == Integer.MAX_VALUE) {
            // Default value in case of errors reading the picture size and the blog settings is set to Original size
            return DEFAULT_MAX_IMAGE_WIDTH;
        } else {
            return Math.min(imageWidthPictureSetting, imageWidthBlogSetting);
        }
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
