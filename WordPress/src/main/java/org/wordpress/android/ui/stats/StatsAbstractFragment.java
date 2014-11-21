package org.wordpress.android.ui.stats;


import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;

import org.wordpress.android.ui.stats.model.TopPostsAndPagesModel;
import org.wordpress.android.util.AppLog;

import java.io.Serializable;


public abstract class StatsAbstractFragment extends Fragment {
    public static final String TAG = StatsAbstractFragment.class.getSimpleName();

    protected static final String ARGS_VIEW_TYPE = "ARGS_VIEW_TYPE";
    protected static final String ARGS_TIMEFRAME = "ARGS_TIMEFRAME";
    protected static final String ARGS_START_DATE = "ARGS_START_DATE";
    protected static final String ARG_REST_RESPONSE = "ARG_REST_RESPONSE";

    public static StatsAbstractFragment newInstance(StatsViewType viewType, int localTableBlogID, StatsTimeframe timeframe, String date) {
        StatsAbstractFragment fragment = null;

        switch (viewType) {
            //case TIMEFRAME_SELECTOR:
               // fragment = new StatsDateSelectorFragment();
              //  break;
            case GRAPH_AND_SUMMARY:
                fragment = new StatsVisitorsAndViewsFragment();
                break;
            case TOP_POSTS_AND_PAGES:
                fragment = new StatsTopPostsAndPagesFragment();
                break;
            case REFERRERS:
                fragment = new StatsReferrersFragment();
                break;
            case CLICKS:
                fragment = new StatsClicksFragment();
                break;
            case GEOVIEWS:
                fragment = new StatsGeoviewsFragment();
                break;
            case AUTHORS:
                fragment = new StatsAuthorsFragment();
                break;
            case VIDEO_PLAYS:
                fragment = new StatsVideoplaysFragment();
                break;
            case COMMENTS:
                fragment = new StatsCommentsFragment();
                break;
            case TAGS_AND_CATEGORIES:
                fragment = new StatsTagsAndCategoriesFragment();
                break;
            case PUBLICIZE:
                fragment = new StatsPublicizeFragment();
                break;
            case FOLLOWERS:
                fragment = new StatsFollowersFragment();
                break;
        }

        Bundle args = new Bundle();
        args.putInt(ARGS_VIEW_TYPE, viewType.ordinal());
        args.putInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, localTableBlogID);
        args.putSerializable(ARGS_TIMEFRAME, timeframe);
        args.putString(ARGS_START_DATE, date);
        fragment.setArguments(args);

        return fragment;
    }

    protected StatsViewType getViewType() {
        int ordinal = getArguments().getInt(ARGS_VIEW_TYPE);
        return StatsViewType.values()[ordinal];
    }

    protected int getLocalTableBlogID() {
        return getArguments().getInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID);
    }

    protected StatsTimeframe getTimeframe() {
        return (StatsTimeframe) getArguments().getSerializable(ARGS_TIMEFRAME);
    }

    // This is the FIRST date used when created the fragment.
    protected String getStartDate() {
        return getArguments().getString(ARGS_START_DATE);
    }

    protected abstract String getTitle();
}