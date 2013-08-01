package org.wordpress.android.ui.stats;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.ui.HorizontalTabView;
import org.wordpress.android.ui.HorizontalTabView.Tab;
import org.wordpress.android.ui.HorizontalTabView.TabListener;
import org.wordpress.android.ui.stats.Stats.Timeframe;

public class StatsGeoviewsFragment extends StatsAbsViewFragment implements TabListener {

    private ViewPager mViewPager;
    private HorizontalTabView mTabView;
    private CustomPagerAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_pager_fragment, container, false);
        
        mViewPager = (ViewPager) view.findViewById(R.id.stats_pager_viewpager);
        mViewPager.setVisibility(View.VISIBLE);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mTabView.setSelectedTab(position);
            }
        });
                
        mAdapter = new CustomPagerAdapter(getChildFragmentManager());
        mViewPager.setAdapter(mAdapter);
        
        mTabView = (HorizontalTabView) view.findViewById(R.id.stats_pager_tabs);
        mTabView.setVisibility(View.VISIBLE);
        mTabView.setTabListener(this);
        
        addTabs(new Stats.Timeframe[]{ Stats.Timeframe.TODAY, Stats.Timeframe.YESTERDAY });
        mTabView.setSelectedTab(0);
        
        return view;
    }
    
    private void addTabs(Timeframe[] timeframes) {
        for (Timeframe timeframe : timeframes) {
            mTabView.addTab(mTabView.newTab().setText(timeframe.getLabel()));
        }
    }

    @Override
    public void onTabSelected(Tab tab) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    private class CustomPagerAdapter extends FragmentPagerAdapter {

        public CustomPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return new Fragment();
        }

        @Override
        public int getCount() {
            return 2;
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0)
                return Stats.Timeframe.TODAY.getLabel();
            else if (position == 1)
                return Stats.Timeframe.YESTERDAY.getLabel();
            else 
                return ""; 
        }
        
    }


    @Override
    public String getTitle() {
        return getString(R.string.stats_view_views_by_country);
    }

}
