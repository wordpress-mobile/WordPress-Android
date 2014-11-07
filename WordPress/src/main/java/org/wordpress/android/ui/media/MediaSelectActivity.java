package org.wordpress.android.ui.media;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.ViewPager;

import org.wordpress.android.R;

/**
 * Allows users to select a variety of media content, videos and images.
 */

public class MediaSelectActivity extends Activity implements MediaSelectFragmentPagerAdapter.MediaSelectCallback {
    public static final int ACTIVITY_REQUEST_CODE_MEDIA_SELECTION = 6000;

    private ViewPager mViewPager;
    private MediaSelectFragmentPagerAdapter mMediaSelectFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewPager = new ViewPager(this);
        mViewPager.setId(R.id.pager);
        setContentView(mViewPager);

        setupTabs();
    }

    @Override
    public void onMediaSelected(Object content, boolean selected) {
        // TODO: update action bar items
    }

    @Override
    public void onMediaPageChanged(int position) {
        // TODO: update action bar items
    }

    @Override
    public void onSelectedCleared() {
        // TODO: update action bar items
    }

    private void setupTabs() {
        mMediaSelectFragment = new MediaSelectFragmentPagerAdapter(this, mViewPager, this);

        // TODO: Check arguments onCreate for filters to add tabs instead of hard-coded here.
        // This will allow users of this Activity to customize the content being displayed.
        int imageFilter = MediaSelectFragmentPagerAdapter.MediaSelectTabFragment.CAPTURE_IMAGE |
                          MediaSelectFragmentPagerAdapter.MediaSelectTabFragment.DEVICE_IMAGES;
        int videoFilter = MediaSelectFragmentPagerAdapter.MediaSelectTabFragment.CAPTURE_VIDEO;
        mMediaSelectFragment.addTab(imageFilter, "Images");
        mMediaSelectFragment.addTab(videoFilter, "Videos");
    }
}
