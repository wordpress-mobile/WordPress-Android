package org.wordpress.android.ui.media;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;

import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.widgets.SlidingTabLayout;
import org.wordpress.android.widgets.WPViewPager;
import org.wordpress.mediapicker.MediaItem;
import org.wordpress.mediapicker.source.MediaSource;
import org.wordpress.mediapicker.MediaPickerFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Allows users to select a variety of videos and images from any source.
 *
 * Title can be set either by defining a string resource, R.string.media_picker_title, or passing
 * a String extra in the {@link android.content.Intent} with the key ACTIVITY_TITLE_KEY.
 *
 * Accepts Image and Video sources as arguments and displays each in a tab.
 *  - Use DEVICE_IMAGE_MEDIA_SOURCES_KEY with a {@link java.util.List} of {@link org.wordpress.mediapicker.source.MediaSource}'s to pass image sources via the Intent
 *  - Use DEVICE_VIDEO_MEDIA_SOURCES_KEY with a {@link java.util.List} of {@link org.wordpress.mediapicker.source.MediaSource}'s to pass video sources via the Intent
 */

public class MediaPickerActivity extends ActionBarActivity
                              implements MediaPickerFragment.OnMediaSelected {
    /**
     * Request code for the {@link android.content.Intent} to start media selection.
     */
    public static final int ACTIVITY_REQUEST_CODE_MEDIA_SELECTION = 6000;
    /**
     * Result code signaling that media has been selected.
     */
    public static final int ACTIVITY_RESULT_CODE_MEDIA_SELECTED   = 6001;
    /**
     * Result code signaling that a gallery should be created with the results.
     */
    public static final int ACTIVITY_RESULT_CODE_GALLERY_CREATED  = 6002;

    /**
     * Pass a {@link String} with this key in the {@link android.content.Intent} to set the title.
     */
    public static final String ACTIVITY_TITLE_KEY           = "activity-title";
    /**
     * Pass an {@link java.util.ArrayList} of {@link org.wordpress.mediapicker.source.MediaSource}'s
     * in the {@link android.content.Intent} to set image sources for selection.
     */
    public static final String DEVICE_IMAGE_MEDIA_SOURCES_KEY = "device-image-media-sources";
    /**
     * Pass an {@link java.util.ArrayList} of {@link org.wordpress.mediapicker.source.MediaSource}'s
     * in the {@link android.content.Intent} to set video sources for selection.
     */
    public static final String DEVICE_VIDEO_MEDIA_SOURCES_KEY = "device- video=media-sources";
    public static final String BLOG_IMAGE_MEDIA_SOURCES_KEY = "blog-image-media-sources";
    public static final String BLOG_VIDEO_MEDIA_SOURCES_KEY = "blog-video-media-sources";
    /**
     * Key to extract the {@link java.util.ArrayList} of {@link org.wordpress.mediapicker.MediaItem}'s
     * that were selected by the user.
     */
    public static final String SELECTED_CONTENT_RESULTS_KEY = "selected-content";

    private static final String CAPTURE_PATH_KEY = "capture-path";

    private static final long   TAB_ANIMATION_DURATION_MS = 250l;

    private MediaPickerAdapter     mMediaPickerAdapter;
    private ArrayList<MediaSource>[] mMediaSources;
    private SlidingTabLayout       mTabLayout;
    private WPViewPager            mViewPager;
    private ActionMode             mActionMode;
    private String                 mCapturePath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        lockRotation();
        addMediaSources();
        setTitle();
        initializeContentView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.media_picker, menu);

        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(CAPTURE_PATH_KEY, mCapturePath);
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(CAPTURE_PATH_KEY)) {
            mCapturePath = savedInstanceState.getString(CAPTURE_PATH_KEY);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getItemId() == R.id.capture_image) {
            MediaUtils.launchCamera(this, new MediaUtils.LaunchCameraCallback() {
                @Override
                public void onMediaCapturePathReady(String mediaCapturePath) {
                    mCapturePath = mediaCapturePath;
                }
            });
            return true;
        } else if (item.getItemId() == R.id.capture_video) {
            MediaUtils.launchVideoCamera(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case MediaUtils.RequestCode.ACTIVITY_REQUEST_CODE_TAKE_PHOTO:
                File file = new File(mCapturePath);
                Uri imageUri = Uri.fromFile(file);

                if (file.exists() && MediaUtils.isValidImage(imageUri.toString())) {
                    // Notify MediaStore of new content
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, imageUri));

                    MediaItem newImage = new MediaItem();
                    newImage.setSource(imageUri);
                    newImage.setPreviewSource(imageUri);
                    ArrayList<MediaItem> imageResult = new ArrayList<>();
                    imageResult.add(newImage);
                    finishWithResults(imageResult, ACTIVITY_RESULT_CODE_MEDIA_SELECTED);
                }
                break;
            case MediaUtils.RequestCode.ACTIVITY_REQUEST_CODE_TAKE_VIDEO:
                Uri videoUri = data != null ? data.getData() : null;

                if (videoUri != null) {
                    // Notify MediaStore of new content
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, videoUri));

                    MediaItem newVideo = new MediaItem();
                    newVideo.setSource(videoUri);
                    newVideo.setPreviewSource(videoUri);
                    ArrayList<MediaItem> videoResult = new ArrayList<>();
                    videoResult.add(newVideo);
                    finishWithResults(videoResult, ACTIVITY_RESULT_CODE_MEDIA_SELECTED);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (mActionMode != null) {
            mActionMode.finish();
        } else {
            finishWithResults(null, ACTIVITY_RESULT_CODE_MEDIA_SELECTED);
            super.onBackPressed();
        }
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);

        mViewPager.setPagingEnabled(false);
        mActionMode = mode;

        animateTabGone();
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        super.onActionModeFinished(mode);

        mViewPager.setPagingEnabled(true);
        mActionMode = null;

        animateTabAppear();
    }

    /*
        OnMediaSelected interface
     */

    @Override
    public void onMediaSelectionStarted() {
    }

    @Override
    public void onMediaSelected(MediaItem mediaContent, boolean selected) {
    }

    @Override
    public void onMediaSelectionConfirmed(ArrayList<MediaItem> mediaContent) {
        if (mediaContent != null) {
            finishWithResults(mediaContent, ACTIVITY_RESULT_CODE_MEDIA_SELECTED);
        } else {
            finish();
        }
    }

    @Override
    public void onMediaSelectionCancelled() {
    }

    @Override
    public boolean onMenuItemSelected(MenuItem menuItem, ArrayList<MediaItem> selectedContent) {
        if (menuItem.getItemId() == R.id.menu_media_content_selection_gallery) {
            finishWithResults(selectedContent, ACTIVITY_RESULT_CODE_GALLERY_CREATED);
        }

        return false;
    }

    @Override
    public ImageLoader.ImageCache getImageCache() {
        return WordPress.getBitmapCache();
    }

    /**
     * Finishes the activity after the user has confirmed media selection.
     *
     * @param results
     *  list of selected media items
     */
    private void finishWithResults(ArrayList<MediaItem> results, int resultCode) {
        Intent result = new Intent();
        result.putParcelableArrayListExtra(SELECTED_CONTENT_RESULTS_KEY, results);
        setResult(resultCode, result);
        finish();
    }

    /**
     * Helper method; sets title to R.string.media_picker_title unless intent defines one
     */
    private void setTitle() {
        final Intent intent = getIntent();

        if (intent != null && intent.hasExtra(ACTIVITY_TITLE_KEY)) {
            String activityTitle = intent.getStringExtra(ACTIVITY_TITLE_KEY);
            setTitle(activityTitle);
        } else {
            setTitle(getString(R.string.media_picker_title));
        }
    }

    /**
     * Helper method; gathers {@link org.wordpress.mediapicker.source.MediaSource}'s from intent
     */
    private void addMediaSources() {
        final Intent intent = getIntent();

        if (intent != null) {
            mMediaSources = new ArrayList[4];

            List<MediaSource> mediaSources = intent.getParcelableArrayListExtra(DEVICE_IMAGE_MEDIA_SOURCES_KEY);
            if (mediaSources != null) {
                mMediaSources[0] = new ArrayList<>();
                mMediaSources[0].add(new MediaSourceDeviceImages(getContentResolver()));
            }

            mediaSources = intent.getParcelableArrayListExtra(DEVICE_VIDEO_MEDIA_SOURCES_KEY);
            if (mediaSources != null) {
                mMediaSources[1] = new ArrayList<>();
                mMediaSources[1].addAll(mediaSources);
            }

            mediaSources = intent.getParcelableArrayListExtra(BLOG_IMAGE_MEDIA_SOURCES_KEY);
            if (mediaSources != null) {
                mMediaSources[2] = new ArrayList<>();
                mMediaSources[2].addAll(mediaSources);
            }

            mediaSources = intent.getParcelableArrayListExtra(BLOG_VIDEO_MEDIA_SOURCES_KEY);
            if (mediaSources != null) {
                mMediaSources[3] = new ArrayList<>();
                mMediaSources[3].addAll(mediaSources);
            }
        }
    }

    /**
     * Helper method; locks device orientation to its current state while media is being selected
     */
    private void lockRotation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_0:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case Surface.ROTATION_90:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case Surface.ROTATION_180:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
            case Surface.ROTATION_270:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
        }
    }

    /**
     * Helper method; sets up the tab bar, media adapter, and ViewPager for displaying media content
     */
    private void initializeContentView() {
        setContentView(R.layout.activity_media_picker);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setElevation(0);
            actionBar.show();
        }

        mMediaPickerAdapter = new MediaPickerAdapter(getFragmentManager());
        mTabLayout = (SlidingTabLayout) findViewById(R.id.media_picker_tabs);
        mViewPager = (WPViewPager) findViewById(R.id.media_picker_pager);

        if (mViewPager != null) {
            mViewPager.setPagingEnabled(true);

            mMediaPickerAdapter.addTab(mMediaSources[0] != null ? mMediaSources[0] : new ArrayList<MediaSource>(), getResources().getString(R.string.tab_title_device_images));
            mMediaPickerAdapter.addTab(mMediaSources[1] != null ? mMediaSources[1] : new ArrayList<MediaSource>(), getResources().getString(R.string.tab_title_device_videos));
            mMediaPickerAdapter.addTab(mMediaSources[2] != null ? mMediaSources[2] : new ArrayList<MediaSource>(), getResources().getString(R.string.tab_title_site_images));
            mMediaPickerAdapter.addTab(mMediaSources[3] != null ? mMediaSources[3] : new ArrayList<MediaSource>(), getResources().getString(R.string.tab_title_site_videos));

            mViewPager.setAdapter(mMediaPickerAdapter);

            if (mTabLayout != null) {
                mTabLayout.setCustomTabView(R.layout.tab_text, R.id.text_tab);
                mTabLayout.setDistributeEvenly(true);
                mTabLayout.setViewPager(mViewPager);
            }
        }
    }

    /**
     * Helper method; animates the tab bar and ViewPager in when ActionMode ends
     */
    private void animateTabAppear() {
        TranslateAnimation tabAppearAnimation = new TranslateAnimation(0, 0, -mTabLayout.getHeight(), 0);
        TranslateAnimation pagerAppearAnimation = new TranslateAnimation(0, 0, -mTabLayout.getHeight(), 0);

        tabAppearAnimation.setDuration(TAB_ANIMATION_DURATION_MS);
        pagerAppearAnimation.setDuration(TAB_ANIMATION_DURATION_MS);
        pagerAppearAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mViewPager.getWidth(), mViewPager.getHeight() - mTabLayout.getHeight());
                mViewPager.setLayoutParams(params);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        mTabLayout.setVisibility(View.VISIBLE);
        mViewPager.startAnimation(pagerAppearAnimation);
        mTabLayout.startAnimation(tabAppearAnimation);
    }

    /**
     * Helper method; animates the tab bar and ViewPager out when ActionMode begins
     */
    private void animateTabGone() {
        TranslateAnimation tabGoneAnimation = new TranslateAnimation(0, 0, 0, -mTabLayout.getHeight());
        TranslateAnimation pagerGoneAnimation = new TranslateAnimation(0, 0, 0, -mTabLayout.getHeight());
        tabGoneAnimation.setDuration(TAB_ANIMATION_DURATION_MS);
        pagerGoneAnimation.setDuration(TAB_ANIMATION_DURATION_MS);
        tabGoneAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mTabLayout.setVisibility(View.GONE);
                mTabLayout.clearAnimation();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        pagerGoneAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                LinearLayout.LayoutParams newParams = new LinearLayout.LayoutParams(mViewPager.getWidth(), mViewPager.getHeight() + mTabLayout.getHeight());
                mViewPager.setLayoutParams(newParams);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mViewPager.clearAnimation();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        mTabLayout.startAnimation(tabGoneAnimation);
        mViewPager.startAnimation(pagerGoneAnimation);
    }

    /**
     * Shows {@link org.wordpress.mediapicker.MediaPickerFragment}'s in a tabbed layout.
     */
    public class MediaPickerAdapter extends FragmentPagerAdapter {
        private class MediaPicker {
            public String pickerTitle;
            public ArrayList<MediaSource> mediaSources;

            public MediaPicker(String name, ArrayList<MediaSource> sources) {
                pickerTitle = name;
                mediaSources = sources;
            }
        }

        private final List<MediaPicker> mMediaPickers;

        private MediaPickerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);

            mMediaPickers = new ArrayList<>();
        }

        @Override
        public Fragment getItem(int position) {
            if (position < mMediaPickers.size()) {
                MediaPicker mediaPicker = mMediaPickers.get(position);
                MediaPickerFragment fragment = new MediaPickerFragment();
                fragment.setMediaSources(mediaPicker.mediaSources);

                return fragment;
            }

            return null;
        }

        @Override
        public int getCount() {
            return mMediaPickers.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mMediaPickers.get(position).pickerTitle;
        }

        public void addTab(ArrayList<MediaSource> mediaSources, String tabName) {
            mMediaPickers.add(new MediaPicker(tabName, mediaSources));
        }
    }
}
