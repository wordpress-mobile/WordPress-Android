package org.wordpress.android.ui.stats;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.ui.HorizontalTabView;
import org.wordpress.android.ui.HorizontalTabView.Tab;
import org.wordpress.android.ui.HorizontalTabView.TabListener;

public abstract class StatsAbsListViewFragment extends StatsAbsViewFragment implements TabListener {

    protected ViewPager mViewPager;
    protected HorizontalTabView mTabView;
    protected SparseArray<Fragment> mFragmentMap;
    protected FragmentPagerAdapter mAdapter;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_pager_fragment, container, false);
        
        mFragmentMap = new SparseArray<Fragment>();
        
        mViewPager = (ViewPager) view.findViewById(R.id.stats_pager_viewpager);
        mViewPager.setVisibility(View.VISIBLE);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mTabView.setSelectedTab(position);
            }
        });

        mTabView = (HorizontalTabView) view.findViewById(R.id.stats_pager_tabs);
        mTabView.setVisibility(View.VISIBLE);
        mTabView.setTabListener(this);
        
        mAdapter = getAdapter();
        mViewPager.setAdapter(mAdapter);
        addTabs();
        mTabView.setSelectedTab(0);
        
        return view;
    }
    
    private void addTabs() {
        for (int i = 0; i < mAdapter.getCount(); i++) {
            mTabView.addTab(mTabView.newTab().setText(mAdapter.getPageTitle(i)));
        }
    }

    @Override
    public void onTabSelected(Tab tab) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onPause() {
        removeFragments();
        super.onPause();
    }

    private void removeFragments() {
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        
        for (int i = 0; i < mAdapter.getCount(); i++) {
            Fragment fragment = mFragmentMap.get(i);
            if (fragment != null)
                ft.remove(fragment);
        }
        ft.commit();
        mFragmentMap.clear();
    }
    
    public abstract FragmentPagerAdapter getAdapter();
    
}
