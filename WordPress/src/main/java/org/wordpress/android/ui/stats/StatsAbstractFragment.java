package org.wordpress.android.ui.stats;


import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;


public abstract class StatsAbstractFragment extends Fragment {
    public static final String TAG = StatsAbstractFragment.class.getSimpleName();

    public static final String ARGS_VIEW_TYPE = "ARGS_VIEW_TYPE";
    public static final String ARGS_TIMEFRAME = "ARGS_TIMEFRAME";
    protected static final String ARGS_SELECTED_DATE = "ARGS_SELECTED_DATE";
    protected static final String ARG_REST_RESPONSE = "ARG_REST_RESPONSE";
    protected static final String ARGS_IS_SINGLE_VIEW = "ARGS_IS_SINGLE_VIEW";

    // The number of results to return for NON Paged REST endpoints.
    private static final int MAX_RESULTS_REQUESTED = 100;

    protected String mDate;
    protected StatsTimeframe mStatsTimeframe = StatsTimeframe.DAY;

    protected abstract StatsService.StatsEndpointsEnum[] getSectionsToUpdate();

    public void refreshStats() {
        refreshStats(-1, null);
    }
    // call an update for the stats shown in the fragment
    public void refreshStats(int pageNumberRequested, StatsService.StatsEndpointsEnum[] sections) {
        if (!isAdded()) {
            return;
        }

        // if no sections to update is passed to the method, default to fragment
        if (sections == null) {
            sections = getSectionsToUpdate();
        }

        //AppLog.d(AppLog.T.STATS, this.getClass().getCanonicalName() + " > refreshStats");

        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            AppLog.w(AppLog.T.STATS, this.getClass().getCanonicalName() + "--> no connection, update canceled");
            return;
        }

        final String blogId = StatsUtils.getBlogId(getLocalTableBlogID());
        final Blog currentBlog = WordPress.getBlog(getLocalTableBlogID());

        if (currentBlog == null) {
            AppLog.w(AppLog.T.STATS, "Current blog is null. This should never happen here.");
            return;
        }

        // Make sure the blogId is available.
        if (blogId == null) {
            AppLog.e(AppLog.T.STATS, "remote blogID is null: " + currentBlog.getHomeURL());
            return;
        }

        // Check credentials for jetpack blogs first
        if (!currentBlog.isDotcomFlag()
                && !currentBlog.hasValidJetpackCredentials()) {
            AppLog.w(AppLog.T.STATS, "Current blog is a Jetpack blog without valid .com credentials stored");
            return;
        }

        // start service to get stats
        Intent intent = new Intent(getActivity(), StatsService.class);
        intent.putExtra(StatsService.ARG_BLOG_ID, blogId);
        intent.putExtra(StatsService.ARG_PERIOD, mStatsTimeframe);
        intent.putExtra(StatsService.ARG_DATE, mDate);
        if (isSingleView()) {
            intent.putExtra(StatsService.ARG_MAX_RESULTS, MAX_RESULTS_REQUESTED);
        }
        if (pageNumberRequested  != -1) {
            intent.putExtra(StatsService.ARG_PAGE_REQUESTED, pageNumberRequested);
        }
        intent.putExtra(StatsService.ARG_SECTION, sections);
        getActivity().startService(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // AppLog.d(AppLog.T.STATS, this.getClass().getCanonicalName() + " > onCreate");

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(ARGS_TIMEFRAME)) {
                mStatsTimeframe = (StatsTimeframe) savedInstanceState.getSerializable(ARGS_TIMEFRAME);
            }
            if (savedInstanceState.containsKey(ARGS_SELECTED_DATE)) {
                mDate = savedInstanceState.getString(ARGS_SELECTED_DATE);
            }
        }

      //  AppLog.d(AppLog.T.STATS, "mStatsTimeframe: " + mStatsTimeframe.getLabel());
      //  AppLog.d(AppLog.T.STATS, "mDate: " + mDate);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
       /* AppLog.d(AppLog.T.STATS, this.getClass().getCanonicalName() + " > saving instance state");
        AppLog.d(AppLog.T.STATS, "mStatsTimeframe: " + mStatsTimeframe.getLabel());
        AppLog.d(AppLog.T.STATS, "mDate: " + mDate); */
        outState.putString(ARGS_SELECTED_DATE, mDate);
        outState.putSerializable(ARGS_TIMEFRAME, mStatsTimeframe);
        super.onSaveInstanceState(outState);
    }

    public static StatsAbstractFragment newInstance(StatsViewType viewType, int localTableBlogID,
                                                    StatsTimeframe timeframe, String date ) {
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

        fragment.setTimeframe(timeframe);
        fragment.setDate(date);

        Bundle args = new Bundle();
        args.putSerializable(ARGS_VIEW_TYPE, viewType);
        args.putInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, localTableBlogID);
        fragment.setArguments(args);

        return fragment;
    }

    public void setDate(String newDate) {
        mDate = newDate;
    }

    public String getDate() {
        return mDate;
    }

    public void setTimeframe(StatsTimeframe newTimeframe) {
        mStatsTimeframe = newTimeframe;
    }

    public StatsTimeframe getTimeframe() {
        return mStatsTimeframe;
    }

    protected StatsViewType getViewType() {
        return (StatsViewType) getArguments().getSerializable(ARGS_VIEW_TYPE);
    }

    protected int getLocalTableBlogID() {
        return getArguments().getInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID);
    }

    protected boolean isSingleView() {
        return getArguments().getBoolean(ARGS_IS_SINGLE_VIEW, false);
    }

    protected abstract String getTitle();
}