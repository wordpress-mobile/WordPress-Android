package org.wordpress.android.ui.media;

import android.Manifest;
import android.annotation.TargetApi;
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
import android.graphics.drawable.ColorDrawable;
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
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.ui.RequestCodes;
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
import java.util.Date;

import javax.inject.Inject;

public class MediaSettingsActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String ARG_MEDIA_LOCAL_ID = "media_local_id";
    public static final int RESULT_MEDIA_DELETED = RESULT_FIRST_USER;

    private long mDownloadId;

    private SiteModel mSite;
    private MediaModel mMedia;

    private ImageView mImageView;
    private EditText mTitleView;
    private EditText mCaptionView;
    private EditText mAltTextView;
    private EditText mDescriptionView;
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

    public static void showForResult(@NonNull Activity activity,
                                     @NonNull SiteModel site,
                                     @NonNull MediaModel media,
                                     View sourceView) {
        // go directly to preview for local images, videos and audio (do nothing for local documents)
        if (MediaUtils.isLocalFile(media.getUploadState())) {
            if (MediaUtils.isValidImage(media.getUrl())
                    || MediaUtils.isAudio(media.getUrl())
                    || media.isVideo()) {
                MediaPreviewActivity.showPreview(activity, site, media.getFilePath(), media.isVideo());
            }
            return;
        }

        Intent intent = new Intent(activity, MediaSettingsActivity.class);
        intent.putExtra(ARG_MEDIA_LOCAL_ID, media.getId());
        intent.putExtra(WordPress.SITE, site);

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.media_settings_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
            makeStatusAndToolbarTransparent();
            //noinspection RestrictedApi
            actionBar.setShowHideAnimationEnabled(true);
        }

        mImageView = (ImageView) findViewById(R.id.image_preview);
        mTitleView = (EditText) findViewById(R.id.edit_title);
        mCaptionView = (EditText) findViewById(R.id.edit_caption);
        mAltTextView = (EditText) findViewById(R.id.edit_alt_text);
        mDescriptionView = (EditText) findViewById(R.id.edit_description);
        mFabView = (FloatingActionButton) findViewById(R.id.fab_button);

        int mediaId;
        if (savedInstanceState != null) {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mediaId = savedInstanceState.getInt(ARG_MEDIA_LOCAL_ID);
        } else {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            mediaId = getIntent().getIntExtra(ARG_MEDIA_LOCAL_ID, 0);
        }

        mMedia = mMediaStore.getMediaWithLocalId(mediaId);
        if (mMedia == null) {
            delayedFinishWithError();
            return;
        }

        // determine media type up front, default to DOCUMENT if we can't detect it's an image, video, or audio file
        if (MediaUtils.isValidImage(mMedia.getUrl())) {
            mMediaType = MediaType.IMAGE;
            setTitle(R.string.media_title_image_details);
        } else if (mMedia.isVideo()) {
            mMediaType = MediaType.VIDEO;
            setTitle(R.string.media_title_video_details);
        } else if (MediaUtils.isAudio(mMedia.getUrl())) {
            mMediaType = MediaType.AUDIO;
            setTitle(R.string.media_title_audio_details);
        } else {
            mMediaType = MediaType.DOCUMENT;
            setTitle(R.string.media_title_document_details);
        }

        // make image 40% of screen height
        int displayHeight = DisplayUtils.getDisplayPixelHeight(this);
        int imageHeight = (int) (displayHeight * 0.4);
        mImageView.getLayoutParams().height = imageHeight;

        // position the fab so it overlaps the image
        if (shouldShowFab()) {
            int fabHeight = DisplayUtils.dpToPx(this, 56);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mFabView.getLayoutParams();
            int topMargin = imageHeight - (fabHeight / 2);
            int rightMargin = getResources().getDimensionPixelSize(R.dimen.fab_margin);
            params.setMargins(0, topMargin, rightMargin, 0);
        }

        // position progress in middle of image
        View progressView = findViewById(R.id.progress);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) progressView.getLayoutParams();
        int topMargin = (imageHeight / 2) - (progressView.getHeight() / 2);
        params.setMargins(0, topMargin, 0, 0);

        // set the height of the gradient scrim that appears atop the image
        int toolbarHeight = DisplayUtils.getActionBarHeight(this);
        ImageView imgScrim = (ImageView) findViewById(R.id.image_gradient_scrim);
        imgScrim.getLayoutParams().height = toolbarHeight * 3;

        ImageView imagePlay = (ImageView) findViewById(R.id.image_play);
        imagePlay.setVisibility(isVideo() || isAudio() ? View.VISIBLE : View.GONE);
        findViewById(R.id.edit_alt_text_layout).setVisibility(isVideo() || isAudio() || isDocument() ? View.GONE : View.VISIBLE);

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
            imagePlay.setOnClickListener(listener);
        }

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
        if (mSite != null) {
            outState.putSerializable(WordPress.SITE, mSite);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        registerReceiver(mDownloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        unregisterReceiver(mDownloadReceiver);
        mDispatcher.unregister(this);
        super.onStop();
    }

    private void delayedFinishWithError() {
        ToastUtils.showToast(this, R.string.error_media_not_found);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                supportFinishAfterTransition();
            }
        }, 1500);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void makeStatusAndToolbarTransparent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (getSupportActionBar() != null) {
                int toolbarColor = ContextCompat.getColor(this, R.color.transparent);
                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(toolbarColor));
            }
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    @Override
    public void finish() {
        super.finish();
        // on Lollipop and above we use a shared element transition set in the intent, otherwise use a
        // slide out transition
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            overridePendingTransition(R.anim.do_nothing, R.anim.activity_slide_out_to_bottom);
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
        boolean showSaveMenu = mSite != null && !mSite.isPrivate();
        boolean showShareMenu = mSite != null && !mSite.isPrivate();
        boolean showTrashMenu = mSite != null;

        MenuItem mnuSave = menu.findItem(R.id.menu_save);
        mnuSave.setVisible(showSaveMenu);
        mnuSave.setEnabled(mDownloadId == 0);

        MenuItem mnuShare = menu.findItem(R.id.menu_share);
        mnuShare.setVisible(showShareMenu);

        MenuItem mnuTrash = menu.findItem(R.id.menu_trash);
        mnuTrash.setVisible(showTrashMenu);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            saveChanges();
            supportFinishAfterTransition();
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
        mCaptionView.setText(mMedia.getCaption());
        mAltTextView.setText(mMedia.getAlt());
        mDescriptionView.setText(mMedia.getDescription());

        TextView txtUrl = (TextView) findViewById(R.id.text_url);
        txtUrl.setText(mMedia.getUrl());

        TextView txtFilename = (TextView) findViewById(R.id.text_filename);
        txtFilename.setText(mMedia.getFileName());

        TextView txtFileType = (TextView) findViewById(R.id.text_filetype);
        txtFileType.setText(mMedia.getFileExtension().toUpperCase());

        float mediaWidth = mMedia.getWidth();
        float mediaHeight = mMedia.getHeight();
        TextView txtDimensions = (TextView) findViewById(R.id.text_image_dimensions);
        TextView txtDimensionsLabel = (TextView) findViewById(R.id.text_image_dimensions_label);
        if (mediaWidth > 0 && mediaHeight > 0) {
            txtDimensions.setVisibility(View.VISIBLE);
            txtDimensionsLabel.setVisibility(View.VISIBLE);
            txtDimensionsLabel.setText(isVideo() ? R.string.media_edit_video_dimensions_caption : R.string
                    .media_edit_image_dimensions_caption);
            String dimens = (int) mediaWidth + " x " + (int) mediaHeight;
            txtDimensions.setText(dimens);
        } else {
            txtDimensions.setVisibility(View.GONE);
            txtDimensionsLabel.setVisibility(View.GONE);
            findViewById(R.id.divider_dimensions).setVisibility(View.GONE);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        hideFab();

        // show fullscreen preview after a brief delay so fab & actionBar animations don't stutter
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                MediaPreviewActivity.showPreview(MediaSettingsActivity.this, mSite, mMedia);
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

        MediaModel media = mMediaStore.getMediaWithLocalId(mMedia.getId());
        if (media == null) {
            AppLog.w(AppLog.T.MEDIA, "MediaSettingsActivity > Cannot save null media");
            ToastUtils.showToast(this, R.string.media_edit_failure);
            return;
        }

        String thisTitle = EditTextUtils.getText(mTitleView);
        String thisCaption = EditTextUtils.getText(mCaptionView);
        String thisAltText = EditTextUtils.getText(mAltTextView);
        String thisDescription = EditTextUtils.getText(mDescriptionView);

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
                supportFinishAfterTransition();
            }
        } else if (!event.isError()) {
            MediaModel media = mMediaStore.getMediaWithLocalId(mMedia.getId());
            if (media != null) {
                mMedia = media;
                showMetaData();
            }
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
