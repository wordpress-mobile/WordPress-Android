package org.wordpress.android.ui.stats;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import org.wordpress.android.ui.stats.Stats.Category;

public abstract class StatsAbsCategoryFragment extends Fragment {

    public static final String TAG = StatsAbsCategoryFragment.class.getSimpleName();
    
    protected static final String ARGS_VIEW_TYPE = "ARGS_VIEW_TYPE";
    protected static final String ARGS_TIME_FRAME = "ARGS_TIME_FRAME";
    
    public static StatsAbsCategoryFragment newInstance(Stats.ViewType viewType, Stats.Timeframe timeframe) {
        
        StatsAbsCategoryFragment fragment = null;

        switch (viewType) {
            case VISITORS_AND_VIEWS:
                fragment = new StatsVisitorsAndViewsFragment();
                break;
            case TOTALS_FOLLOWERS_AND_SHARES:
                fragment = new StatsTotalsFollowersAndSharesFragment();
                break;
            case TOP_AUTHORS:
            case COMMENTS:
                fragment = new StatsListFragment();
                break;
            case TOP_POSTS_AND_PAGES:
                fragment = new StatsTopPostsAndPagesFragment();
                break;
            case VIEWS_BY_COUNTRY:
            case REFERRERS:
            case CLICKS:
            case SEARCH_ENGINE_TERMS:
            case TAGS_AND_CATEGORIES:
            case VIDEO_PLAYS:
                fragment = new StatsListFragment();
                break;
        }
        
        Bundle bundle = new Bundle();
        bundle.putInt(ARGS_VIEW_TYPE, viewType.ordinal());
        bundle.putInt(ARGS_TIME_FRAME, timeframe.ordinal());
        fragment.setArguments(bundle);
        
        return fragment;
    }
    
    protected Stats.ViewType getViewType() {
        return Stats.ViewType.values()[getArguments().getInt(ARGS_VIEW_TYPE)];
    }
    
    protected Stats.Timeframe getTimeframe() {
        return Stats.Timeframe.values()[getArguments().getInt(ARGS_TIME_FRAME)];
    }
    
    public Stats.Category getCategory() {
        switch (getViewType()) {
            case CLICKS:
                return Category.CLICKS;
            case COMMENTS:
                return Category.TOP_COMMENTER; // TODO: top commentor, most commented
            case REFERRERS:
                return Category.REFERRERS;
            case SEARCH_ENGINE_TERMS:
                return Category.SEARCH_ENGINE_TERMS;
            case TAGS_AND_CATEGORIES:
                return Category.TAGS_AND_CATEGORIES;
            case TOP_AUTHORS:
                return Category.AUTHORS;
            case TOP_POSTS_AND_PAGES:
                return Category.POSTS_AND_PAGES;
            case TOTALS_FOLLOWERS_AND_SHARES:
                // TODO
            case VIDEO_PLAYS:
                return Category.VIDEO_PLAYS;
            case VIEWS_BY_COUNTRY:
                return Category.COUNTRY;
            case VISITORS_AND_VIEWS: // TODO
            default: 
                return Category.POSTS_AND_PAGES;
        }
    }
    
    
    protected abstract void refreshData();
 
}
