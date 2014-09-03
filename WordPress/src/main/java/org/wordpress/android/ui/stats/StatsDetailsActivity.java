package org.wordpress.android.ui.stats;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.StatsClick;
import org.wordpress.android.models.StatsClickGroup;
import org.wordpress.android.models.StatsReferrer;
import org.wordpress.android.models.StatsReferrerGroup;
import org.wordpress.android.models.StatsSearchEngineTerm;
import org.wordpress.android.models.StatsTopPostsAndPages;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ptr.PullToRefreshHelper;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;

/**
 * The native stats details activity, accessible when the user taps on a bar in the main chart.
 */
public class StatsDetailsActivity extends WPActionBarActivity {
    private boolean mIsInFront;
    private boolean mIsUpdatingStats;
    private PullToRefreshHelper mPullToRefreshHelper;
    private String mStatsDate = null;
    private final Handler mHandler = new Handler();
    private int mAltRowColor;
    private int mLocalBlogID = -1;

    // Variables that hold data returned from the REST API
    private int mVisitorsCount = 0;
    private int mViewsCount = 0;
    private float mViewsPerVisitor = 0f;
    private LinkedList<StatsTopPostsAndPages> mTopPostsAndPages = new LinkedList<StatsTopPostsAndPages>();
    private LinkedList<StatsSearchEngineTerm> mSearchEngineTerms = new LinkedList<StatsSearchEngineTerm>();
    // more efficient than HashMap for mapping integers to objects
    private SparseArray<MyStatsReferrerGroup> mReferrerGroups = new SparseArray<MyStatsReferrerGroup>();
    private SparseArray<MyStatsClicksGroup> mClickGroups = new SparseArray<MyStatsClicksGroup>();

    // Lists adapters
    private TopPostsAndPagesAdapter mTopPostsAndPagesAdapter;
    private ReferrersAdapter mReferrersAdapter;
    private ClicksAdapter mClicksAdapter;
    private SearchEngineAdapter mSearchEngineAdapter;

    // Used to keep track of status of UI on the "fake" expandable list
    private SparseBooleanArray mReferrerGroupIdToExpandedMap = new SparseBooleanArray();
    private SparseBooleanArray mClickGroupIdToExpandedMap = new SparseBooleanArray();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // split layout into two for 720DP tablets and 600DP tablets in landscape
        if (StatsUIHelper.shouldLoadSplitLayout(this)) {
            setContentView(R.layout.stats_activity_details_split);
        } else {
            setContentView(R.layout.stats_activity_details);
        }

