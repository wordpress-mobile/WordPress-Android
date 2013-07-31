package org.wordpress.android.ui.stats;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.ui.HorizontalTabView;
import org.wordpress.android.ui.HorizontalTabView.Tab;
import org.wordpress.android.ui.HorizontalTabView.TabListener;
import org.wordpress.android.ui.stats.Stats.Timeframe;

public class StatsPhoneFragment extends Fragment implements TabListener {

    public static final String TAG = StatsPhoneFragment.class.getSimpleName();
    
    private static final String ARGS_VIEW_TYPE = "ARGS_VIEW_TYPE";
    
    public static StatsPhoneFragment newInstance(Stats.ViewType statsViewType) {
        StatsPhoneFragment fragment = new StatsPhoneFragment();
        
        Bundle args = new Bundle();
        args.putInt(ARGS_VIEW_TYPE, statsViewType.ordinal());
        fragment.setArguments(args);
        
        return fragment;
    }

    private ViewPager mViewPager;
    private HorizontalTabView mTabView;

    private StatsPagerAdapter mAdapter;

    private StatsAbsCategoryFragment mFragment;

    private Stats.ViewType getStatsViewType() {
        return Stats.ViewType.values()[getArguments().getInt(ARGS_VIEW_TYPE)];
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_pager_fragment, container, false);
        
        switch (getStatsViewType()) {
            case TOTALS_FOLLOWERS_AND_SHARES:
                initTotalsFollowersAndShares(view);
                break;
            case TAGS_AND_CATEGORIES:
                initTagsAndCategories(view);
                break;
            default:
                initViewPager(view);
        }
        
        mFragment = (StatsAbsCategoryFragment) getActivity().getSupportFragmentManager().findFragmentByTag(StatsAbsCategoryFragment.TAG);
        
        return view;
    }

    private void initTotalsFollowersAndShares(View view) {
        if (mFragment == null)
            mFragment = new StatsTotalsFollowersAndSharesFragment();
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.stats_pager_container, mFragment, StatsAbsCategoryFragment.TAG);
        ft.commit();
    }

    private void initTagsAndCategories(View view) {
        if (mFragment == null)
            mFragment = StatsAbsCategoryFragment.newInstance(getStatsViewType(), Timeframe.WEEK);
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.stats_pager_container, mFragment, StatsAbsCategoryFragment.TAG);
        ft.commit();
    }
    
    private void initViewPager(View view) {
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
        
        Stats.ViewType viewType = getStatsViewType();
        Stats.Timeframe[] timeframes = getTimeframe(viewType);
        initViewPagerAdapter(viewType, timeframes);
        addTabs(timeframes);
        
        mTabView.setSelectedTab(0);
    }

    private Stats.Timeframe[] getTimeframe(Stats.ViewType viewtype) {
        Stats.Timeframe[] timeframes = new Stats.Timeframe[] { Timeframe.TODAY, Timeframe.YESTERDAY };
        return timeframes;
    }
    
    private void initViewPagerAdapter(Stats.ViewType viewType, Stats.Timeframe[] timeframes) {
        mAdapter = new StatsPagerAdapter(getChildFragmentManager(), viewType, timeframes);
        mViewPager.setAdapter(mAdapter);
    }

    private void addTabs(Stats.Timeframe[] timeframes) {
        for (int i = 0; i < timeframes.length; i++) {
            String title = timeframes[i].getLabel();
            mTabView.addTab(mTabView.newTab().setText(title));
        }
    }

    @Override
    public void onTabSelected(Tab tab) {
        mViewPager.setCurrentItem(tab.getPosition());
    }
    
    public void refresh() {
        // TODO: remove this hack. It's only for refreshing sample data
        Stats.ViewType viewType = getStatsViewType();
        Stats.Timeframe[] timeframes = getTimeframe(viewType);
        initViewPagerAdapter(viewType, timeframes);
    }
}
