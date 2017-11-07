package org.wordpress.android.ui.media;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.editor.EditorImageMetaData;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.media.MediaPreviewActivity.MediaPreviewSwiped;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPMediaUtils;
import org.wordpress.android.util.WPPermissionUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import javax.inject.Inject;

import static org.wordpress.android.editor.EditorImageMetaData.ARG_EDITOR_IMAGE_METADATA;

public class MediaSettingsActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String ARG_MEDIA_LOCAL_ID = "media_local_id";
    private static final String ARG_ID_LIST = "id_list";
    public static final int RESULT_MEDIA_DELETED = RESULT_FIRST_USER;

    private long mDownloadId;
    private String mTitle;
    private boolean mDidRegisterEventBus;
    private boolean mOverrideClosingTransition;

    private SiteModel mSite;
    private MediaModel mMedia;
    private EditorImageMetaData mEditorImageMetaData;
    private ArrayList<String> mMediaIdList;
    private String[] mAlignmentKeyArray;

    private ImageView mImageView;
    private ImageView mImagePlay;
    private EditText mTitleView;
    private EditText mCaptionView;
    private EditText mAltTextView;
    private EditText mDescriptionView;
    private EditText mImageWidthView;
    private SeekBar mImageWidthSeekBarView;
    private Spinner mAlignmentSpinnerView;
    private FloatingActionButton mFabView;

    private ProgressDialog mProgressDialog;

    private enum MediaType {
        IMAGE,
        VIDEO,
        AUDIO,
        DOCUMENT
    }

    private MediaType mMediaType;

    @Inject
    MediaStore mMediaStore;
    @Inject
    FluxCImageLoader mImageLoader;
    @Inject
    Dispatcher mDispatcher;

    /**
     * @param activity    calling activity
     * @param site        site this media is associated with
     * @param media       media model to display
     * @param mediaIdList optional list of media IDs to page through in preview screen
     * @param sourceView  optional view to use in shared element transition
     */
    public static void showForResult(@NonNull Activity activity,
                                     @NonNull SiteModel site,
                                     @NonNull MediaModel media,
                                     @Nullable ArrayList<String> mediaIdList,
                                     @Nullable View sourceView) {
        // go directly to preview for local images, videos and audio (do nothing for local documents)
        if (MediaUtils.isLocalFile(media.getUploadState())) {
            if (MediaUtils.isValidImage(media.getFilePath())
                    || MediaUtils.isAudio(media.getFilePath())
                    || media.isVideo()) {
                MediaPreviewActivity.showPreview(activity, site, media.getFilePath());
            }
            return;
        }

        Intent intent = new Intent(activity, MediaSettingsActivity.class);
        intent.putExtra(ARG_MEDIA_LOCAL_ID, media.getId());
        intent.putExtra(WordPress.SITE, site);

        if (mediaIdList != null) {
            intent.putExtra(ARG_ID_LIST, mediaIdList);
        }

        ActivityOptionsCompat options;

        if (sourceView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String sharedElementName = activity.getString(R.string.shared_element_media);
            sourceView.setTransitionName(sharedElementName);
            options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, sourceView, sharedElementName);
        } else {
            options = ActivityOptionsCompat.makeCustomAnimation(
                    activity,
                    R.anim.activity_slide_up_from_bottom,
                    R.anim.do_nothing);
        }
        ActivityCompat.startActivityForResult(activity, intent, RequestCodes.MEDIA_SETTINGS, options.toBundle());
    }

    /**
     * @param activity calling activity
     * @param site     site this media is associated with
     * @param media    editor image metadata
     */
    public static void showForResult(@NonNull Activity activity,
                                     @NonNull SiteModel site,
                                     @NonNull EditorImageMetaData media) {

        Intent intent = new Intent(activity, MediaSettingsActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(ARG_EDITOR_IMAGE_METADATA, media);

        ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                activity,
                R.anim.activity_slide_up_from_bottom,
                R.anim.do_nothing);

        ActivityCompat.startActivityForResult(activity, intent, RequestCodes.MEDIA_SETTINGS, options.toBundle());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.media_settings_activity);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        }

        // on Lollipop and above we close with a shared element transition set in the intent, otherwise use a
        // slide out transition when the activity finishes
        mOverrideClosingTransition = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP;

        mImageView = (ImageView) findViewById(R.id.image_preview);
        mImagePlay = (ImageView) findViewById(R.id.image_play);
        mTitleView = (EditText) findViewById(R.id.edit_title);
        mCaptionView = (EditText) findViewById(R.id.edit_caption);
        mAltTextView = (EditText) findViewById(R.id.edit_alt_text);
        mDescriptionView = (EditText) findViewById(R.id.edit_description);
        mImageWidthView = (EditText) findViewById(R.id.edit_image_width);
        mImageWidthSeekBarView = (SeekBar) findViewById(R.id.image_width_seekbar);
        mAlignmentSpinnerView = (Spinner) findViewById(org.wordpress.android.editor.R.id.alignment_spinner);
        mFabView = (FloatingActionButton) findViewById(R.id.fab_button);

        int mediaId;
        if (savedInstanceState != null) {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mEditorImageMetaData = savedInstanceState.getParcelable(ARG_EDITOR_IMAGE_METADATA);
            mediaId = savedInstanceState.getInt(ARG_MEDIA_LOCAL_ID);
            if (savedInstanceState.containsKey(ARG_ID_LIST)) {
                mMediaIdList = savedInstanceState.getStringArrayList(ARG_ID_LIST);
            }
        } else {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            mEditorImageMetaData = getIntent().getParcelableExtra(ARG_EDITOR_IMAGE_METADATA);
            mediaId = getIntent().getIntExtra(ARG_MEDIA_LOCAL_ID, 0);
            if (getIntent().hasExtra(ARG_ID_LIST)) {
                mMediaIdList = getIntent().getStringArrayListExtra(ARG_ID_LIST);
            }
        }

        if (!loadMediaId(mediaId)) {
            delayedFinishWithError();
            return;
        }

        // only show title when toolbar is collapsed
        final CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.app_bar_layout);
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    collapsingToolbar.setTitle(mTitle);
                } else {
                    collapsingToolbar.setTitle(" "); // space between double quotes is on purpose
                }
            }
        });

        // make image 40% of screen height
        int displayHeight = DisplayUtils.getDisplayPixelHeight(this);
        int imageHeight = (int) (displayHeight * 0.4);
        mImageView.getLayoutParams().height = imageHeight;

        // position progress in middle of image
        View progressView = findViewById(R.id.progress);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) progressView.getLayoutParams();
        int topMargin = (imageHeight / 2) - (progressView.getHeight() / 2);
        params.setMargins(0, topMargin, 0, 0);

        // set the height of the gradient scrim that appears atop the image
        int toolbarHeight = DisplayUtils.getActionBarHeight(this);
        ImageView imgScrim = (ImageView) findViewById(R.id.image_gradient_scrim);
        imgScrim.getLayoutParams().height = toolbarHeight * 3;

        adjustToolbar();

        // tap to show full screen view (not supported for documents)
        if (!isDocument()) {
            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showFullScreen();
                }
            };
            mFabView.setOnClickListener(listener);
            mImageView.setOnClickListener(listener);
            mImagePlay.setOnClickListener(listener);
        }
    }

    private boolean isMediaFromEditor() {
        return mEditorImageMetaData != null;
    }

    private void reloadMedia() {
        loadMediaId(mMedia.getId());
    }

    private boolean loadMediaId(int mediaId) {
        MediaModel media;
        if (isMediaFromEditor()) {
            media = convertEditorImageMetaDataToMediaModel(mEditorImageMetaData);
        } else {
            media = mMediaStore.getMediaWithLocalId(mediaId);
        }

        if (media == null) {
            return false;
        }

        mMedia = media;

        // determine media type up front, default to DOCUMENT if we can't detect it's an image, video, or audio file
        if (MediaUtils.isValidImage(mMedia.getUrl())) {
            mMediaType = MediaType.IMAGE;
            mTitle = getString(R.string.media_title_image_details);
        } else if (mMedia.isVideo()) {
            mMediaType = MediaType.VIDEO;
            mTitle = getString(R.string.media_title_video_details);
        } else if (MediaUtils.isAudio(mMedia.getUrl())) {
            mMediaType = MediaType.AUDIO;
            mTitle = getString(R.string.media_title_audio_details);
        } else {
            mMediaType = MediaType.DOCUMENT;
            mTitle = getString(R.string.media_title_document_details);
        }

        mImagePlay.setVisibility(isVideo() || isAudio() ? View.VISIBLE : View.GONE);
        findViewById(R.id.edit_alt_text_layout).setVisibility(isVideo() || isAudio() || isDocument() ? View.GONE : View.VISIBLE);

        showMetaData();

        // audio & documents show a placeholder on top of a gradient, otherwise we show a thumbnail
        if (isAudio() || isDocument()) {
            int padding = getResources().getDimensionPixelSize(R.dimen.margin_extra_extra_large);
            @DrawableRes int imageRes = WPMediaUtils.getPlaceholder(mMedia.getUrl());
            if (imageRes == 0) {
                imageRes = R.drawable.ic_gridicons_page;
            }
            mImageView.setPadding(padding, padding * 2, padding, padding);
            mImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            mImageView.setImageResource(imageRes);
        } else {
            loadImage();
        }

        return true;
    }

    private MediaModel convertEditorImageMetaDataToMediaModel(@NonNull EditorImageMetaData editorImageMetaData) {
        MediaModel mediaModel = new MediaModel();
        mediaModel.setUrl(editorImageMetaData.getSrc());
        mediaModel.setTitle(editorImageMetaData.getTitle());
        mediaModel.setCaption(editorImageMetaData.getCaption());
        mediaModel.setAlt(editorImageMetaData.getAlt());
        mediaModel.setFileName(editorImageMetaData.getSrc().substring(editorImageMetaData.getSrc().lastIndexOf("/") + 1));
        mediaModel.setFileExtension(org.wordpress.android.fluxc.utils.MediaUtils.getExtension(editorImageMetaData.getSrc()));
        mediaModel.setWidth(editorImageMetaData.getWidthInt());
        mediaModel.setHeight(editorImageMetaData.getHeightInt());
        return mediaModel;
    }

    @Override
    protected void onResume() {
        super.onResume();

        long delayMs = getResources().getInteger(R.integer.fab_animation_delay);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && shouldShowFab()) {
                    showFab();
                }
            }
        }, delayMs);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null && !actionBar.isShowing()) {
            actionBar.show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ARG_MEDIA_LOCAL_ID, mMedia.getId());
        outState.putParcelable(ARG_EDITOR_IMAGE_METADATA, mEditorImageMetaData);

        if (mSite != null) {
            outState.putSerializable(WordPress.SITE, mSite);
        }
        if (mMediaIdList != null) {
            outState.putStringArrayList(ARG_ID_LIST, mMediaIdList);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        registerReceiver(mDownloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        mDispatcher.register(this);

        // we only register with EventBus the first time - necessary since we don't unregister in onStop()
        // because we want to keep receiving events while the preview is showing
        if (!mDidRegisterEventBus) {
            EventBus.getDefault().register(this);
            mDidRegisterEventBus = true;
        }
    }

    @Override
    public void onStop() {
        unregisterReceiver(mDownloadReceiver);
        mDispatcher.unregister(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mDidRegisterEventBus) {
            EventBus.getDefault().unregister(this);
        }
        super.onDestroy();
    }

    private void delayedFinishWithError() {
        ToastUtils.showToast(this, R.string.error_media_not_found);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doFinishAfterTransition();
            }
        }, 1500);
    }

    @Override
    public void finish() {
        super.finish();
        if (mOverrideClosingTransition) {
            overridePendingTransition(R.anim.do_nothing, R.anim.activity_slide_out_to_bottom);
        }
    }

    /*
     * adjust the toolbar so it doesn't overlap the status bar
     */
    private void adjustToolbar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                int statusHeight = getResources().getDimensionPixelSize(resourceId);
                View toolbar = findViewById(R.id.toolbar);
                toolbar.getLayoutParams().height += statusHeight;
                toolbar.setPadding(0, statusHeight, 0, 0);
            }
        }
    }

    private boolean shouldShowFab() {
        // fab only shows for images
        return mMedia != null && isImage();
    }

    private void showProgress(boolean show) {
        findViewById(R.id.progress).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onBackPressed() {
        saveChanges();
        finishActivity();
    }

    private void finishActivity() {
        // call finish() rather than super.onBackPressed() to enable skipping shared element transition
        if (mOverrideClosingTransition) {
            finish();
        } else {
            doFinishAfterTransition();
        }
    }

    /*
     * wrapper for supportFinishAfterTransition() which first hides the FAB to prevent it flickering
     * during the shared element transition
     */
    private void doFinishAfterTransition() {
        mFabView.setVisibility(View.GONE);
        supportFinishAfterTransition();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.media_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean showSaveMenu = mSite != null && !mSite.isPrivate() && !isMediaFromEditor();
        boolean showShareMenu = mSite != null && !mSite.isPrivate() && !isMediaFromEditor();
        boolean showTrashMenu = mSite != null && !isMediaFromEditor();
        boolean showRemoveImage = mSite != null && isMediaFromEditor();

        MenuItem mnuSave = menu.findItem(R.id.menu_save);
        mnuSave.setVisible(showSaveMenu);
        mnuSave.setEnabled(mDownloadId == 0);

        MenuItem mnuShare = menu.findItem(R.id.menu_share);
        mnuShare.setVisible(showShareMenu);

        MenuItem mnuTrash = menu.findItem(R.id.menu_trash);
        mnuTrash.setVisible(showTrashMenu);

        MenuItem mnuRemove = menu.findItem(R.id.menu_remove_image);
        mnuRemove.setVisible(showRemoveImage);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.menu_save) {
            saveMediaToDevice();
            return true;
        } else if (item.getItemId() == R.id.menu_share) {
            shareMedia();
            return true;
        } else if (item.getItemId() == R.id.menu_trash) {
            deleteMediaWithConfirmation();
            return true;
        } else if (item.getItemId() == R.id.menu_remove_image) {
            removeMediaFromPostWithConfirmation();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean isImage() {
        return mMediaType == MediaType.IMAGE;
    }

    private boolean isVideo() {
        return mMediaType == MediaType.VIDEO;
    }

    private boolean isAudio() {
        return mMediaType == MediaType.AUDIO;
    }

    private boolean isDocument() {
        return mMediaType == MediaType.DOCUMENT;
    }

    private void showMetaData() {
        mTitleView.setText(mMedia.getTitle());
        mAltTextView.setText(mMedia.getAlt());

        if (isMediaFromEditor()) {
            mDescriptionView.setVisibility(View.GONE);
            mCaptionView.setVisibility(View.GONE);
            setupAlignmentSpinner();

            if (isRealImageSizeKnown()) {
                setupWidthSeekBar();
            }

        } else {
            mDescriptionView.setText(mMedia.getDescription());
            mCaptionView.setText(mMedia.getCaption());

            findViewById(R.id.editor_image_metadadata_container).setVisibility(View.GONE);
        }

        TextView txtUrl = (TextView) findViewById(R.id.text_url);
        txtUrl.setText(mMedia.getUrl());

        TextView txtFilename = (TextView) findViewById(R.id.text_filename);
        txtFilename.setText(mMedia.getFileName());

        TextView txtFileType = (TextView) findViewById(R.id.text_filetype);
        txtFileType.setText(StringUtils.notNullStr(mMedia.getFileExtension()).toUpperCase());

        if (isMediaFromEditor() && isRealImageSizeKnown()){
            showImageDimensions(mEditorImageMetaData.getMaxWidth(), mEditorImageMetaData.getMaxHeight());
        }else{
            showImageDimensions(mMedia.getWidth(), mMedia.getHeight());
        }


        String uploadDate = null;
        if (mMedia.getUploadDate() != null) {
            Date date = DateTimeUtils.dateFromIso8601(mMedia.getUploadDate());
            if (date != null) {
                uploadDate = SimpleDateFormat.getDateInstance().format(date);
            }
        }
        TextView txtUploadDate = (TextView) findViewById(R.id.text_upload_date);
        TextView txtUploadDateLabel = (TextView) findViewById(R.id.text_upload_date_label);
        if (uploadDate != null) {
            txtUploadDate.setVisibility(View.VISIBLE);
            txtUploadDateLabel.setVisibility(View.VISIBLE);
            txtUploadDate.setText(uploadDate);
        } else {
            txtUploadDate.setVisibility(View.GONE);
            txtUploadDateLabel.setVisibility(View.GONE);
        }

        TextView txtDuration = (TextView) findViewById(R.id.text_duration);
        TextView txtDurationLabel = (TextView) findViewById(R.id.text_duration_label);
        if (mMedia.getLength() > 0) {
            txtDuration.setVisibility(View.VISIBLE);
            txtDurationLabel.setVisibility(View.VISIBLE);
            txtDuration.setText(DateUtils.formatElapsedTime(mMedia.getLength()));
        } else {
            txtDuration.setVisibility(View.GONE);
            txtDurationLabel.setVisibility(View.GONE);
            findViewById(R.id.divider_duration).setVisibility(View.GONE);
        }

        boolean hasUrl = !TextUtils.isEmpty(mMedia.getUrl());
        View txtCopyUrl = findViewById(R.id.text_copy_url);
        txtCopyUrl.setVisibility(hasUrl ? View.VISIBLE : View.GONE);
        if (hasUrl) {
            txtCopyUrl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    copyMediaUrlToClipboard();
                }
            });
        }
    }

    /**
     * Initialize the image width SeekBar and accompanying EditText
     */
    private void setupWidthSeekBar() {
        mImageWidthSeekBarView.setMax(mEditorImageMetaData.getMaxWidth());

        if (mMedia.getWidth() != 0 && mMedia.getWidth() <= mEditorImageMetaData.getMaxWidth()) {
            mImageWidthSeekBarView.setProgress(mMedia.getWidth());
            mImageWidthView.setText(String.valueOf(mMedia.getWidth()));
        } else {
            mImageWidthSeekBarView.setProgress(mEditorImageMetaData.getMaxWidth());
            mImageWidthView.setText(String.valueOf(mEditorImageMetaData.getMaxWidth()));
        }

        mImageWidthSeekBarView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
                mImageWidthView.setText(String.valueOf(progress));
            }
        });

        mImageWidthView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                int widthValue;
                if (TextUtils.isEmpty(EditTextUtils.getText(mImageWidthView))) {
                    widthValue = mImageWidthSeekBarView.getProgress();
                } else {
                    widthValue = Integer.parseInt(EditTextUtils.getText(mImageWidthView));
                }

                int width = Math.min(mEditorImageMetaData.getMaxWidth(), Math.max(widthValue, 1));

                //OnSeekBarChangeListener will not be triggered if progress have not changed
                if (mImageWidthSeekBarView.getProgress() == width) {
                    mImageWidthView.setText(String.valueOf(width));
                } else {
                    mImageWidthSeekBarView.setProgress(width);
                }

                mImageWidthView.setSelection((String.valueOf(width).length()));

                return false;
            }
        });

        findViewById(R.id.disabled_width_selector_overlay).setVisibility(View.GONE);
    }

    private void showImageDimensions(int width, int height) {
        TextView txtDimensions = (TextView) findViewById(R.id.text_image_dimensions);
        TextView txtDimensionsLabel = (TextView) findViewById(R.id.text_image_dimensions_label);
        if (width > 0 && height > 0) {
            txtDimensions.setVisibility(View.VISIBLE);
            txtDimensionsLabel.setVisibility(View.VISIBLE);
            txtDimensionsLabel.setText(isVideo() ? R.string.media_edit_video_dimensions_caption : R.string
                    .media_edit_image_dimensions_caption);
            String dimens = width + " x " + height;
            txtDimensions.setText(dimens);
        } else {
            txtDimensions.setVisibility(View.GONE);
            txtDimensionsLabel.setVisibility(View.GONE);
            findViewById(R.id.divider_dimensions).setVisibility(View.GONE);
        }
    }

    /**
     * Initialize the image alignment spinner
     */
    private void setupAlignmentSpinner() {
        String alignment = mEditorImageMetaData.getAlign();
        mAlignmentKeyArray = getResources().getStringArray(R.array.alignment_key_array);
        int alignmentIndex = Arrays.asList(mAlignmentKeyArray).indexOf(alignment);
        mAlignmentSpinnerView.setSelection(alignmentIndex == -1 ? 0 : alignmentIndex);
    }

    /*
     * loads and displays a remote or local image
     */
    private void loadImage() {
        int width = DisplayUtils.getDisplayPixelWidth(this);
        int height = DisplayUtils.getDisplayPixelHeight(this);
        int size = Math.max(width, height);

        String mediaUri;
        if (isVideo()) {
            mediaUri = mMedia.getThumbnailUrl();
        } else {
            mediaUri = mMedia.getUrl();
        }

        if (TextUtils.isEmpty(mediaUri)) {
            if (isVideo()) {
                downloadVideoThumbnail();
            } else {
                ToastUtils.showToast(this, R.string.error_media_load);
            }
            return;
        }

        if (mediaUri.startsWith("http")) {
            showProgress(true);
            String imageUrl = mediaUri;
            if (SiteUtils.isPhotonCapable(mSite)) {
                imageUrl = PhotonUtils.getPhotonImageUrl(mediaUri, size, 0);
            }
            mImageLoader.get(imageUrl, new ImageLoader.ImageListener() {
                @Override
                public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                    if (!isFinishing() && response.getBitmap() != null) {
                        showProgress(false);
                        mImageView.setImageBitmap(response.getBitmap());
                        if (isMediaFromEditor() && !isRealImageSizeKnown()) {
                            int bitmapWidth = response.getBitmap().getWidth();
                            int bitmapHeight = response.getBitmap().getHeight();
                            mEditorImageMetaData.setMaxWidth(bitmapWidth);
                            mEditorImageMetaData.setMaxHeight(bitmapHeight);
                            setupWidthSeekBar();
                            showImageDimensions(bitmapWidth, bitmapHeight);
                        }
                    }
                }

                @Override
                public void onErrorResponse(VolleyError error) {
                    AppLog.e(AppLog.T.MEDIA, error);
                    if (!isFinishing()) {
                        showProgress(false);
                        delayedFinishWithError();
                    }
                }
            }, size, 0);
        } else {
            new LocalImageTask(mediaUri, size).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private boolean isRealImageSizeKnown() {
        return isMediaFromEditor() && mEditorImageMetaData.getMaxWidth() > 0 && mEditorImageMetaData.getMaxHeight() > 0;
    }

    /*
     * downloads and displays the thumbnail for a video that doesn't already have a thumbnail assigned (seen most
     * often with .org and JP sites)
     */
    private void downloadVideoThumbnail() {
        new Thread() {
            @Override
            public void run() {
                int width = DisplayUtils.getDisplayPixelWidth(MediaSettingsActivity.this);
                final Bitmap thumb = ImageUtils.getVideoFrameFromVideo(mMedia.getUrl(), width);
                if (thumb != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!isFinishing()) {
                                WordPress.getBitmapCache().put(mMedia.getUrl(), thumb);
                                mImageView.setImageBitmap(thumb);
                            }
                        }
                    });
                }
            }
        }.start();
    }

    private class LocalImageTask extends AsyncTask<Void, Void, Bitmap> {
        private final String mMediaUri;
        private final int mSize;

        LocalImageTask(@NonNull String mediaUri, int size) {
            mMediaUri = mediaUri;
            mSize = size;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            int orientation = ImageUtils.getImageOrientation(MediaSettingsActivity.this, mMediaUri);
            byte[] bytes = ImageUtils.createThumbnailFromUri(
                    MediaSettingsActivity.this, Uri.parse(mMediaUri), mSize, null, orientation);
            if (bytes != null) {
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isFinishing()) {
                return;
            }
            if (bitmap != null) {
                mImageView.setImageBitmap(bitmap);
            } else {
                delayedFinishWithError();
            }
        }
    }

    private void showFullScreen() {
        saveChanges();
        hideFab();

        // show fullscreen preview after a brief delay so fab & actionBar animations don't stutter
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isMediaFromEditor()) {
                    MediaPreviewActivity.showPreview(MediaSettingsActivity.this, mSite, mEditorImageMetaData.getSrc());
                } else {
                    MediaPreviewActivity.showPreview(MediaSettingsActivity.this, mSite, mMedia, mMediaIdList);
                }

            }
        }, 200);
    }

    private void showFab() {
        if (mFabView.getVisibility() != View.VISIBLE) {
            AniUtils.scaleIn(mFabView, AniUtils.Duration.SHORT);
        }
    }

    private void hideFab() {
        if (mFabView.getVisibility() == View.VISIBLE) {
            AniUtils.scaleOut(mFabView, AniUtils.Duration.SHORT);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        boolean allGranted = WPPermissionUtils.setPermissionListAsked(
                this, requestCode, permissions, grantResults, true);
        if (allGranted && requestCode == WPPermissionUtils.MEDIA_PREVIEW_PERMISSION_REQUEST_CODE) {
            saveMediaToDevice();
        }
    }

    /*
     * receives download completion broadcasts from the DownloadManager
     */
    private final BroadcastReceiver mDownloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long thisId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (thisId == mDownloadId) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(mDownloadId);
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                Cursor cursor = dm.query(query);
                if (cursor != null && cursor.moveToFirst()) {
                    int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
                    if (reason == DownloadManager.STATUS_FAILED) {
                        ToastUtils.showToast(MediaSettingsActivity.this, R.string.error_media_save);
                    }
                }
                mDownloadId = 0;
                invalidateOptionsMenu();
            }
        }
    };

    private void saveChanges() {
        if (isFinishing()) return;

        String thisTitle = EditTextUtils.getText(mTitleView);
        String thisCaption = EditTextUtils.getText(mCaptionView);
        String thisAltText = EditTextUtils.getText(mAltTextView);
        String thisDescription = EditTextUtils.getText(mDescriptionView);

        if (!isMediaFromEditor()) {
            MediaModel media = mMediaStore.getMediaWithLocalId(mMedia.getId());
            if (media == null) {
                AppLog.w(AppLog.T.MEDIA, "MediaSettingsActivity > Cannot save null media");
                ToastUtils.showToast(this, R.string.media_edit_failure);
                return;
            }

            boolean hasChanged = !StringUtils.equals(media.getTitle(), thisTitle)
                    || !StringUtils.equals(media.getCaption(), thisCaption)
                    || !StringUtils.equals(media.getAlt(), thisAltText)
                    || !StringUtils.equals(media.getDescription(), thisDescription);

            if (hasChanged) {
                AppLog.d(AppLog.T.MEDIA, "MediaSettingsActivity > Saving changes");
                media.setTitle(thisTitle);
                media.setCaption(thisCaption);
                media.setAlt(thisAltText);
                media.setDescription(thisDescription);
                mDispatcher.dispatch(MediaActionBuilder.newPushMediaAction(new MediaStore.MediaPayload(mSite, media)));
            }
        } else {
            String newImageWidth = EditTextUtils.getText(mImageWidthView);
            String alignment = mAlignmentKeyArray[mAlignmentSpinnerView.getSelectedItemPosition()];

            boolean hasChanged = !StringUtils.equals(mEditorImageMetaData.getTitle(), thisTitle)
                    || !StringUtils.equals(mEditorImageMetaData.getAlt(), thisAltText)
                    || (isRealImageSizeKnown() && !StringUtils.equals(mEditorImageMetaData.getWidth(), newImageWidth))
                    || !StringUtils.equals(mEditorImageMetaData.getAlign(), alignment);

            if (hasChanged) {
                //do not update image dimensions if we don't know it's real size or no width value was entered
                if (!TextUtils.isEmpty(newImageWidth) && isRealImageSizeKnown()) {
                    int newImageWidthInt = Integer.parseInt(newImageWidth);
                    if (newImageWidthInt > mEditorImageMetaData.getMaxWidth()) {
                        newImageWidthInt = mEditorImageMetaData.getMaxWidth();
                    }

                    String imageHeight = String.valueOf(getRelativeHeightFromWidth(newImageWidthInt));
                    mEditorImageMetaData.setWidth(newImageWidth);
                    mEditorImageMetaData.setHeight(imageHeight);
                }

                mEditorImageMetaData.setTitle(thisTitle);
                mEditorImageMetaData.setAlt(thisAltText);
                mEditorImageMetaData.setAlign(alignment);

                Intent intent = new Intent();
                intent.putExtra(ARG_EDITOR_IMAGE_METADATA, mEditorImageMetaData);

                this.setResult(Activity.RESULT_OK, intent);
            } else {
                this.setResult(Activity.RESULT_CANCELED);
            }
        }
    }

    private int getRelativeHeightFromWidth(int width) {
        int naturalHeight = mEditorImageMetaData.getWidthInt();
        int naturalWidth = mEditorImageMetaData.getWidthInt();

        float ratio = (float) naturalHeight / naturalWidth;
        return (int) (ratio * width);
    }

    /*
     * saves the media to the local device using the Android DownloadManager
     */
    private void saveMediaToDevice() {
        // must request permissions even though they're already defined in the manifest
        String[] permissionList = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        if (!PermissionUtils.checkAndRequestPermissions(this, WPPermissionUtils.MEDIA_PREVIEW_PERMISSION_REQUEST_CODE, permissionList)) {
            return;
        }

        if (!NetworkUtils.checkConnection(this)) {
            return;
        }

        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(mMedia.getUrl()));
        try {
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, mMedia.getFileName());
        } catch (IllegalStateException error) {
            AppLog.e(AppLog.T.MEDIA, error);
            ToastUtils.showToast(MediaSettingsActivity.this, R.string.error_media_save);
            return;
        }
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);

        mDownloadId = dm.enqueue(request);
        invalidateOptionsMenu();
        ToastUtils.showToast(this, R.string.media_downloading);
    }

    private void shareMedia() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, mMedia.getUrl());
        if (!TextUtils.isEmpty(mMedia.getTitle())) {
            intent.putExtra(Intent.EXTRA_SUBJECT, mMedia.getTitle());
        } else if (!TextUtils.isEmpty(mMedia.getDescription())) {
            intent.putExtra(Intent.EXTRA_SUBJECT, mMedia.getDescription());
        }
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share_link)));
        } catch (android.content.ActivityNotFoundException ex) {
            ToastUtils.showToast(this, R.string.reader_toast_err_share_intent);
        }
    }

    private void deleteMediaWithConfirmation() {
        @StringRes int resId = isVideo() ? R.string.confirm_delete_media_video : R.string.confirm_delete_media_image;
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setMessage(resId)
                .setCancelable(true).setPositiveButton(
                        R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteMedia();
                            }
                        }).setNegativeButton(R.string.cancel, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteMedia() {
        if (!NetworkUtils.checkConnection(this)) return;

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(getString(R.string.deleting_media_dlg));
        mProgressDialog.show();

        AppLog.v(AppLog.T.MEDIA, "Deleting " + mMedia.getTitle() + " (id=" + mMedia.getMediaId() + ")");
        MediaStore.MediaPayload payload = new MediaStore.MediaPayload(mSite, mMedia);
        mDispatcher.dispatch(MediaActionBuilder.newDeleteMediaAction(payload));
    }

    private void removeMediaFromPostWithConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setMessage(R.string.confirm_remove_media_image)
                .setCancelable(true).setPositiveButton(
                        R.string.remove, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                removeMediaFromPost();
                            }
                        }).setNegativeButton(R.string.cancel, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void removeMediaFromPost() {
        mEditorImageMetaData.markAsRemoved();

        Intent intent = new Intent();
        intent.putExtra(ARG_EDITOR_IMAGE_METADATA, mEditorImageMetaData);

        this.setResult(Activity.RESULT_OK, intent);

        finishActivity();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaChanged(MediaStore.OnMediaChanged event) {
        if (event.cause == MediaAction.DELETE_MEDIA) {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            if (event.isError()) {
                ToastUtils.showToast(this, R.string.error_generic);
            } else {
                setResult(RESULT_MEDIA_DELETED);
                doFinishAfterTransition();
            }
        } else if (!event.isError()) {
            reloadMedia();
        }
    }

    /*
     * user swiped to another media item in the preview activity, so update this one to show the same media
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaPreviewSwiped(MediaPreviewSwiped event) {
        if (event.mediaId != mMedia.getId()) {
            loadMediaId(event.mediaId);
            // set the flag to prevent the shared element transition when exiting this activity - otherwise the
            // user will see a shared element transition back to the original image selected in the media browser
            mOverrideClosingTransition = true;
        }
    }

    private void copyMediaUrlToClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), mMedia.getUrl()));
            ToastUtils.showToast(this, R.string.media_edit_copy_url_toast);
        } catch (Exception e) {
            AppLog.e(AppLog.T.UTILS, e);
            ToastUtils.showToast(this, R.string.error_copy_to_clipboard);
        }
    }
}
