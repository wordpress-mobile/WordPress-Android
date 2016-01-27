package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NoConnectionError;
import com.android.volley.VolleyError;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.stats.models.PostViewsModel;
import org.wordpress.android.ui.stats.models.VisitModel;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


/**
 *  Single item details activity.
 */
public class StatsSingleItemDetailsActivity extends AppCompatActivity
        implements StatsBarGraph.OnGestureListener{

    public static final String ARG_REMOTE_BLOG_ID = "ARG_REMOTE_BLOG_ID";
    public static final String ARG_REMOTE_ITEM_ID = "ARG_REMOTE_ITEM_ID";
    public static final String ARG_REMOTE_ITEM_TYPE = "ARG_REMOTE_ITEM_TYPE";
    public static final String ARG_ITEM_TITLE = "ARG_ITEM_TITLE";
    public static final String ARG_ITEM_URL = "ARG_ITEM_URL";
    private static final String ARG_REST_RESPONSE = "ARG_REST_RESPONSE";
    private static final String ARG_SELECTED_GRAPH_BAR = "ARG_SELECTED_GRAPH_BAR";
    private static final String ARG_PREV_NUMBER_OF_BARS = "ARG_PREV_NUMBER_OF_BARS";
    private static final String SAVED_STATS_SCROLL_POSITION = "SAVED_STATS_SCROLL_POSITION";

    private boolean mIsUpdatingStats;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private ScrollViewExt mOuterScrollView;

    private final Handler mHandler = new Handler();

    private LinearLayout mGraphContainer;
    private TextView mStatsViewsLabel;
    private TextView mStatsViewsTotals;

    private LinearLayout mMonthsAndYearsModule;
    private LinearLayout mMonthsAndYearsList;
    private RelativeLayout mMonthsAndYearsHeader;
    private LinearLayout mMonthsAndYearsEmptyPlaceholder;

    private LinearLayout mAveragesModule;
    private LinearLayout mAveragesList;
    private RelativeLayout mAveragesHeader;
    private LinearLayout mAveragesEmptyPlaceholder;

    private LinearLayout mRecentWeeksModule;
    private LinearLayout mRecentWeeksList;
    private RelativeLayout mRecentWeeksHeader;
    private LinearLayout mRecentWeeksEmptyPlaceholder;
    private String mRemoteBlogID, mRemoteItemID, mRemoteItemType, mItemTitle, mItemURL;
    private PostViewsModel mRestResponseParsed;
    private int mSelectedBarGraphIndex = -1;
    private int mPrevNumberOfBarsGraph = -1;

    private SparseBooleanArray mYearsIdToExpandedMap;
    private SparseBooleanArray mAveragesIdToExpandedMap;
    private SparseBooleanArray mRecentWeeksIdToExpandedMap;

    private static final String ARG_YEARS_EXPANDED_ROWS = "ARG_YEARS_EXPANDED_ROWS";
    private static final String ARG_AVERAGES_EXPANDED_ROWS = "ARG_AVERAGES_EXPANDED_ROWS";
    private static final String ARG_RECENT_EXPANDED_ROWS = "ARG_RECENT_EXPANDED_ROWS";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.stats_activity_single_post_details);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // pull to refresh setup
        mSwipeToRefreshHelper = new SwipeToRefreshHelper(this, (CustomSwipeRefreshLayout) findViewById(R.id.ptr_layout),
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

        TextView mStatsForLabel = (TextView) findViewById(R.id.stats_summary_title);
        mGraphContainer = (LinearLayout) findViewById(R.id.stats_bar_chart_fragment_container);
        mStatsViewsLabel = (TextView) findViewById(R.id.stats_views_label);
        mStatsViewsTotals = (TextView) findViewById(R.id.stats_views_totals);

        mMonthsAndYearsModule = (LinearLayout) findViewById(R.id.stats_months_years_module);
        mMonthsAndYearsHeader = (RelativeLayout) findViewById(R.id.stats_months_years_header);
        mMonthsAndYearsList = (LinearLayout) findViewById(R.id.stats_months_years_list_linearlayout);
        mMonthsAndYearsEmptyPlaceholder = (LinearLayout) findViewById(R.id.stats_months_years_empty_module_placeholder);

        mAveragesModule = (LinearLayout) findViewById(R.id.stats_averages_module);
        mAveragesHeader = (RelativeLayout) findViewById(R.id.stats_averages_list_header);
        mAveragesList = (LinearLayout) findViewById(R.id.stats_averages_list_linearlayout);
        mAveragesEmptyPlaceholder = (LinearLayout) findViewById(R.id.stats_averages_empty_module_placeholder);

        mRecentWeeksModule = (LinearLayout) findViewById(R.id.stats_recent_weeks_module);
        mRecentWeeksHeader = (RelativeLayout) findViewById(R.id.stats_recent_weeks_list_header);
        mRecentWeeksList = (LinearLayout) findViewById(R.id.stats_recent_weeks_list_linearlayout);
        mRecentWeeksEmptyPlaceholder = (LinearLayout) findViewById(R.id.stats_recent_weeks_empty_module_placeholder);

        mYearsIdToExpandedMap = new SparseBooleanArray();
        mAveragesIdToExpandedMap = new SparseBooleanArray();
        mRecentWeeksIdToExpandedMap = new SparseBooleanArray();

        setTitle(R.string.stats);
        mOuterScrollView = (ScrollViewExt) findViewById(R.id.scroll_view_stats);

        if (savedInstanceState != null) {
            mRemoteItemID = savedInstanceState.getString(ARG_REMOTE_ITEM_ID);
            mRemoteBlogID = savedInstanceState.getString(ARG_REMOTE_BLOG_ID);
            mRemoteItemType = savedInstanceState.getString(ARG_REMOTE_ITEM_TYPE);
            mItemTitle = savedInstanceState.getString(ARG_ITEM_TITLE);
            mItemURL = savedInstanceState.getString(ARG_ITEM_URL);
            mRestResponseParsed = (PostViewsModel) savedInstanceState.getSerializable(ARG_REST_RESPONSE);
            mSelectedBarGraphIndex = savedInstanceState.getInt(ARG_SELECTED_GRAPH_BAR, -1);
            mPrevNumberOfBarsGraph = savedInstanceState.getInt(ARG_PREV_NUMBER_OF_BARS, -1);

            final int yScrollPosition = savedInstanceState.getInt(SAVED_STATS_SCROLL_POSITION);
            if(yScrollPosition != 0) {
                mOuterScrollView.postDelayed(new Runnable() {
                    public void run() {
                        if (!isFinishing()) {
                            mOuterScrollView.scrollTo(0, yScrollPosition);
                        }
                    }
                }, StatsConstants.STATS_SCROLL_TO_DELAY);
            }
            if (savedInstanceState.containsKey(ARG_AVERAGES_EXPANDED_ROWS)) {
                mAveragesIdToExpandedMap = savedInstanceState.getParcelable(ARG_AVERAGES_EXPANDED_ROWS);
            }
            if (savedInstanceState.containsKey(ARG_RECENT_EXPANDED_ROWS)) {
                mRecentWeeksIdToExpandedMap = savedInstanceState.getParcelable(ARG_RECENT_EXPANDED_ROWS);
            }
            if (savedInstanceState.containsKey(ARG_YEARS_EXPANDED_ROWS)) {
                mYearsIdToExpandedMap = savedInstanceState.getParcelable(ARG_YEARS_EXPANDED_ROWS);
            }
        } else if (getIntent() != null && getIntent().getExtras() != null) {
            Bundle extras = getIntent().getExtras();
            mRemoteItemID = extras.getString(ARG_REMOTE_ITEM_ID);
            mRemoteBlogID = extras.getString(ARG_REMOTE_BLOG_ID);
            mRemoteItemType = extras.getString(ARG_REMOTE_ITEM_TYPE);
            mItemTitle = extras.getString(ARG_ITEM_TITLE);
            mItemURL = extras.getString(ARG_ITEM_URL);
            mRestResponseParsed = (PostViewsModel) extras.getSerializable(ARG_REST_RESPONSE);
            mSelectedBarGraphIndex = extras.getInt(ARG_SELECTED_GRAPH_BAR, -1);
        }

        if (mRemoteBlogID == null || mRemoteItemID == null) {
            Toast.makeText(this, R.string.stats_generic_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (savedInstanceState == null) {
            AnalyticsUtils.trackWithBlogDetails(
                    AnalyticsTracker.Stat.STATS_SINGLE_POST_ACCESSED,
                    mRemoteBlogID
            );
        }

        // Setup the main top label that opens the post in the Reader where possible
        if (mItemTitle != null || mItemURL != null) {
            mStatsForLabel.setVisibility(View.VISIBLE);
            mStatsForLabel.setText(mItemTitle != null ? mItemTitle : mItemURL );
            // make the label clickable if the URL is available
            if (mItemURL != null) {
                mStatsForLabel.setTextColor(getResources().getColor(R.color.stats_link_text_color));
                mStatsForLabel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final Context ctx = v.getContext();
                        StatsUtils.openPostInReaderOrInAppWebview(ctx,
                                mRemoteBlogID,
                                mRemoteItemID,
                                mRemoteItemType,
                                mItemURL);
                    }
                });
            } else {
                mStatsForLabel.setTextColor(getResources().getColor(R.color.grey_darken_20));
            }
        } else {
            mStatsForLabel.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(ARG_SELECTED_GRAPH_BAR, mSelectedBarGraphIndex);
        outState.putInt(ARG_PREV_NUMBER_OF_BARS, mPrevNumberOfBarsGraph);
        outState.putString(ARG_REMOTE_BLOG_ID, mRemoteBlogID);
        outState.putString(ARG_REMOTE_ITEM_ID, mRemoteItemID);
        outState.putString(ARG_REMOTE_ITEM_TYPE, mRemoteItemType);
        outState.putString(ARG_ITEM_TITLE, mItemTitle);
        outState.putString(ARG_ITEM_URL, mItemURL);

        outState.putSerializable(ARG_REST_RESPONSE, mRestResponseParsed);
        if (mOuterScrollView.getScrollY() != 0) {
            outState.putInt(SAVED_STATS_SCROLL_POSITION, mOuterScrollView.getScrollY());
        }

        if (mAveragesIdToExpandedMap.size() > 0){
            outState.putParcelable(ARG_AVERAGES_EXPANDED_ROWS, new SparseBooleanArrayParcelable(mAveragesIdToExpandedMap));
        }
        if (mRecentWeeksIdToExpandedMap.size() > 0) {
            outState.putParcelable(ARG_RECENT_EXPANDED_ROWS, new SparseBooleanArrayParcelable(mRecentWeeksIdToExpandedMap));
        }
        if (mYearsIdToExpandedMap.size() > 0) {
            outState.putParcelable(ARG_YEARS_EXPANDED_ROWS, new SparseBooleanArrayParcelable(mYearsIdToExpandedMap));
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRestResponseParsed == null) {
            // check if network is available, if not shows the empty UI immediately
            if (!NetworkUtils.checkConnection(this)) {
                mSwipeToRefreshHelper.setRefreshing(false);
                setupEmptyUI();
            } else {
                setupEmptyGraph("");
                showHideEmptyModulesIndicator(true);
                refreshStats();
            }
        } else {
            updateUI();
        }
        ActivityId.trackLastActivity(ActivityId.STATS_POST_DETAILS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsUpdatingStats = false;
        mSwipeToRefreshHelper.setRefreshing(false);
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

        if (mIsUpdatingStats) {
            AppLog.w(AppLog.T.STATS, "stats details are already updating for the following postID "
                    + mRemoteItemID + ", refresh cancelled.");
            return;
        }

        if (!NetworkUtils.checkConnection(this)) {
            mSwipeToRefreshHelper.setRefreshing(false);
            return;
        }

        final RestClientUtils restClientUtils = WordPress.getRestClientUtilsV1_1();


        // View and visitor counts for a site
        final String singlePostRestPath = String.format(
                "/sites/%s/stats/post/%s", mRemoteBlogID, mRemoteItemID);

        AppLog.d(AppLog.T.STATS, "Enqueuing the following Stats request " + singlePostRestPath);

        RestBatchCallListener vListener = new RestBatchCallListener(this);
        restClientUtils.get(singlePostRestPath, vListener, vListener);

        mIsUpdatingStats = true;
        mSwipeToRefreshHelper.setRefreshing(true);
    }

    private void showHideEmptyModulesIndicator(boolean show) {
        if (isFinishing()) {
            return;
        }

        mMonthsAndYearsModule.setVisibility(View.VISIBLE);
        mRecentWeeksModule.setVisibility(View.VISIBLE);
        mAveragesModule.setVisibility(View.VISIBLE);

        mMonthsAndYearsHeader.setVisibility(show ? View.GONE : View.VISIBLE);
        mRecentWeeksHeader.setVisibility(show ? View.GONE : View.VISIBLE);
        mAveragesHeader.setVisibility(show ? View.GONE : View.VISIBLE);

        mMonthsAndYearsList.setVisibility(show ? View.GONE : View.VISIBLE);
        mAveragesList.setVisibility(show ? View.GONE : View.VISIBLE);
        mRecentWeeksList.setVisibility(show ? View.GONE : View.VISIBLE);

        mMonthsAndYearsEmptyPlaceholder.setVisibility(show ? View.VISIBLE : View.GONE);
        mRecentWeeksEmptyPlaceholder.setVisibility(show ? View.VISIBLE : View.GONE);
        mAveragesEmptyPlaceholder.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void setupEmptyUI() {
        if (isFinishing()) {
            return;
        }

        setupEmptyGraph(null);

        mMonthsAndYearsModule.setVisibility(View.GONE);
        mRecentWeeksModule.setVisibility(View.GONE);
        mAveragesModule.setVisibility(View.GONE);

        mRecentWeeksIdToExpandedMap.clear();
        mAveragesIdToExpandedMap.clear();
        mYearsIdToExpandedMap.clear();
    }

    private void setupEmptyGraph(String emptyLabel) {
        if (isFinishing()) {
            return;
        }
        Context context = mGraphContainer.getContext();
        if (context != null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View emptyBarGraphView = inflater.inflate(R.layout.stats_bar_graph_empty, mGraphContainer, false);
            if (emptyLabel != null) {
                final TextView emptyLabelField = (TextView) emptyBarGraphView.findViewById(R.id.stats_bar_graph_empty_label);
                emptyLabelField.setText(emptyLabel);
            }
            mGraphContainer.removeAllViews();
            mGraphContainer.addView(emptyBarGraphView);
        }
        mStatsViewsLabel.setText("");
        mStatsViewsTotals.setText("");
    }

    private VisitModel[] getDataToShowOnGraph () {
        if (mRestResponseParsed == null) {
            return new VisitModel[0];
        }

        final VisitModel[] dayViews = mRestResponseParsed.getDayViews();
        if (dayViews == null) {
            return new VisitModel[0];
        }

        int numPoints = Math.min(StatsUIHelper.getNumOfBarsToShow(), dayViews.length);
        int currentPointIndex = numPoints - 1;
        VisitModel[] visitModels = new VisitModel[numPoints];

        for (int i = dayViews.length - 1; i >= 0 && currentPointIndex >= 0; i--) {
            visitModels[currentPointIndex] = dayViews[i];
            currentPointIndex--;
        }

        return visitModels;
    }

    private void updateUI() {
        if (isFinishing()) {
            return;
        }
        final VisitModel[] dataToShowOnGraph = getDataToShowOnGraph();

        if (dataToShowOnGraph == null || dataToShowOnGraph.length == 0) {
            setupEmptyUI();
            return;
        }

        final String[] horLabels = new String[dataToShowOnGraph.length];
        String[] mStatsDate = new String[dataToShowOnGraph.length];
        GraphView.GraphViewData[] views = new GraphView.GraphViewData[dataToShowOnGraph.length];

        for (int i = 0; i < dataToShowOnGraph.length; i++) {
            int currentItemValue = dataToShowOnGraph[i].getViews();
            views[i] = new GraphView.GraphViewData(i, currentItemValue);

            String currentItemStatsDate = dataToShowOnGraph[i].getPeriod();
            horLabels[i] = StatsUtils.parseDate(
                    currentItemStatsDate,
                    StatsConstants.STATS_INPUT_DATE_FORMAT,
                    StatsConstants.STATS_OUTPUT_DATE_MONTH_SHORT_DAY_SHORT_FORMAT
            );
            mStatsDate[i] = currentItemStatsDate;
        }

        GraphViewSeries mCurrentSeriesOnScreen = new GraphViewSeries(views);
        mCurrentSeriesOnScreen.getStyle().color = getResources().getColor(R.color.stats_bar_graph_main_series);
        mCurrentSeriesOnScreen.getStyle().highlightColor = getResources().getColor(R.color.stats_bar_graph_main_series_highlight);
        mCurrentSeriesOnScreen.getStyle().outerhighlightColor = getResources().getColor(R.color.stats_bar_graph_outer_highlight);
        mCurrentSeriesOnScreen.getStyle().padding = DisplayUtils.dpToPx(this, 5);

        StatsBarGraph mGraphView;
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
        mGraphView.getGraphViewStyle().setMaxColumnWidth(
                DisplayUtils.dpToPx(this, StatsConstants.STATS_GRAPH_BAR_MAX_COLUMN_WIDTH_DP)
        );
        mGraphView.setHorizontalLabels(horLabels);
        mGraphView.setGestureListener(this);

        // Reset the bar selected upon rotation of the device when the no. of bars can change with orientation.
        // Only happens on 720DP tablets
        if (mPrevNumberOfBarsGraph != -1 && mPrevNumberOfBarsGraph != dataToShowOnGraph.length) {
            mSelectedBarGraphIndex = dataToShowOnGraph.length - 1;
        } else {
            mSelectedBarGraphIndex = (mSelectedBarGraphIndex != -1) ? mSelectedBarGraphIndex : dataToShowOnGraph.length - 1;
        }

        mGraphView.highlightBar(mSelectedBarGraphIndex);
        mPrevNumberOfBarsGraph = dataToShowOnGraph.length;

        setMainViewsLabel(
                StatsUtils.parseDate(
                        mStatsDate[mSelectedBarGraphIndex],
                        StatsConstants.STATS_INPUT_DATE_FORMAT,
                        StatsConstants.STATS_OUTPUT_DATE_MONTH_LONG_DAY_SHORT_FORMAT
                ),
                dataToShowOnGraph[mSelectedBarGraphIndex].getViews()
        );

        showHideEmptyModulesIndicator(false);

        mMonthsAndYearsList.setVisibility(View.VISIBLE);
        List<PostViewsModel.Year> years = mRestResponseParsed.getYears();
        MonthsAndYearsListAdapter monthsAndYearsListAdapter = new MonthsAndYearsListAdapter(this, years, mRestResponseParsed.getHighestMonth());
        StatsUIHelper.reloadGroupViews(this, monthsAndYearsListAdapter, mYearsIdToExpandedMap, mMonthsAndYearsList);

        mAveragesList.setVisibility(View.VISIBLE);
        List<PostViewsModel.Year> averages = mRestResponseParsed.getAverages();
        MonthsAndYearsListAdapter averagesListAdapter = new MonthsAndYearsListAdapter(this, averages, mRestResponseParsed.getHighestDayAverage());
        StatsUIHelper.reloadGroupViews(this, averagesListAdapter, mAveragesIdToExpandedMap, mAveragesList);

        mRecentWeeksList.setVisibility(View.VISIBLE);
        List<PostViewsModel.Week> recentWeeks = mRestResponseParsed.getWeeks();
        RecentWeeksListAdapter recentWeeksListAdapter = new RecentWeeksListAdapter(this, recentWeeks, mRestResponseParsed.getHighestWeekAverage());
        StatsUIHelper.reloadGroupViews(this, recentWeeksListAdapter, mRecentWeeksIdToExpandedMap, mRecentWeeksList);
     }


    private void setMainViewsLabel(String dateFormatted, int totals) {
        mStatsViewsLabel.setText(getString(R.string.stats_views) + ": "
                + dateFormatted);
        mStatsViewsTotals.setText(FormatUtils.formatDecimal(totals));
    }


    private class RecentWeeksListAdapter extends BaseExpandableListAdapter {
        public static final String GROUP_DATE_FORMAT = "MMM dd";
        public final LayoutInflater inflater;
        private final List<PostViewsModel.Week> groups;
        private final int maxReachedValue;

        public RecentWeeksListAdapter(Context context, List<PostViewsModel.Week> groups, int maxReachedValue) {
            this.groups = groups;
            this.inflater = LayoutInflater.from(context);
            this.maxReachedValue = maxReachedValue;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            PostViewsModel.Week currentWeek = groups.get(groupPosition);
            return currentWeek.getDays().get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        @Override
        public View getChildView(int groupPosition, final int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {

            final PostViewsModel.Day currentDay = (PostViewsModel.Day) getChild(groupPosition, childPosition);

            final StatsViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.stats_list_cell, parent, false);
                holder = new StatsViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (StatsViewHolder) convertView.getTag();
            }

            holder.setEntryText(StatsUtils.parseDate(currentDay.getDay(), StatsConstants.STATS_INPUT_DATE_FORMAT, "EEE, MMM dd"));

            // Intercept clicks at row level and eat the event. We don't want to show the ripple here.
            holder.rowContent.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                        }
                    });
            holder.rowContent.setBackgroundColor(Color.TRANSPARENT);

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(currentDay.getCount()));

            // show the trophy indicator if the value is the maximum reached
            if (currentDay.getCount() == maxReachedValue && maxReachedValue > 0) {
                holder.imgMore.setVisibility(View.VISIBLE);
                holder.imgMore.setImageDrawable(getResources().getDrawable(R.drawable.stats_icon_trophy));
                holder.imgMore.setBackgroundColor(Color.TRANSPARENT); // Hide the default click indicator
            } else {
                holder.imgMore.setVisibility(View.GONE);
            }

            holder.networkImageView.setVisibility(View.GONE);
            return convertView;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            PostViewsModel.Week week = groups.get(groupPosition);
            return week.getDays().size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return groups.get(groupPosition);
        }

        @Override
        public int getGroupCount() {
            return groups.size();
        }


        @Override
        public long getGroupId(int groupPosition) {
            return 0;
        }

        @Override
        public View getGroupView(final int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {

            final StatsViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.stats_list_cell, parent, false);
                holder = new StatsViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (StatsViewHolder) convertView.getTag();
            }

            PostViewsModel.Week week = (PostViewsModel.Week) getGroup(groupPosition);

            int total = week.getTotal();

            // change the color of the text if one of its childs has reached maximum value
            int numberOfChilds = getChildrenCount(groupPosition);
            boolean shouldShowTheTrophyIcon = false;
            if (maxReachedValue > 0) {
                for (int i = 0; i < numberOfChilds; i++) {
                    PostViewsModel.Day currentChild = (PostViewsModel.Day) getChild(groupPosition, i);
                    if (currentChild.getCount() == maxReachedValue) {
                        shouldShowTheTrophyIcon = true;
                    }
                }
            }

            // Build the label to show on the group
            String name;
            PostViewsModel.Day firstChild = (PostViewsModel.Day) getChild(groupPosition, 0);
            if (numberOfChilds > 1) {
                PostViewsModel.Day lastChild = (PostViewsModel.Day) getChild(groupPosition, getChildrenCount(groupPosition) - 1);
                name = StatsUtils.parseDate(firstChild.getDay(), StatsConstants.STATS_INPUT_DATE_FORMAT, GROUP_DATE_FORMAT)
                        + " - " + StatsUtils.parseDate(lastChild.getDay(), StatsConstants.STATS_INPUT_DATE_FORMAT, GROUP_DATE_FORMAT);
            } else {
                name = StatsUtils.parseDate(firstChild.getDay(), StatsConstants.STATS_INPUT_DATE_FORMAT, GROUP_DATE_FORMAT);
            }

            holder.setEntryText(name, getResources().getColor(R.color.stats_link_text_color));

            holder.networkImageView.setVisibility(View.GONE);

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));
            if (shouldShowTheTrophyIcon) {
                holder.imgMore.setVisibility(View.VISIBLE);
                holder.imgMore.setImageDrawable(getResources().getDrawable(R.drawable.stats_icon_trophy));
                holder.imgMore.setBackgroundColor(Color.TRANSPARENT); // Hide the default click indicator
            } else {
                holder.imgMore.setVisibility(View.GONE);
            }

            // expand/collapse chevron
            holder.chevronImageView.setVisibility(numberOfChilds > 0 ? View.VISIBLE : View.GONE);
            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }

    }


    private class MonthsAndYearsListAdapter extends BaseExpandableListAdapter {
        public final LayoutInflater inflater;
        private final List<PostViewsModel.Year> groups;
        private final int maxReachedValue;

        public MonthsAndYearsListAdapter(Context context, List<PostViewsModel.Year> groups, int maxReachedValue) {
            this.groups = groups;
            this.inflater = LayoutInflater.from(context);
            this.maxReachedValue = maxReachedValue;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            PostViewsModel.Year currentYear = groups.get(groupPosition);
            return currentYear.getMonths().get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        @Override
        public View getChildView(int groupPosition, final int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {

            final PostViewsModel.Month currentMonth = (PostViewsModel.Month) getChild(groupPosition, childPosition);

            final StatsViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.stats_list_cell, parent, false);
                holder = new StatsViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (StatsViewHolder) convertView.getTag();
            }

            holder.setEntryText(StatsUtils.parseDate(currentMonth.getMonth(), "MM", StatsConstants.STATS_OUTPUT_DATE_MONTH_LONG_FORMAT));

            // Intercept clicks at row level and eat the event. We don't want to show the ripple here.
            holder.rowContent.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                        }
                    });
            holder.rowContent.setBackgroundColor(Color.TRANSPARENT);

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(currentMonth.getCount()));

            // show the trophy indicator if the value is the maximum reached
            if (currentMonth.getCount() == maxReachedValue && maxReachedValue > 0) {
                holder.imgMore.setVisibility(View.VISIBLE);
                holder.imgMore.setImageDrawable(getResources().getDrawable(R.drawable.stats_icon_trophy));
                holder.imgMore.setBackgroundColor(Color.TRANSPARENT); // Hide the default click indicator
            } else {
                holder.imgMore.setVisibility(View.GONE);
            }

            holder.networkImageView.setVisibility(View.GONE);
            return convertView;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            PostViewsModel.Year currentYear = groups.get(groupPosition);
            return currentYear.getMonths().size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return groups.get(groupPosition);
        }

        @Override
        public int getGroupCount() {
            return groups.size();
        }


        @Override
        public long getGroupId(int groupPosition) {
            return 0;
        }

        @Override
        public View getGroupView(final int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {

            final StatsViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.stats_list_cell, parent, false);
                holder = new StatsViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (StatsViewHolder) convertView.getTag();
            }

            PostViewsModel.Year year = (PostViewsModel.Year) getGroup(groupPosition);

            String name = year.getLabel();
            int total = year.getTotal();

            // change the color of the text if one of its childs has reached maximum value
            int numberOfChilds = getChildrenCount(groupPosition);
            boolean shouldShowTheTrophyIcon = false;
            if (maxReachedValue > 0) {
                for (int i = 0; i < numberOfChilds; i++) {
                    PostViewsModel.Month currentChild = (PostViewsModel.Month) getChild(groupPosition, i);
                    if (currentChild.getCount() == maxReachedValue) {
                        shouldShowTheTrophyIcon = true;
                        break;
                    }
                }
            }

            holder.setEntryText(name, getResources().getColor(R.color.stats_link_text_color));

            if (shouldShowTheTrophyIcon) {
                holder.imgMore.setVisibility(View.VISIBLE);
                holder.imgMore.setImageDrawable(getResources().getDrawable(R.drawable.stats_icon_trophy));
                holder.imgMore.setBackgroundColor(Color.TRANSPARENT); // Hide the default click indicator
            } else {
                holder.imgMore.setVisibility(View.GONE);
            }

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            holder.networkImageView.setVisibility(View.GONE);

            // expand/collapse chevron
            holder.chevronImageView.setVisibility(numberOfChilds > 0 ? View.VISIBLE : View.GONE);
            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }

    }


    private class RestBatchCallListener implements RestRequest.Listener, RestRequest.ErrorListener {

        private final WeakReference<Activity> mActivityRef;

        public RestBatchCallListener(Activity activity) {
            mActivityRef = new WeakReference<>(activity);
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
                    //AppLog.d(AppLog.T.STATS, "The REST response: " + response.toString());
                    mSelectedBarGraphIndex = -1;
                    try {
                        mRestResponseParsed = new PostViewsModel(response);
                    } catch (JSONException e) {
                        AppLog.e(AppLog.T.STATS, "Cannot parse the JSON response", e);
                        resetModelVariables();
                    }

                    // Update the UI
                    mHandler.post(new Runnable() {
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
            StatsUtils.logVolleyErrorDetails(volleyError);
            if (mActivityRef.get() == null || mActivityRef.get().isFinishing()) {
                return;
            }
            resetModelVariables();

            String label = mActivityRef.get().getString(R.string.error_refresh_stats);
            if (volleyError instanceof NoConnectionError) {
                label += "\n" + mActivityRef.get().getString(R.string.no_network_message);
            }

            ToastUtils.showToast(mActivityRef.get(), label, ToastUtils.Duration.LONG);
            mIsUpdatingStats = false;
            mSwipeToRefreshHelper.setRefreshing(false);

            // Update the UI
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateUI();
                }
            });
        }
    }

    private void resetModelVariables() {
        mRestResponseParsed = null;
        mSelectedBarGraphIndex = -1;
        mAveragesIdToExpandedMap.clear();
        mYearsIdToExpandedMap.clear();
    }

    @Override
    public void onBarTapped(int tappedBar) {
        mSelectedBarGraphIndex = tappedBar;
        final VisitModel[] dataToShowOnGraph = getDataToShowOnGraph();
        String currentItemStatsDate = dataToShowOnGraph[mSelectedBarGraphIndex].getPeriod();
        currentItemStatsDate = StatsUtils.parseDate(
                currentItemStatsDate,
                StatsConstants.STATS_INPUT_DATE_FORMAT,
                StatsConstants.STATS_OUTPUT_DATE_MONTH_LONG_DAY_SHORT_FORMAT
        );
        setMainViewsLabel(currentItemStatsDate, dataToShowOnGraph[mSelectedBarGraphIndex].getViews());
    }

}
