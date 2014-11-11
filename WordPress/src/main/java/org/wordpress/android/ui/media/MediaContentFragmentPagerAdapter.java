package org.wordpress.android.ui.media;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays pages of media content in tabs.
 */

public class MediaContentFragmentPagerAdapter extends FragmentPagerAdapter
        implements AdapterView.OnItemClickListener,
                   ViewPager.OnPageChangeListener,
                   ActionBar.TabListener {

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

    private MediaContentFragmentPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);

        mTabs = new ArrayList<TabInfo>();
    }

    public MediaContentFragmentPagerAdapter(Activity activity, ViewPager viewPager) {
        this(activity.getFragmentManager());

        mActivity = activity;

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
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        if (position < mTabs.size()) {
            mActionBar.setSelectedNavigationItem(position);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
    }

    /** Used to add filtered {@link org.wordpress.android.ui.media.MediaContentTabFragment}'s. */
    public void addTab(int filter, String tabName) {
        if (filter > 0) {
            Bundle tabArguments = new Bundle();
            tabArguments.putInt(MediaContentTabFragment.FILTER_ARG, filter);
            addTab(MediaContentTabFragment.class, tabName, tabArguments);
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
