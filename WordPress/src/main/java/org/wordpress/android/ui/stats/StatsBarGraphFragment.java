package org.wordpress.android.ui.stats;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
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
import android.text.Html;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewDataInterface;
import com.jjoe64.graphview.GraphViewSeries;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.StatsBarChartDataTable;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.WPTextView;

import java.util.HashMap;
import java.util.Map;

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
    private int mLastTappedBar = -1;
    private int mLastHighlightedBar = -1;
    private Tooltip mTooltip;

    public static StatsBarGraphFragment newInstance(StatsBarChartUnit unit, int localTableBlogID) {
        StatsBarGraphFragment fragment = new StatsBarGraphFragment();

        Bundle args = new Bundle();
        args.putInt(ARGS_BAR_CHART_UNIT, unit.ordinal());
        args.putInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, localTableBlogID);
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
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mGraphView == null) {
                return;
            }
            String action = intent.getAction();
            // If it's not a "tap confirmed", or a "show tap" event, do nothing.
            if (StatsActivity.STATS_GESTURE_OTHER.equals(action)) {
                return;
            }

            mLastTappedBar = mGraphView.getTappedBar();
            if (mLastTappedBar == -1) {
                return;
            }

            if (getBarChartUnit() != StatsBarChartUnit.DAY) {
                if (mTooltip.isVisible()) {
                    // The same bar is pressed dismiss all
                    if (mLastHighlightedBar == mLastTappedBar) {
                        mTooltip.hideTooltip(true);
                        mLastHighlightedBar = -1;
                        mGraphView.highlightBar(mLastHighlightedBar);
                    } else {
                        mLastHighlightedBar = mLastTappedBar;
                        // update the values and change the highlight
                        mGraphView.highlightBar(mLastHighlightedBar);
                        handleBarChartTap(mLastHighlightedBar);
                    }
                } else {
                    mLastHighlightedBar = mLastTappedBar;
                    mGraphView.highlightBar(mLastHighlightedBar);
                    handleBarChartTap(mLastHighlightedBar);
                }
            } else {
                mGraphView.highlightAndDismissBar(mLastTappedBar);
                handleBarChartTap(mLastTappedBar);
            }
        }
    };

    private void handleBarChartTap(int tappedBar) {
        if (tappedBar < 0 || mStatsDate.length < tappedBar) {
            return;
        }

        String date = mStatsDate[tappedBar];
        StatsBarChartUnit unit = getBarChartUnit();
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("unit", unit.name());
        AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_TAPPED_BAR_CHART, properties);
        if (unit == StatsBarChartUnit.DAY) {
            Intent statsWebViewIntent = new Intent(this.getActivity(), StatsDetailsActivity.class);
            statsWebViewIntent.putExtra(StatsActivity.STATS_DETAILS_DATE, date);
            statsWebViewIntent.putExtra(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, getLocalTableBlogID());
            this.getActivity().startActivity(statsWebViewIntent);
        } else {
            // Week or Month on the screen. Show a toast.
            GraphViewDataInterface[] views = mViewsSeries.getData();
            GraphViewDataInterface[] visitors = mVisitorsSeries.getData();
            mTooltip.setValues(date, unit, (int) views[tappedBar].getY(), (int) visitors[tappedBar].getY(),
            mGraphView.getMiddlePointOfTappedBar(tappedBar));
            mTooltip.showTooltip(true);
        }
    }

    private StatsBarChartUnit getBarChartUnit() {
        int ordinal = getArguments().getInt(ARGS_BAR_CHART_UNIT);
        return StatsBarChartUnit.values()[ordinal];
    }

    protected int getLocalTableBlogID() {
        return getArguments().getInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (WordPress.getBlog(getLocalTableBlogID()) == null) {
            return null;
        }

        String blogId = WordPress.getBlog(getLocalTableBlogID()).getDotComBlogId();
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
            mTooltip = new Tooltip(getActivity());
            mGraphContainer.addView(mTooltip);
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

    private class Tooltip extends LinearLayout {
        private LinearLayout mInternalContainer;
        private LinearLayout mArrow;
        private int mArrawWidthPixel;
        private WPTextView mVisitors;
        private WPTextView mViews;
        private WPTextView mViewsForVisitors;
        private WPTextView mDate;

        public Tooltip(Context ctx) {
            super(ctx);
            int width = LinearLayout.LayoutParams.MATCH_PARENT;
            int height = LayoutParams.WRAP_CONTENT;
            setLayoutParams(new LinearLayout.LayoutParams(width, height));
            setOrientation(LinearLayout.VERTICAL);
            setGravity(Gravity.CENTER);
            setVisibility(View.GONE);

            // Setting up internal items: The arrow and the 2nd LL with labels in it.
            // 1. setting up the arrow
            mArrow = new LinearLayout(ctx);
            mArrawWidthPixel = getResources().getDimensionPixelSize(R.dimen.margin_large);
            mArrow.setLayoutParams(new LinearLayout.LayoutParams(mArrawWidthPixel, mArrawWidthPixel));
            mArrow.setBackgroundColor(getResources().getColor(R.color.grey_light));
            mArrow.setRotation(45f);
            mArrow.setTranslationY(mArrawWidthPixel / 2);
            addView(mArrow);

            // 2. setting up the internal LL that holds labels
            mInternalContainer = new LinearLayout(ctx);
            mInternalContainer.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                    )
            );
            mInternalContainer.setOrientation(LinearLayout.VERTICAL);
            mInternalContainer.setGravity(Gravity.CENTER);
            mInternalContainer.setBackgroundColor(getResources().getColor(R.color.grey_light));
            int padding = getResources().getDimensionPixelSize(R.dimen.margin_medium);
            mInternalContainer.setPadding(padding, padding, padding, padding);

            mDate = new WPTextView(ctx);
            setupLabel(mDate);
            mViews = new WPTextView(ctx);
            setupLabel(mViews);
            mVisitors = new WPTextView(ctx);
            setupLabel(mVisitors);
            mViewsForVisitors = new WPTextView(ctx);
            setupLabel(mViewsForVisitors);
            addView(mInternalContainer);
        }

        private void setupLabel(TextView textView) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            textView.setLayoutParams(params);
            textView.setTextColor(getResources().getColor(R.color.grey_dark));
            int textSize = getResources().getDimensionPixelSize(R.dimen.text_sz_small);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            mInternalContainer.addView(textView);
        }

        public void setValues(String date, StatsBarChartUnit unit, int views, int visitors, float pos) {
            ObjectAnimator mover = ObjectAnimator.ofFloat(mArrow, "x", pos - (mArrawWidthPixel / 2));
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.play(mover);
            animatorSet.start();

            if (unit == StatsBarChartUnit.WEEK) {
                mDate.setText(getString(R.string.stats_tooltip_week_of)
                        + " " + StatsUtils.parseDate(date, "yyyy'W'MM'W'dd", "EEEE, MMMM d, yyyy"));
            } else {
                // Month
                mDate.setText(StatsUtils.parseDate(date, "yyyy-MM", "MMMM yyyy"));
            }

            double viewsPerVisitor = 0d;
            if (visitors == 0) {
                visitors = 1;
            }
            if (views == 0) {
                visitors = 0;
                views = 0;
            } else {
                viewsPerVisitor = (double) views / visitors;
            }
            mVisitors.setText(Html.fromHtml(String.format("<b>%s</b> %s",
                    FormatUtils.formatDecimal(visitors),
                    getString(R.string.stats_totals_visitors)))
            );
            mViews.setText(Html.fromHtml(String.format("<b>%s</b> %s",
                            FormatUtils.formatDecimal(views),
                            getString(R.string.stats_totals_views)))
            );
            mViewsForVisitors.setText(Html.fromHtml(String.format("<b>%.2f</b> %s",
                    viewsPerVisitor,
                    getString(R.string.stats_totals_views_per_visitor)))
            );
        }

        public boolean isVisible() {
            return getVisibility() == View.VISIBLE;
        }

        public void showTooltip(boolean animate) {
            if (getVisibility() != View.VISIBLE) {
                if (animate) {
                    Animation expand = new ScaleAnimation(1.0f, 1.0f, 0.0f, 1.0f);
                    expand.setDuration(250);
                    expand.setInterpolator(StatsUIHelper.getInterpolator());
                    startAnimation(expand);
                }
                setVisibility(View.VISIBLE);
            }
        }
        public void hideTooltip(boolean animate) {
            if (getVisibility() != View.GONE) {
                if (animate) {
                    Animation expand = new ScaleAnimation(1.0f, 1.0f, 1.0f, 0.0f);
                    expand.setDuration(250);
                    expand.setInterpolator(StatsUIHelper.getInterpolator());
                    expand.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) { }
                        @Override
                        public void onAnimationEnd(Animation animation) {
                            setVisibility(View.GONE);
                        }
                        @Override
                        public void onAnimationRepeat(Animation animation) { }
                    });
                    startAnimation(expand);
                } else {
                    setVisibility(View.GONE);
                }
            }
        }
    }
}
