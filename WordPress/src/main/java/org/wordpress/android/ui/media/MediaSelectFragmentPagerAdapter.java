package org.wordpress.android.ui.media;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.AdapterView;

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
        public static final int CAPTURE_IMAGE = 0x1;
        public static final int CAPTURE_VIDEO = 0x2;
        public static final int DEVICE_IMAGES = 0x4;
        public static final int DEVICE_VIDEOS = 0x8;
        public static final int WP_IMAGES     = 0x10;
        public static final int WP_VIDEOS     = 0x20;

        private int mFilter;

        public MediaSelectTabFragment() {
            super();
        }
    }

    public interface MediaSelectCallback {
        public void onMediaSelected(Object content, boolean selected);

        public void onMediaPageChanged(int position);

        public void onSelectedCleared();
    }

    private Activity mActivity;
    private ViewPager mViewPager;
    private ActionBar mActionBar;
    private List<Class<?>> mTabs;
    private MediaSelectCallback mListener;

    public MediaSelectFragmentPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);

        mTabs = new ArrayList<Class<?>>();
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
    }

    /** Used to add filtered {@link org.wordpress.android.ui.media.MediaSelectFragmentPagerAdapter.MediaSelectTabFragment}'s. */
    public void addTab(int filter, String name) {
        if (filter > 0) {
            ActionBar.Tab newTab = mActionBar.newTab();
            newTab.setText(name);
            newTab.setTag(name);
            newTab.setTabListener(this);
            mTabs.add(MediaSelectTabFragment.class);
            mActionBar.addTab(newTab);
            notifyDataSetChanged();
        }
    }

    /** Used to add custom fragments as tabs. */
    public void addTab(Fragment fragment, String name) {
        if (fragment != null) {
            ActionBar.Tab newTab = mActionBar.newTab();
            newTab.setText(name);
            newTab.setTag(name);
            newTab.setTabListener(this);
            mTabs.add(fragment.getClass());
            mActionBar.addTab(newTab);
            notifyDataSetChanged();
        }
    }

    @Override
    public Fragment getItem(int position) {
        if (position < mTabs.size()) {
            return Fragment.instantiate(mActivity, mTabs.get(position).getName(), null);
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
        if (position < mTabs.size()) mActionBar.setSelectedNavigationItem(position);
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
}
