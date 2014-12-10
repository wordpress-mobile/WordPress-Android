package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.ui.stats.models.PostViewsModel;
import org.wordpress.android.ui.stats.models.SingleItemModel;
import org.wordpress.android.ui.stats.models.VisitModel;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ptr.SwipeToRefreshHelper;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


/**
 *  Single item details activity.
 */
public class StatsSinglePostDetailsActivity extends ActionBarActivity
        implements StatsBarGraph.OnGestureListener{
    public static final String ARG_REMOTE_POST_OBJECT = "ARG_REMOTE_POST_OBJECT";
    public static final String ARG_REST_RESPONSE = "ARG_REST_RESPONSE";
    private static final String ARG_SELECTED_GRAPH_BAR = "ARG_SELECTED_GRAPH_BAR";

    private boolean mIsInFront;
    private boolean mIsUpdatingStats;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;

    private final Handler mHandler = new Handler();

    private LinearLayout mGraphContainer;
    private StatsBarGraph mGraphView;
    private GraphViewSeries mCurrentSeriesOnScreen;
    private TextView mWholeResponse;
    private TextView mStatsViewsLabel;
    private TextView mStatsViewsTotals;
    private LinearLayout mMonthsAndYearsList;
    private LinearLayout mAveragesList;

    private int mLocalBlogID = -1;
    private SingleItemModel mRemotePostItem; // The original item returned from TopPostsAndPages endpoint
    private PostViewsModel mRestResponseParsed;
    private int mSelectedBarGraphIndex = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.stats_activity_single_post_details);

        if (savedInstanceState == null) {
            // AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_ACCESSED);
            // TODO: add analytics here
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // pull to refresh setup
        mSwipeToRefreshHelper = new SwipeToRefreshHelper(this, (SwipeRefreshLayout) findViewById(R.id.ptr_layout),
                new SwipeToRefreshHelper.RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        if (!NetworkUtils.checkConnection(getBaseContext())) {
                            mSwipeToRefreshHelper.setRefreshing(false);
                            return;
                        }
                        refreshStats();
                    }
                }
        );

        mGraphContainer = (LinearLayout) findViewById(R.id.stats_bar_chart_fragment_container);
        mWholeResponse = (TextView) findViewById(R.id.stats_all_response);
        mStatsViewsLabel = (TextView) findViewById(R.id.stats_views_label);
        mStatsViewsTotals = (TextView) findViewById(R.id.stats_views_totals);
        mMonthsAndYearsList = (LinearLayout) findViewById(R.id.stats_months_years_list_linearlayout);
        mAveragesList = (LinearLayout) findViewById(R.id.stats_averages_list_linearlayout);

        if (savedInstanceState != null) {
            mLocalBlogID = savedInstanceState.getInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, -1);
            mRemotePostItem = (SingleItemModel) savedInstanceState.getSerializable(ARG_REMOTE_POST_OBJECT);
            mRestResponseParsed = (PostViewsModel) savedInstanceState.getSerializable(ARG_REST_RESPONSE);
            mSelectedBarGraphIndex = savedInstanceState.getInt(ARG_SELECTED_GRAPH_BAR, -1);
        } else if (getIntent() != null) {
            Bundle extras = getIntent().getExtras();
            mLocalBlogID = extras.getInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, -1);
            mRemotePostItem = (SingleItemModel) extras.getSerializable(ARG_REMOTE_POST_OBJECT);
            mRestResponseParsed = (PostViewsModel) extras.getSerializable(ARG_REST_RESPONSE);
            mSelectedBarGraphIndex = extras.getInt(ARG_SELECTED_GRAPH_BAR, -1);
        }

        String prefix = getString(R.string.stats_for);
        setTitle(String.format(prefix, mRemotePostItem.getTitle()));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, mLocalBlogID);
        outState.putInt(ARG_SELECTED_GRAPH_BAR, mSelectedBarGraphIndex);
        outState.putSerializable(ARG_REMOTE_POST_OBJECT, mRemotePostItem);
        outState.putSerializable(ARG_REST_RESPONSE, mRestResponseParsed);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsInFront = true;
        if (mRestResponseParsed == null) {
            setupEmptyUI(true);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    refreshStats();
                }
            }, 75L);
        } else {
            updateUI();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsInFront = false;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refreshStats() {
        final String remotePostID = mRemotePostItem.getItemID();
        if (mIsUpdatingStats) {
            AppLog.w(AppLog.T.STATS, "stats details are already updating for the following postID "
                    + mRemotePostItem.getItemID() + ", refresh cancelled.");
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(this)) {
            mSwipeToRefreshHelper.setRefreshing(false);
            ToastUtils.showToast(this, this.getString(R.string.connection_error), ToastUtils.Duration.LONG);
            return;
        }

        final RestClientUtils restClientUtils = WordPress.getRestClientUtilsV1_1();
        final String blogId = mRemotePostItem.getBlogID();

        // View and visitor counts for a site
        final String singlePostRestPath = String.format(
                "/sites/%s/stats/post/%s", blogId, remotePostID);

        AppLog.d(AppLog.T.STATS, "Enqueuing the following Stats request " + singlePostRestPath);

        RestBatchCallListener vListener = new RestBatchCallListener(this, blogId, remotePostID);
        restClientUtils.get(singlePostRestPath, vListener, vListener);

        mIsUpdatingStats = true;
        mSwipeToRefreshHelper.setRefreshing(true);

        mMonthsAndYearsList.setVisibility(View.GONE);
        mAveragesList.setVisibility(View.GONE);

        return;
    }

    private void setupEmptyUI(boolean isLoading) {
        Context context = mGraphContainer.getContext();
        if (context != null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View emptyBarGraphView = inflater.inflate(R.layout.stats_bar_graph_empty, mGraphContainer, false);
            /*if (isLoading) {
                final TextView emptyLabel = (TextView) emptyBarGraphView.findViewById(R.id.stats_bar_graph_empty_label);
                emptyLabel.setText("Loading...");
            }*/
            if (emptyBarGraphView != null) {
                mGraphContainer.removeAllViews();
                mGraphContainer.addView(emptyBarGraphView);
            }
        }
        mWholeResponse.setText("");
        mStatsViewsLabel.setText("");
        mStatsViewsTotals.setText("");
        mMonthsAndYearsList.setVisibility(View.GONE);
        mAveragesList.setVisibility(View.GONE);
        return;
    }

    private VisitModel[] getDataToShowOnGraph () {
        if (mRestResponseParsed == null) {
            return new VisitModel[0];
        }

        final VisitModel[] dayViews = mRestResponseParsed.getDayViews();
        if (dayViews == null) {
            return new VisitModel[0];
        }

        int numPoints = Math.min(getNumOfPoints(), dayViews.length);
        int currentPointIndex = numPoints - 1;
        VisitModel[] visitModels = new VisitModel[numPoints];

        for (int i = dayViews.length - 1; i >= 0 && currentPointIndex >= 0; i--) {
            visitModels[currentPointIndex] = dayViews[i];
            currentPointIndex--;
        }

        return visitModels;
    }

    private int getNumOfPoints() {
        return 7;
        /*
        if (getTimeframe() == StatsTimeframe.DAY) {
            return 7;
        } else {
            return 12;
        }
        */
    }

    private void updateUI() {
        final VisitModel[] dataToShowOnGraph = getDataToShowOnGraph();

        if (dataToShowOnGraph == null || dataToShowOnGraph.length == 0) {
            setupEmptyUI(false);
            return;
        }

        final String[] horLabels = new String[dataToShowOnGraph.length];
        String[] mStatsDate = new String[dataToShowOnGraph.length];
        GraphView.GraphViewData[] views = new GraphView.GraphViewData[dataToShowOnGraph.length];

        for (int i = 0; i < dataToShowOnGraph.length; i++) {
            int currentItemValue = 0;
            currentItemValue = dataToShowOnGraph[i].getViews();
            views[i] = new GraphView.GraphViewData(i, currentItemValue);

            String currentItemStatsDate = dataToShowOnGraph[i].getPeriod();
            horLabels[i] = StatsUtils.parseDate(currentItemStatsDate, "yyyy-MM-dd", "MMM d");
            mStatsDate[i] = currentItemStatsDate;
        }

        mCurrentSeriesOnScreen = new GraphViewSeries(views);
        mCurrentSeriesOnScreen.getStyle().color = getResources().getColor(R.color.stats_bar_graph_views);
        mCurrentSeriesOnScreen.getStyle().padding = DisplayUtils.dpToPx(this, 1);

        if (mGraphContainer.getChildCount() >= 1 && mGraphContainer.getChildAt(0) instanceof GraphView) {
            mGraphView = (StatsBarGraph) mGraphContainer.getChildAt(0);
        } else {
            mGraphContainer.removeAllViews();
            mGraphView = new StatsBarGraph(this);
            mGraphContainer.addView(mGraphView);
        }


        mGraphView.removeAllSeries();
        mGraphView.addSeries(mCurrentSeriesOnScreen);
        //mGraphView.getGraphViewStyle().setNumHorizontalLabels(getNumOfHorizontalLabels(dataToShowOnGraph.length));
        mGraphView.getGraphViewStyle().setNumHorizontalLabels(dataToShowOnGraph.length);
        mGraphView.setHorizontalLabels(horLabels);
        mGraphView.setGestureListener(this);
        mSelectedBarGraphIndex = (mSelectedBarGraphIndex != -1) ? mSelectedBarGraphIndex : dataToShowOnGraph.length - 1;
        mGraphView.highlightBar(mSelectedBarGraphIndex);

        setMainViewsLabel(StatsUtils.parseDate(mStatsDate[mSelectedBarGraphIndex], "yyyy-MM-dd", "MMM d"),
                dataToShowOnGraph[mSelectedBarGraphIndex].getViews());

        if (mRestResponseParsed.getOriginalResponse() != null) {
            mWholeResponse.setText(mRestResponseParsed.getOriginalResponse().toString());
        }

        //FIXME: Ugly trick to show all the months on the screen in a simple list. Need to change this. Shame on me!
        mMonthsAndYearsList.setVisibility(View.VISIBLE);
        List<PostViewsModel.Year> years = mRestResponseParsed.getYears();
        Integer[] allMonths = new Integer[years.size() * 13];
        for (int i = 0; i < years.size() ; i++) {
            PostViewsModel.Year currentYear = years.get(i);
            int correctPositionOfTheYearLabel = i * 13;
            allMonths[correctPositionOfTheYearLabel] = Integer.parseInt(currentYear.getLabel());
            int[] currentMonths = currentYear.getMonths();
            int startPositionForMonths = correctPositionOfTheYearLabel + 1;
            for (int j = 0; j < 12; j++) {
                allMonths[startPositionForMonths + j] = currentMonths[j];
            }
        }
        StatsUIHelper.reloadLinearLayout(this, new TemporaryAdapter(this, allMonths, mRestResponseParsed.getHighestMonth()), mMonthsAndYearsList, allMonths.length);


        //FIXME: Ugly trick to show all the months on the screen in a simple list. Need to change this. Shame on me!
        mAveragesList.setVisibility(View.VISIBLE);
        List<PostViewsModel.Average> averages = mRestResponseParsed.getAverages();
        Integer[] allAverages = new Integer[averages.size() * 13];
        for (int i = 0; i < averages.size() ; i++) {
            PostViewsModel.Average currentAverage = averages.get(i);
            int correctPositionOfTheAverageLabel = i * 13;
            allAverages[correctPositionOfTheAverageLabel] = Integer.parseInt(currentAverage.getLabel());
            int[] currentAverages = currentAverage.getMonths();
            int startPositionForAverages = correctPositionOfTheAverageLabel + 1;
            for (int j = 0; j < 12; j++) {
                allAverages[startPositionForAverages + j] = currentAverages[j];
            }
        }
        StatsUIHelper.reloadLinearLayout(this, new TemporaryAdapter(this, allAverages, mRestResponseParsed.getHighestDayAverage()), mAveragesList, allAverages.length);
    }

    private void setMainViewsLabel(String dateFormatted, int totals) {
        mStatsViewsLabel.setText(getString(R.string.stats_views) + ": "
                + dateFormatted);
        mStatsViewsTotals.setText(String.valueOf(totals));
    }


    public class TemporaryAdapter extends ArrayAdapter<Integer> {

        private final Integer[] list;
        private final Activity context;
        private final LayoutInflater inflater;
        private final int mValueToHighlight;

        public TemporaryAdapter(Activity context, Integer[] list, int valueToHighlight) {
            super(context, R.layout.stats_list_cell, list);
            this.context = context;
            this.list = list;
            this.mValueToHighlight = valueToHighlight;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;
            // reuse views
            if (rowView == null) {
                rowView = inflater.inflate(R.layout.stats_list_cell, null);
                // configure view holder
                StatsViewHolder viewHolder = new StatsViewHolder(rowView);
                rowView.setTag(viewHolder);
            }

            int currentRowData = list[position];
            StatsViewHolder holder = (StatsViewHolder) rowView.getTag();
            // fill data
            if (position == 0 || (position % 13) == 0) {
                // entries
                holder.entryTextView.setText("" + currentRowData);
                // totals
                holder.totalsTextView.setText("");
            } else {
                // entries
                holder.entryTextView.setText("" + (position % 13));
                // totals
                if (currentRowData == mValueToHighlight) {
                    holder.totalsTextView.setTextColor(Color.BLUE);
                }
                holder.totalsTextView.setText(FormatUtils.formatDecimal(currentRowData));
            }
            // no icon
            holder.networkImageView.setVisibility(View.GONE);

            return rowView;
        }
    }


    private class RestBatchCallListener implements RestRequest.Listener, RestRequest.ErrorListener {
        private final String mRequestBlogId, mRemotePostID;

        private final WeakReference<Activity> mActivityRef;

        public RestBatchCallListener(Activity activity, String mRequestBlogId, String remotePostID) {
            mActivityRef = new WeakReference<Activity>(activity);
            this.mRequestBlogId = mRequestBlogId;
            this.mRemotePostID = remotePostID;
        }

        @Override
        public void onResponse(final JSONObject response) {
            if (mActivityRef.get() == null || mActivityRef.get().isFinishing()) {
                return;
            }
            mIsUpdatingStats = false;
            mSwipeToRefreshHelper.setRefreshing(false);
            // single background thread used to parse the response in BG.
            ThreadPoolExecutor parseResponseExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
            parseResponseExecutor.submit(new Thread() {
                @Override
                public void run() {
                    AppLog.d(AppLog.T.STATS, "The REST response: " + response.toString());
                    mSelectedBarGraphIndex = -1;
                    try {
                        mRestResponseParsed = new PostViewsModel(response);
                    } catch (JSONException e) {
                        AppLog.e(AppLog.T.STATS, "Cannot parse the JSON response", e);
                        resetModelVariables();
                    }
                    // Update the UI
                    final boolean post = mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            updateUI();
                        }
                    });
                }
            });
        }

        @Override
        public void onErrorResponse(final VolleyError volleyError) {
            if (volleyError != null) {
                AppLog.e(AppLog.T.STATS, "Error while reading Stats details "
                        + volleyError.getMessage(), volleyError);
            }
            if (mActivityRef.get() == null || mActivityRef.get().isFinishing()) {
                return;
            }
            resetModelVariables();
            ToastUtils.showToast(mActivityRef.get(),
                    mActivityRef.get().getString(R.string.error_refresh_stats),
                    ToastUtils.Duration.LONG);
            mIsUpdatingStats = false;
            mSwipeToRefreshHelper.setRefreshing(false);
            updateUI();
        }
    }

    private void resetModelVariables() {
        mRestResponseParsed = null;
        mSelectedBarGraphIndex = -1;
    }


    @Override
    public void onBarTapped(int tappedBar) {
        mSelectedBarGraphIndex = tappedBar;
        final VisitModel[] dataToShowOnGraph = getDataToShowOnGraph();
        String currentItemStatsDate = dataToShowOnGraph[mSelectedBarGraphIndex].getPeriod();
        currentItemStatsDate = StatsUtils.parseDate(currentItemStatsDate, "yyyy-MM-dd", "MMM d");
        setMainViewsLabel(currentItemStatsDate, dataToShowOnGraph[mSelectedBarGraphIndex].getViews());
    }

}
