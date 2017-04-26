package org.wordpress.android.ui.media;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
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
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.ToastUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.inject.Inject;

import uk.co.senab.photoview.PhotoViewAttacher;

public class MediaPreviewActivity extends AppCompatActivity {

    private static final String ARG_MEDIA_CONTENT_URI = "content_uri";
    private static final String ARG_MEDIA_LOCAL_ID = "media_local_id";
    private static final String ARG_IS_VIDEO = "is_video";

    private String mContentUri;
    private int mMediaId;
    private boolean mIsVideo;
    private boolean mEnableMetadata;
    private boolean mShowEditMenuItem;

    private SiteModel mSite;

    private ImageView mImageView;
    private VideoView mVideoView;
    private ViewGroup mMetadataView;
    private Toolbar mToolbar;

    @Inject MediaStore mMediaStore;
    @Inject FluxCImageLoader mImageLoader;
    @Inject Dispatcher mDispatcher;

    private static final long FADE_DELAY_MS = 4000;
    private final Handler mFadeHandler = new Handler();

    /**
     * @param context     self explanatory
     * @param sourceView  optional imageView on calling activity which shows thumbnail of same media
     * @param contentUri  local content:// uri of media
     * @param isVideo     whether the passed media is a video - assumed to be an image otherwise
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
     * @param site        site which contains this media item
     * @param mediaId     local ID in site's media library
     */
    public static void showPreview(Context context,
                                   View sourceView,
                                   SiteModel site,
                                   int mediaId) {
        Intent intent = new Intent(context, MediaPreviewActivity.class);
        intent.putExtra(ARG_MEDIA_LOCAL_ID, mediaId);
        intent.putExtra(WordPress.SITE, site);
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
        mMetadataView = (ViewGroup) findViewById(R.id.layout_metadata);
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

        boolean hasEditFragment = hasEditFragment();
        setLookClosable(hasEditFragment);

        String mediaUri;
        if (!TextUtils.isEmpty(mContentUri)) {
            mediaUri = mContentUri;
        } else if (mMediaId != 0) {
            MediaModel media = mMediaStore.getMediaWithLocalId(mMediaId);
            if (media == null) {
                delayedFinish(true);
                return;
            }
            mIsVideo = media.isVideo();
            mEnableMetadata = true;
            mediaUri = media.getUrl();
            loadMetaData(media);
            if (!hasEditFragment) {
                fadeInMetadata();
            }
        } else {
            delayedFinish(true);
            return;
        }

        if (mEnableMetadata) {
            setSupportActionBar(mToolbar);
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setTitle(R.string.media);
            }
        } else {
            mToolbar.setVisibility(View.GONE);
        }

        mImageView.setVisibility(mIsVideo ?  View.GONE : View.VISIBLE);
        mVideoView.setVisibility(mIsVideo ? View.VISIBLE : View.GONE);

        mShowEditMenuItem = mEnableMetadata && mSite != null && !hasEditFragment;

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
        if (mSite != null) {
            outState.putSerializable(WordPress.SITE, mSite);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        mDispatcher.unregister(this);
        super.onStop();
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
        MediaEditFragment fragment = getEditFragment();
        if (fragment != null) {
            ActivityUtils.hideKeyboard(this);
            fragment.saveChanges();
        }

        setLookClosable(false);
        showEditMenuItem(true);

        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.media_preview, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem mnuEdit = menu.findItem(R.id.menu_edit);
        mnuEdit.setVisible(mShowEditMenuItem);

        MenuItem mnuShare = menu.findItem(R.id.menu_share);
        mnuShare.setVisible(mMediaId != 0);

        return super.onPrepareOptionsMenu(menu);
    }

    private void showEditMenuItem(boolean show) {
        mShowEditMenuItem = show;
        invalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.menu_edit) {
            showEditFragment(mMediaId);
            return true;
        } else if (item.getItemId() == R.id.menu_share) {
            shareMedia();
        }

