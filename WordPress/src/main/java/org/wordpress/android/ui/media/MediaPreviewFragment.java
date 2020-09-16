package org.wordpress.android.ui.media;

import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.MediaController;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.ui.utils.AuthenticationUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageManager.RequestListener;
import org.wordpress.android.util.image.ImageType;

import javax.inject.Inject;

import uk.co.senab.photoview.PhotoViewAttacher;

public class MediaPreviewFragment extends Fragment implements MediaController.MediaPlayerControl {
    public static final String TAG = "media_preview_fragment";
    public static final String USER_AGENT = "wpandroid-exoplayer";

    static final String ARG_MEDIA_CONTENT_URI = "content_uri";
    static final String ARG_MEDIA_ID = "media_id";
    private static final String ARG_TITLE = "title";
    private static final String ARG_POSITION = "position";
    private static final String ARG_AUTOPLAY = "autoplay";
    private static final String ARG_VIDEO_THUMB = "video_thumb";

    public interface OnMediaTappedListener {
        void onMediaTapped();
    }

    private String mContentUri;
    private String mVideoThumbnailUrl;
    private String mTitle;
    private boolean mIsVideo;
    private boolean mIsAudio;
    private boolean mFragmentWasPaused;
    private boolean mAutoPlay;
    private int mPosition;

    private SiteModel mSite;

    private ImageView mImageView;
    private PlayerView mExoPlayerView;
    private ViewGroup mVideoFrame;
    private ViewGroup mAudioFrame;

    private MediaPlayer mAudioPlayer;
    private MediaController mControls;

    private OnMediaTappedListener mMediaTapListener;

    @Inject MediaStore mMediaStore;
    @Inject ImageManager mImageManager;
    @Inject AuthenticationUtils mAuthenticationUtils;

    private SimpleExoPlayer mPlayer;
    private int mCurrentWindow = 0;

