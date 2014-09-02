package org.wordpress.android.ui.stats;

import android.app.Fragment;
import android.os.Bundle;

/**
 * A generic view for all the different stats views.
 */
public abstract class StatsAbsViewFragment extends Fragment {
    public static final String TAG = StatsAbsViewFragment.class.getSimpleName();

    public static StatsAbsViewFragment newInstance(StatsViewType viewType, int localTableBlogID) {
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
        args.putInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, localTableBlogID);
        fragment.setArguments(args);

        return fragment;
    }

    private static final String ARGS_VIEW_TYPE = "ARGS_VIEW_TYPE";

    protected StatsViewType getViewType() {
        int ordinal = getArguments().getInt(ARGS_VIEW_TYPE);
        return StatsViewType.values()[ordinal];
    }

    protected int getLocalTableBlogID() {
        return getArguments().getInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID);
    }

    protected abstract String getTitle();
}