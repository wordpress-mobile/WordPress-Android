package org.wordpress.android.ui.media;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
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
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

import uk.co.senab.photoview.PhotoViewAttacher;

public class MediaPreviewActivity extends AppCompatActivity implements MediaController.MediaPlayerControl {

    private static final String ARG_MEDIA_CONTENT_URI = "content_uri";
    private static final String ARG_IS_VIDEO = "is_video";
    private static final String ARG_IS_AUDIO = "is_audio";
    private static final String ARG_POSITION = "position";
    private static final String ARG_TITLE = "title";

    private String mContentUri;
    private String mTitle;
    private boolean mIsVideo;
    private boolean mIsAudio;

    private SiteModel mSite;

    private Toolbar mToolbar;
    private ImageView mImageView;
    private VideoView mVideoView;
    private ViewGroup mVideoFrame;
    private ViewGroup mAudioFrame;

    private MediaPlayer mAudioPlayer;
    private MediaController mControls;

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

        startIntent(context, intent);
    }

    /**
     * @param context     self explanatory
     * @param site        optional site this media is associated with
     * @param media       media model
     */
    public static void showPreview(Context context,
                                   SiteModel site,
                                   MediaModel media) {
        Intent intent = new Intent(context, MediaPreviewActivity.class);
        intent.putExtra(ARG_MEDIA_CONTENT_URI, media.getUrl());
        intent.putExtra(ARG_TITLE, media.getTitle());
        intent.putExtra(ARG_IS_VIDEO, media.isVideo());

        String mimeType = StringUtils.notNullStr(media.getMimeType()).toLowerCase();
        intent.putExtra(ARG_IS_AUDIO, mimeType.startsWith("audio"));

        if (site != null) {
            intent.putExtra(WordPress.SITE, site);
        }

        startIntent(context, intent);
    }

    private static void startIntent(Context context, Intent intent) {
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
        mImageView = (ImageView) findViewById(R.id.image_preview);
        mVideoView = (VideoView) findViewById(R.id.video_preview);

        mVideoFrame = (ViewGroup) findViewById(R.id.frame_video);
        mAudioFrame = (ViewGroup) findViewById(R.id.frame_audio);

        int position;
        if (savedInstanceState != null) {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mContentUri = savedInstanceState.getString(ARG_MEDIA_CONTENT_URI);
            mTitle = savedInstanceState.getString(ARG_TITLE);
            mIsVideo = savedInstanceState.getBoolean(ARG_IS_VIDEO);
            mIsAudio = savedInstanceState.getBoolean(ARG_IS_AUDIO);
            position = savedInstanceState.getInt(ARG_POSITION, 0);
        } else {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            mContentUri = getIntent().getStringExtra(ARG_MEDIA_CONTENT_URI);
            mTitle = getIntent().getStringExtra(ARG_TITLE);
            mIsVideo = getIntent().getBooleanExtra(ARG_IS_VIDEO, false);
            mIsAudio = getIntent().getBooleanExtra(ARG_IS_AUDIO, false);
            position = 0;
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

        mImageView.setVisibility(mIsVideo || mIsAudio ? View.GONE : View.VISIBLE);
        mVideoFrame.setVisibility(mIsVideo ? View.VISIBLE : View.GONE);
        mAudioFrame.setVisibility(mIsAudio ? View.VISIBLE : View.GONE);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToolbar();
                if (mControls != null) {
                    mControls.show();
                }
            }
        };

        if (mIsVideo) {
            mVideoFrame.setOnClickListener(listener);
            playVideo(mContentUri, position);
        } else if (mIsAudio) {
            mAudioFrame.setOnClickListener(listener);
            if (!TextUtils.isEmpty(mTitle)) {
                TextView txtAudioTitle = (TextView) findViewById(R.id.text_audio_title);
                txtAudioTitle.setText(mTitle);
                txtAudioTitle.setVisibility(View.VISIBLE);
            }
            playAudio(mContentUri, position);
        } else {
            loadImage(mContentUri);
        }

        mFadeHandler.postDelayed(fadeOutRunnable, FADE_DELAY_MS);
    }

    @Override
    protected void onStop() {
        if (mControls != null) {
            mControls.hide();
        }
        if (mAudioPlayer != null && mAudioPlayer.isPlaying()) {
            mAudioPlayer.stop();
        }
        if (mVideoView.isPlaying()) {
            mVideoView.stopPlayback();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mAudioPlayer != null) {
            mAudioPlayer.release();
            mAudioPlayer = null;
        }
        if (mVideoView.isPlaying()) {
            mVideoView.stopPlayback();
            mVideoView.setMediaController(null);
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
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
        outState.putString(ARG_TITLE, mTitle);

        if (mIsVideo) {
            outState.putBoolean(ARG_IS_VIDEO, true);
            outState.putInt(ARG_POSITION, mVideoView.getCurrentPosition());
        } else if (mIsAudio) {
            outState.putBoolean(ARG_IS_AUDIO, true);
            if (mAudioPlayer != null) {
                outState.putInt(ARG_POSITION, mAudioPlayer.getCurrentPosition());
            }
        }

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
     * initialize the media controls (audio/video only)
     */
    private void initControls() {
        mControls = new MediaController(this);
        if (mIsVideo) {
            mControls.setAnchorView(mVideoFrame);
            mControls.setMediaPlayer(mVideoView);
        } else if (mIsAudio) {
            mControls.setAnchorView(mAudioFrame);
            mControls.setMediaPlayer(this);
        }
    }

    private void playVideo(@NonNull String mediaUri, final int position) {
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
                if (!isFinishing()) {
                    showProgress(false);
                    mp.start();
                    if (position > 0) {
                        mp.seekTo(position);
                    }
                    mControls.show();
                }
            }
        });

        initControls();
        mVideoView.setVideoURI(Uri.parse(mediaUri));
        mVideoView.requestFocus();
    }

    private void playAudio(@NonNull String mediaUri, final int position) {
        mAudioPlayer = new MediaPlayer();
        mAudioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mAudioPlayer.setDataSource(this, Uri.parse(mediaUri));
        } catch (Exception e) {
            AppLog.e(AppLog.T.MEDIA, e);
            delayedFinish(true);
        }

        mAudioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (!isFinishing()) {
                    showProgress(false);
                    mp.start();
                    if (position > 0) {
                        mp.seekTo(position);
                    }
                    mControls.show();
                }
            }
        });
        mAudioPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                delayedFinish(false);
                return false;
            }
        });

        initControls();
        showProgress(true);
        mAudioPlayer.prepareAsync();
    }

    private final Runnable fadeOutRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isFinishing() && mToolbar.getVisibility() == View.VISIBLE) {
                AniUtils.startAnimation(mToolbar, R.anim.toolbar_fade_out_and_up, new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) { }
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mToolbar.setVisibility(View.GONE);
                    }
                    @Override
                    public void onAnimationRepeat(Animation animation) { }
                });
            }
        }
    };

    private void showToolbar() {
        if (!isFinishing()) {
            mFadeHandler.removeCallbacks(fadeOutRunnable);
            mFadeHandler.postDelayed(fadeOutRunnable, FADE_DELAY_MS);
            if (mToolbar.getVisibility() != View.VISIBLE) {
                AniUtils.startAnimation(mToolbar, R.anim.toolbar_fade_in_and_down, new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        mToolbar.setVisibility(View.VISIBLE);
                    }
                    @Override
                    public void onAnimationEnd(Animation animation) { }
                    @Override
                    public void onAnimationRepeat(Animation animation) { }
                });
            }
        }
    }

    /*
     * MediaController.MediaPlayerControl - for audio playback only
     */
    @Override
    public void start() {
        if (mAudioPlayer != null) {
            mAudioPlayer.start();
        }
    }

    @Override
    public void pause() {
        if (mAudioPlayer != null) {
            mAudioPlayer.pause();
        }
    }

    @Override
    public int getDuration() {
        if (mAudioPlayer != null) {
            return mAudioPlayer.getDuration();
        }
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (mAudioPlayer != null) {
            return mAudioPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public void seekTo(int pos) {
        if (mAudioPlayer != null) {
            mAudioPlayer.seekTo(pos);
        }
    }

    @Override
    public boolean isPlaying() {
        return mAudioPlayer != null && mAudioPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

}
