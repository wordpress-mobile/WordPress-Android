package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.reader.ReaderViewPagerTransformer.TransformType;
import org.wordpress.android.ui.reader.models.ReaderImageList;
import org.wordpress.android.ui.reader.utils.ReaderImageScanner;
import org.wordpress.android.util.ToastUtils;

import javax.annotation.Nonnull;

/**
 * Full-screen photo viewer
 */
public class ReaderPhotoViewerActivity extends Activity
                                       implements ReaderPhotoViewerFragment.ReaderPhotoListener {

    private String mInitialImageUrl;
    private boolean mIsPrivate;
    private String mContent;
    private ViewPager mViewPager;
    private TextView mTxtTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.reader_activity_photo_viewer);

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mViewPager.setPageTransformer(false, new ReaderViewPagerTransformer(TransformType.FLOW));

        mTxtTitle = (TextView) findViewById(R.id.text_title);

        if (savedInstanceState != null) {
            mInitialImageUrl = savedInstanceState.getString(ReaderConstants.ARG_IMAGE_URL);
            mIsPrivate = savedInstanceState.getBoolean(ReaderConstants.ARG_IS_PRIVATE);
            mContent = savedInstanceState.getString(ReaderConstants.ARG_CONTENT);
        } else if (getIntent() != null) {
            mInitialImageUrl = getIntent().getStringExtra(ReaderConstants.ARG_IMAGE_URL);
            mIsPrivate = getIntent().getBooleanExtra(ReaderConstants.ARG_IS_PRIVATE, false);
            mContent = getIntent().getStringExtra(ReaderConstants.ARG_CONTENT);
        }

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                updateTitle(position);
                showTitle();
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
                if (!imageList.hasImageUrl(mInitialImageUrl)) {
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
            updateTitle(position);
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
        if (mViewPager.getAdapter() != null) {
            PhotoPagerAdapter adapter = (PhotoPagerAdapter) mViewPager.getAdapter();
            String imageUrl = adapter.getImageUrl(mViewPager.getCurrentItem());
            outState.putString(ReaderConstants.ARG_IMAGE_URL, imageUrl);
        }

        outState.putBoolean(ReaderConstants.ARG_IS_PRIVATE, mIsPrivate);
        outState.putString(ReaderConstants.ARG_CONTENT, mContent);
        super.onSaveInstanceState(outState);
    }

    private boolean isActivePosition(int position) {
        return (mViewPager != null && mViewPager.getCurrentItem() == position);
    }

    @Override
    public void onTapPhoto(int position) {
        showTitle();
    }

    @Override
    public void onTapOutsidePhoto(int position) {
        showTitle();
    }

    @Override
    public void onPhotoLoaded(int position) {
        // show title after first photo loads
        if (position == 0) {
            showTitle();
        }
    }

    @Override
    public void onPhotoFailed(int position) {
        if (isActivePosition(position)) {
            ToastUtils.showToast(this, R.string.reader_toast_err_view_image, ToastUtils.Duration.LONG);
        }
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
        if (isFinishing()) {
            return;
        }

        // only show count if there's more than one image
        int numImages = getImageCount();
        if (numImages <= 1) {
            return;
        }

        String title = getString(R.string.reader_title_photo_viewer, position + 1, numImages);
        mTxtTitle.setText(title);
    }

    private boolean isTitleShowing() {
        return (mTxtTitle.getVisibility() == View.VISIBLE);
    }

    private void showTitle() {
        if (isFinishing()) {
            return;
        }

        mTxtTitle.clearAnimation();
        if (isTitleShowing()) {
            ReaderAnim.fadeOut(mTxtTitle, ReaderAnim.Duration.MEDIUM);
        } else {
            ReaderAnim.fadeInFadeOut(mTxtTitle, ReaderAnim.Duration.MEDIUM);
        }
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
                    position,
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