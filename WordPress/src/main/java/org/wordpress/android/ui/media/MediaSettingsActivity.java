package org.wordpress.android.ui.media;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.IntegerRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.elevation.ElevationOverlayProvider;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.editor.EditorImageMetaData;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.media.MediaPreviewActivity.MediaPreviewSwiped;
import org.wordpress.android.ui.utils.AuthenticationUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ColorUtils;
import org.wordpress.android.util.ContextExtensionsKt;
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
import org.wordpress.android.util.ViewUtilsKt;
import org.wordpress.android.util.WPMediaUtils;
import org.wordpress.android.util.WPPermissionUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageManager.RequestListener;
import org.wordpress.android.util.image.ImageType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;

import static org.wordpress.android.editor.EditorImageMetaData.ARG_EDITOR_IMAGE_METADATA;

public class MediaSettingsActivity extends LocaleAwareActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String ARG_MEDIA_LOCAL_ID = "media_local_id";
    private static final String ARG_ID_LIST = "id_list";
    private static final String ARG_DELETE_MEDIA_DIALOG_VISIBLE = "delete_media_dialog_visible";
    public static final int RESULT_MEDIA_DELETED = RESULT_FIRST_USER;

    private long mDownloadId;
    private String mTitle;
    private boolean mDidRegisterEventBus;

    private SiteModel mSite;
    private MediaModel mMedia;
    private EditorImageMetaData mEditorImageMetaData;
    private ArrayList<String> mMediaIdList;
    private String[] mAlignmentKeyArray;

    private MediaSettingsImageSize mImageSize = MediaSettingsImageSize.FULL;

    private ImageView mImageView;
    private ImageView mImagePlay;
    private EditText mTitleView;
    private EditText mCaptionView;
    private EditText mAltTextView;
    private EditText mDescriptionView;
    private EditText mLinkView;
    private CheckBox mLinkTargetNewWindowView;
    private TextView mImageSizeView;
    private SeekBar mImageSizeSeekBarView;
    private Spinner mAlignmentSpinnerView;
    private FloatingActionButton mFabView;

    private AlertDialog mDeleteMediaConfirmationDialog;

    private ProgressDialog mProgressDialog;

    private enum MediaType {
        IMAGE,
        VIDEO,
        AUDIO,
        DOCUMENT
    }

    private MediaType mMediaType;

    @Inject MediaStore mMediaStore;
    @Inject Dispatcher mDispatcher;
    @Inject ImageManager mImageManager;
    @Inject AuthenticationUtils mAuthenticationUtils;

    /**
     * @param activity    calling activity
     * @param site        site this media is associated with
     * @param media       media model to display
     * @param mediaIdList optional list of media IDs to page through in preview screen
     */
    public static void showForResult(@NonNull Activity activity,
                                     @NonNull SiteModel site,
                                     @NonNull MediaModel media,
                                     @Nullable ArrayList<String> mediaIdList) {
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

        ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                activity,
                R.anim.activity_slide_up_from_bottom,
                R.anim.do_nothing);
        ActivityCompat.startActivityForResult(activity, intent, RequestCodes.MEDIA_SETTINGS, options.toBundle());
    }

    /**
     * @param activity    calling activity
     * @param site        site this media is associated with
     * @param editorMedia editor image metadata
     */
    public static void showForResult(@NonNull Activity activity,
                                     @NonNull SiteModel site,
                                     @NonNull EditorImageMetaData editorMedia) {
        Intent intent = new Intent(activity, MediaSettingsActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(ARG_EDITOR_IMAGE_METADATA, editorMedia);

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

        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        }

        mImageView = findViewById(R.id.image_preview);
        mImagePlay = findViewById(R.id.image_play);
        mTitleView = findViewById(R.id.edit_title);
        mCaptionView = findViewById(R.id.edit_caption);
        mAltTextView = findViewById(R.id.edit_alt_text);
        mDescriptionView = findViewById(R.id.edit_description);
        mLinkView = findViewById(R.id.edit_link);
        mLinkTargetNewWindowView = findViewById(R.id.edit_link_target_new_widnow_checkbox);
        mImageSizeView = findViewById(R.id.image_size_hint);
        mImageSizeSeekBarView = findViewById(R.id.image_size_seekbar);
        mAlignmentSpinnerView = findViewById(org.wordpress.android.editor.R.id.alignment_spinner);
        mFabView = findViewById(R.id.fab_button);

        int mediaId;
        if (savedInstanceState != null) {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mEditorImageMetaData = savedInstanceState.getParcelable(ARG_EDITOR_IMAGE_METADATA);
            mediaId = savedInstanceState.getInt(ARG_MEDIA_LOCAL_ID);
            if (savedInstanceState.containsKey(ARG_ID_LIST)) {
                mMediaIdList = savedInstanceState.getStringArrayList(ARG_ID_LIST);
            }

            if (savedInstanceState.getBoolean(ARG_DELETE_MEDIA_DIALOG_VISIBLE, false)) {
                deleteMediaWithConfirmation();
            }
        } else {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            mEditorImageMetaData = getIntent().getParcelableExtra(ARG_EDITOR_IMAGE_METADATA);
            mediaId = getIntent().getIntExtra(ARG_MEDIA_LOCAL_ID, 0);
            if (getIntent().hasExtra(ARG_ID_LIST)) {
                mMediaIdList = getIntent().getStringArrayListExtra(ARG_ID_LIST);
            }
        }

        if (isMediaFromEditor() ? !loadMediaFromEditor() : !loadMediaWithId(mediaId)) {
            delayedFinishWithError();
            return;
        }

        // only show title when toolbar is collapsed
        final CollapsingToolbarLayout collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        AppBarLayout appBarLayout = findViewById(R.id.app_bar_layout);
        collapsingToolbar.setCollapsedTitleTextColor(
                AppCompatResources.getColorStateList(this,
                        ContextExtensionsKt.getColorResIdFromAttribute(this, R.attr.colorOnPrimarySurface)));

        ElevationOverlayProvider elevationOverlayProvider = new ElevationOverlayProvider(this);

        float appbarElevation = getResources().getDimension(R.dimen.appbar_elevation);
        int elevatedColor = elevationOverlayProvider
                .compositeOverlayIfNeeded(ContextExtensionsKt.getColorFromAttribute(this, R.attr.wpColorAppBar),
                        appbarElevation);

        collapsingToolbar.setContentScrimColor(elevatedColor);

        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            int mScrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (mScrollRange == -1) {
                    mScrollRange = appBarLayout.getTotalScrollRange();
                }
                if (mScrollRange + verticalOffset == 0) {
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
        ImageView imgScrim = findViewById(R.id.image_gradient_scrim);
        imgScrim.getLayoutParams().height = toolbarHeight * 3;

        adjustToolbar();

        // tap to show full screen view (not supported for documents)
        if (!isDocument()) {
            View.OnClickListener listener = v -> showFullScreen();
            mImageView.setOnClickListener(listener);
            mImagePlay.setOnClickListener(listener);
            mFabView.setOnClickListener(listener);
            mFabView.setOnLongClickListener(view -> {
                if (view.isHapticFeedbackEnabled()) {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                }

                Toast.makeText(view.getContext(), R.string.button_preview, Toast.LENGTH_SHORT).show();
                return true;
            });
            ViewUtilsKt.redirectContextClickToLongPressListener(mFabView);
        }
    }

    private boolean isMediaFromEditor() {
        return mEditorImageMetaData != null;
    }

    private void reloadMedia() {
        loadMediaWithId(mMedia.getId());
    }

    private boolean loadMediaWithId(int mediaId) {
        MediaModel media = mMediaStore.getMediaWithLocalId(mediaId);
        return loadMedia(media);
    }

    private boolean loadMediaFromEditor() {
        MediaModel media = getMediaModelFromEditorImageMetaData();
        return loadMedia(media);
    }

    private boolean loadMedia(MediaModel media) {
        if (media == null) {
            return false;
        }

        mMedia = media;

        // try to get a file without parameters so we can more reliably determine media type
        String uriFilePath = !TextUtils.isEmpty(mMedia.getUrl()) ? Uri.parse(mMedia.getUrl()).getPath() : "";

        // determine media type up front, default to DOCUMENT if we can't detect it's an image, video, or audio file
        if (MediaUtils.isValidImage(uriFilePath)) {
            mMediaType = MediaType.IMAGE;
            mTitle = getString(R.string.media_title_image_details);
        } else if (mMedia.isVideo()) {
            mMediaType = MediaType.VIDEO;
            mTitle = getString(R.string.media_title_video_details);
        } else if (MediaUtils.isAudio(uriFilePath)) {
            mMediaType = MediaType.AUDIO;
            mTitle = getString(R.string.media_title_audio_details);
        } else {
            mMediaType = MediaType.DOCUMENT;
            mTitle = getString(R.string.media_title_document_details);
        }

        mImagePlay.setVisibility(isVideo() || isAudio() ? View.VISIBLE : View.GONE);
        findViewById(R.id.edit_alt_text_layout)
                .setVisibility(isVideo() || isAudio() || isDocument() ? View.GONE : View.VISIBLE);

        showMetaData();

        // audio & documents show a placeholder on top of a gradient, otherwise we show a thumbnail
        if (isAudio() || isDocument()) {
            int padding = getResources().getDimensionPixelSize(R.dimen.margin_extra_extra_large);
            @DrawableRes int imageRes = WPMediaUtils.getPlaceholder(mMedia.getUrl());
            ColorUtils.INSTANCE.setImageResourceWithTint(mImageView,
                    imageRes != 0 ? imageRes : R.drawable.ic_pages_white_24dp, R.color.neutral_30);
            mImageView.setPadding(padding, padding * 2, padding, padding);
            mImageView.setImageResource(imageRes);
        } else {
            loadImage();
        }

        return true;
    }

    private MediaModel getMediaModelFromEditorImageMetaData() {
        MediaModel mediaModel = new MediaModel();
        mediaModel.setUrl(mEditorImageMetaData.getSrc());
        mediaModel.setTitle(mEditorImageMetaData.getTitle());
        mediaModel.setCaption(mEditorImageMetaData.getCaption());
        mediaModel.setAlt(mEditorImageMetaData.getAlt());
        if (!TextUtils.isEmpty(mEditorImageMetaData.getSrc())) {
            mediaModel.setFileName(
                    mEditorImageMetaData.getSrc().substring(mEditorImageMetaData.getSrc().lastIndexOf("/") + 1));
        }
        mediaModel.setFileExtension(
                org.wordpress.android.fluxc.utils.MediaUtils.getExtension(mEditorImageMetaData.getSrc()));
        mediaModel.setWidth(mEditorImageMetaData.getWidthInt());
        mediaModel.setHeight(mEditorImageMetaData.getHeightInt());
        return mediaModel;
    }

    @Override
    protected void onResume() {
        super.onResume();

        long delayMs = getResources().getInteger(R.integer.fab_animation_delay);
        new Handler().postDelayed(() -> {
            if (!isFinishing() && shouldShowFab()) {
                showFab();
            }
        }, delayMs);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null && !actionBar.isShowing()) {
            actionBar.show();
        }
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ARG_MEDIA_LOCAL_ID, mMedia.getId());
        outState.putParcelable(ARG_EDITOR_IMAGE_METADATA, mEditorImageMetaData);

        if (mDeleteMediaConfirmationDialog != null) {
            outState.putBoolean(ARG_DELETE_MEDIA_DIALOG_VISIBLE, mDeleteMediaConfirmationDialog.isShowing());
        }

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
        new Handler().postDelayed(this::finish, 1500);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.do_nothing, R.anim.activity_slide_out_to_bottom);
    }

    /*
     * adjust the toolbar so it doesn't overlap the status bar
     */
    private void adjustToolbar() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            int statusHeight = getResources().getDimensionPixelSize(resourceId);
            View toolbar = findViewById(R.id.toolbar);
            toolbar.getLayoutParams().height += statusHeight;
            toolbar.setPadding(0, statusHeight, 0, 0);
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
        super.onBackPressed();
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
        } else if (item.getItemId() == R.id.menu_trash || item.getItemId() == R.id.menu_remove_image) {
            deleteMediaWithConfirmation();
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
            mLinkView.setText(mEditorImageMetaData.getLinkUrl());
            mLinkTargetNewWindowView.setChecked(mEditorImageMetaData.isLinkTargetBlank());

            findViewById(R.id.edit_description_container).setVisibility(View.GONE);
            findViewById(R.id.divider_dimensions).setVisibility(View.GONE);

            setupAlignmentSpinner();
            setupImageSizeSeekBar();
        } else {
            mDescriptionView.setText(mMedia.getDescription());

            findViewById(R.id.media_customisation_options).setVisibility(View.GONE);
            findViewById(R.id.edit_link_container).setVisibility(View.GONE);
        }

        mCaptionView.setText(mMedia.getCaption());

        TextView txtUrl = findViewById(R.id.text_url);
        txtUrl.setText(mMedia.getUrl());

        TextView txtFilename = findViewById(R.id.text_filename);
        txtFilename.setText(mMedia.getFileName());

        TextView txtFileType = findViewById(R.id.text_filetype);
        txtFileType.setText(StringUtils.notNullStr(mMedia.getFileExtension()).toUpperCase(Locale.ROOT));

        showImageDimensions(mMedia.getWidth(), mMedia.getHeight());

        String uploadDate = null;
        if (mMedia.getUploadDate() != null) {
            Date date = DateTimeUtils.dateFromIso8601(mMedia.getUploadDate());
            if (date != null) {
                uploadDate = SimpleDateFormat.getDateInstance().format(date);
            }
        }
        TextView txtUploadDate = findViewById(R.id.text_upload_date);
        TextView txtUploadDateLabel = findViewById(R.id.text_upload_date_label);
        if (uploadDate != null) {
            txtUploadDate.setVisibility(View.VISIBLE);
            txtUploadDateLabel.setVisibility(View.VISIBLE);
            txtUploadDate.setText(uploadDate);
        } else {
            txtUploadDate.setVisibility(View.GONE);
            txtUploadDateLabel.setVisibility(View.GONE);
        }

        TextView txtDuration = findViewById(R.id.text_duration);
        TextView txtDurationLabel = findViewById(R.id.text_duration_label);
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
            txtCopyUrl.setOnClickListener(v -> copyMediaUrlToClipboard());
        }
    }


    /**
     * Initialize the image width SeekBar and accompanying EditText
     */
    private void setupImageSizeSeekBar() {
        // image size is parsed from html, so we can get non standard values (anything that matches ^size-.*)
        // in this case we will default to the full size
        mImageSize = MediaSettingsImageSize.fromCssClass(this, mEditorImageMetaData.getSize());

        mImageSizeSeekBarView.setMax(MediaSettingsImageSize.values().length - 1);
        mImageSizeSeekBarView.setProgress(Arrays.asList(MediaSettingsImageSize.values()).indexOf(mImageSize));

        mImageSizeView.setText(mImageSize.getLabel());

        mImageSizeSeekBarView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mImageSize = MediaSettingsImageSize.values()[progress];
                mImageSizeView.setText(mImageSize.getLabel());
            }
        });
    }

    private void showImageDimensions(int width, int height) {
        TextView txtDimensions = findViewById(R.id.text_image_dimensions);
        TextView txtDimensionsLabel = findViewById(R.id.text_image_dimensions_label);
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

    private void hideImageDimensions() {
        findViewById(R.id.text_image_dimensions).setVisibility(View.GONE);
        findViewById(R.id.text_image_dimensions_label).setVisibility(View.GONE);
        findViewById(R.id.divider_dimensions).setVisibility(View.GONE);

        // Hide file type divider too if duration is hidden (i.e. media length is zero) to remove bottom divider.
        if (mMedia.getLength() == 0) {
            findViewById(R.id.divider_filetype).setVisibility(View.GONE);
        }
    }

    /**
     * Initialize the image alignment spinner
     */
    private void setupAlignmentSpinner() {
        String alignment = mEditorImageMetaData.getAlign();
        mAlignmentKeyArray = getResources().getStringArray(R.array.alignment_key_array);
        int alignmentIndex = Arrays.asList(mAlignmentKeyArray).indexOf(alignment);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.alignment_array, R.layout.media_settings_alignment_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mAlignmentSpinnerView.setAdapter(adapter);
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

        showProgress(true);
        String imageUrl = mediaUri;
        if (SiteUtils.isPhotonCapable(mSite)) {
            imageUrl = PhotonUtils.getPhotonImageUrl(mediaUri, size, 0, mSite.isPrivateWPComAtomic());
        }
        mImageManager.loadWithResultListener(mImageView, ImageType.IMAGE, imageUrl, ScaleType.CENTER, null,
                new RequestListener<Drawable>() {
                    @Override
                    public void onResourceReady(@NotNull Drawable resource, @Nullable Object model) {
                        if (!isFinishing()) {
                            showProgress(false);

                            if (isMediaFromEditor()) {
                                hideImageDimensions();
                            }
                        }
                    }

                    @Override
                    public void onLoadFailed(@Nullable Exception e, @Nullable Object model) {
                        if (!isFinishing()) {
                            if (e != null) {
                                AppLog.e(T.MEDIA, e);
                            }
                            showProgress(false);
                            delayedFinishWithError();
                        }
                    }
                });
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
                final Bitmap thumb = ImageUtils.getVideoFrameFromVideo(
                        mMedia.getUrl(),
                        width,
                        mAuthenticationUtils.getAuthHeaders(mMedia.getUrl())
                );
                if (thumb != null) {
                    runOnUiThread(() -> {
                        if (!isFinishing()) {
                            WordPress.getBitmapCache().put(mMedia.getUrl(), thumb);
                            mImageView.setImageBitmap(thumb);
                        }
                    });
                }
            }
        }.start();
    }

    private void showFullScreen() {
        saveChanges();
        hideFab();

        // show fullscreen preview after a brief delay so fab & actionBar animations don't stutter
        new Handler().postDelayed(() -> {
            if (isMediaFromEditor()) {
                MediaPreviewActivity.showPreview(MediaSettingsActivity.this, mSite, mEditorImageMetaData.getSrc());
            } else {
                MediaPreviewActivity.showPreview(MediaSettingsActivity.this, mSite, mMedia, mMediaIdList);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
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
                    // meaning of `reason` depends on the value of COLUMN_STATUS
                    int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
                    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_FAILED) {
                        ToastUtils.showToast(MediaSettingsActivity.this, R.string.error_media_save);
                        // If an HTTP error occurred, this will hold the HTTP status code as defined in RFC 2616.
                        // Otherwise, it will hold one of the ERROR_* constants
                        AppLog.e(AppLog.T.MEDIA, "MediaSettingsActivity > save > STATUS_FAILED - reason: " + reason);
                    }
                }
                mDownloadId = 0;
                invalidateOptionsMenu();
            }
        }
    };

    private void saveChanges() {
        if (isFinishing()) {
            return;
        }

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
                mDispatcher.dispatch(MediaActionBuilder.newPushMediaAction(new MediaPayload(mSite, media)));
            }
        } else {
            String alignment = mAlignmentKeyArray[mAlignmentSpinnerView.getSelectedItemPosition()];
            String size = getString(mImageSize.getCssClass());
            String linkUrl = EditTextUtils.getText(mLinkView);
            boolean linkTargetBlank = mLinkTargetNewWindowView.isChecked();
            boolean hasSizeChanged = !StringUtils.equals(mEditorImageMetaData.getSize(), size);

            boolean hasChanged = hasSizeChanged
                                 || !StringUtils.equals(mEditorImageMetaData.getTitle(), thisTitle)
                                 || !StringUtils.equals(mEditorImageMetaData.getAlt(), thisAltText)
                                 || !StringUtils.equals(mEditorImageMetaData.getCaption(), thisCaption)
                                 || !StringUtils.equals(mEditorImageMetaData.getAlign(), alignment)
                                 || !StringUtils.equals(mEditorImageMetaData.getLinkUrl(), linkUrl)
                                 || linkTargetBlank != mEditorImageMetaData.isLinkTargetBlank();

            if (hasChanged) {
                mEditorImageMetaData.setTitle(thisTitle);
                mEditorImageMetaData.setSize(size);
                mEditorImageMetaData.setAlt(thisAltText);
                mEditorImageMetaData.setAlign(alignment);
                mEditorImageMetaData.setCaption(thisCaption);
                mEditorImageMetaData.setLinkUrl(linkUrl);
                mEditorImageMetaData.setLinkTargetBlank(linkTargetBlank);

                // we only update width and height attributes on self hosted and Jetpack sites
                // because css image size classes wont have any effect there
                if (!mSite.isWPCom() && hasSizeChanged) {
                    updateImageSizeParameters();
                }

                Intent intent = new Intent();
                intent.putExtra(ARG_EDITOR_IMAGE_METADATA, mEditorImageMetaData);

                this.setResult(Activity.RESULT_OK, intent);
            } else {
                this.setResult(Activity.RESULT_CANCELED);
            }
        }
    }

    private void updateImageSizeParameters() {
        // if caption is empty we can safely remove width and height attributes
        if (mImageSize == MediaSettingsImageSize.FULL && TextUtils.isEmpty(mEditorImageMetaData.getCaption())) {
            mEditorImageMetaData.setWidth(null);
            mEditorImageMetaData.setHeight(null);
            return;
        }

        int imageWidth = mEditorImageMetaData.getWidthInt();
        int imageHeight = mEditorImageMetaData.getHeightInt();
        int newImageSize = getResources().getInteger(mImageSize.getSize());

        float aspectRatio = ((float) imageWidth / (float) imageHeight);

        if (imageWidth > imageHeight) {
            imageHeight = Math.round(newImageSize / aspectRatio);
            imageWidth = newImageSize;
        } else if (imageWidth < imageHeight) {
            imageWidth = Math.round(newImageSize * aspectRatio);
            imageHeight = newImageSize;
        } else {
            // image is square
            imageHeight = newImageSize;
            imageWidth = newImageSize;
        }

        mEditorImageMetaData.setWidth(Integer.toString(imageWidth));
        mEditorImageMetaData.setHeight(Integer.toString(imageHeight));
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
        if (!PermissionUtils.checkAndRequestPermissions(this, WPPermissionUtils.MEDIA_PREVIEW_PERMISSION_REQUEST_CODE,
                permissionList)) {
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
        request.addRequestHeader("User-Agent", WordPress.getUserAgent());

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

    /*
     * Depending on the media source it either removes it from post or deletes it from MediaBrowser
     */
    private void deleteMediaWithConfirmation() {
        if (mDeleteMediaConfirmationDialog != null) {
            mDeleteMediaConfirmationDialog.show();
            return;
        }

        @StringRes int resId;

        if (isMediaFromEditor()) {
            resId = R.string.confirm_remove_media_image;
        } else if (isVideo()) {
            resId = R.string.confirm_delete_media_video;
        } else {
            resId = R.string.confirm_delete_media_image;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setMessage(resId)
                .setCancelable(true).setPositiveButton(
                        isMediaFromEditor() ? R.string.remove : R.string.delete, (dialog, which) -> {
                            if (isMediaFromEditor()) {
                                removeMediaFromPost();
                            } else {
                                deleteMedia();
                            }
                        }).setNegativeButton(R.string.cancel, null);

        mDeleteMediaConfirmationDialog = builder.create();
        mDeleteMediaConfirmationDialog.show();
    }

    private void deleteMedia() {
        if (!NetworkUtils.checkConnection(this)) {
            return;
        }

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(getString(R.string.deleting_media_dlg));
        mProgressDialog.show();

        AppLog.v(AppLog.T.MEDIA, "Deleting " + mMedia.getTitle() + " (id=" + mMedia.getMediaId() + ")");
        MediaPayload payload = new MediaPayload(mSite, mMedia);
        mDispatcher.dispatch(MediaActionBuilder.newDeleteMediaAction(payload));
    }


    private void removeMediaFromPost() {
        mEditorImageMetaData.markAsRemoved();

        Intent intent = new Intent();
        intent.putExtra(ARG_EDITOR_IMAGE_METADATA, mEditorImageMetaData);

        this.setResult(Activity.RESULT_OK, intent);

        finish();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaChanged(OnMediaChanged event) {
        if (event.cause == MediaAction.DELETE_MEDIA) {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            if (event.isError()) {
                ToastUtils.showToast(this, R.string.error_generic);
            } else {
                setResult(RESULT_MEDIA_DELETED);
                finish();
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
            loadMediaWithId(event.mediaId);
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

    public enum MediaSettingsImageSize {
        THUMBNAIL(R.string.image_size_thumbnail_label, R.string.image_size_thumbnail_css_class,
                R.integer.image_size_thumbnail_px),
        MEDIUM(R.string.image_size_medium_label, R.string.image_size_medium_css_class, R.integer.image_size_medium_px),
        LARGE(R.string.image_size_large_label, R.string.image_size_large_css_class, R.integer.image_size_large_px),
        FULL(R.string.image_size_full_label, R.string.image_size_full_css_class, R.integer.image_size_large_px);

        private final int mLabel;
        private final int mCssClass;
        private final int mSize;

        MediaSettingsImageSize(@StringRes int label, @StringRes int cssClass, @IntegerRes int size) {
            mLabel = label;
            mCssClass = cssClass;
            mSize = size;
        }

        public int getSize() {
            return mSize;
        }

        public int getLabel() {
            return mLabel;
        }

        public int getCssClass() {
            return mCssClass;
        }

        public static MediaSettingsImageSize fromCssClass(Context context, String cssClass) {
            for (MediaSettingsImageSize mediaSettingsImageSize : values()) {
                if (context.getString(mediaSettingsImageSize.mCssClass).equals(cssClass)) {
                    return mediaSettingsImageSize;
                }
            }
            return FULL;
        }
    }
}
