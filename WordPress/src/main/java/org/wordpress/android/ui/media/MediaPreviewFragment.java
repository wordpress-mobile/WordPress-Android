package org.wordpress.android.ui.media;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
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

public class MediaPreviewFragment extends Fragment implements MediaController.MediaPlayerControl {

    private static final String ARG_MEDIA_CONTENT_URI = "content_uri";
    private static final String ARG_IS_VIDEO = "is_video";
    private static final String ARG_IS_AUDIO = "is_audio";
    private static final String ARG_POSITION = "position";
    private static final String ARG_TITLE = "title";

    private String mContentUri;
    private String mTitle;
    private boolean mIsVideo;
    private boolean mIsAudio;
    private int mPosition;

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
    public static MediaPreviewFragment newInstance(
            Context context,
            SiteModel site,
            String contentUri,
            boolean isVideo) {
        Bundle args = new Bundle();
        args.putString(ARG_MEDIA_CONTENT_URI, contentUri);
        args.putBoolean(ARG_IS_VIDEO, isVideo);
        if (site != null) {
            args.putSerializable(WordPress.SITE, site);
        }

        MediaPreviewFragment fragment = new MediaPreviewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * @param context     self explanatory
     * @param site        optional site this media is associated with
     * @param media       media model
     */
    public static MediaPreviewFragment newInstance(
            Context context,
            SiteModel site,
            MediaModel media) {
        Bundle args = new Bundle();
        args.putString(ARG_MEDIA_CONTENT_URI, media.getUrl());
        args.putString(ARG_TITLE, media.getTitle());
        args.putBoolean(ARG_IS_VIDEO, media.isVideo());

        String mimeType = StringUtils.notNullStr(media.getMimeType()).toLowerCase();
        args.putBoolean(ARG_IS_AUDIO, mimeType.startsWith("audio"));

        if (site != null) {
            args.putSerializable(WordPress.SITE, site);
        }

        MediaPreviewFragment fragment = new MediaPreviewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        Bundle args = getArguments();
        mSite = (SiteModel) args.getSerializable(WordPress.SITE);
        mContentUri = args.getString(ARG_MEDIA_CONTENT_URI);
        mTitle = args.getString(ARG_TITLE);
        mIsVideo = args.getBoolean(ARG_IS_VIDEO);
        mIsAudio = args.getBoolean(ARG_IS_AUDIO);

        if (savedInstanceState != null) {
            mPosition = savedInstanceState.getInt(ARG_POSITION, 0);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.media_preview_fragment, container, false);

        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mImageView = (ImageView) view.findViewById(R.id.image_preview);
        mVideoView = (VideoView) view.findViewById(R.id.video_preview);

        mVideoFrame = (ViewGroup) view.findViewById(R.id.frame_video);
        mAudioFrame = (ViewGroup) view.findViewById(R.id.frame_audio);

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
            playVideo(mContentUri, mPosition);
        } else if (mIsAudio) {
            mAudioFrame.setOnClickListener(listener);
            if (!TextUtils.isEmpty(mTitle)) {
                TextView txtAudioTitle = (TextView) view.findViewById(R.id.text_audio_title);
                txtAudioTitle.setText(mTitle);
                txtAudioTitle.setVisibility(View.VISIBLE);
            }
            playAudio(mContentUri, mPosition);
        } else {
            loadImage(mContentUri);
        }

        mFadeHandler.postDelayed(fadeOutRunnable, FADE_DELAY_MS);

        return view;
    }

    @Override
    public void onStop() {
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
    public void onDestroy() {
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mIsVideo) {
            outState.putInt(ARG_POSITION, mVideoView.getCurrentPosition());
        } else if (mIsAudio && mAudioPlayer != null) {
            outState.putInt(ARG_POSITION, mAudioPlayer.getCurrentPosition());
        }
    }

    private void showProgress(boolean show) {
        if (isAdded()) {
            getView().findViewById(R.id.progress).setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /*
     * loads and displays a remote or local image
     */
    private void loadImage(@NonNull String mediaUri) {
        int width = DisplayUtils.getDisplayPixelWidth(getActivity());
        int height = DisplayUtils.getDisplayPixelHeight(getActivity());
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
                    if (isAdded() && response.getBitmap() != null) {
                        showProgress(false);
                        setBitmap(response.getBitmap());
                    }
                }
                @Override
                public void onErrorResponse(VolleyError error) {
                    AppLog.e(AppLog.T.MEDIA, error);
                    if (isAdded()) {
                        showProgress(false);
                        ToastUtils.showToast(getActivity(), R.string.error_media_load);
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
            int orientation = ImageUtils.getImageOrientation(getActivity(), mMediaUri);
            byte[] bytes = ImageUtils.createThumbnailFromUri(
                    getActivity(), Uri.parse(mMediaUri), mSize, null, orientation);
            if (bytes != null) {
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isAdded()) {
                if (bitmap != null) {
                    setBitmap(bitmap);
                } else {
                    ToastUtils.showToast(getActivity(), R.string.error_media_load);
                }
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
        mControls = new MediaController(getActivity());
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
                return false;
            }
        });

        showProgress(true);
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (isAdded()) {
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
            mAudioPlayer.setDataSource(getActivity(), Uri.parse(mediaUri));
        } catch (Exception e) {
            AppLog.e(AppLog.T.MEDIA, e);
            ToastUtils.showToast(getActivity(), R.string.error_media_load);
        }

        mAudioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (isAdded()) {
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
            if (isAdded() && mToolbar.getVisibility() == View.VISIBLE) {
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
        if (isAdded()) {
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
