package org.wordpress.android.ui.media;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.chrisbanes.photoview.PhotoView;
import com.github.chrisbanes.photoview.PhotoViewAttacher;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.StyledPlayerView;

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

public class MediaPreviewFragment extends Fragment {
    public static final String TAG = "media_preview_fragment";

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
    private boolean mAutoPlay;
    private int mPosition;

    private SiteModel mSite;

    private PhotoView mImageView;
    private StyledPlayerView mExoPlayerView;
    private PlayerControlView mExoPlayerControlsView;
    private ImageView mExoPlayerArtworkView;
    private OnMediaTappedListener mMediaTapListener;

    @Inject MediaStore mMediaStore;
    @Inject ImageManager mImageManager;
    @Inject AuthenticationUtils mAuthenticationUtils;
    @Inject ExoPlayerUtils mExoPlayerUtils;

    private SimpleExoPlayer mPlayer;

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
        mExoPlayerArtworkView = mExoPlayerView.findViewById(R.id.exo_artwork);
        mExoPlayerControlsView = view.findViewById(R.id.controls);

        FrameLayout videoFrame = view.findViewById(R.id.frame_video);
        RelativeLayout audioFrame = view.findViewById(R.id.frame_audio);

        videoFrame.setVisibility(mIsVideo ? View.VISIBLE : View.GONE);
        audioFrame.setVisibility(mIsAudio ? View.VISIBLE : View.GONE);

        if (mIsAudio && !TextUtils.isEmpty(mTitle)) {
            TextView txtAudioTitle = view.findViewById(R.id.text_audio_title);
            txtAudioTitle.setText(mTitle);
            txtAudioTitle.setVisibility(View.VISIBLE);
        }

        if (showAudioOrVideo()) {
            View.OnClickListener listener = v -> {
                if (mMediaTapListener != null) {
                    mMediaTapListener.onMediaTapped();
                }
            };
            audioFrame.setOnClickListener(listener);
            videoFrame.setOnClickListener(listener);
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (showAudioOrVideo()) {
            initializePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (showAudioOrVideo() || mPlayer != null) {
            releasePlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPlayer != null) releasePlayer();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (showAudioOrVideo()) {
            if (mPlayer == null) initializePlayer();
        } else {
            loadImage(mContentUri, mImageView);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (showAudioOrVideo()) {
            outState.putInt(ARG_POSITION, mPosition);
        }
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
    private void loadImage(String mediaUri, ImageView imageView) {
        if (imageView == null) {
            return;
        }

        if (TextUtils.isEmpty(mediaUri)) {
            showLoadingError();
            return;
        }

        imageView.setVisibility(View.VISIBLE);
        if ((mSite == null || SiteUtils.isPhotonCapable(mSite)) && !UrlUtils.isContentUri(mediaUri)) {
            int maxWidth = Math.max(DisplayUtils.getWindowPixelWidth(requireActivity()),
                    DisplayUtils.getWindowPixelHeight(requireActivity()));

            boolean isPrivateAtomicSite = mSite != null && mSite.isPrivateWPComAtomic();
            mediaUri = PhotonUtils.getPhotonImageUrl(mediaUri, maxWidth, 0, isPrivateAtomicSite);
        }
        showProgress(true);

        mImageManager.loadWithResultListener(imageView, ImageType.IMAGE, Uri.parse(mediaUri), ScaleType.CENTER, null,
                new RequestListener<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Object model) {
                        if (isAdded()) {
                            PhotoViewAttacher attacher = mImageView.getAttacher();
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
                            if (!mIsVideo) {
                                showLoadingError();
                            }
                        }
                    }
                });
    }

    private void initializePlayer() {
        mPlayer = (new SimpleExoPlayer.Builder(requireContext())).build();
        mPlayer.addListener(new PlayerEventListener());

        if (mIsVideo) {
            if (!mAutoPlay && !TextUtils.isEmpty(mVideoThumbnailUrl)) {
                loadImage(mVideoThumbnailUrl, mExoPlayerArtworkView);
            }
            mExoPlayerView.setPlayer(mPlayer);
            mExoPlayerView.requestFocus();
        } else if (mIsAudio) {
            mExoPlayerControlsView.setPlayer(mPlayer);
            mExoPlayerControlsView.requestFocus();
        }

        Uri uri = Uri.parse(mContentUri);
        mPlayer.setPlayWhenReady(mAutoPlay);
        mPlayer.seekTo(0, mPosition);

        MediaSource mediaSource = mExoPlayerUtils.buildMediaSource(uri);
        showProgress(true);
        mPlayer.prepare(mediaSource);
    }

    private void releasePlayer() {
        if (mPlayer == null) {
            return;
        }
        mPosition = (int) mPlayer.getCurrentPosition();
        mPlayer.release();
        mPlayer = null;
    }

    boolean showAudioOrVideo() {
        return mIsVideo || mIsAudio;
    }

    private class PlayerEventListener implements Player.Listener {
        @Override public void onLoadingChanged(boolean isLoading) {
            showProgress(isLoading);
        }
    }
}