    /**
     * @param site       optional site this media is associated with
     * @param contentUri URI of media - can be local or remote
     */
    public static MediaPreviewFragment newInstance(
            @Nullable SiteModel site,
            @NonNull String contentUri,
            boolean autoPlay) {
        Bundle args = new Bundle();
        args.putString(ARG_MEDIA_CONTENT_URI, contentUri);
        args.putBoolean(ARG_AUTOPLAY, autoPlay);
        if (site != null) {
            args.putSerializable(WordPress.SITE, site);
        }

        MediaPreviewFragment fragment = new MediaPreviewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * @param site     optional site this media is associated with
     * @param media    media model
     * @param autoPlay true = play video/audio after fragment is created
     */
    public static MediaPreviewFragment newInstance(
            @Nullable SiteModel site,
            @NonNull MediaModel media,
            boolean autoPlay) {
        Bundle args = new Bundle();
        args.putString(ARG_MEDIA_CONTENT_URI, media.getUrl());
        args.putString(ARG_TITLE, media.getTitle());
        args.putBoolean(ARG_AUTOPLAY, autoPlay);
        if (site != null) {
            args.putSerializable(WordPress.SITE, site);
        }
        if (media.isVideo() && !TextUtils.isEmpty(media.getThumbnailUrl())) {
            args.putString(ARG_VIDEO_THUMB, media.getThumbnailUrl());
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
        mAutoPlay = args.getBoolean(ARG_AUTOPLAY);
        mVideoThumbnailUrl = args.getString(ARG_VIDEO_THUMB);

        mIsVideo = MediaUtils.isVideo(mContentUri);
        mIsAudio = MediaUtils.isAudio(mContentUri);

        if (savedInstanceState != null) {
            mPosition = savedInstanceState.getInt(ARG_POSITION, 0);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.media_preview_fragment, container, false);

        mImageView = view.findViewById(R.id.image_preview);
        mExoPlayerView = view.findViewById(R.id.video_preview);

        mVideoFrame = view.findViewById(R.id.frame_video);
        mAudioFrame = view.findViewById(R.id.frame_audio);

        mVideoFrame.setVisibility(mIsVideo ? View.VISIBLE : View.GONE);
        mAudioFrame.setVisibility(mIsAudio ? View.VISIBLE : View.GONE);

        if (mIsAudio && !TextUtils.isEmpty(mTitle)) {
            TextView txtAudioTitle = view.findViewById(R.id.text_audio_title);
            txtAudioTitle.setText(mTitle);
            txtAudioTitle.setVisibility(View.VISIBLE);
        }

        if (mIsVideo || mIsAudio) {
            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mControls != null) {
                        mControls.show();
                    }
                    if (mMediaTapListener != null) {
                        mMediaTapListener.onMediaTapped();
                    }
                }
            };
            mVideoFrame.setOnClickListener(listener);
            mAudioFrame.setOnClickListener(listener);
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > VERSION_CODES.M) {
            initializePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > VERSION_CODES.M) {
            releasePlayer();
        }
    }

    @Override
    public void onPause() {
        mFragmentWasPaused = true;
        if (mIsAudio) {
            pauseMedia();
        }
        if (mIsVideo) {
            if (Util.SDK_INT <= VERSION_CODES.M) {
                releasePlayer();
            }
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mFragmentWasPaused) {
            mFragmentWasPaused = false;
        } else if (mIsAudio) {
            if (mAutoPlay) {
                playMedia();
            }
        } else if (mIsVideo) {
            /*if (!mAutoPlay && !TextUtils.isEmpty(mVideoThumbnailUrl)) {
                loadImage(mVideoThumbnailUrl);
            } else {
                playMedia();
            }*/
            if (Util.SDK_INT <= VERSION_CODES.M || mPlayer == null) {
                initializePlayer();
            }
        } else {
            loadImage(mContentUri);
        }
    }

    void playMedia() {
        if (mIsVideo) {
            playVideo(mContentUri, mPosition);
        } else if (mIsAudio) {
            playAudio(mContentUri, mPosition);
        }
    }

    @Override
    public void onDestroy() {
        if (mAudioPlayer != null) {
            mAudioPlayer.release();
            mAudioPlayer = null;
        }
        /*if (mExoPlayerView.isPlaying()) {
            mExoPlayerView.stopPlayback();
            mExoPlayerView.setMediaController(null);
        }*/
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mIsVideo) {
            outState.putInt(ARG_POSITION, (int) mPlayer.getCurrentPosition());
        } else if (mIsAudio && mAudioPlayer != null) {
            outState.putInt(ARG_POSITION, mAudioPlayer.getCurrentPosition());
        }
    }

    void pauseMedia() {
        if (mIsAudio && mControls != null) {
            mControls.hide();
        }
        if (mAudioPlayer != null && mAudioPlayer.isPlaying()) {
            mPosition = mAudioPlayer.getCurrentPosition();
            mAudioPlayer.pause();
        }
        /*if (mExoPlayerView.isPlaying()) {
            mPosition = mExoPlayerView.getCurrentPosition();
            mExoPlayerView.pause();
        }*/
    }

    void setOnMediaTappedListener(OnMediaTappedListener listener) {
        mMediaTapListener = listener;
    }

    private void showProgress(boolean show) {
        if (isAdded()) {
            getView().findViewById(R.id.progress).setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showLoadingError() {
        if (isAdded()) {
            getView().findViewById(R.id.text_error).setVisibility(View.VISIBLE);
        }
    }

    /*
     * loads and displays a remote or local image
     */
    private void loadImage(String mediaUri) {
        if (TextUtils.isEmpty(mediaUri)) {
            showLoadingError();
            return;
        }

        mImageView.setVisibility(View.VISIBLE);
        if ((mSite == null || SiteUtils.isPhotonCapable(mSite)) && !UrlUtils.isContentUri(mediaUri)) {
            int maxWidth = Math.max(DisplayUtils.getDisplayPixelWidth(getActivity()),
                    DisplayUtils.getDisplayPixelHeight(getActivity()));

            boolean isPrivateAtomicSite = mSite != null && mSite.isPrivateWPComAtomic();
            mediaUri = PhotonUtils.getPhotonImageUrl(mediaUri, maxWidth, 0, isPrivateAtomicSite);
        }
        showProgress(true);

        mImageManager.loadWithResultListener(mImageView, ImageType.IMAGE, Uri.parse(mediaUri), ScaleType.CENTER, null,
                new RequestListener<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Object model) {
                        if (isAdded()) {
                            // assign the photo attacher to enable pinch/zoom - must come before
                            // setImageBitmap
                            // for it to be correctly resized upon loading
                            PhotoViewAttacher attacher = new PhotoViewAttacher(mImageView);
                            attacher.setOnViewTapListener((view, x, y) -> {
                                if (mMediaTapListener != null) {
                                    mMediaTapListener.onMediaTapped();
                                }
                            });
                            showProgress(false);
                        }
                    }

                    @Override
                    public void onLoadFailed(@Nullable Exception e, @Nullable Object model) {
                        if (isAdded()) {
                            if (e != null) {
                                AppLog.e(T.MEDIA, e);
                            }
                            showProgress(false);
                            showLoadingError();
                        }
                    }
                });
    }

    /*
     * initialize the media controls (audio/video only)
     */
    private void initControls() {
        mControls = new MediaController(getActivity());
        /*if (mIsVideo) {
            mControls.setAnchorView(mVideoFrame);
//            mControls.setMediaPlayer(mExoPlayerView);
        } else */
        if (mIsAudio) {
            mControls.setAnchorView(mAudioFrame);
            mControls.setMediaPlayer(this);
        }
    }

    private void playVideo(@NonNull String mediaUri, final int position) {
        /*mExoPlayerView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return false;
            }
        });

        showProgress(true);
        mExoPlayerView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (isAdded()) {
                    showProgress(false);
                    mImageView.setVisibility(View.GONE);
                    if (mAutoPlay) {
                        mp.start();
                    }
                    if (position > 0) {
                        mp.seekTo(position);
                    }
                    mControls.show();
                }
            }
        });*/

        initControls();
//        mExoPlayerView.setVideoURI(Uri.parse(mediaUri), mAuthenticationUtils.getAuthHeaders(mediaUri));
        mExoPlayerView.requestFocus();
    }

    private void playAudio(@NonNull String mediaUri, final int position) {
        mAudioPlayer = new MediaPlayer();
        mAudioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mAudioPlayer.setDataSource(getActivity(), Uri.parse(mediaUri));
        } catch (Exception e) {
            AppLog.e(AppLog.T.MEDIA, e);
            showLoadingError();
            return;
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

    private MediaSource buildMediaSource(Uri uri) {
        DefaultHttpDataSourceFactory dataSourceFactory =
                new DefaultHttpDataSourceFactory(USER_AGENT);
        dataSourceFactory.getDefaultRequestProperties().set(mAuthenticationUtils.getAuthHeaders(uri.toString()));
        return new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri);
    }

    private void initializePlayer() {
        mPlayer = new SimpleExoPlayer.Builder(requireActivity()).build();
        mPlayer.addListener(new PlayerEventListener());
        mExoPlayerView.setPlayer(mPlayer);

        Uri uri = Uri.parse(mContentUri);
        MediaSource mediaSource = buildMediaSource(uri);
        mPlayer.setPlayWhenReady(mAutoPlay);
        mPlayer.seekTo(mCurrentWindow, mPosition);
        mPlayer.prepare(mediaSource, false, false);
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            mPosition = (int) mPlayer.getCurrentPosition();
            mCurrentWindow = mPlayer.getCurrentWindowIndex();
            mPlayer.release();
            mPlayer = null;
        }
    }

    private class PlayerEventListener implements Player.EventListener {
        @Override public void onLoadingChanged(boolean isLoading) {
            showProgress(isLoading);
        }
    }
}
