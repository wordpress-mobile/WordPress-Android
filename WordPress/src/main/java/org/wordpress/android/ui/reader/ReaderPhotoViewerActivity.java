package org.wordpress.android.ui.reader;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.reader.ReaderViewPagerTransformer.TransformType;
import org.wordpress.android.ui.reader.models.ReaderImageList;
import org.wordpress.android.ui.reader.utils.ReaderImageScanner;
import org.wordpress.android.ui.reader.views.ReaderPhotoView.PhotoViewListener;
import org.wordpress.android.ui.reader.views.ReaderViewPager;

import javax.annotation.Nonnull;

/**
 * Full-screen photo viewer - uses a ViewPager to enable scrolling between images in a blog
 * post, but also supports viewing a single image
 */
public class ReaderPhotoViewerActivity extends ActionBarActivity
        implements PhotoViewListener {

    private String mInitialImageUrl;
    private boolean mIsPrivate;
    private String mContent;
    private ReaderViewPager mViewPager;
    private PhotoPagerAdapter mAdapter;
    private TextView mTxtTitle;
    private boolean mIsTitleVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reader_activity_photo_viewer);

        mViewPager = (ReaderViewPager) findViewById(R.id.viewpager);
        mTxtTitle = (TextView) findViewById(R.id.text_title);

        // title is hidden until we know we can show it
        mTxtTitle.setVisibility(View.GONE);

        if (savedInstanceState != null) {
            mInitialImageUrl = savedInstanceState.getString(ReaderConstants.ARG_IMAGE_URL);
            mIsPrivate = savedInstanceState.getBoolean(ReaderConstants.ARG_IS_PRIVATE);
            mContent = savedInstanceState.getString(ReaderConstants.ARG_CONTENT);
        } else if (getIntent() != null) {
            mInitialImageUrl = getIntent().getStringExtra(ReaderConstants.ARG_IMAGE_URL);
            mIsPrivate = getIntent().getBooleanExtra(ReaderConstants.ARG_IS_PRIVATE, false);
            mContent = getIntent().getStringExtra(ReaderConstants.ARG_CONTENT);
        }

        mViewPager.setPageTransformer(false, new ReaderViewPagerTransformer(TransformType.FLOW));
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                updateTitle(position);
            }
        });

        mViewPager.setAdapter(getAdapter());
        loadImageList();
    }

    private void loadImageList() {
        final ReaderImageList imageList;
        if (TextUtils.isEmpty(mContent)) {
            // content will be empty when we're viewing a single image
            imageList = new ReaderImageList(mIsPrivate);
        } else {
            // otherwise content is HTML so parse images from it
            imageList = new ReaderImageScanner(mContent, mIsPrivate).getImageList();
        }

        // make sure initial image is in the list
        if (!TextUtils.isEmpty(mInitialImageUrl) && !imageList.hasImageUrl(mInitialImageUrl)) {
            imageList.addImageUrl(0, mInitialImageUrl);
        }

        getAdapter().setImageList(imageList, mInitialImageUrl);
    }

    private PhotoPagerAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new PhotoPagerAdapter(getFragmentManager());
        }
        return mAdapter;
    }

    private boolean hasAdapter() {
        return (mAdapter != null);
    }

    @Override
    public void onSaveInstanceState(@Nonnull Bundle outState) {
        if (hasAdapter()) {
            String imageUrl = getAdapter().getImageUrl(mViewPager.getCurrentItem());
            outState.putString(ReaderConstants.ARG_IMAGE_URL, imageUrl);
        }

        outState.putBoolean(ReaderConstants.ARG_IS_PRIVATE, mIsPrivate);
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

        final String title = getString(R.string.reader_title_photo_viewer, position + 1, getImageCount());
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
            ReaderAnim.fadeOut(mTxtTitle, ReaderAnim.Duration.SHORT);
        } else {
            ReaderAnim.fadeIn(mTxtTitle, ReaderAnim.Duration.SHORT);
        }
        mIsTitleVisible = !mIsTitleVisible;
    }

    @Override
    public void onTapPhotoView() {
        toggleTitle();
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
        public Fragment getItem(int position) {
            return ReaderPhotoViewerFragment.newInstance(
                    mImageList.get(position),
                    mImageList.isPrivate());
        }

        @Override
        public int getCount() {
            return (mImageList != null ? mImageList.size(): 0);
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