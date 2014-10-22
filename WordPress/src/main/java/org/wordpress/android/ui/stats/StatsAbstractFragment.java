package org.wordpress.android.ui.stats;


import android.app.Fragment;
import android.os.Bundle;


public abstract class StatsAbstractFragment extends Fragment {
    public static final String TAG = StatsAbstractFragment.class.getSimpleName();

    private static final String ARGS_VIEW_TYPE = "ARGS_VIEW_TYPE";
    private static final String ARGS_TIMEFRAME = "ARGS_TIMEFRAME";

    public static StatsAbstractFragment newInstance(StatsViewType viewType, int localTableBlogID, StatsTimeframe timeframe) {
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
        }

        Bundle args = new Bundle();
        args.putInt(ARGS_VIEW_TYPE, viewType.ordinal());
        args.putInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, localTableBlogID);
        args.putSerializable(StatsAbstractFragment.ARGS_TIMEFRAME, timeframe);
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
        return (StatsTimeframe) getArguments().getSerializable(StatsAbstractFragment.ARGS_TIMEFRAME);
    }

    protected abstract String getTitle();
}