        if (WordPress.wpDB == null) {
            Toast.makeText(this, R.string.fatal_db_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (savedInstanceState == null) {
            // AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_ACCESSED);
            // TODO: add analytics here
        }

        mAltRowColor = getResources().getColor(R.color.stats_alt_row);

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

        if (getIntent() != null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null && extras.containsKey(StatsActivity.STATS_DETAILS_DATE)) {
                String date = extras.getString(StatsActivity.STATS_DETAILS_DATE);
                mStatsDate = date;
            }
            mLocalBlogID = getIntent().getIntExtra(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, -1);
        }

        setTitle(getString(R.string.stats));

        // Setting up the main date label
       ((TextView) findViewById(R.id.stats_views_visitors_date)).setText(
               StatsUtils.parseDate(mStatsDate, "yyyy-MM-dd", "EEEE, MMMM d, yyyy")
       );

        // Setting up top posts and pages
        mTopPostsAndPagesAdapter = new TopPostsAndPagesAdapter(this, mTopPostsAndPages);
        TextView topPostsAndPagesEmptyTextView = ((TextView) findViewById(R.id.stats_top_posts_and_pages_empty_text));
        setupEmptyLabel(topPostsAndPagesEmptyTextView, R.string.stats_empty_top_posts_title,
                R.string.stats_empty_top_posts_desc);

        // Setting up referrers
        mReferrersAdapter = new ReferrersAdapter(this, mReferrerGroups);
        TextView referrersEmptyTextView = ((TextView) findViewById(R.id.stats_referrers_list_empty_text));
        setupEmptyLabel(referrersEmptyTextView, R.string.stats_empty_referrers_title,
                R.string.stats_empty_referrers_desc);

        // Setting up clicks
        mClicksAdapter = new ClicksAdapter(this, mClickGroups);
        TextView clicksEmptyLabel = ((TextView) findViewById(R.id.stats_clicks_list_empty_text));
        setupEmptyLabel(clicksEmptyLabel, R.string.stats_empty_clicks_title,
                R.string.stats_empty_clicks_desc);

        // Setting up Search engine terms
        mSearchEngineAdapter = new SearchEngineAdapter(this, mSearchEngineTerms);
        TextView seEmptyLabel = ((TextView) findViewById(R.id.stats_searchengine_list_empty_text));
        setupEmptyLabel(seEmptyLabel, R.string.stats_empty_search_engine_terms_title,
                R.string.stats_empty_search_engine_terms_desc);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPullToRefreshHelper.registerReceiver(this);
        mIsInFront = true;
        refreshStats();
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

    private void refreshStats() {
        if (mIsUpdatingStats) {
            AppLog.w(AppLog.T.STATS, "stats details are already updating for the following date "
                    + mStatsDate + ", refresh cancelled.");
            return;
        }

        final Blog currentBlog = WordPress.getBlog(mLocalBlogID);

        if (mStatsDate == null || currentBlog == null || !NetworkUtils.isNetworkAvailable(this)) {
            mPullToRefreshHelper.setRefreshing(false);
            int errorMessageId = (mStatsDate == null || currentBlog == null) ? R.string.error_refresh_stats
                    : R.string.connection_error;
            ToastUtils.showToast(this, this.getString(errorMessageId), ToastUtils.Duration.LONG);
            return;
        }

        final String blogId = StatsUtils.getBlogId(mLocalBlogID);

        // View and visitor counts for a site
        final String viewAndVisitorsPath = String.format(
                "/sites/%s/stats/visits?date=%s&unit=day&quantity=1", blogId, mStatsDate);
        // Top posts and pages
        final String topPostsAndPagesPath = String.format(
                "/sites/%s/stats/top-posts?date=%s&days=1", blogId, mStatsDate);
        // Site's referrers
        final String referrersPath = String.format(
                "/sites/%s/stats/referrers?date=%s&days=1", blogId, mStatsDate);
        // Clicks
        final String clicksPath = String.format("/sites/%s/stats/clicks?date=%s&days=1", blogId, mStatsDate);
        // Search engine terms
        final String searchEngineTermsPath = String.format("/sites/%s/stats/search-terms?date=%s", blogId, mStatsDate);

        final String parametersSepator = "&urls%5B%5D=";

        String path = new StringBuilder("batch/?urls%5B%5D=")
                .append(Uri.encode(viewAndVisitorsPath))
                .append(parametersSepator)
                .append(Uri.encode(topPostsAndPagesPath))
                .append(parametersSepator)
                .append(Uri.encode(referrersPath))
                .append(parametersSepator)
                .append(Uri.encode(clicksPath))
                .append(parametersSepator)
                .append(Uri.encode(searchEngineTermsPath))
                .toString();

        AppLog.d(AppLog.T.STATS, "Enqueuing the following Stats request " + path);

        final RestClientUtils restClientUtils = WordPress.getRestClientUtils();
        RestBatchCallListener listener = new RestBatchCallListener(this, blogId, viewAndVisitorsPath,
                topPostsAndPagesPath, referrersPath, clicksPath, searchEngineTermsPath);
        restClientUtils.get(path, listener, listener);
        mIsUpdatingStats = true;
        mPullToRefreshHelper.setRefreshing(true);
    }

    private class RestBatchCallListener implements RestRequest.Listener, RestRequest.ErrorListener {
        private final String mRequestBlogId, mViewAndVisitorsPath, mTopPostsAndPagesPath,
                mReferrersPath, mClicksPath, mSearchEngineTermsPath;

        private final WeakReference<Activity> mActivityRef;
        private Serializable mErrorObjectsingleCall = null;

        public RestBatchCallListener(Activity activity, String mRequestBlogId, String viewAndVisitorsPath,
                                     String topPostsAndPagesPath, String referrersPath, String clicksPath,
                                     String searchEngineTermsPath) {
            mActivityRef = new WeakReference<Activity>(activity);

            this.mRequestBlogId = mRequestBlogId;
            this.mViewAndVisitorsPath = viewAndVisitorsPath;
            this.mClicksPath = clicksPath;
            this.mReferrersPath = referrersPath;
            this.mSearchEngineTermsPath = searchEngineTermsPath;
            this.mTopPostsAndPagesPath = topPostsAndPagesPath;
        }

        @Override
        public void onResponse(final JSONObject response) {
            if (mActivityRef.get() == null) {
                return;
            }
            mIsUpdatingStats = false;
            mPullToRefreshHelper.setRefreshing(false);
            AppLog.d(AppLog.T.STATS, "The REST response: " + response.toString());
            // single background thread used to parse the response.
            ThreadPoolExecutor parseResponseExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
            parseResponseExecutor.submit(new Thread() {
                @Override
                public void run() {
                    parseTopPostsAndPagesResponse(response);
                    parseReferrersResponse(response);
                    parseClicksResponse(response);
                    parseSearchEngineTermsResponse(response);
                    parseViewAndVisitorsResponse(response);

                    // Update the UI
                    final boolean post = mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView) findViewById(R.id.stats_visitors_and_views_today_visitors_count)).setText(
                                    FormatUtils.formatDecimal(mVisitorsCount)
                            );
                            ((TextView) findViewById(R.id.stats_visitors_and_views_today_views_count)).setText(
                                    FormatUtils.formatDecimal(mViewsCount)
                            );
                            ((TextView) findViewById(R.id.stats_visitors_and_views_best_ever_views_count)).setText(
                                    String.format("%.2f", mViewsPerVisitor)
                            );

                            // Update the Top posts and pages section
                            final LinearLayout linearLayoutTopPostsAndPages =
                                    (LinearLayout) findViewById(R.id.stats_top_posts_and_pages_listview);
                            final TextView emptyTextViewTopPostsAndPages =
                                    (TextView) findViewById(R.id.stats_top_posts_and_pages_empty_text);
                            if (mTopPostsAndPagesAdapter != null) {
                                mTopPostsAndPagesAdapter.notifyDataSetChanged();
                                StatsUIHelper.reloadLinearLayout(mActivityRef.get(), mTopPostsAndPagesAdapter,
                                        linearLayoutTopPostsAndPages);
                            }
                            if (mTopPostsAndPagesAdapter == null || mTopPostsAndPages.size() == 0) {
                                emptyTextViewTopPostsAndPages.setVisibility(View.VISIBLE);
                                linearLayoutTopPostsAndPages.setVisibility(View.GONE);
                            } else {
                                emptyTextViewTopPostsAndPages.setVisibility(View.GONE);
                                linearLayoutTopPostsAndPages.setVisibility(View.VISIBLE);
                            }

                            // Update the referrers section
                            final LinearLayout linearLayoutReferrers =
                                    (LinearLayout) findViewById(R.id.stats_referrers_list_listview);
                            final TextView emptyTextViewReferrers =
                                    (TextView) findViewById(R.id.stats_referrers_list_empty_text);
                            if (mReferrersAdapter != null) {
                                mReferrersAdapter.notifyDataSetChanged();
                                StatsUIHelper.reloadGroupViews(mActivityRef.get(), mReferrersAdapter,
                                        mReferrerGroupIdToExpandedMap, linearLayoutReferrers);
                            }
                            if (mReferrersAdapter == null || mReferrerGroups.size() == 0) {
                                emptyTextViewReferrers.setVisibility(View.VISIBLE);
                                linearLayoutReferrers.setVisibility(View.GONE);
                            } else {
                                emptyTextViewReferrers.setVisibility(View.GONE);
                                linearLayoutReferrers.setVisibility(View.VISIBLE);
                            }

                            // Update the clicks section
                            final LinearLayout linearLayoutClicks =
                                    (LinearLayout) findViewById(R.id.stats_clicks_list_listview);
                            final TextView emptyTextViewClicks =
                                    (TextView) findViewById(R.id.stats_clicks_list_empty_text);
                            if (mClicksAdapter != null) {
                                mClicksAdapter.notifyDataSetChanged();
                                StatsUIHelper.reloadGroupViews(mActivityRef.get(), mClicksAdapter,
                                        mClickGroupIdToExpandedMap, linearLayoutClicks);
                            }
                            if (mClicksAdapter == null || mClickGroups.size() == 0) {
                                emptyTextViewClicks.setVisibility(View.VISIBLE);
                                linearLayoutClicks.setVisibility(View.GONE);
                            } else {
                                emptyTextViewClicks.setVisibility(View.GONE);
                                linearLayoutClicks.setVisibility(View.VISIBLE);
                            }

                            // Update search engine terms
                            final LinearLayout linearLayoutSearch =
                                    (LinearLayout) findViewById(R.id.stats_searchengine_list_listview);
                            final TextView emptyTextViewSearch =
                                    (TextView) findViewById(R.id.stats_searchengine_list_empty_text);
                            if (mSearchEngineAdapter != null) {
                                mSearchEngineAdapter.notifyDataSetChanged();
                                StatsUIHelper.reloadLinearLayout(mActivityRef.get(), mSearchEngineAdapter,
                                        linearLayoutSearch);
                            }
                            if (mSearchEngineAdapter == null || mSearchEngineTerms.size() == 0) {
                                emptyTextViewSearch.setVisibility(View.VISIBLE);
                                linearLayoutSearch.setVisibility(View.GONE);
                            } else {
                                emptyTextViewSearch.setVisibility(View.GONE);
                                linearLayoutSearch.setVisibility(View.VISIBLE);
                            }

                            // TODO: Should we show a error if mErrorObjectsingleCall is not null?
                        }
                    });
                }
            });
        }

        private void parseViewAndVisitorsResponse(final JSONObject response) {
            mVisitorsCount = 0;
            mViewsCount = 0;
            mViewsPerVisitor = 0f;
            if (!response.has(mViewAndVisitorsPath)) {
                return;
            }
            try {
                final JSONObject currentJsonObject = response.getJSONObject(mViewAndVisitorsPath);
                if (isSingleCallResponseError(mViewAndVisitorsPath, currentJsonObject)) {
                    return;
                }

                int visitors = 0;
                int views = 0;
                JSONArray data = currentJsonObject.getJSONArray("data");
                JSONArray fields = currentJsonObject.getJSONArray("fields");
                if (data.length() == 0) {
                    return;
                }
                JSONArray dataPositionZero = data.getJSONArray(0);
                if (dataPositionZero.length() != fields.length()) {
                    // there is something weird here.
                    return;
                }
                for (int i = 0; i < fields.length(); i++) {
                    if (fields.get(i).toString().equalsIgnoreCase("views")) {
                        views = Integer.parseInt(dataPositionZero.get(i).toString());
                    }
                    if (fields.get(i).toString().equalsIgnoreCase("visitors")) {
                        visitors = Integer.parseInt(dataPositionZero.get(i).toString());
                    }
                }

                // Do some magic when visitors count == 0 - Private blog? weird case?
                if (visitors == 0) {
                   visitors = 1;
                }
                if (views == 0) {
                    mVisitorsCount = 0;
                    mViewsCount = 0;
                    mViewsPerVisitor = 0f;
                } else {
                    mViewsCount = views;
                    mVisitorsCount = visitors;
                    mViewsPerVisitor = ((float) views) / visitors;
                }
            } catch (JSONException e) {
                logSingleCallError(mViewAndVisitorsPath, e);
            }
        }

        private void parseClicksResponse(final JSONObject response) {
            mClickGroups.clear();
            if (!response.has(mClicksPath)) {
                return;
            }
            try {
                final JSONObject currentJsonObject = response.getJSONObject(mClicksPath);
                if (isSingleCallResponseError(mClicksPath, currentJsonObject)) {
                    return;
                }

                JSONArray groups = currentJsonObject.getJSONArray("clicks");
                String date = currentJsonObject.getString("date");
                int groupsCount = Math.min(groups.length(), StatsUIHelper.STATS_GROUP_MAX_ITEMS);

                // insert groups
                for (int i = 0; i < groupsCount; i++) {
                    JSONObject group = groups.getJSONObject(i);
                    MyStatsClicksGroup statGroup = new MyStatsClicksGroup(mRequestBlogId, date, group);
                    mClickGroups.append(i, statGroup);
                    // insert children, only if there is more than one entry
                    JSONArray clicks = group.getJSONArray("results");
                    int childCount = Math.min(clicks.length(), StatsUIHelper.STATS_CHILD_MAX_ITEMS);
                    if (childCount > 1) {
                        for (int j = 0; j < childCount; j++) {
                            StatsClick stat = new StatsClick(mRequestBlogId, date, statGroup.getGroupId(),
                                    clicks.getJSONArray(j));
                            statGroup.clicks.add(stat);
                        }
                    }
                }
            } catch (JSONException e) {
                logSingleCallError(mClicksPath, e);
            }
        }

        private void parseReferrersResponse(final JSONObject response) {
            mReferrerGroups.clear();
            if (!response.has(mReferrersPath)) {
                return;
            }
            try {
                final JSONObject currentJsonObject = response.getJSONObject(mReferrersPath);
                if (isSingleCallResponseError(mReferrersPath, currentJsonObject)) {
                    return;
                }

                JSONArray groups = currentJsonObject.getJSONArray("referrers");
                String date = currentJsonObject.getString("date");
                int groupsCount = Math.min(groups.length(), StatsUIHelper.STATS_GROUP_MAX_ITEMS);

                // insert groups
                for (int i = 0; i < groupsCount; i++) {
                    JSONObject group = groups.getJSONObject(i);
                    MyStatsReferrerGroup statGroup = new MyStatsReferrerGroup(mRequestBlogId, date, group);
                    mReferrerGroups.append(i, statGroup);
                    // insert children, only if there is more than one entry
                    JSONArray referrers = group.getJSONArray("results");
                    int childCount = Math.min(referrers.length(), StatsUIHelper.STATS_CHILD_MAX_ITEMS);
                    if (childCount > 1) {
                        for (int j = 0; j < childCount; j++) {
                            StatsReferrer stat = new StatsReferrer(mRequestBlogId, date, statGroup.getGroupId(),
                                    referrers.getJSONArray(j));
                            statGroup.referrers.add(stat);
                        }
                    }
                }
            } catch (JSONException e) {
                logSingleCallError(mReferrersPath, e);
            }
        }

        private void parseSearchEngineTermsResponse(final JSONObject response) {
            mSearchEngineTerms.clear();
            if (!response.has(mSearchEngineTermsPath)) {
                return;
            }
            try {
                final JSONObject currentJsonObject = response.getJSONObject(mSearchEngineTermsPath);
                if (isSingleCallResponseError(mSearchEngineTermsPath, currentJsonObject)) {
                    return;
                }
                if (!currentJsonObject.has("search-terms")) {
                    return;
                }

                JSONArray results = currentJsonObject.getJSONArray("search-terms");
                String date = currentJsonObject.getString("date");
                int count = Math.min(results.length(), StatsUIHelper.STATS_GROUP_MAX_ITEMS);

                for (int i = 0; i < count; i++) {
                    JSONArray result = results.getJSONArray(i);
                    StatsSearchEngineTerm stat = new StatsSearchEngineTerm(mRequestBlogId, date, result);
                    mSearchEngineTerms.add(stat);
                }
            } catch (JSONException e) {
                logSingleCallError(mSearchEngineTermsPath, e);
            }
        }

        private void parseTopPostsAndPagesResponse(final JSONObject response) {
            mTopPostsAndPages.clear();
            if (!response.has(mTopPostsAndPagesPath)) {
                return;
            }
            try {
                final JSONObject currentTopPostsAndPagesJsonObject = response.getJSONObject(mTopPostsAndPagesPath);
                if (isSingleCallResponseError(mTopPostsAndPagesPath, currentTopPostsAndPagesJsonObject)) {
                    return;
                }
                if (!currentTopPostsAndPagesJsonObject.has("top-posts")) {
                    return;
                }

                JSONArray results = currentTopPostsAndPagesJsonObject.getJSONArray("top-posts");
                int count = Math.min(results.length(), StatsUIHelper.STATS_GROUP_MAX_ITEMS);

                //  String date = currentTopPostsAndPagesJsonObject.getString("date");

                for (int i = 0; i < count; i++) {
                    JSONObject result = results.getJSONObject(i);
                    StatsTopPostsAndPages stat = new StatsTopPostsAndPages(mRequestBlogId, result);
                    mTopPostsAndPages.add(stat);
                }
            } catch (JSONException e) {
                logSingleCallError(mTopPostsAndPagesPath, e);
            }
        }

        private boolean isSingleCallResponseError(String restCallPATH, final JSONObject response) {
            if (response.has("errors")) {
                mErrorObjectsingleCall = response.toString();
                AppLog.e(AppLog.T.STATS, "The single call " + restCallPATH
                        + " failed with the following response: " + response.toString());
                return true;
            }

            return false;
        }

        private void logSingleCallError(String restCallPATH, Exception e) {
            AppLog.e(AppLog.T.STATS, "Single call failed " + restCallPATH, e);
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

    private class MyStatsClicksGroup extends StatsClickGroup {
        public MyStatsClicksGroup(String blogId, String date, JSONObject result) throws JSONException {
            super(blogId, date, result);
        }

        public final LinkedList<StatsClick> clicks = new LinkedList<StatsClick>();
    }

    private class MyStatsReferrerGroup extends StatsReferrerGroup {
        public MyStatsReferrerGroup(String blogId, String date, JSONObject result) throws JSONException {
            super(blogId, date, result);
        }

        public final LinkedList<StatsReferrer> referrers = new LinkedList<StatsReferrer>();
    }

    private class TopPostsAndPagesAdapter extends ArrayAdapter<StatsTopPostsAndPages> {
        private final List<StatsTopPostsAndPages> mList;
        private final Activity mContext;

        public TopPostsAndPagesAdapter(Activity context, List<StatsTopPostsAndPages> list) {
            super(context, R.layout.stats_list_cell, list);
            this.mContext = context;
            this.mList = list;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                LayoutInflater inflator = mContext.getLayoutInflater();
                view = inflator.inflate(R.layout.stats_list_cell, null);
                final StatsViewHolder viewHolder = new StatsViewHolder(view);
                view.setTag(viewHolder);
            } else {
                view = convertView;
            }

            final StatsViewHolder holder = (StatsViewHolder) view.getTag();
            StatsTopPostsAndPages element = mList.get(position);

            final String entry = element.getTitle();
            final String url = element.getUrl();
            int total = element.getViews();

            // entries
            holder.setEntryTextOrLink(url, entry);

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // no icon
            holder.networkImageView.setVisibility(View.GONE);

            int bgColor = (position % 2 == 1 ? mAltRowColor : Color.TRANSPARENT);
            view.setBackgroundColor(bgColor);
            return view;
        }
    }

    private class SearchEngineAdapter extends ArrayAdapter<StatsSearchEngineTerm> {
        private final List<StatsSearchEngineTerm> mList;
        private final Activity mContext;

        public SearchEngineAdapter(Activity context, List<StatsSearchEngineTerm> list) {
            super(context, R.layout.stats_list_cell, list);
            this.mContext = context;
            this.mList = list;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                LayoutInflater inflator = mContext.getLayoutInflater();
                view = inflator.inflate(R.layout.stats_list_cell, null);
                final StatsViewHolder viewHolder = new StatsViewHolder(view);
                view.setTag(viewHolder);
            } else {
                view = convertView;
            }

            final StatsViewHolder holder = (StatsViewHolder) view.getTag();
            StatsSearchEngineTerm element = mList.get(position);

            final String entry = element.getSearch();
            int total = element.getViews();

            // entries
            holder.entryTextView.setText(entry);

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // no icon
            holder.networkImageView.setVisibility(View.GONE);

            int bgColor = (position % 2 == 1 ? mAltRowColor : Color.TRANSPARENT);
            view.setBackgroundColor(bgColor);
            return view;
        }
    }

    private class ReferrersAdapter extends BaseExpandableListAdapter {
        private final SparseArray<MyStatsReferrerGroup> mGroups;
        private LayoutInflater mInflater;
        private Activity mActivity;

        public ReferrersAdapter(Activity act, SparseArray<MyStatsReferrerGroup> groups) {
            super();
            mActivity = act;
            mGroups = groups;
            mInflater = act.getLayoutInflater();
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return mGroups.get(groupPosition).referrers.get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        @Override
        public View getChildView(int groupPosition, final int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {
            final StatsReferrer children = (StatsReferrer) getChild(groupPosition, childPosition);
            View view = mInflater.inflate(R.layout.stats_list_cell, parent, false);
            view.setTag(new StatsViewHolder(view));
            final StatsViewHolder holder = (StatsViewHolder) view.getTag();

            String name = children.getName();
            int total = children.getTotal();

            // name, url
            holder.setEntryTextOrLink(name, name);

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // no icon, make it invisible so children are indented
            holder.networkImageView.setVisibility(View.INVISIBLE);

            int bgColor = (childPosition % 2 == 1 ? mAltRowColor : Color.TRANSPARENT);
            view.setBackgroundColor(bgColor);
            return view;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return mGroups.get(groupPosition).referrers.size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return mGroups.get(groupPosition);
        }

        @Override
        public int getGroupCount() {
            return mGroups.size();
        }

        @Override
        public void onGroupCollapsed(int groupPosition) {
            super.onGroupCollapsed(groupPosition);
        }

        @Override
        public void onGroupExpanded(int groupPosition) {
            super.onGroupExpanded(groupPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {
            MyStatsReferrerGroup group = (MyStatsReferrerGroup) getGroup(groupPosition);

            View view = mInflater.inflate(R.layout.stats_list_cell, parent, false);
            view.setTag(new StatsViewHolder(view));

            final StatsViewHolder holder = (StatsViewHolder) view.getTag();

            String name = group.getName();
            int total = group.getTotal();
            String url = group.getUrl();
            String icon = group.getIcon();
            int children = group.getChildren();

            holder.setEntryTextOrLink(url, name);

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // icon
            holder.showNetworkImage(icon);

            // expand/collapse chevron
            holder.chevronImageView.setVisibility(children > 0 ? View.VISIBLE : View.GONE);

            int bgColor = (groupPosition % 2 == 1 ? mAltRowColor : Color.TRANSPARENT);
            view.setBackgroundColor(bgColor);

            return view;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }

    private class ClicksAdapter extends BaseExpandableListAdapter {
        private final SparseArray<MyStatsClicksGroup> mGroups;
        private LayoutInflater mInflater;
        private Activity mActivity;

        public ClicksAdapter(Activity act, SparseArray<MyStatsClicksGroup> groups) {
            super();
            mActivity = act;
            mGroups = groups;
            mInflater = act.getLayoutInflater();
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return mGroups.get(groupPosition).clicks.get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        @Override
        public View getChildView(int groupPosition, final int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {
            final StatsClick children = (StatsClick) getChild(groupPosition, childPosition);
            View view = mInflater.inflate(R.layout.stats_list_cell, parent, false);
            view.setTag(new StatsViewHolder(view));
            final StatsViewHolder holder = (StatsViewHolder) view.getTag();

            String name = children.getName();
            int total = children.getTotal();

            // name, url
            holder.setEntryTextOrLink(name, name);

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // no icon, make it invisible so children are indented
            holder.networkImageView.setVisibility(View.INVISIBLE);

            int bgColor = (childPosition % 2 == 1 ? mAltRowColor : Color.TRANSPARENT);
            view.setBackgroundColor(bgColor);
            return view;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return mGroups.get(groupPosition).clicks.size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return mGroups.get(groupPosition);
        }

        @Override
        public int getGroupCount() {
            return mGroups.size();
        }

        @Override
        public void onGroupCollapsed(int groupPosition) {
            super.onGroupCollapsed(groupPosition);
        }

        @Override
        public void onGroupExpanded(int groupPosition) {
            super.onGroupExpanded(groupPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {
            MyStatsClicksGroup group = (MyStatsClicksGroup) getGroup(groupPosition);

            View view = mInflater.inflate(R.layout.stats_list_cell, parent, false);
            view.setTag(new StatsViewHolder(view));

            final StatsViewHolder holder = (StatsViewHolder) view.getTag();

            String name = group.getName();
            int total = group.getTotal();
            String url = group.getUrl();
            String icon = group.getIcon();
            int children = group.getChildren();

            holder.setEntryTextOrLink(url, name);

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // icon
            holder.showNetworkImage(icon);

            // expand/collapse chevron
            holder.chevronImageView.setVisibility(children > 0 ? View.VISIBLE : View.GONE);

            int bgColor = (groupPosition % 2 == 1 ? mAltRowColor : Color.TRANSPARENT);
            view.setBackgroundColor(bgColor);

            return view;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }

    private void setupEmptyLabel(TextView textView, int titleStringId, int descStringId) {
        String label = "<b>" + getString(titleStringId) + "</b> " + getString(descStringId);
        if (label.contains("<")) {
            textView.setText(Html.fromHtml(label));
        } else {
            textView.setText(label);
        }
        textView.setVisibility(View.VISIBLE);
    }
}
