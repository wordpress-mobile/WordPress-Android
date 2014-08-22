package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;

import org.wordpress.android.R;
import org.wordpress.android.ui.reader.ReaderViewPagerTransformer.TransformType;
import org.wordpress.android.ui.reader.models.ReaderImageList;
import org.wordpress.android.ui.reader.utils.ReaderImageScanner;

import javax.annotation.Nonnull;

/**
 * Full-screen photo viewer
 */
public class ReaderPhotoViewerActivity extends Activity {

    private String mInitialImageUrl;
    private boolean mIsPrivate;
    private String mContent;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader_activity_photo_viewer);

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mViewPager.setPageTransformer(false, new ReaderViewPagerTransformer(TransformType.FLOW));

        if (savedInstanceState != null) {
            mInitialImageUrl = savedInstanceState.getString(ReaderConstants.ARG_IMAGE_URL);
            mIsPrivate = savedInstanceState.getBoolean(ReaderConstants.ARG_IS_PRIVATE);
            mContent = savedInstanceState.getString(ReaderConstants.ARG_CONTENT);
        } else if (getIntent() != null) {
            mInitialImageUrl = getIntent().getStringExtra(ReaderConstants.ARG_IMAGE_URL);
            mIsPrivate = getIntent().getBooleanExtra(ReaderConstants.ARG_IS_PRIVATE, false);
            mContent = getIntent().getStringExtra(ReaderConstants.ARG_CONTENT);
        }

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
                if (!imageList.hasImageUrl(mInitialImageUrl)) {
                    imageList.addImageUrl(0, mInitialImageUrl);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing()) {
                            setImageList(imageList);
                        }
                    }
                });
            }
        }.start();
    }

    private void setImageList(ReaderImageList imageList) {
        PhotoPagerAdapter adapter = new PhotoPagerAdapter(getFragmentManager(), imageList);
        mViewPager.setAdapter(adapter);
        int position = adapter.indexOfImageUrl(mInitialImageUrl);
        if (adapter.isValidPosition(position)) {
            mViewPager.setCurrentItem(position);
        }
    }

    @Override
    public void onSaveInstanceState(@Nonnull Bundle outState) {
        if (mViewPager.getAdapter() != null) {
            PhotoPagerAdapter adapter = (PhotoPagerAdapter) mViewPager.getAdapter();
            String imageUrl = adapter.getImageUrl(mViewPager.getCurrentItem());
            outState.putString(ReaderConstants.ARG_IMAGE_URL, imageUrl);
        }

        outState.putBoolean(ReaderConstants.ARG_IS_PRIVATE, mIsPrivate);
        outState.putString(ReaderConstants.ARG_CONTENT, mContent);
        super.onSaveInstanceState(outState);
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
                    mImageList.get(position), mImageList.isPrivate());
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