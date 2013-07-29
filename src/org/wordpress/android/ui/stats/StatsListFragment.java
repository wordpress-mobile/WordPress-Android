package org.wordpress.android.ui.stats;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.ui.HorizontalTabView;
import org.wordpress.android.ui.stats.Stats.Timeframe;

public class StatsListFragment extends Fragment {

    public static final String TAG = StatsListFragment.class.getSimpleName();
    
    private static final String ARGS_VIEW_TYPE = "ARGS_VIEW_TYPE";
    
    public static StatsListFragment newInstance(Stats.ViewType statsViewType) {
        StatsListFragment fragment = new StatsListFragment();
        

        Bundle args = new Bundle();
        args.putInt(ARGS_VIEW_TYPE, statsViewType.ordinal());
        fragment.setArguments(args);
        
        return fragment;
    }

    private ViewPager mViewPager;
    private HorizontalTabView mTabView;
    private Stats.ViewType mStatsViewType;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_pager_fragment, container, false);
        
        mViewPager = (ViewPager) view.findViewById(R.id.stats_viewpager);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mTabView.setSelectedTab(position);
            }
        });
                
        mTabView = (HorizontalTabView) view.findViewById(R.id.stats_tabs);

        refreshViews();   
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
    }


    private Stats.ViewType getStatsViewType() {
        return Stats.ViewType.values()[getArguments().getInt(ARGS_VIEW_TYPE)];
    }
    
    private void refreshViews() {
        mStatsViewType = getStatsViewType();
        switch (mStatsViewType) {
            case VISITORS_AND_VIEWS:
                // do something
            case TOTALS_FOLLOWERS_AND_SHARES:
                // do something
            case TOP_AUTHORS:
            case COMMENTS:
            case TOP_POSTS_AND_PAGES:
            case VIEWS_BY_COUNTRY:
            case REFERRERS:
            case CLICKS:
            case SEARCH_ENGINE_TERMS:
            case TAGS_AND_CATEGORIES:
            case VIDEO_PLAYS:
                // do something
                
        }
        
        Stats.Timeframe[] timeframes = new Stats.Timeframe[] { Timeframe.TODAY, Timeframe.YESTERDAY };
        mViewPager.setAdapter(new StatsPagerAdapter(getChildFragmentManager(), mStatsViewType, timeframes));
        
        for (int i = 0; i < timeframes.length; i++) {
            String title = timeframes[i].getLabel();
            mTabView.addTab(mTabView.newTab().setText(title));
        }
        mTabView.setSelectedTab(0);
    }

    
}
