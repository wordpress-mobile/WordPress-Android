package org.wordpress.android.ui.media;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

import org.greenrobot.eventbus.EventBus;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPViewPagerTransformer;
import org.wordpress.android.widgets.WPViewPagerTransformer.TransformType;

import java.util.ArrayList;

import javax.inject.Inject;

public class MediaPreviewActivity extends LocaleAwareActivity implements MediaPreviewFragment.OnMediaTappedListener {
    private static final String ARG_ID_OR_URL_LIST = "id_list";
    private static final String ARG_PREVIEW_TYPE = "preview_type";

    enum PreviewType {
        SINGLE_ITEM,
        MULTI_MEDIA_IDS,
        MULTI_IMAGE_URLS;

        boolean isMulti() {
            return this == MULTI_MEDIA_IDS || this == MULTI_IMAGE_URLS;
        }
    }

    // will contain a list of media IDs when PreviewType = MULTI_MEDIA_IDS or a list of image URLs
    // when PreviewType = MULTI_IMAGE_URLS
    private ArrayList<String> mMediaIdOrUrlList;

    // initial media item to show, based either on ID or URI
    private int mMediaId;
    private String mContentUri;

    private int mLastPosition;
    private PreviewType mPreviewType;

    // note that mSite may be null
    private SiteModel mSite;

    private Toolbar mToolbar;
    private ViewPager mViewPager;
    private MediaPagerAdapter mPagerAdapter;

    private static final long FADE_DELAY_MS = 3000;
    private final Handler mFadeHandler = new Handler();

    @Inject MediaStore mMediaStore;

    static class MediaPreviewSwiped {
        final int mediaId;

        MediaPreviewSwiped(int mediaId) {
            this.mediaId = mediaId;
        }
    }

    /**
     * @param context self explanatory
     * @param site optional site this media is associated with
     * @param contentUri URI of initial media - can be local or remote
     */
    public static void showPreview(@NonNull Context context,
                                   @Nullable SiteModel site,
                                   @NonNull String contentUri) {
        Intent intent = new Intent(context, MediaPreviewActivity.class);
        intent.putExtra(MediaPreviewFragment.ARG_MEDIA_CONTENT_URI, contentUri);
        intent.putExtra(ARG_PREVIEW_TYPE, PreviewType.SINGLE_ITEM);
        if (site != null) {
            intent.putExtra(WordPress.SITE, site);
        }

        startIntent(context, intent);
    }

    /**
     * @param context self explanatory
     * @param site optional site this media is associated with
     * @param media initial media model to show
     * @param mediaIdList optional list of media IDs to page through
     */
    public static void showPreview(@NonNull Context context,
                                   @Nullable SiteModel site,
                                   @NonNull MediaModel media,
                                   @Nullable ArrayList<String> mediaIdList) {
        Intent intent = new Intent(context, MediaPreviewActivity.class);
        intent.putExtra(MediaPreviewFragment.ARG_MEDIA_ID, media.getId());
        intent.putExtra(MediaPreviewFragment.ARG_MEDIA_CONTENT_URI, media.getUrl());
        if (site != null) {
            intent.putExtra(WordPress.SITE, site);
        }
        if (mediaIdList != null && mediaIdList.size() > 1) {
            intent.putStringArrayListExtra(ARG_ID_OR_URL_LIST, mediaIdList);
            intent.putExtra(ARG_PREVIEW_TYPE, PreviewType.MULTI_MEDIA_IDS);
        } else {
            intent.putExtra(ARG_PREVIEW_TYPE, PreviewType.SINGLE_ITEM);
        }

        startIntent(context, intent);
    }

