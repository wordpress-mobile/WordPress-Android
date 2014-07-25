package org.wordpress.android.ui.stats;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewDataInterface;
import com.jjoe64.graphview.GraphViewSeries;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.StatsBarChartDataTable;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;

/**
 * A fragment that shows stats bar chart data.
 */
public class StatsBarGraphFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String ARGS_BAR_CHART_UNIT = "ARGS_TIMEFRAME";

    private LinearLayout mGraphContainer;
    private final ContentObserver mContentObserver = new BarGraphContentObserver(new Handler());
    private StatsBarGraph mGraphView;
    private GraphViewSeries mViewsSeries;
    private GraphViewSeries mVisitorsSeries;
    private String[] mStatsDate;
    private Toast mTappedToast = null;
    private int mLastTappedBar = -1;

    public static StatsBarGraphFragment newInstance(StatsBarChartUnit unit) {
        StatsBarGraphFragment fragment = new StatsBarGraphFragment();

        Bundle args = new Bundle();
        args.putInt(ARGS_BAR_CHART_UNIT, unit.ordinal());
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mGraphContainer = (LinearLayout) inflater.inflate(R.layout.stats_bar_graph_fragment, container, false);
        mGraphContainer.setTag(getArguments().getInt(ARGS_BAR_CHART_UNIT, -1));
        return mGraphContainer;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().restartLoader(getBarChartUnit().ordinal(), null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getContentResolver().registerContentObserver(
                StatsContentProvider.STATS_BAR_CHART_DATA_URI, true, mContentObserver
        );

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.registerReceiver(mReceiver, new IntentFilter(StatsActivity.STATS_GESTURE_SINGLE_TAP_CONFIRMED));
        lbm.registerReceiver(mReceiver, new IntentFilter(StatsActivity.STATS_GESTURE_SHOW_TAP));
        lbm.registerReceiver(mReceiver, new IntentFilter(StatsActivity.STATS_GESTURE_OTHER));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().getContentResolver().unregisterContentObserver(mContentObserver);

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.unregisterReceiver(mReceiver);
        mTappedToast = null;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mGraphView == null) {
                return;
            }
            String action = intent.getAction();
            // If it's not a "tap confirmed", or a "show tap" event, redraw the graph only if
            // has one bar in highlighted state
            if (StatsActivity.STATS_GESTURE_OTHER.equals(action)) {
                if (mLastTappedBar != -1) {
                    mLastTappedBar = -1;
                    mGraphView.highlightBar(-1);
                }
                return;
            }

            if (mLastTappedBar == -1) {
                mLastTappedBar = mGraphView.getTappedBar();
            }
            if (StatsActivity.STATS_GESTURE_SINGLE_TAP_CONFIRMED.equals(action)) {
                mGraphView.highlightAndDismissBar(mLastTappedBar);
                handleBarChartTap(mLastTappedBar);
                mLastTappedBar = -1;
            } else if (StatsActivity.STATS_GESTURE_SHOW_TAP.equals(action)) {
                mGraphView.highlightBar(mLastTappedBar);
            }
        }
    };

    private void handleBarChartTap(int tappedBar) {
        if (tappedBar < 0 || mStatsDate.length < tappedBar) {
            return;
        }

        String date = mStatsDate[tappedBar];
        StatsBarChartUnit unit = getBarChartUnit();
        if (unit == StatsBarChartUnit.DAY) {
            StatsUtils.StatsCredentials credentials = StatsUtils.getCurrentBlogStatsCredentials();
            if (credentials == null) {
                // Credentials empty, do nothing.
                return;
            }

            String statsAuthenticatedUser = credentials.getUsername();
            String statsAuthenticatedPassword =  credentials.getPassword();
            Intent statsWebViewIntent = new Intent(this.getActivity(), StatsDetailsActivity.class);
            statsWebViewIntent.putExtra(StatsWebViewActivity.STATS_AUTHENTICATED_USER, statsAuthenticatedUser);
            statsWebViewIntent.putExtra(StatsWebViewActivity.STATS_AUTHENTICATED_PASSWD,
                    statsAuthenticatedPassword);
            statsWebViewIntent.putExtra(StatsActivity.STATS_DETAILS_DATE, date);
            this.getActivity().startActivity(statsWebViewIntent);
        } else {
            // Week or Month on the screen. Show a toast.
            GraphViewDataInterface[] views = mViewsSeries.getData();
            GraphViewDataInterface[] visitors = mVisitorsSeries.getData();
            String formattedDate;

            if (unit == StatsBarChartUnit.WEEK) {
                formattedDate = StatsUtils.parseDate(date, "yyyy'W'MM'W'dd", "MMM d");
            } else {
                // Month
                formattedDate = StatsUtils.parseDate(date, "yyyy-MM", "MMM yyyy");
            }

            String message = String.format("%s - %s %d - %s %d", formattedDate, getString(R.string.stats_totals_views),
                    (int) views[tappedBar].getY(), getString(R.string.stats_totals_visitors),
                    (int) visitors[tappedBar].getY());
            if (mTappedToast != null) {
                mTappedToast.cancel();
                mTappedToast = null;
            }
            mTappedToast = ToastUtils.showToast(this.getActivity(), message, ToastUtils.Duration.LONG);
        }
    }


    private StatsBarChartUnit getBarChartUnit() {
        int ordinal = getArguments().getInt(ARGS_BAR_CHART_UNIT);
        return StatsBarChartUnit.values()[ordinal];
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (WordPress.getCurrentBlog() == null) {
            return null;
        }

        String blogId = WordPress.getCurrentBlog().getDotComBlogId();
        if (TextUtils.isEmpty(blogId)) {
            blogId = "0";
        }
        StatsBarChartUnit unit = getBarChartUnit();
        return new CursorLoader(getActivity(),
                                StatsContentProvider.STATS_BAR_CHART_DATA_URI,
                                null,
                                "blogId=? AND unit=?",
                                new String[] {blogId, unit.name()},
                                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (getActivity() == null) {
            return;
        }

        if (!cursor.moveToFirst()) {
            Context context = mGraphContainer.getContext();
            if (context != null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                View emptyBarGraphView = inflater.inflate(R.layout.stats_bar_graph_empty, mGraphContainer, false);
                if (emptyBarGraphView != null) {
                    mGraphContainer.addView(emptyBarGraphView);
                }
            }
            return;
        }

        int numPoints = Math.min(getNumOfPoints(), cursor.getCount());
        final String[] horLabels = new String[numPoints];
        mStatsDate = new String[numPoints];
        GraphView.GraphViewData[] views = new GraphView.GraphViewData[numPoints];
        GraphView.GraphViewData[] visitors = new GraphView.GraphViewData[numPoints];

        StatsBarChartUnit unit = getBarChartUnit();
        for (int i = numPoints - 1; i >= 0; i--) {
            views[i] = new GraphView.GraphViewData(i, getViews(cursor));
            visitors[i] = new GraphView.GraphViewData(i, getVisitors(cursor));
            horLabels[i] = getDateLabel(cursor, unit);
            mStatsDate[i] = getDate(cursor);
            cursor.moveToNext();
        }

        mViewsSeries = new GraphViewSeries(views);
        mVisitorsSeries = new GraphViewSeries(visitors);

        mViewsSeries.getStyle().color = getResources().getColor(R.color.stats_bar_graph_views);
        mViewsSeries.getStyle().padding = DisplayUtils.dpToPx(getActivity(), 1);
        mVisitorsSeries.getStyle().color = getResources().getColor(R.color.stats_bar_graph_visitors);
        mVisitorsSeries.getStyle().padding = DisplayUtils.dpToPx(getActivity(), 3);

        if (mGraphContainer.getChildCount() >= 1 && mGraphContainer.getChildAt(0) instanceof GraphView) {
            mGraphView = (StatsBarGraph) mGraphContainer.getChildAt(0);
        } else {
            mGraphContainer.removeAllViews();
            mGraphView = new StatsBarGraph(getActivity());
            mGraphContainer.addView(mGraphView);
        }

        if (mGraphView != null) {
            mGraphView.removeAllSeries();
            mGraphView.addSeries(mViewsSeries);
            mGraphView.addSeries(mVisitorsSeries);
            mGraphView.getGraphViewStyle().setNumHorizontalLabels(getNumOfHorizontalLabels(numPoints));
            mGraphView.setHorizontalLabels(horLabels);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        // noop
    }

    private int getNumOfPoints() {
        if (getBarChartUnit() == StatsBarChartUnit.DAY) {
            return 7;
        } else {
            return 12;
        }
    }

    private int getNumOfHorizontalLabels(int numPoints) {
        if (getBarChartUnit() == StatsBarChartUnit.DAY) {
            return numPoints / 2;
        } else {
            return numPoints / 3;
        }
    }

    private int getViews(Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndex(StatsBarChartDataTable.Columns.VIEWS));
    }

    private int getVisitors(Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndex(StatsBarChartDataTable.Columns.VISITORS));
    }

    private String getDateLabel(Cursor cursor, StatsBarChartUnit unit) {
        String cursorDate = StringUtils.notNullStr(
                cursor.getString(cursor.getColumnIndex(StatsBarChartDataTable.Columns.DATE))
        );

        switch (unit) {
            case DAY:
                return StatsUtils.parseDate(cursorDate, "yyyy-MM-dd", "MMM d");
            case WEEK:
                // first four digits are the year
                // followed by Wxx where xx is the month
                // followed by Wxx where xx is the day of the month
                // ex: 2013W07W22 = July 22, 2013
                return StatsUtils.parseDate(cursorDate, "yyyy'W'MM'W'dd", "MMM d");
            case MONTH:
                return StatsUtils.parseDate(cursorDate, "yyyy-MM", "MMM yyyy");
            default:
                return cursorDate;
        }
    }

    private String getDate(Cursor cursor) {
        return StringUtils.notNullStr(cursor.getString(cursor.getColumnIndex(StatsBarChartDataTable.Columns.DATE)));
    }

    class BarGraphContentObserver extends ContentObserver {
        public BarGraphContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            if (isAdded()) {
                getLoaderManager().restartLoader(0, null, StatsBarGraphFragment.this);
            }
        }
    }
}
