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
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.StatsBarChartDataTable;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.StringUtils;

import java.util.Random;

/**
 * A fragment that shows stats bar chart data.
 */
public class StatsBarGraphFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String ARGS_BAR_CHART_UNIT = "ARGS_TIMEFRAME";

    private LinearLayout mGraphContainer;
    private final ContentObserver mContentObserver = new BarGraphContentObserver(new Handler());
    private float lastTappedX, lastTappedY;
    private StatsBarGraph graphView;

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
        mGraphContainer = (LinearLayout)inflater.inflate(R.layout.stats_bar_graph_fragment, container, false);
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
        getActivity().getContentResolver().registerContentObserver(StatsContentProvider.STATS_BAR_CHART_DATA_URI, true, mContentObserver);

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.registerReceiver(mReceiver, new IntentFilter(StatsActivity.STATS_TOUCH_DETECTED));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().getContentResolver().unregisterContentObserver(mContentObserver);

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.unregisterReceiver(mReceiver);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (StatsActivity.STATS_TOUCH_DETECTED.equals(action)) {
                AppLog.w(AppLog.T.STATS, ">>>> BroadcastReceiver -> touch detected");
                AppLog.w(AppLog.T.STATS, "lastTappedX " + lastTappedX);
                AppLog.w(AppLog.T.STATS, "lastTappedY " + lastTappedY);
                // 1. detect if the tap is inside a bar
                // 2. always reset variables to 0
                if (graphView != null && lastTappedX != 0.0 &&  lastTappedY != 0.0
                        && graphView.isTapOnBar(lastTappedX, lastTappedY)) {
                    Random rnd = new Random();
                 /*   for (GraphViewSeries graphViewSeries : graphView.getAllSeries()) {
                        int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
                        graphViewSeries.getStyle().color = color;
                    }*/
                    graphView.redrawAll();
                }
                lastTappedX = 0.0f;
                lastTappedY = 0.0f;
                AppLog.w(AppLog.T.STATS, "<<<< BroadcastReceiver -> touch detected");
            }
        }
    };

    private StatsBarChartUnit getBarChartUnit() {
        int ordinal = getArguments().getInt(ARGS_BAR_CHART_UNIT);
        return StatsBarChartUnit.values()[ordinal];
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (WordPress.getCurrentBlog() == null)
            return null;

        String blogId = WordPress.getCurrentBlog().getDotComBlogId();
        if (TextUtils.isEmpty(blogId))
            blogId = "0";
        StatsBarChartUnit unit = getBarChartUnit();
        return new CursorLoader(getActivity(),
                                StatsContentProvider.STATS_BAR_CHART_DATA_URI,
                                null,
                                "blogId=? AND unit=?",
                                new String[] { blogId, unit.name() },
                                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (getActivity() == null)
            return;

        if (!cursor.moveToFirst()) {
            Context context = mGraphContainer.getContext();
            if (context != null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                View emptyBarGraphView = inflater.inflate(R.layout.stats_bar_graph_empty, mGraphContainer, false);
                if (emptyBarGraphView != null)
                    mGraphContainer.addView(emptyBarGraphView);
            }
            return;
        }

        int numPoints = Math.min(getNumOfPoints(), cursor.getCount());
        final String[] horLabels = new String[numPoints];
        GraphView.GraphViewData[] views = new GraphView.GraphViewData[numPoints];
        GraphView.GraphViewData[] visitors = new GraphView.GraphViewData[numPoints];

        StatsBarChartUnit unit = getBarChartUnit();
        for (int i = numPoints - 1; i >= 0; i--) {
            views[i] = new GraphView.GraphViewData(i, getViews(cursor));
            visitors[i] = new GraphView.GraphViewData(i, getVisitors(cursor));
            horLabels[i] = getDateLabel(cursor, unit);
            cursor.moveToNext();
        }

        GraphViewSeries viewsSeries = new GraphViewSeries(views);
        GraphViewSeries visitorsSeries = new GraphViewSeries(visitors);

        viewsSeries.getStyle().color = getResources().getColor(R.color.stats_bar_graph_views);
        viewsSeries.getStyle().padding = DisplayUtils.dpToPx(getActivity(), 1);
        visitorsSeries.getStyle().color = getResources().getColor(R.color.stats_bar_graph_visitors);
        visitorsSeries.getStyle().padding = DisplayUtils.dpToPx(getActivity(), 3);

        if (mGraphContainer.getChildCount() >= 1 && mGraphContainer.getChildAt(0) instanceof GraphView) {
            graphView = (StatsBarGraph) mGraphContainer.getChildAt(0);
        } else {
            mGraphContainer.removeAllViews();
            graphView = new StatsBarGraph(getActivity());
            mGraphContainer.addView(graphView);
        }

        if (graphView != null) {
            graphView.removeAllSeries();
            graphView.addSeries(viewsSeries);
            graphView.addSeries(visitorsSeries);
            graphView.getGraphViewStyle().setNumHorizontalLabels(getNumOfHorizontalLabels(numPoints));
            graphView.setHorizontalLabels(horLabels);

            graphView.setOnTouchListener(mBarGraphTouchListener);

        }
    }

    /*
    The bar graph is only getting the ACTION_DOWN event. So we record the coordinate of the ACTION_DOWN
    event in a local variable. The StatsActivity, actually detects the tap and fires a local broadcast.
    The bargraph can now  start the math and diplay the details view.
     */
    private View.OnTouchListener mBarGraphTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                StatsBarGraph currentGraphView = (StatsBarGraph) view;
                float x = motionEvent.getX(motionEvent.getActionIndex()); //the location of the touch on the graphview
                AppLog.w(AppLog.T.STATS, "currentGraphView x value " + x);
             /*   int width = currentGraphView.getWidth(); //the width of the graphview
                float xValue =  (x/width); //the x-Value of the graph where you touched
                AppLog.e(AppLog.T.STATS, ">>> graphView.onTouch");
                AppLog.w(AppLog.T.STATS, "x " + x);
                AppLog.w(AppLog.T.STATS, "width " + width);
                AppLog.w(AppLog.T.STATS, "xValue " + xValue);*/
                lastTappedX = x;

                float y = motionEvent.getY(motionEvent.getActionIndex());
/*                int height = currentGraphView.getHeight();
                float yValue =  (y/height);
                AppLog.w(AppLog.T.STATS, "y " + y);
                AppLog.w(AppLog.T.STATS, "height " + height);
                AppLog.w(AppLog.T.STATS, "yValue " + yValue);
                AppLog.e(AppLog.T.STATS, "<<< graphView.onTouch");*/
                lastTappedY = y;
            }
            return false;
        }
    };

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        //noop
    }

    private boolean hasActivity() {
        return getActivity() != null;
    }

    private int getNumOfPoints() {
        if (hasActivity() && DisplayUtils.isTablet(getActivity())) {
            return 30;
        }

        if (getBarChartUnit() == StatsBarChartUnit.DAY)
            return 7;
        else
            return 12;
    }

    private int getNumOfHorizontalLabels(int numPoints) {
        if (hasActivity() && DisplayUtils.isTablet(getActivity())) {
            return numPoints / 5;
        }

        if (getBarChartUnit() == StatsBarChartUnit.DAY)
            return numPoints / 2;
        else
            return numPoints / 3;
    }

    private int getViews(Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndex(StatsBarChartDataTable.Columns.VIEWS));
    }

    private int getVisitors(Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndex(StatsBarChartDataTable.Columns.VISITORS));
    }

    private String getDateLabel(Cursor cursor, StatsBarChartUnit unit) {
        String cursorDate = StringUtils.notNullStr(cursor.getString(cursor.getColumnIndex(StatsBarChartDataTable.Columns.DATE)));

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