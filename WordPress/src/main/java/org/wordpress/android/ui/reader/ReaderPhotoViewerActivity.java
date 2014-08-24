package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.reader.ReaderPhotoView.PhotoViewListener;
import org.wordpress.android.ui.reader.ReaderViewPagerTransformer.TransformType;
import org.wordpress.android.ui.reader.models.ReaderImageList;
import org.wordpress.android.ui.reader.utils.ReaderImageScanner;

import javax.annotation.Nonnull;

/**
 * Full-screen photo viewer - uses a ViewPager to enable scrolling between images in a blog
 * post, but also supports viewing a single image
 */
public class ReaderPhotoViewerActivity extends Activity
                                       implements PhotoViewListener {

    private String mInitialImageUrl;
    private boolean mIsPrivate;
    private String mContent;
    private ViewPager mViewPager;
    private TextView mTxtTitle;
    private boolean mIsTitleVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.reader_activity_photo_viewer);

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
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

        loadImageList();
    }

    private void loadImageList() {
        new Thread() {
            @Override
            public void run() {
                // parse list of images from content that was (optionally) passed to
                // this activity, and make sure the list includes the passed url
                ReaderImageScanner scanner = new ReaderImageScanner(mContent, mIsPrivate);
                final ReaderImageList imageList = scanner.getImageList();
                if (!TextUtils.isEmpty(mInitialImageUrl) && !imageList.hasImageUrl(mInitialImageUrl)) {
                    imageList.addImageUrl(0, mInitialImageUrl);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing()) {
                            setImageList(imageList, mInitialImageUrl);
                        }
                    }
                });
            }
        }.start();
    }

    private void setImageList(ReaderImageList imageList, String initialImageUrl) {
        PhotoPagerAdapter adapter = new PhotoPagerAdapter(getFragmentManager(), imageList);
        mViewPager.setAdapter(adapter);

        int position = adapter.indexOfImageUrl(initialImageUrl);
        if (adapter.isValidPosition(position)) {
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

    private PhotoPagerAdapter getPageAdapter() {
        if (mViewPager == null || mViewPager.getAdapter() == null) {
            return null;
        }
        return (PhotoPagerAdapter) mViewPager.getAdapter();
    }

    @Override
    public void onSaveInstanceState(@Nonnull Bundle outState) {
        PhotoPagerAdapter adapter = getPageAdapter();
        if (adapter != null) {
            String imageUrl = adapter.getImageUrl(mViewPager.getCurrentItem());
            outState.putString(ReaderConstants.ARG_IMAGE_URL, imageUrl);
        }

        outState.putBoolean(ReaderConstants.ARG_IS_PRIVATE, mIsPrivate);
        outState.putString(ReaderConstants.ARG_CONTENT, mContent);

        super.onSaveInstanceState(outState);
    }

    private int getImageCount() {
        PhotoPagerAdapter adapter = getPageAdapter();
        if (adapter != null) {
            return adapter.getCount();
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
        private final ReaderImageList mImageList;

        PhotoPagerAdapter(FragmentManager fm, ReaderImageList imageList) {
            super(fm);
            if (imageList != null) {
                mImageList = (ReaderImageList) imageList.clone();
            } else {
                mImageList = new ReaderImageList(mIsPrivate);
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
            return mImageList.size();
        }

        private int indexOfImageUrl(String imageUrl) {
            return mImageList.indexOfImageUrl(imageUrl);
        }

        private boolean isValidPosition(int position) {
            return (position >= 0 && position < getCount());
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