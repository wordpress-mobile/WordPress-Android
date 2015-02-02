package org.wordpress.android.ui.stats;


import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

import org.wordpress.android.ui.stats.service.StatsService;


public abstract class StatsAbstractFragment extends Fragment {
    public static final String TAG = StatsAbstractFragment.class.getSimpleName();

    protected static final String ARGS_VIEW_TYPE = "ARGS_VIEW_TYPE";
    protected static final String ARGS_TIMEFRAME = "ARGS_TIMEFRAME";
    protected static final String ARGS_START_DATE = "ARGS_START_DATE";
    protected static final String ARG_REST_RESPONSE = "ARG_REST_RESPONSE";

    protected TimeframeDateProvider mTimelineProvider;
    protected OnRequestDataListener mMoreDataListener;

    // Container Activity must implement this interface
    public interface TimeframeDateProvider {
        public String getCurrentDate();
        public StatsTimeframe getCurrentTimeFrame();
    }

    // Container Activity must implement this interface
    public interface OnRequestDataListener {
        public void onRefreshRequested(StatsService.StatsEndpointsEnum[] endPointsNeedUpdate);
        public void onMoreDataRequested(StatsService.StatsEndpointsEnum endPointNeedUpdate, int pageNumber);
    }

    public static StatsAbstractFragment newInstance(StatsViewType viewType, int localTableBlogID) {
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
            case SEARCH_TERMS:
                fragment = new StatsSearchTermsFragment();
                break;
        }

        Bundle args = new Bundle();
        args.putInt(ARGS_VIEW_TYPE, viewType.ordinal());
        args.putInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, localTableBlogID);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mTimelineProvider = (TimeframeDateProvider) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement TimeframeDateProvider");
        }

        try {
            mMoreDataListener = (OnRequestDataListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnRequestMoreDataListener");
        }
    }

    protected StatsViewType getViewType() {
        int ordinal = getArguments().getInt(ARGS_VIEW_TYPE);
        return StatsViewType.values()[ordinal];
    }

    protected int getLocalTableBlogID() {
        return getArguments().getInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID);
    }

    protected StatsTimeframe getTimeframe() {
        return mTimelineProvider.getCurrentTimeFrame();
    }

    protected String getStartDate() {
        return mTimelineProvider.getCurrentDate();
    }

    protected abstract String getTitle();

    protected abstract void resetDataModel();

}