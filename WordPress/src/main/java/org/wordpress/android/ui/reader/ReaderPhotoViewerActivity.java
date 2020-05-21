package org.wordpress.android.ui.reader;

import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.wordpress.android.R;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.reader.models.ReaderImageList;
import org.wordpress.android.ui.reader.utils.ReaderImageScanner;
import org.wordpress.android.ui.reader.views.ReaderPhotoView.PhotoViewListener;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.widgets.WPViewPager;
import org.wordpress.android.widgets.WPViewPagerTransformer;
import org.wordpress.android.widgets.WPViewPagerTransformer.TransformType;

/**
 * Full-screen photo viewer - uses a ViewPager to enable scrolling between images in a blog
 * post, but also supports viewing a single image
 */
public class ReaderPhotoViewerActivity extends LocaleAwareActivity
        implements PhotoViewListener {
    private String mInitialImageUrl;
    private boolean mIsPrivate;
    private boolean mIsGallery;
    private String mContent;
    private WPViewPager mViewPager;
    private PhotoPagerAdapter mAdapter;
    private TextView mTxtTitle;
    private boolean mIsTitleVisible;
    private Toolbar mToolbar;
    private static final long FADE_DELAY_MS = 3000;
    private final Handler mFadeHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reader_activity_photo_viewer);

        mViewPager = (WPViewPager) findViewById(R.id.viewpager);
        mTxtTitle = (TextView) findViewById(R.id.text_title);

        // title is hidden until we know we can show it
        mTxtTitle.setVisibility(View.GONE);

        if (savedInstanceState != null) {
            mInitialImageUrl = savedInstanceState.getString(ReaderConstants.ARG_IMAGE_URL);
            mIsPrivate = savedInstanceState.getBoolean(ReaderConstants.ARG_IS_PRIVATE);
            mIsGallery = savedInstanceState.getBoolean(ReaderConstants.ARG_IS_GALLERY);
            mContent = savedInstanceState.getString(ReaderConstants.ARG_CONTENT);
        } else if (getIntent() != null) {
            mInitialImageUrl = getIntent().getStringExtra(ReaderConstants.ARG_IMAGE_URL);
            mIsPrivate = getIntent().getBooleanExtra(ReaderConstants.ARG_IS_PRIVATE, false);
            mIsGallery = getIntent().getBooleanExtra(ReaderConstants.ARG_IS_GALLERY, false);
            mContent = getIntent().getStringExtra(ReaderConstants.ARG_CONTENT);
        }

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mViewPager.setPageTransformer(false, new WPViewPagerTransformer(TransformType.FLOW));
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                updateTitle(position);
                showToolbar();
            }
        });

        mViewPager.setAdapter(getAdapter());
        loadImageList();
        showToolbar();
    }

    private void loadImageList() {
        // content will be empty when viewing a single image, otherwise content is HTML
        // so parse images from it
        final ReaderImageList imageList;
        if (TextUtils.isEmpty(mContent)) {
            imageList = new ReaderImageList(mIsPrivate);
        } else {
            int minImageWidth = mIsGallery ? ReaderConstants.MIN_GALLERY_IMAGE_WIDTH : 0;
            imageList = new ReaderImageScanner(mContent, mIsPrivate).getImageList(0, minImageWidth);
        }

        // make sure initial image is in the list
        if (!TextUtils.isEmpty(mInitialImageUrl) && !imageList.hasImageUrl(mInitialImageUrl)) {
            imageList.addImageUrl(0, mInitialImageUrl);
        }

        getAdapter().setImageList(imageList, mInitialImageUrl);
    }

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private PhotoPagerAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new PhotoPagerAdapter(getSupportFragmentManager());
        }
        return mAdapter;
    }

    private boolean hasAdapter() {
        return (mAdapter != null);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (hasAdapter()) {
            String imageUrl = getAdapter().getImageUrl(mViewPager.getCurrentItem());
            outState.putString(ReaderConstants.ARG_IMAGE_URL, imageUrl);
        }

        outState.putBoolean(ReaderConstants.ARG_IS_PRIVATE, mIsPrivate);
        outState.putBoolean(ReaderConstants.ARG_IS_GALLERY, mIsGallery);
        outState.putString(ReaderConstants.ARG_CONTENT, mContent);

        super.onSaveInstanceState(outState);
    }

    private int getImageCount() {
        if (hasAdapter()) {
            return getAdapter().getCount();
        } else {
            return 0;
        }
    }

    private void updateTitle(int position) {
        if (isFinishing() || !canShowTitle()) {
            return;
        }

        String titlePhotoViewer = getString(R.string.reader_title_photo_viewer);
        String title = String.format(titlePhotoViewer, position + 1, getImageCount());
        if (title.equals(mTxtTitle.getText())) {
            return;
        }

        mTxtTitle.setText(title);
    }

    /*
     * title (image count) is only shown if there are multiple images
     */
    private boolean canShowTitle() {
        return (getImageCount() > 1);
    }

    private void toggleTitle() {
        if (isFinishing() || !canShowTitle()) {
            return;
        }

        mTxtTitle.clearAnimation();
        if (mIsTitleVisible) {
            AniUtils.fadeOut(mTxtTitle, AniUtils.Duration.SHORT);
        } else {
            AniUtils.fadeIn(mTxtTitle, AniUtils.Duration.SHORT);
        }
        mIsTitleVisible = !mIsTitleVisible;
    }

    @Override
    public void onTapPhotoView() {
        toggleTitle();
        showToolbar();
    }

    private class PhotoPagerAdapter extends FragmentStatePagerAdapter {
        private ReaderImageList mImageList;

        PhotoPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        void setImageList(ReaderImageList imageList, String initialImageUrl) {
            mImageList = (ReaderImageList) imageList.clone();
            notifyDataSetChanged();

            int position = indexOfImageUrl(initialImageUrl);
            if (isValidPosition(position)) {
                mViewPager.setCurrentItem(position);
                if (canShowTitle()) {
                    mTxtTitle.setVisibility(View.VISIBLE);
                    mIsTitleVisible = true;
                    updateTitle(position);
                } else {
                    mIsTitleVisible = false;
                }
            }
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            // work around "Fragement no longer exists for key" Android bug
            // by catching the IllegalStateException
            // https://code.google.com/p/android/issues/detail?id=42601
            try {
                super.restoreState(state, loader);
            } catch (IllegalStateException e) {
                AppLog.e(AppLog.T.READER, e);
            }
        }

        @Override
        public Fragment getItem(int position) {
            return ReaderPhotoViewerFragment.newInstance(
                    mImageList.get(position),
                    mImageList.isPrivate());
        }

        @Override
        public int getCount() {
            return (mImageList != null ? mImageList.size() : 0);
        }

        private int indexOfImageUrl(String imageUrl) {
            if (mImageList == null) {
                return -1;
            }
            return mImageList.indexOfImageUrl(imageUrl);
        }

        private boolean isValidPosition(int position) {
            return (mImageList != null
                    && position >= 0
                    && position < getCount());
        }

        private String getImageUrl(int position) {
            if (isValidPosition(position)) {
                return mImageList.get(position);
            } else {
                return null;
            }
        }
    }
}
