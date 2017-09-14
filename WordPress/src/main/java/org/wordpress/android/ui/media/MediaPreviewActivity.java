package org.wordpress.android.ui.media;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

import uk.co.senab.photoview.PhotoViewAttacher;

public class MediaPreviewActivity extends AppCompatActivity {

    private static final String ARG_MEDIA_CONTENT_URI = "content_uri";
    private static final String ARG_IS_VIDEO = "is_video";

    private String mContentUri;
    private boolean mIsVideo;

    private SiteModel mSite;

    private Toolbar mToolbar;
    private ImageView mImageView;
    private VideoView mVideoView;

    private static final long FADE_DELAY_MS = 3000;
    private final Handler mFadeHandler = new Handler();

    @Inject MediaStore mMediaStore;
    @Inject FluxCImageLoader mImageLoader;

    /**
     * @param context     self explanatory
     * @param site        optional site this media is associated with
     * @param contentUri  URI of media - can be local or remote
     * @param isVideo     whether the passed media is a video - assumed to be an image otherwise
     */
    public static void showPreview(Context context,
                                   SiteModel site,
                                   String contentUri,
                                   boolean isVideo) {
        Intent intent = new Intent(context, MediaPreviewActivity.class);
        intent.putExtra(ARG_MEDIA_CONTENT_URI, contentUri);
        intent.putExtra(ARG_IS_VIDEO, isVideo);
        if (site != null) {
            intent.putExtra(WordPress.SITE, site);
        }

        ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                context,
                R.anim.fade_in,
                R.anim.fade_out);
        ActivityCompat.startActivity(context, intent, options.toBundle());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.media_preview_activity);
        View videoFrame = findViewById(R.id.frame_video);
        mImageView = (ImageView) findViewById(R.id.image_preview);
        mVideoView = (VideoView) findViewById(R.id.video_preview);

        if (savedInstanceState != null) {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mContentUri = savedInstanceState.getString(ARG_MEDIA_CONTENT_URI);
            mIsVideo = savedInstanceState.getBoolean(ARG_IS_VIDEO);
        } else {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            mContentUri = getIntent().getStringExtra(ARG_MEDIA_CONTENT_URI);
            mIsVideo = getIntent().getBooleanExtra(ARG_IS_VIDEO, false);
        }

        if (TextUtils.isEmpty(mContentUri)) {
            delayedFinish(true);
            return;
        }

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        int toolbarColor = ContextCompat.getColor(this, R.color.transparent);
        mToolbar.setBackgroundDrawable(new ColorDrawable(toolbarColor));
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mImageView.setVisibility(mIsVideo ?  View.GONE : View.VISIBLE);
        videoFrame.setVisibility(mIsVideo ? View.VISIBLE : View.GONE);

        if (mIsVideo) {
            playVideo(mContentUri);
        } else {
            loadImage(mContentUri);
        }

        mFadeHandler.postDelayed(fadeOutRunnable, FADE_DELAY_MS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_MEDIA_CONTENT_URI, mContentUri);
        outState.putBoolean(ARG_IS_VIDEO, mIsVideo);
        if (mSite != null) {
            outState.putSerializable(WordPress.SITE, mSite);
        }
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
            int orientation = ImageUtils.getImageOrientation(MediaPreviewActivity.this, mMediaUri);
            byte[] bytes = ImageUtils.createThumbnailFromUri(
                    MediaPreviewActivity.this, Uri.parse(mMediaUri), mSize, null, orientation);
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
        PhotoViewAttacher attacher = new PhotoViewAttacher(mImageView);
        attacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float x, float y) {
                showToolbar();
            }
        });
        mImageView.setImageBitmap(bmp);
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

    private final Runnable fadeOutRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isFinishing() && mToolbar.getVisibility() == View.VISIBLE) {
                AniUtils.fadeOut(mToolbar, AniUtils.Duration.MEDIUM);
            }
        }
    };

    private void showToolbar() {
        if (!isFinishing()) {
            mFadeHandler.removeCallbacks(fadeOutRunnable);
            mFadeHandler.postDelayed(fadeOutRunnable, FADE_DELAY_MS);
            if (mToolbar.getVisibility() != View.VISIBLE) {
                AniUtils.fadeIn(mToolbar, AniUtils.Duration.MEDIUM);
            }
        }
    }

}