    /**
     * @param context self explanatory
     * @param site optional site this media is associated with
     * @param imageUrlList optional list of image URLs to page through
     * @param contentUri URI of initial media
     */
    public static void showPreview(@NonNull Context context,
                                   @Nullable SiteModel site,
                                   @Nullable ArrayList<String> imageUrlList,
                                   @NonNull String contentUri) {
        Intent intent = new Intent(context, MediaPreviewActivity.class);
        intent.putExtra(MediaPreviewFragment.ARG_MEDIA_CONTENT_URI, contentUri);
        if (site != null) {
            intent.putExtra(WordPress.SITE, site);
        }
        if (imageUrlList != null && imageUrlList.size() > 1) {
            intent.putStringArrayListExtra(ARG_ID_OR_URL_LIST, imageUrlList);
            intent.putExtra(ARG_PREVIEW_TYPE, PreviewType.MULTI_IMAGE_URLS);
        } else {
            intent.putExtra(ARG_PREVIEW_TYPE, PreviewType.SINGLE_ITEM);
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
            mMediaId = savedInstanceState.getInt(MediaPreviewFragment.ARG_MEDIA_ID);
            mContentUri = savedInstanceState.getString(MediaPreviewFragment.ARG_MEDIA_CONTENT_URI);
            mPreviewType = (PreviewType) savedInstanceState.getSerializable(ARG_PREVIEW_TYPE);
            if (savedInstanceState.containsKey(ARG_ID_OR_URL_LIST)) {
                mMediaIdOrUrlList = savedInstanceState.getStringArrayList(ARG_ID_OR_URL_LIST);
            }
        } else {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            mMediaId = getIntent().getIntExtra(MediaPreviewFragment.ARG_MEDIA_ID, 0);
            mContentUri = getIntent().getStringExtra(MediaPreviewFragment.ARG_MEDIA_CONTENT_URI);
            mPreviewType = (PreviewType) getIntent().getSerializableExtra(ARG_PREVIEW_TYPE);
            if (getIntent().hasExtra(ARG_ID_OR_URL_LIST)) {
                mMediaIdOrUrlList = getIntent().getStringArrayListExtra(ARG_ID_OR_URL_LIST);
            }
        }

        if (TextUtils.isEmpty(mContentUri)) {
            delayedFinish();
            return;
        }

        mToolbar = findViewById(R.id.toolbar);
        int toolbarColor = ContextCompat.getColor(this, R.color.black_translucent_40);
        //noinspection deprecation
        mToolbar.setBackgroundDrawable(new ColorDrawable(toolbarColor));
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        View fragmentContainer = findViewById(R.id.fragment_container);
        mViewPager = findViewById(R.id.viewpager);

        // use a ViewPager if we're passed a list of media, otherwise show a single fragment
        if (getPreviewType().isMulti()) {
            fragmentContainer.setVisibility(View.GONE);
            mViewPager.setVisibility(View.VISIBLE);
            setupViewPager();
        } else {
            fragmentContainer.setVisibility(View.VISIBLE);
            mViewPager.setVisibility(View.GONE);
            showPreviewFragment();
        }

        showToolbar();
        mFadeHandler.postDelayed(mFadeOutRunnable, FADE_DELAY_MS);
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
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putInt(MediaPreviewFragment.ARG_MEDIA_ID, mMediaId);
        outState.putString(MediaPreviewFragment.ARG_MEDIA_CONTENT_URI, mContentUri);
        outState.putSerializable(ARG_PREVIEW_TYPE, mPreviewType);
        if (mMediaIdOrUrlList != null) {
            outState.putStringArrayList(ARG_ID_OR_URL_LIST, mMediaIdOrUrlList);
        }
    }

