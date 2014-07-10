package org.wordpress.android.ui.stats;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
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
import org.wordpress.android.WordPressDB;
import org.wordpress.android.datasets.StatsBarChartDataTable;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.util.AppLog;
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
    private StatsBarGraph graphView;
    private GraphViewSeries viewsSeries;
    private GraphViewSeries visitorsSeries;
    private String[] statsDate;

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
                int tappedBar;
                if (graphView != null && (tappedBar = graphView.getTappedBar()) != -1) {
                    graphView.highlightBar(tappedBar);
                    handleBarChartTap(tappedBar);
                }
            }
        }
    };

    private void handleBarChartTap(int tappedBar) {
        if (tappedBar < 0 || statsDate.length < tappedBar) {
            return;
        }

        String date = statsDate[tappedBar];
        StatsBarChartUnit unit = getBarChartUnit();
        if (unit == StatsBarChartUnit.DAY) {
            // make sure to load the no-chrome version of Stats over https
            String url = "https://wordpress.com/my-stats/?no-chrome&blog=" + WordPress.getCurrentRemoteBlogId() + "&day=" + date + "&unit=1";

            //TODO: We have similar code in StatsActivity. Do not extract a common method for now since
            // the logic below will be gone shortly.

            // 1. Read the credentials at blog level (Jetpack connected with a wpcom account != main account)
            // 2. If credentials are empty read the global wpcom credentials
            // 3. Check that credentials are not empty before launching the activity
            String statsAuthenticatedUser = WordPress.getCurrentBlog().getDotcom_username();
            String statsAuthenticatedPassword = WordPress.getCurrentBlog().getDotcom_password();

            if (org.apache.commons.lang.StringUtils.isEmpty(statsAuthenticatedPassword)
                    || org.apache.commons.lang.StringUtils.isEmpty(statsAuthenticatedUser)) {
                // Let's try the global wpcom credentials
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
                statsAuthenticatedUser = settings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
                statsAuthenticatedPassword = WordPressDB.decryptPassword(
                        settings.getString(WordPress.WPCOM_PASSWORD_PREFERENCE, null)
                );
            }

            if (org.apache.commons.lang.StringUtils.isEmpty(statsAuthenticatedPassword)
                    || org.apache.commons.lang.StringUtils.isEmpty(statsAuthenticatedUser)) {
                // Still empty do nothing but write the log.
                AppLog.e(AppLog.T.STATS, "WPCOM Credentials for the current blog are null! This should never happen here.");
                return;
            }

            Intent statsWebViewIntent = new Intent(this.getActivity(), StatsWebViewActivity.class);
            statsWebViewIntent.putExtra(StatsWebViewActivity.STATS_AUTHENTICATED_USER, statsAuthenticatedUser);
            statsWebViewIntent.putExtra(StatsWebViewActivity.STATS_AUTHENTICATED_PASSWD,
                    statsAuthenticatedPassword);
            statsWebViewIntent.putExtra(StatsWebViewActivity.STATS_AUTHENTICATED_URL, url);
            this.getActivity().startActivity(statsWebViewIntent);
        } else {
            // Week or Month on the screen. Show a toast.
            GraphViewDataInterface[] views = viewsSeries.getData();
            GraphViewDataInterface[] visitors = visitorsSeries.getData();
            String formattedDate;

            if (unit == StatsBarChartUnit.WEEK) {
                formattedDate = StatsUtils.parseDate(date, "yyyy'W'MM'W'dd", "MMM d");
            } else {
                //Month
                formattedDate = StatsUtils.parseDate(date, "yyyy-MM", "MMM yyyy");
            }

            String message = String.format("%s - %s %d - %s %d", formattedDate, getString(R.string.stats_totals_views),
                    (int) views[tappedBar].getY(), getString(R.string.stats_totals_visitors),
                    (int) visitors[tappedBar].getY() );

            ToastUtils.showToast(this.getActivity(), message, ToastUtils.Duration.LONG);
        }
    }


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
        statsDate = new String[numPoints];
        GraphView.GraphViewData[] views = new GraphView.GraphViewData[numPoints];
        GraphView.GraphViewData[] visitors = new GraphView.GraphViewData[numPoints];

        StatsBarChartUnit unit = getBarChartUnit();
        for (int i = numPoints - 1; i >= 0; i--) {
            views[i] = new GraphView.GraphViewData(i, getViews(cursor));
            visitors[i] = new GraphView.GraphViewData(i, getVisitors(cursor));
            horLabels[i] = getDateLabel(cursor, unit);
            statsDate[i] = getDate(cursor);
            cursor.moveToNext();
        }

        viewsSeries = new GraphViewSeries(views);
        visitorsSeries = new GraphViewSeries(visitors);

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
        }
    }

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