package org.wordpress.android.ui.stats;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragment;

public abstract class StatsAbsViewFragment extends SherlockFragment {
    
    public static final String TAG = StatsAbsViewFragment.class.getSimpleName();

    public static StatsAbsViewFragment newInstance(Stats.ViewType viewType) {
        StatsAbsViewFragment fragment = null;
        
        switch (viewType) {
            case CLICKS:
                fragment = new StatsClicksFragment();
                break;
            case COMMENTS:
                fragment = new StatsCommentsFragment();
                break;
            case REFERRERS:
                fragment = new StatsReferrersFragment();
                break;
            case SEARCH_ENGINE_TERMS:
                fragment = new StatsSearchEngineTermsFragment();
                break;
            case TAGS_AND_CATEGORIES:
                fragment = new StatsTagsAndCategoriesFragment();
                break;
            case TOP_AUTHORS:
                fragment = new StatsTopAuthorsFragment();
                break;
            case TOP_POSTS_AND_PAGES:
                fragment = new StatsTopPostsAndPagesFragment();
                break;
            case TOTALS_FOLLOWERS_AND_SHARES:
                fragment = new StatsTotalsFollowersAndSharesFragment();
                break;
            case VIDEO_PLAYS:
                fragment = new StatsVideoFragment();
                break;
            case VIEWS_BY_COUNTRY:
                fragment = new StatsGeoviewsFragment();
                break;
            case VISITORS_AND_VIEWS:
                fragment = new StatsVisitorsAndViewsFragment();
                break;
            
        }
    
        Bundle args = new Bundle();
        args.putInt(ARGS_VIEW_TYPE, viewType.ordinal());
        fragment.setArguments(args);
        
        return fragment;
    }

    protected static final String ARGS_VIEW_TYPE = "ARGS_VIEW_TYPE";
    
    protected Stats.ViewType getViewType() {
        int ordinal = getArguments().getInt(ARGS_VIEW_TYPE);
        return Stats.ViewType.values()[ordinal];
    }
    
    public abstract String getTitle();
    
}
