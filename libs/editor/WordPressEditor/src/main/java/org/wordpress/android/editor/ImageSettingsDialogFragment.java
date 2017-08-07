package org.wordpress.android.editor;

import android.app.DialogFragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
    private SeekBar mWidthSeekBar;
    private TextView mWidthLabel;
    private Spinner mAlignmentSpinner;
    private String[] mAlignmentKeyArray;
    private EditText mLinkTo;
    private CheckBox mFeaturedCheckBox;

    private boolean mIsFeatured;

    private CharSequence mPreviousActionBarTitle;
    private boolean mPreviousHomeAsUpEnabled;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
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
        mWidthSeekBar = (SeekBar) view.findViewById(R.id.image_width_seekbar);
        mFeaturedCheckBox = (CheckBox) view.findViewById(R.id.featuredImage);
        mWidthLabel = (TextView) view.findViewById(R.id.image_width_caption);

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

                setupWidthSeekBar(mImageMeta.getInt(ATTR_DIMEN_WIDTH));

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
        if (item.getItemId() == android.R.id.home) {
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

            int newWidth = mWidthSeekBar.getProgress() * 10;
            metaData.put(ATTR_DIMEN_WIDTH, newWidth);
            metaData.put(ATTR_DIMEN_HEIGHT, getRelativeHeightFromWidth(newWidth));
        } catch (JSONException e) {
            AppLog.d(AppLog.T.EDITOR, "Unable to build JSON object from new meta data");
        }

        return metaData;
    }

    private void showWidthText(int imageWidth) {
        String label = getString(R.string.image_width) + " - " + Integer.toString(imageWidth) + "px";
        mWidthLabel.setText(label);
    }

    /**
     * Initialize the image width SeekBar and accompanying EditText
     */
    private void setupWidthSeekBar(int imageWidth) {
        mWidthSeekBar.setMax(mMaxImageWidth / 10);

        if (imageWidth != 0) {
            mWidthSeekBar.setProgress(imageWidth / 10);
            showWidthText(imageWidth);
        }

        mWidthSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
                showWidthText(progress * 10);
            }
        });
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
