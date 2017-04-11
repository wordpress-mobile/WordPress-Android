package org.wordpress.android.ui.media;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

import uk.co.senab.photoview.PhotoViewAttacher;

public class MediaPreviewActivity extends AppCompatActivity {

    private static final String ARG_MEDIA_CONTENT_URI = "content_uri";
    private static final String ARG_MEDIA_LOCAL_ID = "media_local_id";
    private static final String ARG_IS_VIDEO = "is_video";

    private String mContentUri;
    private int mMediaId;
    private boolean mIsVideo;

    private ImageView mImageView;
    private VideoView mVideoView;

    @Inject MediaStore mMediaStore;
    @Inject FluxCImageLoader mImageLoader;

    /**
     * @param context     self explanatory
     * @param sourceView  optional imageView on calling activity which shows thumbnail of same media
     * @param contentUri  local content:// uri of media
     * @param isVideo     whether the passed media is a video - assumed to be an image otherwise
     *
     */
    public static void showPreview(Context context,
                                   View sourceView,
                                   String contentUri,
                                   boolean isVideo) {
        Intent intent = new Intent(context, MediaPreviewActivity.class);
        intent.putExtra(ARG_MEDIA_CONTENT_URI, contentUri);
        intent.putExtra(ARG_IS_VIDEO, isVideo);
        showPreviewIntent(context, sourceView, intent);
    }

    /**
     * @param context     self explanatory
     * @param sourceView  optional imageView on calling activity which shows thumbnail of same media
     * @param mediaId     local ID in site's media library
     * @param isVideo     whether the passed media is a video - assumed to be an image otherwise
     *
     */
    public static void showPreview(Context context,
                                   View sourceView,
                                   int mediaId,
                                   boolean isVideo) {
        Intent intent = new Intent(context, MediaPreviewActivity.class);
        intent.putExtra(ARG_MEDIA_LOCAL_ID, mediaId);
        intent.putExtra(ARG_IS_VIDEO, isVideo);
        showPreviewIntent(context, sourceView, intent);
    }

    private static void showPreviewIntent(Context context, View sourceView, Intent intent) {
        ActivityOptionsCompat options;
        if (sourceView != null) {
            int startWidth = sourceView.getWidth();
            int startHeight = sourceView.getHeight();
            int startX = startWidth / 2;
            int startY = startHeight / 2;

            options = ActivityOptionsCompat.makeScaleUpAnimation(
                    sourceView,
                    startX,
                    startY,
                    startWidth,
                    startHeight);
        } else {
            options = ActivityOptionsCompat.makeBasic();
        }
        ActivityCompat.startActivity(context, intent, options.toBundle());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.media_preview_activity);
        mImageView = (ImageView) findViewById(R.id.image_preview);
        mVideoView = (VideoView) findViewById(R.id.video_preview);

        if (savedInstanceState != null) {
            mContentUri = savedInstanceState.getString(ARG_MEDIA_CONTENT_URI);
            mMediaId = savedInstanceState.getInt(ARG_MEDIA_LOCAL_ID);
            mIsVideo = savedInstanceState.getBoolean(ARG_IS_VIDEO);
        } else {
            mContentUri = getIntent().getStringExtra(ARG_MEDIA_CONTENT_URI);
            mMediaId = getIntent().getIntExtra(ARG_MEDIA_LOCAL_ID, 0);
            mIsVideo = getIntent().getBooleanExtra(ARG_IS_VIDEO, false);
        }

        mImageView.setVisibility(mIsVideo ?  View.GONE : View.VISIBLE);
        mVideoView.setVisibility(mIsVideo ? View.VISIBLE : View.GONE);

        String mediaUri;
        if (!TextUtils.isEmpty(mContentUri)) {
            mediaUri = mContentUri;
        } else if (mMediaId != 0) {
            MediaModel media = mMediaStore.getMediaWithLocalId(mMediaId);
            if (media == null) {
                delayedFinish(true);
                return;
            }
            mediaUri = media.getUrl();
        } else {
            delayedFinish(true);
            return;
        }

        if (mIsVideo) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
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

        if (mediaUri.startsWith("http")) {
            showProgress(true);
            String imageUrl = PhotonUtils.getPhotonImageUrl(mediaUri, width, height);
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
                        delayedFinish(true);
                    }
                }
            }, width, height);
        } else {
            byte[] bytes = ImageUtils.createThumbnailFromUri(this, Uri.parse(mediaUri), width, null, 0);
            if (bytes == null) {
                delayedFinish(true);
                return;
            }

            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bmp != null) {
                mImageView.setImageBitmap(bmp);
            } else {
                delayedFinish(true);
            }
        }

        // assign the photo attacher to enable pinch/zoom
        PhotoViewAttacher attacher = new PhotoViewAttacher(mImageView);
        attacher.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
            @Override
            public void onPhotoTap(View view, float v, float v2) {
                finish();
            }
        });
        attacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float v, float v2) {
                finish();
            }
        });
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

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                controls.show();
                mp.start();
            }
        });

        mVideoView.setVideoURI(Uri.parse(mediaUri));
        mVideoView.requestFocus();
    }
}