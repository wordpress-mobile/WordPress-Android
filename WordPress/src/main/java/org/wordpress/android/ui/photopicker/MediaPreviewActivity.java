package org.wordpress.android.ui.photopicker;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

import uk.co.senab.photoview.PhotoViewAttacher;

public class MediaPreviewActivity extends AppCompatActivity {

    private static final String ARG_MEDIA_URI = "media_uri";
    private static final String ARG_IS_VIDEO = "is_video";

    private String mMediaUri;
    private boolean mIsVideo;

    private ImageView mImageView;
    private VideoView mVideoView;

    @Inject FluxCImageLoader mImageLoader;

    /**
     * Shows full-screen preview of passed media
     *
     * @param context     self-explanatory
     * @param sourceView  optional imageView on calling activity which shows thumbnail of same media
     * @param mediaUri    Uri of media - can be local content:// or remote http(s)://
     * @param isVideo     whether the passed media is a video - assumed to be image otherwise
     *
     * TODO: handle audio and other file types
     */
    public static void showPreview(Context context,
                                   View sourceView,
                                   String mediaUri,
                                   boolean isVideo) {
        Intent intent = new Intent(context, MediaPreviewActivity.class);
        intent.putExtra(ARG_MEDIA_URI, mediaUri);
        intent.putExtra(ARG_IS_VIDEO, isVideo);

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
            mMediaUri = savedInstanceState.getString(ARG_MEDIA_URI);
            mIsVideo = savedInstanceState.getBoolean(ARG_IS_VIDEO);
        } else {
            mMediaUri = getIntent().getStringExtra(ARG_MEDIA_URI);
            mIsVideo = getIntent().getBooleanExtra(ARG_IS_VIDEO, false);
        }

        if (TextUtils.isEmpty(mMediaUri)) {
            delayedFinish(true);
            return;
        }

        mImageView.setVisibility(mIsVideo ?  View.GONE : View.VISIBLE);
        mVideoView.setVisibility(mIsVideo ? View.VISIBLE : View.GONE);

        if (mIsVideo) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            playVideo();
        } else {
            loadImage();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_MEDIA_URI, mMediaUri);
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
    private void loadImage() {
        int width = DisplayUtils.getDisplayPixelWidth(this);
        int height = DisplayUtils.getDisplayPixelHeight(this);

        if (mMediaUri.startsWith("http")) {
            showProgress(true);
            String imageUrl = PhotonUtils.getPhotonImageUrl(mMediaUri, width, height);
            mImageLoader.get(imageUrl, new ImageLoader.ImageListener() {
                @Override
                public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                    if (response.getBitmap() != null) {
                        showProgress(false);
                        mImageView.setImageBitmap(response.getBitmap());
                    }
                }
                @Override
                public void onErrorResponse(VolleyError error) {
                    AppLog.e(AppLog.T.MEDIA, error);
                    showProgress(false);
                    delayedFinish(true);
                }
            }, width, height);
        } else {
            byte[] bytes = ImageUtils.createThumbnailFromUri(this, Uri.parse(mMediaUri), width, null, 0);
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
    private void playVideo() {
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

        mVideoView.setVideoURI(Uri.parse(mMediaUri));
        mVideoView.requestFocus();
    }
}