    private void delayedFinish() {
        ToastUtils.showToast(this, R.string.error_media_not_found);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 1500);
    }

    /*
     * shows a single preview fragment within this activity - called when we can't use a ViewPager to swipe
     * between media (ie: we're previewing a local file)
     */
    private void showPreviewFragment() {
        MediaPreviewFragment fragment;
        MediaModel media = mMediaStore.getMediaWithLocalId(mMediaId);
        if (media != null) {
            fragment = MediaPreviewFragment.newInstance(mSite, media, true);
        } else {
            fragment = MediaPreviewFragment.newInstance(mSite, mContentUri, true);
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, MediaPreviewFragment.TAG)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();

        fragment.setOnMediaTappedListener(this);
    }

    private final Runnable mFadeOutRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isFinishing() && mToolbar.getVisibility() == View.VISIBLE) {
                AniUtils.startAnimation(mToolbar, R.anim.toolbar_fade_out_and_up, new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mToolbar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
            }
        }
    };

    private void showToolbar() {
        if (!isFinishing()) {
            if (getPreviewType().isMulti()) {
                int position = mViewPager.getCurrentItem();
                int count = mPagerAdapter.getCount();
                if (count > 1) {
                    String title = String.format(
                            getString(R.string.media_preview_title),
                            position + 1,
                            count);
                    mToolbar.setTitle(title);
                }
            }

            mFadeHandler.removeCallbacks(mFadeOutRunnable);
            mFadeHandler.postDelayed(mFadeOutRunnable, FADE_DELAY_MS);
            if (mToolbar.getVisibility() != View.VISIBLE) {
                AniUtils.startAnimation(mToolbar, R.anim.toolbar_fade_in_and_down, new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        mToolbar.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
            }
        }
    }

    private PreviewType getPreviewType() {
        return mPreviewType != null ? mPreviewType : PreviewType.SINGLE_ITEM;
    }

    private void setupViewPager() {
        mPagerAdapter = new MediaPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setPageTransformer(false, new WPViewPagerTransformer(TransformType.SLIDE_OVER));

        // determine the position of the original media item so we can page to it immediately
        int initialPos = 0;
        String compareTo = getPreviewType() == PreviewType.MULTI_MEDIA_IDS ? Integer.toString(mMediaId) : mContentUri;
        if (compareTo != null) {
            for (int i = 0; i < mMediaIdOrUrlList.size(); i++) {
                if (compareTo.equals(mMediaIdOrUrlList.get(i))) {
                    initialPos = i;
                    break;
                }
            }
        }

        mViewPager.setCurrentItem(initialPos);
        mPagerAdapter.unpauseFragment(initialPos);
        mLastPosition = initialPos;

        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                switch (getPreviewType()) {
                    case MULTI_MEDIA_IDS:
                        // pause the outgoing fragment and unpause the incoming one - this prevents audio/video from
                        // playing in inactive fragments
                        if (mLastPosition != position) {
                            mPagerAdapter.pauseFragment(mLastPosition);
                        }
                        mPagerAdapter.unpauseFragment(position);
                        mMediaId = Integer.valueOf(mMediaIdOrUrlList.get(position));
                        // fire event so settings activity shows the same media as this activity (user may have swiped)
                        EventBus.getDefault().post(new MediaPreviewSwiped(mMediaId));
                        break;
                    case MULTI_IMAGE_URLS:
                        mContentUri = mMediaIdOrUrlList.get(position);
                        break;
                }
                mLastPosition = position;
                showToolbar();
            }
        });
    }

    /*
     * make sure toolbar appears when user taps the media in the fragment
     */
    @Override
    public void onMediaTapped() {
        showToolbar();
    }

    class MediaPagerAdapter extends FragmentStatePagerAdapter {
        private final SparseArray<Fragment> mFragmentMap = new SparseArray<>();
        private boolean mDidAutoPlay;

        MediaPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            MediaPreviewFragment fragment;
            switch (getPreviewType()) {
                case MULTI_MEDIA_IDS:
                    int id = Integer.valueOf(mMediaIdOrUrlList.get(position));
                    MediaModel media = mMediaStore.getMediaWithLocalId(id);
                    // make sure we autoplay the initial item (relevant only for audio/video)
                    boolean autoPlay;
                    if (id == mMediaId && !mDidAutoPlay) {
                        autoPlay = true;
                        mDidAutoPlay = true;
                    } else {
                        autoPlay = false;
                    }
                    fragment = MediaPreviewFragment.newInstance(mSite, media, autoPlay);
                    break;
                case MULTI_IMAGE_URLS:
                    String imageUrl = mMediaIdOrUrlList.get(position);
                    fragment = MediaPreviewFragment.newInstance(null, imageUrl, false);
                    break;
                default:
                    // should never get here
                    throw new RuntimeException("Unhandled preview type");
            }

            fragment.setOnMediaTappedListener(MediaPreviewActivity.this);
            return fragment;
        }

        @Override
        public int getCount() {
            return mMediaIdOrUrlList != null ? mMediaIdOrUrlList.size() : 0;
        }

        @NonNull
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Object item = super.instantiateItem(container, position);
            if (item instanceof Fragment) {
                mFragmentMap.put(position, (Fragment) item);
            }
            return item;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            mFragmentMap.remove(position);
            super.destroyItem(container, position, object);
        }

        void pauseFragment(int position) {
            Fragment fragment = mFragmentMap.get(position);
            if (fragment != null) {
                ((MediaPreviewFragment) fragment).pauseMedia();
            }
        }

        void unpauseFragment(int position) {
            Fragment fragment = mFragmentMap.get(position);
            if (fragment != null) {
                ((MediaPreviewFragment) fragment).playMedia();
            }
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            // work around https://code.google.com/p/android/issues/detail?id=42601
            try {
                super.restoreState(state, loader);
            } catch (IllegalStateException e) {
                AppLog.e(AppLog.T.MEDIA, e);
            }
        }
    }
}
