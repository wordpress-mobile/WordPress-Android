package org.wordpress.android.ui.media;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.widgets.WPViewPager;

import java.util.ArrayList;

/**
 * Allows users to select a variety of media content, videos and images.
 *
 * When starting this Activity for a result (such as in the Post Editor) add PARAMETER_REQUEST_KEY=true to the intent.
 * This will toggle use of the NavigationDrawer for navigation.
 *
 * The default behavior of the tabs in this Activity is to display all media content in two tabs, Images and Videos.
 * To override this behavior (such as on the Media page) simply add PARAMETER_TAB_CONFIG=[config] when starting the
 * Activity. The config specifies a list of tabs to display as well as the content to show in each tab. For example,
 *      PARAMETER_TAB_CONFIG=["WordPress Images;WP_IMAGES","Device Videos;DEVICE_VIDEOS"]
 * will display two tabs, 'WordPress Images' containing WordPress library images and 'Device Videos' containing videos
 * on the users device.
 */

public class MediaSelectActivity extends WPActionBarActivity implements MediaContentTabFragment.OnMediaContentSelected {
    public static final int    ACTIVITY_REQUEST_CODE_MEDIA_SELECTION = 6000;
    public static final String PARAMETER_REQUEST_KEY                 = "requestCode";
    public static final String PARAMETER_TAB_CONFIG                  = "tabConfiguration";
    public static final String FILTER_CAPTURE_IMAGE                  = "CAPTURE_IMAGE";
    public static final String FILTER_CAPTURE_VIDEO                  = "CAPTURE_VIDEO";
    public static final String FILTER_DEVICE_IMAGES                  = "DEVICE_IMAGES";
    public static final String FILTER_DEVICE_VIDEOS                  = "DEVICE_VIDEOS";
    public static final String FILTER_WP_IMAGES                      = "WP_IMAGES";
    public static final String FILTER_WP_VIDEOS                      = "WP_VIDEOS";
    public static final String SELECTED_CONTENT_RESULTS_KEY          = "selected_content";

    private boolean                          mStartedForResult;
    private WPViewPager                      mViewPager;
    private MediaContentFragmentPagerAdapter mMediaSelectFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mStartedForResult = intent != null && intent.getBooleanExtra(PARAMETER_REQUEST_KEY, false);

        initializeContentView();
        intializeTabs(intent);
    }

    @Override
    public void onBackPressed() {
        if (mStartedForResult) {
            finishWithNoResults();
        } else {
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager.getBackStackEntryCount() > 0) {
                fragmentManager.popBackStack();
            }
        }

        super.onBackPressed();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (mStartedForResult && item.getItemId() == android.R.id.home) {
            finishWithNoResults();
            return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onMediaContentSelectionStarted() {
    }

    @Override
    public void onMediaContentSelected(MediaContent mediaContent, boolean selected) {
    }

    @Override
    public void onMediaContentSelectionConfirmed(ArrayList<MediaContent> mediaContent) {
        if (mStartedForResult) {
            Intent result = new Intent();
            result.putParcelableArrayListExtra(SELECTED_CONTENT_RESULTS_KEY, mediaContent);

            setResult(ACTIVITY_REQUEST_CODE_MEDIA_SELECTION, result);
            finish();
        }
    }

    @Override
    public void onMediaContentSelectionCancelled() {
    }

    /** Helper method to conditionally initialize view depending on intended use of the class. */
    private void initializeContentView() {
        mViewPager = new WPViewPager(this, null);
        mViewPager.setPagingEnabled(true);
        mViewPager.setId(R.id.pager);

        if (mStartedForResult) {
            setContentView(mViewPager);
        }
        else {
            createMenuDrawer(mViewPager);
        }
    }

    /** Helper method to conditionally setup the tabs based on Intent parameter. */
    private void intializeTabs(Intent intent) {
        if (intent == null) {
            setupTabs(null);
        }
        else {
            setupTabs(intent.getStringArrayExtra(PARAMETER_TAB_CONFIG));
        }
    }

    /** Helper method to add tabs for Images and Videos. */
    private void setupTabs(String[] tabConfiguration) {
        mMediaSelectFragment = new MediaContentFragmentPagerAdapter(this, mViewPager);

        if (tabConfiguration == null) {
            // Default configuration shows all filters
            int imageFilter = MediaContentTabFragment.CAPTURE_IMAGE |
                              MediaContentTabFragment.DEVICE_IMAGES |
                              MediaContentTabFragment.WP_IMAGES;
            int videoFilter = MediaContentTabFragment.CAPTURE_VIDEO |
                              MediaContentTabFragment.DEVICE_VIDEOS |
                              MediaContentTabFragment.WP_VIDEOS;
            mMediaSelectFragment.addTab(imageFilter, "Images");
            mMediaSelectFragment.addTab(videoFilter, "Videos");
        } else {
            for (String tab : tabConfiguration) {
                String tabName = tab.substring(0, tab.indexOf(';'));
                int tabFilter = filterFromString(tab.substring(tab.indexOf(';') + 1, tab.length()));
                mMediaSelectFragment.addTab(tabFilter, tabName);
            }
        }
    }

    /** Helper method to convert a String parameter in an intent to a filter for the tab fragment. */
    private int filterFromString(String filterString) {
        String[] filterContent = filterString.split("\\|");
        int filter = 0;

        for (String part : filterContent) {
            if (FILTER_CAPTURE_IMAGE.equals(part)) {
                filter |= MediaContentTabFragment.CAPTURE_IMAGE;
            } else if (FILTER_CAPTURE_VIDEO.equals(part)) {
                filter |= MediaContentTabFragment.CAPTURE_VIDEO;
            } else if (FILTER_DEVICE_IMAGES.equals(part)) {
                filter |= MediaContentTabFragment.DEVICE_IMAGES;
            } else if (FILTER_DEVICE_VIDEOS.equals(part)) {
                filter |= MediaContentTabFragment.DEVICE_VIDEOS;
            } else if (FILTER_WP_IMAGES.equals(part)) {
                filter |= MediaContentTabFragment.WP_IMAGES;
            } else if (FILTER_WP_VIDEOS.equals(part)) {
                filter |= MediaContentTabFragment.WP_VIDEOS;
            }
        }

        return filter;
    }

    /** Helper method to end the activity with a result code but no data. */
    private void finishWithNoResults() {
        setResult(ACTIVITY_REQUEST_CODE_MEDIA_SELECTION);
        finish();
    }
}
