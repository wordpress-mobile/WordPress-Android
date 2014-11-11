package org.wordpress.android.ui.media;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.ui.media.content.MediaContent;
import org.wordpress.android.widgets.WPViewPager;

import java.util.ArrayList;

/**
 * Allows users to select a variety of media content, videos and images.
 */

public class MediaSelectActivity extends Activity implements MediaContentTabFragment.OnMediaContentSelected {
    public static final int    ACTIVITY_REQUEST_CODE_MEDIA_SELECTION = 6000;
    public static final String SELECTED_CONTENT_RESULTS_KEY          = "selected_content";

    private WPViewPager                      mViewPager;
    private MediaContentFragmentPagerAdapter mMediaSelectFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewPager = new WPViewPager(this, null);
        mViewPager.setPagingEnabled(true);
        mViewPager.setId(R.id.pager);
        setContentView(mViewPager);

        setupTabs();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        finishWithNoResults();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
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
        Intent result = new Intent();
        result.putParcelableArrayListExtra(SELECTED_CONTENT_RESULTS_KEY, mediaContent);

        setResult(ACTIVITY_REQUEST_CODE_MEDIA_SELECTION, result);
        finish();
    }

    @Override
    public void onMediaContentSelectionCancelled() {
    }

    /** Helper method to add tabs for Images and Videos. */
    private void setupTabs() {
        mMediaSelectFragment = new MediaContentFragmentPagerAdapter(this, mViewPager);

        // TODO: Check arguments onCreate for filters to add tabs instead of hard-coded here.
        // This will allow users of this Activity to customize the content being displayed.
        int imageFilter = MediaContentTabFragment.CAPTURE_IMAGE |
                          MediaContentTabFragment.DEVICE_IMAGES |
                          MediaContentTabFragment.WP_IMAGES;
        int videoFilter = MediaContentTabFragment.CAPTURE_VIDEO |
                          MediaContentTabFragment.DEVICE_VIDEOS |
                          MediaContentTabFragment.WP_VIDEOS;
        mMediaSelectFragment.addTab(imageFilter, "Images");
        mMediaSelectFragment.addTab(videoFilter, "Videos");
    }

    /** Helper method to end the activity with a result code but no data. */
    private void finishWithNoResults() {
        setResult(ACTIVITY_REQUEST_CODE_MEDIA_SELECTION);
        finish();
    }
}
