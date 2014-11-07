package org.wordpress.android.ui.media;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import org.wordpress.android.R;
import org.wordpress.android.ui.media.content.CaptureMediaContent;
import org.wordpress.android.ui.media.content.DeviceImageMediaContent;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays pages of media content in tabs.
 */

public class MediaSelectFragmentPagerAdapter extends FragmentPagerAdapter
        implements AdapterView.OnItemClickListener, ViewPager.OnPageChangeListener, ActionBar.TabListener {

    /**
     * Helper fragment for easy instantiation of common media import sources. Will handle device sources
     * via MediaStore queries, capturing new media from device camera, and WordPress media assets.
     */
    public static class MediaSelectTabFragment extends Fragment {
        // Bit flags for fragment filters
        public static final String FILTER_ARG = "";
        public static final int CAPTURE_NONE  = 0x00;
        public static final int CAPTURE_IMAGE = 0x01;
        public static final int CAPTURE_VIDEO = 0x02;
        public static final int DEVICE_IMAGES = 0x04;
        public static final int DEVICE_VIDEOS = 0x08;
        public static final int WP_IMAGES     = 0x10;
        public static final int WP_VIDEOS     = 0x20;

        private int mFilter;
        private MediaContentGridView mGridView;

        public MediaSelectTabFragment() {
            super();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Bundle args = getArguments();

            if (args != null) {
                mFilter = args.getInt(FILTER_ARG);
            } else {
                mFilter = CAPTURE_NONE;
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);

            layoutGridView();

            if ((mFilter & CAPTURE_IMAGE) != 0) {
                mGridView.addContent(new CaptureMediaContent(CaptureMediaContent.CAPTURE_TYPE_IMAGE));
            }

            if ((mFilter & CAPTURE_VIDEO) != 0) {
                mGridView.addContent(new CaptureMediaContent(CaptureMediaContent.CAPTURE_TYPE_VIDEO));
            }

            if ((mFilter & DEVICE_IMAGES) != 0) {
                addMediaStoreImages();
            }

            return mGridView;
        }

        private void layoutGridView() {
            Resources resources = getActivity().getResources();
            int numColumns = resources.getInteger(R.integer.media_grid_num_columns);
            int gridPadding = Math.round(resources.getDimension(R.dimen.media_grid_padding));
            int columnSpacingY = Math.round(resources.getDimension(R.dimen.media_grid_column_spacing_vertical));
            int columnSpacingX = Math.round(resources.getDimension(R.dimen.media_grid_column_spacing_horizontal));

            mGridView = new MediaContentGridView(getActivity());
            mGridView.setNumColumns(numColumns);
            mGridView.setVerticalSpacing(columnSpacingY);
            mGridView.setHorizontalSpacing(columnSpacingX);
            mGridView.setPadding(gridPadding, gridPadding, gridPadding, gridPadding);
        }

        private void addMediaStoreImages() {
            // Add device images to the gridview
            Uri thumbUri = MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI;
            String[] thumbColumns = { MediaStore.Images.Thumbnails._ID, MediaStore.Images.Thumbnails.DATA };
            Cursor thumbCursor = MediaStore.Images.Thumbnails.query(getActivity().getContentResolver(), thumbUri, thumbColumns);

            Uri imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String[] imageColumns= { MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA, MediaStore.Images.Media.TITLE };
            Cursor imageCursor = MediaStore.Images.Media.query(getActivity().getContentResolver(), imageUri, imageColumns);

            if (imageCursor.moveToFirst() && thumbCursor.moveToFirst()) {
                List<String> images = new ArrayList<String>();
                do {
                    String id = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.MediaColumns._ID));
                    String data = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
                    String name = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.MediaColumns.TITLE));
                    String thumbnail = thumbCursor.getString(thumbCursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA));

                    if(!images.contains(name)) {
                        DeviceImageMediaContent newContent = new DeviceImageMediaContent(data);
                        newContent.setName(name);

                        Log.i("TEST", "thumbnail:" + thumbnail);
                        newContent.setThumbUri(thumbnail);

                        mGridView.addContent(newContent);
                        images.add(name);
                    }
                } while(imageCursor.moveToNext() && thumbCursor.moveToNext());
            }
        }
    }

    /** Callback for various media selection events. */
    public interface MediaSelectCallback {
        public void onMediaSelected(Object content, boolean selected);

        public void onMediaPageChanged(int position);

        public void onSelectedCleared();
    }

    private class TabInfo {
        public Class<?> classType;
        public Bundle args;

        public TabInfo(Class<?> classType, Bundle args) {
            this.classType = classType;
            this.args = args;
        }
    }

    private Activity mActivity;
    private ViewPager mViewPager;
    private ActionBar mActionBar;
    private List<TabInfo> mTabs;
    private MediaSelectCallback mListener;

    private MediaSelectFragmentPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);

        mTabs = new ArrayList<TabInfo>();
    }

    public MediaSelectFragmentPagerAdapter(Activity activity, ViewPager viewPager, MediaSelectCallback listener) {
        this(activity.getFragmentManager());

        mActivity = activity;
        mListener = listener;

        if ((mActionBar = activity.getActionBar()) != null) {
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }

        mViewPager = viewPager;
        mViewPager.setAdapter(this);
        mViewPager.setOnPageChangeListener(this);

        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    }

    @Override
    public Fragment getItem(int position) {
        if (position < mTabs.size()) {
            TabInfo tab = mTabs.get(position);
            return Fragment.instantiate(mActivity, tab.classType.getName(), tab.args);
        }

        return null;
    }

    @Override
    public int getCount() {
        return mTabs.size();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mListener != null) {
            mListener.onMediaSelected(view, true);
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        if (position < mTabs.size()) mActionBar.setSelectedNavigationItem(position);
        if (mListener != null) mListener.onMediaPageChanged(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        if (mListener != null) mListener.onSelectedCleared();
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
    }

    public void setListener(MediaSelectCallback listener) {
        mListener = listener;
    }

    /** Used to add filtered {@link org.wordpress.android.ui.media.MediaSelectFragmentPagerAdapter.MediaSelectTabFragment}'s. */
    public void addTab(int filter, String tabName) {
        if (filter > 0) {
            Bundle tabArguments = new Bundle();
            tabArguments.putInt(MediaSelectTabFragment.FILTER_ARG, filter);
            addTab(MediaSelectTabFragment.class, tabName, tabArguments);
        }
    }

    /** Used to add custom fragments as tabs. */
    public void addTab(Class<?> fragmentClass, String tabName, Bundle args) {
        if (fragmentClass != null) {
            ActionBar.Tab newTab = mActionBar.newTab();
            newTab.setText(tabName);
            newTab.setTag(args);
            newTab.setTabListener(this);
            mTabs.add(new TabInfo(fragmentClass, args));
            mActionBar.addTab(newTab);
            notifyDataSetChanged();
        }
    }
}