        return super.onOptionsItemSelected(item);
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
            String imageUrl = PhotonUtils.getPhotonImageUrl(mediaUri, size, 0);
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
            byte[] bytes = ImageUtils.createThumbnailFromUri(this, Uri.parse(mediaUri), size, null, 0);
            if (bytes == null) {
                delayedFinish(true);
                return;
            }

            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bmp != null) {
                setBitmap(bmp);
            } else {
                delayedFinish(true);
                return;
            }
        }
    }

    private void setBitmap(@NonNull Bitmap bmp) {
        // assign the photo attacher to enable pinch/zoom - must come before setImageBitmap
        // for it to be correctly resized upon loading
        PhotoViewAttacher attacher = new PhotoViewAttacher(mImageView);

        // fade in metadata when tapped
        if (mEnableMetadata) {
            attacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
                @Override
                public void onViewTap(View view, float x, float y) {
                    if (!hasEditFragment()) {
                        fadeInMetadata();
                    }
                }
            });
        }

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
    
    private void loadMetaData(@NonNull final MediaModel media) {
        boolean isLocal = MediaUtils.isLocalFile(media.getUploadState());

        TextView titleView = (TextView) mMetadataView.findViewById(R.id.media_details_file_name_or_title);
        TextView captionView = (TextView) mMetadataView.findViewById(R.id.media_details_caption);
        TextView descriptionView = (TextView) mMetadataView.findViewById(R.id.media_details_description);
        TextView dateView = (TextView) mMetadataView.findViewById(R.id.media_details_date);
        TextView fileTypeView = (TextView) mMetadataView.findViewById(R.id.media_details_file_type);

        if (TextUtils.isEmpty(media.getCaption())) {
            captionView.setVisibility(View.GONE);
        } else {
            captionView.setText(media.getCaption());
            captionView.setVisibility(View.VISIBLE);
        }

        if (TextUtils.isEmpty(media.getDescription())) {
            descriptionView.setVisibility(View.GONE);
        } else {
            descriptionView.setText(media.getDescription());
            descriptionView.setVisibility(View.VISIBLE);
        }

        String datePrefix = isLocal ?
                getString(R.string.media_details_label_date_added) :
                getString(R.string.media_details_label_date_uploaded);
        dateView.setText(datePrefix + " " + getDisplayDate(media.getUploadDate()));

        String fileURL = media.getUrl();
        String fileName = media.getFileName();

        titleView.setText(TextUtils.isEmpty(media.getTitle()) ? fileName : media.getTitle());

        float mediaWidth = media.getWidth();
        float mediaHeight = media.getHeight();

        // show dimens & file ext together
        String dimens =
                (mediaWidth > 0 && mediaHeight > 0) ? (int) mediaWidth + " x " + (int) mediaHeight : null;
        String fileExt =
                TextUtils.isEmpty(fileURL) ? null : fileURL.replaceAll(".*\\.(\\w+)$", "$1").toUpperCase();
        boolean hasDimens = !TextUtils.isEmpty(dimens);
        boolean hasExt = !TextUtils.isEmpty(fileExt);
        if (hasDimens & hasExt) {
            fileTypeView.setText(fileExt + ", " + dimens);
            fileTypeView.setVisibility(View.VISIBLE);
        } else if (hasExt) {
            fileTypeView.setText(fileExt);
            fileTypeView.setVisibility(View.VISIBLE);
        } else {
            fileTypeView.setVisibility(View.GONE);
        }
    }

    private final Runnable fadeOutRunnable = new Runnable() {
        @Override
        public void run() {
            fadeOutMetadata();
        }
    };

    private void fadeOutMetadata() {
        if (!isFinishing() && mMetadataView.getVisibility() == View.VISIBLE) {
            AniUtils.fadeOut(mMetadataView, AniUtils.Duration.LONG);
        }
    }

    private void fadeInMetadata() {
        if (!isFinishing()) {
            mFadeHandler.removeCallbacks(fadeOutRunnable);
            if (mMetadataView.getVisibility() != View.VISIBLE) {
                AniUtils.fadeIn(mMetadataView, AniUtils.Duration.LONG);
            }
            mFadeHandler.postDelayed(fadeOutRunnable, FADE_DELAY_MS);
        }
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

    private boolean hasEditFragment() {
        return getEditFragment() != null;
    }

    private MediaEditFragment getEditFragment() {
        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentByTag(MediaEditFragment.TAG);
        if (fragment != null) {
            return (MediaEditFragment) fragment;
        }
        return null;
    }

    private void showEditFragment(int localMediaId) {
        MediaEditFragment fragment = getEditFragment();
        if (fragment == null) {
            fragment = MediaEditFragment.newInstance(mSite, localMediaId);
            FragmentManager fm = getFragmentManager();
            fm.beginTransaction()
                .replace(R.id.fragment_container, fragment, MediaEditFragment.TAG)
                .addToBackStack(null)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commitAllowingStateLoss();
        } else {
            fragment.loadMedia(localMediaId);
        }

        setLookClosable(true);
        showEditMenuItem(false);
        fadeOutMetadata();
    }

    private void setLookClosable(boolean lookClosable) {
        if (mToolbar != null) {
            mToolbar.setNavigationIcon(lookClosable ? R.drawable.ic_close_white_24dp : R.drawable.ic_arrow_left_white_24dp);
        }
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
        if (!event.isError() && mMediaId != 0) {
            MediaModel media = mMediaStore.getMediaWithLocalId(mMediaId);
            if (media != null) {
                loadMetaData(media);
                fadeInMetadata();
            }
        }
    }
}