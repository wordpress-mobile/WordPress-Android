package org.wordpress.android.ui.media;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPPermissionUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.inject.Inject;

public class MediaSettingsActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String ARG_MEDIA_CONTENT_URI = "content_uri";
    private static final String ARG_MEDIA_LOCAL_ID = "media_local_id";
    private static final String ARG_IS_VIDEO = "is_video";

    private String mContentUri;
    private int mMediaId;
    private long mDownloadId;
    private boolean mIsVideo;

    private SiteModel mSite;

    private ImageView mImageView;
    private VideoView mVideoView;
    private EditText mTitleView;
    private EditText mCaptionView;
    private EditText mDescriptionView;
    private Toolbar mToolbar;

    @Inject
    MediaStore mMediaStore;
    @Inject
    FluxCImageLoader mImageLoader;
    @Inject
    Dispatcher mDispatcher;

    /**
     * @param context    self explanatory
     * @param contentUri local content:// uri of media
     * @param isVideo    whether the passed media is a video - assumed to be an image otherwise
     */
    public static void show(Context context,
                            String contentUri,
                            boolean isVideo) {
        Intent intent = new Intent(context, MediaSettingsActivity.class);
        intent.putExtra(ARG_MEDIA_CONTENT_URI, contentUri);
        intent.putExtra(ARG_IS_VIDEO, isVideo);
        showSettingsIntent(context, intent);
    }

    /**
     * @param context self explanatory
     * @param site    site which contains this media item
     * @param mediaId local ID in site's media library
     */
    public static void show(Context context,
                            SiteModel site,
                            int mediaId) {
        Intent intent = new Intent(context, MediaSettingsActivity.class);
        intent.putExtra(ARG_MEDIA_LOCAL_ID, mediaId);
        intent.putExtra(WordPress.SITE, site);
        showSettingsIntent(context, intent);
    }

    private static void showSettingsIntent(Context context, Intent intent) {
        ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                context,
                R.anim.activity_slide_up_from_bottom,
                R.anim.do_nothing);
        ActivityCompat.startActivity(context, intent, options.toBundle());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.media_settings_activity);

        mImageView = (ImageView) findViewById(R.id.image_preview);
        mVideoView = (VideoView) findViewById(R.id.video_preview);

        mTitleView = (EditText) findViewById(R.id.edit_title);
        mCaptionView = (EditText) findViewById(R.id.edit_caption);
        mDescriptionView = (EditText) findViewById(R.id.edit_description);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        if (savedInstanceState != null) {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mContentUri = savedInstanceState.getString(ARG_MEDIA_CONTENT_URI);
            mMediaId = savedInstanceState.getInt(ARG_MEDIA_LOCAL_ID);
            mIsVideo = savedInstanceState.getBoolean(ARG_IS_VIDEO);
        } else {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            mContentUri = getIntent().getStringExtra(ARG_MEDIA_CONTENT_URI);
            mMediaId = getIntent().getIntExtra(ARG_MEDIA_LOCAL_ID, 0);
            mIsVideo = getIntent().getBooleanExtra(ARG_IS_VIDEO, false);
        }

        String mediaUri = null;
        if (!TextUtils.isEmpty(mContentUri)) {
            mediaUri = mContentUri;
        } else if (mMediaId != 0) {
            MediaModel media = mMediaStore.getMediaWithLocalId(mMediaId);
            if (media == null) {
                delayedFinish(true);
                return;
            }
            mIsVideo = media.isVideo();
            mediaUri = media.getUrl();
        }

        if (TextUtils.isEmpty(mediaUri)) {
            delayedFinish(true);
            return;
        }

        //setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.media);
            actionBar.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.transparent)));
        }

        mImageView.setVisibility(mIsVideo ? View.GONE : View.VISIBLE);
        findViewById(R.id.frame_video).setVisibility(mIsVideo ? View.VISIBLE : View.GONE);
        loadMediaMetaData();

        if (mIsVideo) {
            playVideo(mediaUri);
        } else {
            loadImage(mediaUri);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_MEDIA_CONTENT_URI, mContentUri);
        outState.putInt(ARG_MEDIA_LOCAL_ID, mMediaId);
        outState.putBoolean(ARG_IS_VIDEO, mIsVideo);
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

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.do_nothing, R.anim.activity_slide_out_to_bottom);
    }

    private void delayedFinish(boolean showError) {
        if (showError) {
            ToastUtils.showToast(this, R.string.error_media_not_found);
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 1500);
    }

    private void showProgress(boolean show) {
        findViewById(R.id.progress).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onBackPressed() {
        saveChanges();
        invalidateOptionsMenu();
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.media_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean showSaveMenu = mMediaId != 0 && mSite != null && !mSite.isPrivate();
        boolean showShareMenu = mMediaId != 0 && mSite != null && !mSite.isPrivate();

        MenuItem mnuSave = menu.findItem(R.id.menu_save);
        mnuSave.setVisible(showSaveMenu);
        mnuSave.setEnabled(mDownloadId == 0);

        MenuItem mnuShare = menu.findItem(R.id.menu_share);
        mnuShare.setVisible(showShareMenu);

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
        }

        return super.onOptionsItemSelected(item);
    }

    void loadMediaMetaData() {
        MediaModel media = mMediaStore.getMediaWithLocalId(mMediaId);
        if (media != null) {
            mTitleView.setText(media.getTitle());
            mCaptionView.setText(media.getCaption());
            mDescriptionView.setText(media.getDescription());

            mTitleView.requestFocus();
            mTitleView.setSelection(mTitleView.getText().length());
        } else {
            ToastUtils.showToast(this, R.string.error_media_load);
        }
    }

    /*
     * loads and displays a remote or local image
     */
    private void loadImage(@NonNull String mediaUri) {
        int width = DisplayUtils.getDisplayPixelWidth(this);
        int height = DisplayUtils.getDisplayPixelHeight(this);
        int size = Math.max(width, height);

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
                        setBitmap(response.getBitmap());
                    }
                }

                @Override
                public void onErrorResponse(VolleyError error) {
                    AppLog.e(AppLog.T.MEDIA, error);
                    if (!isFinishing()) {
                        showProgress(false);
                        delayedFinish(true);
                    }
                }
            }, size, 0);
        } else {
            new LocalImageTask(mediaUri, size).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
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
                setBitmap(bitmap);
            } else {
                delayedFinish(true);
            }
        }
    }

    private void setBitmap(@NonNull Bitmap bmp) {
        // assign the photo attacher to enable pinch/zoom - must come before setImageBitmap
        // for it to be correctly resized upon loading
        //PhotoViewAttacher attacher = new PhotoViewAttacher(mImageView);
        mImageView.setImageBitmap(bmp);
        invalidateOptionsMenu();
    }

    /*
     * loads and plays a remote or local video
     */
    private void playVideo(@NonNull String mediaUri) {
        final MediaController controls = new MediaController(this);
        mVideoView.setMediaController(controls);

        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                delayedFinish(false);
                return false;
            }
        });

        showProgress(true);
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                showProgress(false);
                controls.show();
                mp.start();
            }
        });

        mVideoView.setVideoURI(Uri.parse(mediaUri));
        mVideoView.requestFocus();
    }


    /*
     * returns the passed string formatted as a short date if it's valid ISO 8601 date,
     * otherwise returns the passed string
     */
    private String getDisplayDate(String dateString) {
        if (dateString != null) {
            Date date = DateTimeUtils.dateFromIso8601(dateString);
            if (date != null) {
                return SimpleDateFormat.getDateInstance().format(date);
            }
        }
        return dateString;
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
                if (cursor.moveToFirst()) {
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

    public void saveChanges() {
        if (isFinishing()) return;

        MediaModel media = mMediaStore.getMediaWithLocalId(mMediaId);
        if (media == null) {
            AppLog.w(AppLog.T.MEDIA, "MediaSettingsActivity > Cannot save null media");
            ToastUtils.showToast(this, R.string.media_edit_failure);
            return;
        }

        String thisTitle = EditTextUtils.getText(mTitleView);
        String thisCaption = EditTextUtils.getText(mCaptionView);
        String thisDescription = EditTextUtils.getText(mDescriptionView);

        boolean hasChanged = !StringUtils.equals(media.getTitle(), thisTitle)
                || !StringUtils.equals(media.getCaption(), thisCaption)
                || !StringUtils.equals(media.getDescription(), thisDescription);
        if (hasChanged) {
            AppLog.d(AppLog.T.MEDIA, "MediaSettingsActivity > Saving changes");
            media.setTitle(thisTitle);
            media.setCaption(thisCaption);
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

        MediaModel media = mMediaStore.getMediaWithLocalId(mMediaId);
        if (media == null) {
            ToastUtils.showToast(this, R.string.error_media_not_found);
            return;
        }

        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(media.getUrl()));
        try {
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, media.getFileName());
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
        MediaModel media = mMediaStore.getMediaWithLocalId(mMediaId);
        if (media == null) {
            ToastUtils.showToast(this, R.string.error_media_not_found);
            return;
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, media.getUrl());
        if (!TextUtils.isEmpty(media.getTitle())) {
            intent.putExtra(Intent.EXTRA_SUBJECT, media.getTitle());
        } else if (!TextUtils.isEmpty(media.getDescription())) {
            intent.putExtra(Intent.EXTRA_SUBJECT, media.getDescription());
        }
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share_link)));
        } catch (android.content.ActivityNotFoundException ex) {
            ToastUtils.showToast(this, R.string.reader_toast_err_share_intent);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaChanged(MediaStore.OnMediaChanged event) {
        if (!event.isError()) {
            loadMediaMetaData();
        }
    }
}
