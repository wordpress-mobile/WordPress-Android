package org.wordpress.android.ui.media;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
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
import android.view.animation.Animation;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

public class MediaPreviewActivity extends AppCompatActivity {

    static final String ARG_MEDIA_CONTENT_URI = "content_uri";
    static final String ARG_IS_VIDEO = "is_video";
    static final String ARG_IS_AUDIO = "is_audio";
    static final String ARG_TITLE = "title";
    static final String ARG_MEDIA_ID = "media_id";

    private int mMediaId;
    private String mContentUri;
    private String mTitle;
    private boolean mIsVideo;
    private boolean mIsAudio;

    private SiteModel mSite;

    private Toolbar mToolbar;

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
        intent.putExtra(ARG_MEDIA_ID, media.getId());
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

        if (savedInstanceState != null) {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mMediaId = savedInstanceState.getInt(ARG_MEDIA_ID);
            mContentUri = savedInstanceState.getString(ARG_MEDIA_CONTENT_URI);
            mTitle = savedInstanceState.getString(ARG_TITLE);
            mIsVideo = savedInstanceState.getBoolean(ARG_IS_VIDEO);
            mIsAudio = savedInstanceState.getBoolean(ARG_IS_AUDIO);
        } else {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            mMediaId = getIntent().getIntExtra(ARG_MEDIA_ID, 0);
            mContentUri = getIntent().getStringExtra(ARG_MEDIA_CONTENT_URI);
            mTitle = getIntent().getStringExtra(ARG_TITLE);
            mIsVideo = getIntent().getBooleanExtra(ARG_IS_VIDEO, false);
            mIsAudio = getIntent().getBooleanExtra(ARG_IS_AUDIO, false);
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

        mFadeHandler.postDelayed(fadeOutRunnable, FADE_DELAY_MS);

        if (getPreviewFragment() == null) {
            showPreviewFragment();
        }
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
        outState.putBoolean(ARG_IS_VIDEO, mIsVideo);
        outState.putBoolean(ARG_IS_AUDIO, mIsAudio);
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

    private MediaPreviewFragment getPreviewFragment() {
        return (MediaPreviewFragment) getFragmentManager().findFragmentByTag(MediaPreviewFragment.TAG);
    }

    private void showPreviewFragment() {
        MediaPreviewFragment fragment;
        MediaModel media = mMediaStore.getMediaWithLocalId(mMediaId);
        if (media != null) {
            fragment = MediaPreviewFragment.newInstance(mSite, media);
        } else {
            fragment = MediaPreviewFragment.newInstance(mSite, mContentUri, mIsVideo);
        }
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, MediaPreviewFragment.TAG)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null)
                .commit();
        fragment.setOnMediaTappedListener(new MediaPreviewFragment.OnMediaTappedListener() {
            @Override
            public void onMediaTapped() {
                showToolbar();
            }
        });
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
}
