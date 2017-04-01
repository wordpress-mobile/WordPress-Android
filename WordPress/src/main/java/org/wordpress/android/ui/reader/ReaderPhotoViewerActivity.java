package org.wordpress.android.ui.reader;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import org.wordpress.android.R;
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
public class ReaderPhotoViewerActivity extends AppCompatActivity
        implements PhotoViewListener {

    private String mInitialImageUrl;
    private boolean mPrivateEh;
    private boolean mGalleryEh;
    private String mContent;
    private WPViewPager mViewPager;
    private PhotoPagerAdapter mAdapter;
    private TextView mTxtTitle;
    private boolean mTitleVisibleEh;

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
            mPrivateEh = savedInstanceState.getBoolean(ReaderConstants.ARG_IS_PRIVATE);
            mGalleryEh = savedInstanceState.getBoolean(ReaderConstants.ARG_IS_GALLERY);
            mContent = savedInstanceState.getString(ReaderConstants.ARG_CONTENT);
        } else if (getIntent() != null) {
            mInitialImageUrl = getIntent().getStringExtra(ReaderConstants.ARG_IMAGE_URL);
            mPrivateEh = getIntent().getBooleanExtra(ReaderConstants.ARG_IS_PRIVATE, false);
            mGalleryEh = getIntent().getBooleanExtra(ReaderConstants.ARG_IS_GALLERY, false);
            mContent = getIntent().getStringExtra(ReaderConstants.ARG_CONTENT);
        }

        mViewPager.setPageTransformer(false, new WPViewPagerTransformer(TransformType.FLOW));
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                updateTitle(position);
            }
        });

        mViewPager.setAdapter(getAdapter());
        loadImageList();
    }

    private void loadImageList() {
        // content will be empty when viewing a single image, otherwise content is HTML
        // so parse images from it
        final ReaderImageList imageList;
        if (TextUtils.isEmpty(mContent)) {
            imageList = new ReaderImageList(mPrivateEh);
        } else {
            int minImageWidth = mGalleryEh ? ReaderConstants.MIN_GALLERY_IMAGE_WIDTH : 0;
            imageList = new ReaderImageScanner(mContent, mPrivateEh).getImageList(0, minImageWidth);
        }

        // make sure initial image is in the list
        if (!TextUtils.isEmpty(mInitialImageUrl) && !imageList.imageUrlEh(mInitialImageUrl)) {
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

    private boolean adapterEh() {
        return (mAdapter != null);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (adapterEh()) {
            String imageUrl = getAdapter().getImageUrl(mViewPager.getCurrentItem());
            outState.putString(ReaderConstants.ARG_IMAGE_URL, imageUrl);
        }

        outState.putBoolean(ReaderConstants.ARG_IS_PRIVATE, mPrivateEh);
        outState.putBoolean(ReaderConstants.ARG_IS_GALLERY, mGalleryEh);
        outState.putString(ReaderConstants.ARG_CONTENT, mContent);

        super.onSaveInstanceState(outState);
    }

    private int getImageCount() {
        if (adapterEh()) {
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
        if (mTitleVisibleEh) {
            AniUtils.fadeOut(mTxtTitle, AniUtils.Duration.SHORT);
        } else {
            AniUtils.fadeIn(mTxtTitle, AniUtils.Duration.SHORT);
        }
        mTitleVisibleEh = !mTitleVisibleEh;
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
            if (validPositionEh(position)) {
                mViewPager.setCurrentItem(position);
                if (canShowTitle()) {
                    mTxtTitle.setVisibility(View.VISIBLE);
                    mTitleVisibleEh = true;
                    updateTitle(position);
                } else {
                    mTitleVisibleEh = false;
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
                    mImageList.privateEh());
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

        private boolean validPositionEh(int position) {
            return (mImageList != null
                    && position >= 0
                    && position < getCount());
        }

        private String getImageUrl(int position) {
            if (validPositionEh(position)) {
                return mImageList.get(position);
            } else {
                return null;
            }
        }
    }
}
