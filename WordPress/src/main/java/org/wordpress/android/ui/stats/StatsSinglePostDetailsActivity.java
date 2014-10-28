package org.wordpress.android.ui.stats;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.stats.model.VisitModel;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ptr.PullToRefreshHelper;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;

/**
 *  Single item details activity.
 */
public class StatsSinglePostDetailsActivity extends WPActionBarActivity {
    public static final String ARG_REMOTE_POST_ID = "ARG_REMOTE_POST_ID";
    public static final String ARG_REST_RESPONSE = "ARG_REST_RESPONSE";

    private boolean mIsInFront;
    private boolean mIsUpdatingStats;
    private PullToRefreshHelper mPullToRefreshHelper;
  //  private String mStatsDate = null;
    private final Handler mHandler = new Handler();
   // private int mAltRowColor;
    private int mLocalBlogID = -1;
    private String mRemotePostID = null; // This is a string since postID could be very looong.

    // Variables that hold data returned from the REST API
    private int mVisitorsCount = 0;
    private int mViewsCount = 0;
    private float mViewsPerVisitor = 0f;


    private LinearLayout mGraphContainer;
    private StatsBarGraph mGraphView;
    private GraphViewSeries mCurrentSeriesOnScreen;
    private TextView mWholeResponse;
    private PostViewsModel mRestResponseParsed;
    private String mRestResponse;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.stats_activity_details);

        if (savedInstanceState == null) {
            // AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_ACCESSED);
            // TODO: add analytics here
        }

       // mAltRowColor = getResources().getColor(R.color.stats_alt_row);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // pull to refresh setup
        mPullToRefreshHelper = new PullToRefreshHelper(this, (PullToRefreshLayout) findViewById(R.id.ptr_layout),
                new PullToRefreshHelper.RefreshListener() {
                    @Override
                    public void onRefreshStarted(View view) {
                        if (!NetworkUtils.checkConnection(getBaseContext())) {
                            mPullToRefreshHelper.setRefreshing(false);
                            return;
                        }
                        refreshStats();
                    }
                }
        );

        mGraphContainer = (LinearLayout) findViewById(R.id.stats_bar_chart_fragment_container);
        mWholeResponse = (TextView) findViewById(R.id.stats_all_response);

        if (savedInstanceState != null) {
            mLocalBlogID = savedInstanceState.getInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, -1);
            mRemotePostID = savedInstanceState.getString(ARG_REMOTE_POST_ID, null);
            mRestResponse = savedInstanceState.getString(ARG_REST_RESPONSE, null);
        } else if (getIntent() != null) {
            Bundle extras = getIntent().getExtras();
            mLocalBlogID = extras.getInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, -1);
            mRemotePostID = extras.getString(ARG_REMOTE_POST_ID, null);
            mRestResponse = extras.getString(ARG_REST_RESPONSE, null);
        }

        setTitle(getString(R.string.stats));
        setupEmptyGraph();

        if (mRestResponse == null) {
            refreshStats();
        } else {
            mRestResponseParsed = new PostViewsModel(mRestResponse);
            updateGraph(-1);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, mLocalBlogID);
        outState.putString(ARG_REMOTE_POST_ID, mRemotePostID);
        outState.putSerializable(ARG_REST_RESPONSE, mRestResponse);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPullToRefreshHelper.registerReceiver(this);
        mIsInFront = true;
        //refreshStats();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsInFront = false;
        mPullToRefreshHelper.unregisterReceiver(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.stats_details, menu);
        return true;
    }

    private class PostViewsModel implements Serializable {
        private String mOriginalResponse;
        //private int mHighestMonth, mHighestDayAverage, mHighestWeekAverage;
        //private String mDate;
        private VisitModel[] mDayViews;

        public String getOriginalResponse() {
            return mOriginalResponse;
        }

        public VisitModel[] getDayViews() {
            return mDayViews;
        }

        PostViewsModel(String response) {
            try {
                this.mOriginalResponse = response;
                JSONObject responseObj = new JSONObject(response);
                parseResponseObject(responseObj);
            } catch (JSONException e) {
                AppLog.e(AppLog.T.STATS, "Cannot parse the JSON response", e);
            }
        }

        PostViewsModel(JSONObject response) {
            if (response == null) {
                return;
            }
            this.mOriginalResponse = response.toString();
            parseResponseObject(response);
        }

        private void parseResponseObject(JSONObject response) {
            try {
                JSONArray dataJSON =  response.getJSONArray("data");
                if (dataJSON != null) {
                    // Read the position/index of each field in the response
                    HashMap<String, Integer> columnsMapping = new HashMap<String, Integer>(2);
                    JSONArray fieldsJSON = response.getJSONArray("fields");
                    try {
                        for (int i = 0; i < fieldsJSON.length(); i++) {
                            final String field = fieldsJSON.getString(i);
                            columnsMapping.put(field, i);
                        }
                    } catch (JSONException e) {
                        AppLog.e(AppLog.T.STATS, "Cannot read the fields indexes from the JSON response", e);
                        throw e;
                    }

                    VisitModel[] visitModels = new VisitModel[dataJSON.length()];
                    int viewsColumnIndex = columnsMapping.get("views");
                    int periodColumnIndex = columnsMapping.get("period");

                    for (int i = 0; i < dataJSON.length(); i++) {
                        try {
                            JSONArray currentDayData = dataJSON.getJSONArray(i);
                            VisitModel currentVisitModel = new VisitModel();
                            currentVisitModel.setPeriod(currentDayData.getString(periodColumnIndex));
                            currentVisitModel.setViews(currentDayData.getInt(viewsColumnIndex));
                            visitModels[i] = currentVisitModel;
                        } catch (JSONException e) {
                            AppLog.e(AppLog.T.STATS, "Cannot create the Visit at index " + i, e);
                        }
                    }
                    mDayViews = visitModels;
                } else {
                    mDayViews = null;
                }
            } catch (JSONException e) {
                AppLog.e(AppLog.T.STATS, e);
                mDayViews = null;
            }
        }
    }

    private void refreshStats() {
        if (mIsUpdatingStats) {
            AppLog.w(AppLog.T.STATS, "stats details are already updating for the following postID "
                    + mRemotePostID + ", refresh cancelled.");
            return;
        }

        final Blog currentBlog = WordPress.getBlog(mLocalBlogID);

        if (mRemotePostID == null || currentBlog == null || !NetworkUtils.isNetworkAvailable(this)) {
            mPullToRefreshHelper.setRefreshing(false);
            int errorMessageId = (mRemotePostID == null || currentBlog == null) ? R.string.error_refresh_stats
                    : R.string.connection_error;
            ToastUtils.showToast(this, this.getString(errorMessageId), ToastUtils.Duration.LONG);
            return;
        }

        final RestClientUtils restClientUtils = WordPress.getRestClientUtilsV1_1();
        final String blogId = StatsUtils.getBlogId(mLocalBlogID);

        // View and visitor counts for a site
        final String singlePostRestPath = String.format(
                "/sites/%s/stats/post-views?post=%s", blogId, mRemotePostID);

        AppLog.d(AppLog.T.STATS, "Enqueuing the following Stats request " + singlePostRestPath);

        RestBatchCallListener vListener = new RestBatchCallListener(this, blogId, mRemotePostID);
        restClientUtils.get(singlePostRestPath, vListener, vListener);

        mIsUpdatingStats = true;
        mPullToRefreshHelper.setRefreshing(true);
        return;
    }

    private void setupEmptyGraph() {
        Context context = mGraphContainer.getContext();
        if (context != null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View emptyBarGraphView = inflater.inflate(R.layout.stats_bar_graph_empty, mGraphContainer, false);
            if (emptyBarGraphView != null) {
                mGraphContainer.removeAllViews();
                mGraphContainer.addView(emptyBarGraphView);
            }
        }
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

        int numPoints = Math.min(10, dayViews.length); //FIXME : 10 for now. this oculd change when the screen size changes
        int currentPointIndex = numPoints - 1;
        VisitModel[] visitModels = new VisitModel[numPoints];

        for (int i = dayViews.length - 1; i >= 0 && currentPointIndex >= 0; i--) {
            visitModels[currentPointIndex] = dayViews[i];
            currentPointIndex--;
        }

        return visitModels;
    }

    private void updateGraph(int barToHighlight) {
        final VisitModel[] dataToShowOnGraph = getDataToShowOnGraph();
        final String[] horLabels = new String[dataToShowOnGraph.length];
        String[] mStatsDate = new String[dataToShowOnGraph.length];
        GraphView.GraphViewData[] views = new GraphView.GraphViewData[dataToShowOnGraph.length];
        boolean isEmptyGraph = true;

        for (int i = 0; i < dataToShowOnGraph.length; i++) {
            int currentItemValue = 0;
            currentItemValue = dataToShowOnGraph[i].getViews();
            views[i] = new GraphView.GraphViewData(i, currentItemValue);

            String currentItemStatsDate = dataToShowOnGraph[i].getPeriod();
            horLabels[i] = StatsUtils.parseDate(currentItemStatsDate, "yyyy-MM-dd", "MMM d");
            mStatsDate[i] = currentItemStatsDate;
            if (currentItemValue > 0) {
                isEmptyGraph = false;
            }
        }

        mCurrentSeriesOnScreen = new GraphViewSeries(views);
        mCurrentSeriesOnScreen.getStyle().color = getResources().getColor(R.color.stats_bar_graph_views);
        mCurrentSeriesOnScreen.getStyle().padding = DisplayUtils.dpToPx(this, 1);

        if (isEmptyGraph) {
            setupEmptyGraph();
        } else {
            if (mGraphContainer.getChildCount() >= 1 && mGraphContainer.getChildAt(0) instanceof GraphView) {
                mGraphView = (StatsBarGraph) mGraphContainer.getChildAt(0);
            } else {
                mGraphContainer.removeAllViews();
                mGraphView = new StatsBarGraph(this);
                mGraphContainer.addView(mGraphView);
            }

            if (mGraphView != null) {
                mGraphView.removeAllSeries();
                mGraphView.addSeries(mCurrentSeriesOnScreen);
                //mGraphView.getGraphViewStyle().setNumHorizontalLabels(getNumOfHorizontalLabels(dataToShowOnGraph.length));
                mGraphView.getGraphViewStyle().setNumHorizontalLabels(dataToShowOnGraph.length);
                mGraphView.setHorizontalLabels(horLabels);
                //mGraphView.setGestureListener(this);

                 if (barToHighlight != -1) {
                    mGraphView.highlightBar(barToHighlight);
                } else {
                    mGraphView.highlightBar(dataToShowOnGraph.length - 1);
                }
            }
        }

        if (mRestResponseParsed.getOriginalResponse() != null) {
            mWholeResponse.setText(mRestResponseParsed.getOriginalResponse().toString());
        }

        //int barSelectedOnGraph = barToHighlight != -1 ? barToHighlight : dataToShowOnGraph.length - 1;
        //updateOverviewAreaBelowTheGraph(barSelectedOnGraph);
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
            if (mActivityRef.get() == null) {
                return;
            }
            mIsUpdatingStats = false;
            mPullToRefreshHelper.setRefreshing(false);
            AppLog.d(AppLog.T.STATS, "The REST response: " + response.toString());
            mRestResponse = response.toString();
            mRestResponseParsed = new PostViewsModel(response);

            // single background thread used to parse the response.
            ThreadPoolExecutor parseResponseExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
            parseResponseExecutor.submit(new Thread() {
                @Override
                public void run() {
                    // Update the UI
                    final boolean post = mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            updateGraph(-1);
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
            ToastUtils.showToast(mActivityRef.get(),
                    mActivityRef.get().getString(R.string.error_refresh_stats),
                    ToastUtils.Duration.LONG);
            mIsUpdatingStats = false;
            mPullToRefreshHelper.setRefreshing(false);
        }
    }
}
