package org.wordpress.android.ui.media;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import org.greenrobot.eventbus.EventBus;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPViewPagerTransformer;
import org.wordpress.android.widgets.WPViewPagerTransformer.TransformType;

import java.util.ArrayList;

import javax.inject.Inject;


public class MediaPreviewActivity extends AppCompatActivity implements MediaPreviewFragment.OnMediaTappedListener {
    private static final String ARG_ID_LIST = "id_list";
    private static final String ARG_URL_LIST = "url_list";

    enum PreviewType {
        SINGLE_ITEM,
        MULTI_MEDIA_IDS,
        MULTI_IMAGE_URLS;

        boolean isMulti() {
            return this == MULTI_MEDIA_IDS || this == MULTI_IMAGE_URLS;
        }
    }

    private int mMediaId;
    private ArrayList<String> mMediaIdList;
    private ArrayList<String> mMediaUrlList;
    private String mContentUri;
    private int mLastPosition;

    private SiteModel mSite;

    private Toolbar mToolbar;
    private ViewPager mViewPager;
    private MediaPagerAdapter mPagerAdapter;

    private static final long FADE_DELAY_MS = 3000;
    private final Handler mFadeHandler = new Handler();

    @Inject MediaStore mMediaStore;
    @Inject FluxCImageLoader mImageLoader;

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
        if (site != null) {
            intent.putExtra(WordPress.SITE, site);
        }

        startIntent(context, intent);
    }

    /**
     * @param context self explanatory
     * @param site optional site this media is associated with
     * @param media media model
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
        if (mediaIdList != null) {
            intent.putStringArrayListExtra(ARG_ID_LIST, mediaIdList);
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
        if (imageUrlList != null) {
            intent.putStringArrayListExtra(ARG_URL_LIST, imageUrlList);
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
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
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
            if (savedInstanceState.containsKey(ARG_ID_LIST)) {
                mMediaIdList = savedInstanceState.getStringArrayList(ARG_ID_LIST);
            } else if (savedInstanceState.containsKey(ARG_URL_LIST)) {
                mMediaUrlList = savedInstanceState.getStringArrayList(ARG_URL_LIST);
            }
        } else {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            mMediaId = getIntent().getIntExtra(MediaPreviewFragment.ARG_MEDIA_ID, 0);
            mContentUri = getIntent().getStringExtra(MediaPreviewFragment.ARG_MEDIA_CONTENT_URI);
            if (getIntent().hasExtra(ARG_ID_LIST)) {
                mMediaIdList = getIntent().getStringArrayListExtra(ARG_ID_LIST);
            } else if (getIntent().hasExtra(ARG_URL_LIST)) {
                mMediaUrlList = getIntent().getStringArrayListExtra(ARG_URL_LIST);
            }
        }

        if (TextUtils.isEmpty(mContentUri)) {
            delayedFinish();
            return;
        }

        mToolbar = findViewById(R.id.toolbar);
        int toolbarColor = ContextCompat.getColor(this, R.color.transparent);
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
        if (mMediaIdList != null) {
            outState.putStringArrayList(ARG_ID_LIST, mMediaIdList);
        }
        if (mMediaUrlList != null) {
            outState.putStringArrayList(ARG_URL_LIST, mMediaUrlList);
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
            fragment = MediaPreviewFragment.newInstance(mSite, mContentUri);
        }
        getFragmentManager().beginTransaction()
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
        if (mMediaIdList != null && mMediaIdList.size() > 1) {
            return PreviewType.MULTI_MEDIA_IDS;
        }
        if (mMediaUrlList != null && mMediaUrlList.size() > 1) {
            return PreviewType.MULTI_IMAGE_URLS;
        }
        return PreviewType.SINGLE_ITEM;
    }

    private void setupViewPager() {
        mPagerAdapter = new MediaPagerAdapter(getFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setPageTransformer(false, new WPViewPagerTransformer(TransformType.SLIDE_OVER));

        // determine the position of the original media item so we can page to it immediately
        int initialPos = 0;
        switch (getPreviewType()) {
            case MULTI_MEDIA_IDS:
                for (int i = 0; i < mMediaIdList.size(); i++) {
                    int thisId = Integer.valueOf(mMediaIdList.get(i));
                    if (thisId == mMediaId) {
                        initialPos = i;
                        break;
                    }
                }
                break;
            case MULTI_IMAGE_URLS:
                for (int i = 0; i < mMediaUrlList.size(); i++) {
                    String thisUrl = mMediaUrlList.get(i);
                    if (StringUtils.equals(thisUrl, mContentUri)) {
                        initialPos = i;
                        break;
                    }
                }
                break;
        }

        mViewPager.setCurrentItem(initialPos);
        mPagerAdapter.unpauseFragment(initialPos);
        mLastPosition = initialPos;

        switch (getPreviewType()) {
            case MULTI_MEDIA_IDS:
                mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        // pause the outgoing fragment and unpause the incoming one - this prevents audio/video from
                        // playing in inactive fragments
                        if (mLastPosition != position) {
                            mPagerAdapter.pauseFragment(mLastPosition);
                        }
                        mPagerAdapter.unpauseFragment(position);
                        mLastPosition = position;
                        mMediaId = Integer.valueOf(mMediaIdList.get(position));
                        // fire event so settings activity shows the same media as this activity (user may have swiped)
                        EventBus.getDefault().post(new MediaPreviewSwiped(mMediaId));
                    }
                });
                break;
            case MULTI_IMAGE_URLS:
                mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        mLastPosition = position;
                        mContentUri = mMediaUrlList.get(position);
                    }
                });
                break;
        }
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
                    int id = Integer.valueOf(mMediaIdList.get(position));
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
                    String imageUrl = mMediaUrlList.get(position);
                    fragment = MediaPreviewFragment.newInstance(null, imageUrl);
                    break;
                default:
                    // should never get here
                    return null;
            }

            fragment.setOnMediaTappedListener(MediaPreviewActivity.this);
            return fragment;
        }

        @Override
        public int getCount() {
            switch (getPreviewType()) {
                case MULTI_MEDIA_IDS:
                    return mMediaIdList.size();
                case MULTI_IMAGE_URLS:
                    return mMediaUrlList.size();
                default:
                    // should never get here
                    return 0;
            }
        }

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
