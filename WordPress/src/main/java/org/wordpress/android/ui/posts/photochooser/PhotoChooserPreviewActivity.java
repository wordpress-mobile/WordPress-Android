package org.wordpress.android.ui.posts.photochooser;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import org.wordpress.android.R;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.ToastUtils;

import uk.co.senab.photoview.PhotoViewAttacher;

public class PhotoChooserPreviewActivity extends AppCompatActivity {

    public static final String ARG_MEDIA_URI = "media_uri";
    public static final String ARG_IS_VIDEO = "is_video";

    private Uri mMediaUri;
    private boolean mIsVideo;
    private ImageView mImageView;
    private VideoView mVideoView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.photo_chooser_preview_activity);
        mImageView = (ImageView) findViewById(R.id.image_preview);
        mVideoView = (VideoView) findViewById(R.id.video_preview);

        String uriString;
        if (savedInstanceState != null) {
            uriString = savedInstanceState.getString(ARG_MEDIA_URI);
            mIsVideo = savedInstanceState.getBoolean(ARG_IS_VIDEO);
        } else {
            uriString = getIntent().getStringExtra(ARG_MEDIA_URI);
            mIsVideo = getIntent().getBooleanExtra(ARG_IS_VIDEO, false);
        }

        mMediaUri = Uri.parse(uriString);

        mImageView.setVisibility(mIsVideo ? View.GONE : View.VISIBLE);
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
        outState.putString(ARG_MEDIA_URI, mMediaUri.toString());
        outState.putBoolean(ARG_IS_VIDEO, mIsVideo);
    }

    private void delayedFinish() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 1500);
    }

    private void loadImage() {
        // load a scaled version of the image to prevent OOM exception
        int maxWidth = DisplayUtils.getDisplayPixelWidth(this);
        byte[] bytes = ImageUtils.createThumbnailFromUri(this, mMediaUri, maxWidth, null, 0);
        if (bytes == null) {
            ToastUtils.showToast(this, R.string.error_media_load);
            delayedFinish();
            return;
        }

        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bmp == null) {
            ToastUtils.showToast(this, R.string.error_media_load);
            delayedFinish();
            return;
        }

        mImageView.setImageBitmap(bmp);

        // attach the photo zoomer
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

    private void playVideo() {
        final MediaController controls = new MediaController(this);
        mVideoView.setMediaController(controls);

        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                delayedFinish();
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

        mVideoView.setVideoURI(mMediaUri);
        mVideoView.requestFocus();
    }
}
