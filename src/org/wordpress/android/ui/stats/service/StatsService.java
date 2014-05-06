package org.wordpress.android.ui.stats.service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import android.app.Service;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.StatsBarChartDataTable;
import org.wordpress.android.datasets.StatsClickGroupsTable;
import org.wordpress.android.datasets.StatsClicksTable;
import org.wordpress.android.datasets.StatsGeoviewsTable;
import org.wordpress.android.datasets.StatsReferrerGroupsTable;
import org.wordpress.android.datasets.StatsReferrersTable;
import org.wordpress.android.datasets.StatsSearchEngineTermsTable;
import org.wordpress.android.datasets.StatsTopPostsAndPagesTable;
import org.wordpress.android.models.StatsBarChartData;
import org.wordpress.android.models.StatsClick;
import org.wordpress.android.models.StatsClickGroup;
import org.wordpress.android.models.StatsGeoview;
import org.wordpress.android.models.StatsReferrer;
import org.wordpress.android.models.StatsReferrerGroup;
import org.wordpress.android.models.StatsSearchEngineTerm;
import org.wordpress.android.models.StatsSummary;
import org.wordpress.android.models.StatsTopPostsAndPages;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.ui.stats.StatsBarChartUnit;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StatUtils;
import org.wordpress.android.util.StringUtils;

/**
 * Background service to retrieve latest stats - Uses a Thread to enqueue network calls in Volley;
 * Volley takes care of handling multithreading. UI refresh is done by using a ThreadPoolExecutor
 * with a single thread.
 */

public class StatsService extends Service {

    public static final String ARG_BLOG_ID = "blog_id";

    // broadcast action to notify clients of update start/end
    public static final String ACTION_STATS_UPDATING = "wp-stats-updating";
    public static final String EXTRA_IS_UPDATING = "is-updating";

    // broadcast action to notify clients when summary data has changed
    public static final String ACTION_STATS_SUMMARY_UPDATED = "STATS_SUMMARY_UPDATED";
    public static final String STATS_SUMMARY_UPDATED_EXTRA = "STATS_SUMMARY_UPDATED_EXTRA";

    private static final long TWO_DAYS = 2 * 24 * 60 * 60 * 1000;

    private final Object mSyncObject = new Object();

    private String mCurrentBlogId;
    private final LinkedList<Request<JSONObject>> statsNetworkRequests = new LinkedList<Request<JSONObject>>();
    private int numberOfNetworkCalls = -1; // The number of networks calls made by Stats.
    private int numberOfFinishedNetworkCalls = 0;
    private ThreadPoolExecutor updateUIExecutor;
    private Thread orchestrator;

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(T.STATS, "service created");
    }

    @Override
    public void onDestroy() {
        stopRefresh();
        AppLog.i(T.STATS, "service destroyed");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String blogId = StringUtils.notNullStr(intent.getStringExtra(ARG_BLOG_ID));

        if (mCurrentBlogId == null) {
            startTasks(blogId, startId);
        } else if (blogId.equals(mCurrentBlogId)) {
            // already running on the same blogID
            // Do nothing
            AppLog.i(T.STATS, "StatsService is already running on this blogID - " + mCurrentBlogId);
        } else {
            // stats is running on a different blogID
            stopRefresh();
            startTasks(blogId, startId);
        }

        this.mCurrentBlogId = blogId;
        return START_NOT_STICKY;
    }

    private void stopRefresh() {
        for (Request<JSONObject> req : statsNetworkRequests) {
            if (req != null && !req.hasHadResponseDelivered() && !req.isCanceled()) {
                req.cancel();
            }
        }
        statsNetworkRequests.clear();
        if (orchestrator != null) {
            orchestrator.interrupt();
        }
        orchestrator = null;
        this.mCurrentBlogId = null;
    }    

    private void startTasks(final String blogId, final int startId) {

        orchestrator = new Thread() {
            @Override
            public void run() {
                updateUIExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1); // single thread otherwise the UI is sluggish
                RestClientUtils restClientUtils = WordPress.getRestClientUtils();
                final String today = StatUtils.getCurrentDate();
                final String yesterday = StatUtils.getYesterdaysDate();

                AppLog.i(T.STATS, "update started for blogID - " + blogId);
                broadcastUpdate(true);

                // visitors and views
                String path = String.format("sites/%s/stats", blogId);
                SummaryRestListener summaryListener = new SummaryRestListener(blogId);
                statsNetworkRequests.add(restClientUtils.get(path, summaryListener, summaryListener));

                // bar charts
                path = "batch/?urls%5B%5D=" 
                        + Uri.encode(getBarChartPath(blogId, StatsBarChartUnit.WEEK, 30)) 
                        + "&urls%5B%5D=" 
                        + Uri.encode(getBarChartPath(blogId, StatsBarChartUnit.MONTH, 30));
                BarChartListener barChartRestListener = new BarChartListener(
                        blogId,
                        getBarChartPath(blogId, StatsBarChartUnit.WEEK, 30), StatsBarChartUnit.WEEK,
                        getBarChartPath(blogId, StatsBarChartUnit.MONTH, 30), StatsBarChartUnit.MONTH);
                statsNetworkRequests.add(restClientUtils.get(path, barChartRestListener, barChartRestListener));

                // top posts and pages
                path = "batch/?urls%5B%5D=" 
                        + Uri.encode(String.format("/sites/%s/stats/top-posts?date=%s", blogId, today))
                        + "&urls%5B%5D=" 
                        + Uri.encode(String.format("/sites/%s/stats/top-posts?date=%s", blogId, yesterday));
                TopPostAndPageListener topPostAndPageRestListener = new TopPostAndPageListener(
                        blogId,
                        String.format("/sites/%s/stats/top-posts?date=%s", blogId, today),
                        String.format("/sites/%s/stats/top-posts?date=%s", blogId, yesterday)
                        );
                statsNetworkRequests.add(restClientUtils.get(path, topPostAndPageRestListener,
                        topPostAndPageRestListener));

                // referrers
                path = "batch/?urls%5B%5D="
                        + Uri.encode(String.format("/sites/%s/stats/referrers?date=%s", blogId, today))
                        + "&urls%5B%5D="
                        + Uri.encode(String.format("/sites/%s/stats/referrers?date=%s", blogId, yesterday));
                ReferrersListener referrersListener = new ReferrersListener(
                        blogId,
                        String.format("/sites/%s/stats/referrers?date=%s", blogId, today),
                        String.format("/sites/%s/stats/referrers?date=%s", blogId, yesterday)
                        );
                statsNetworkRequests.add(restClientUtils.get(path, referrersListener, referrersListener));

                // clicks
                path = "batch/?urls%5B%5D="
                        + Uri.encode(String.format("/sites/%s/stats/clicks?date=%s", blogId, today))
                        + "&urls%5B%5D="
                        + Uri.encode(String.format("/sites/%s/stats/clicks?date=%s", blogId, yesterday));
                ClicksListener clicksListener = new ClicksListener(
                        blogId,
                        String.format("/sites/%s/stats/clicks?date=%s", blogId, today),
                        String.format("/sites/%s/stats/clicks?date=%s", blogId, yesterday)
                        );
                statsNetworkRequests.add(restClientUtils.get(path, clicksListener, clicksListener));

                // search engine terms
                path = "batch/?urls%5B%5D="
                        + Uri.encode(String.format("/sites/%s/stats/search-terms?date=%s", blogId, today))
                        + "&urls%5B%5D="
                        + Uri.encode(String.format("/sites/%s/stats/search-terms?date=%s", blogId, yesterday));
                SearchEngineTermsListener searchEngineTermsListener = new SearchEngineTermsListener(
                        blogId,
                        String.format("/sites/%s/stats/search-terms?date=%s", blogId, today),
                        String.format("/sites/%s/stats/search-terms?date=%s", blogId, yesterday)
                        );
                statsNetworkRequests.add(restClientUtils
                        .get(path, searchEngineTermsListener, searchEngineTermsListener));

                // views by country - put at the end since this will start other networks calls on
                // finish
                path = "batch/?urls%5B%5D="
                        + Uri.encode(String.format("/sites/%s/stats/country-views?date=%s", blogId, today))
                        + "&urls%5B%5D="
                        + Uri.encode(String.format("/sites/%s/stats/country-views?date=%s", blogId, yesterday));
                ViewsByCountryListener viewsByCountryListener = new ViewsByCountryListener(
                        blogId,
                        String.format("/sites/%s/stats/country-views?date=%s", blogId, today),
                        String.format("/sites/%s/stats/country-views?date=%s", blogId, yesterday)
                        );
                statsNetworkRequests.add(restClientUtils.get(path, viewsByCountryListener, viewsByCountryListener));

                numberOfNetworkCalls = statsNetworkRequests.size();

                while (!isDone()) {
                    waitForResponse();
                }

                // Initiates an orderly shutdown in which previously submitted tasks are executed,
                // but no new tasks will be accepted.
                // At this point all Threads previously enqueued in updateUIExecutor already
                // finished their execution.
                updateUIExecutor.shutdown();
                mCurrentBlogId = null;

                broadcastUpdate(false);
                stopSelf(startId);
            } // end run
        };

        orchestrator.start();
    }

    private class SearchEngineTermsListener extends AbsListener {

        SearchEngineTermsListener(String blogID, String todayAPICallPath, String yesterdayAPICallPath) {
            super(blogID, todayAPICallPath, yesterdayAPICallPath);
        }

        @Override
        protected void parseSingleDayResponse(final JSONObject response) throws JSONException, RemoteException,
                OperationApplicationException {
            String date = response.getString("date");
            long dateMs = StatUtils.toMs(date);

            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

            ContentProviderOperation delete_op = ContentProviderOperation
                    .newDelete(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI)
                    .withSelection("blogId=? AND (date=? OR date<=?)",
                            new String[] {
                                    mBlogID, dateMs + "", (dateMs - TWO_DAYS) + ""
                            }).build();

            operations.add(delete_op);

            JSONArray results = response.getJSONArray("search-terms");

            int count = Math.min(results.length(), StatsActivity.STATS_GROUP_MAX_ITEMS);
            for (int i = 0; i < count; i++) {
                JSONArray result = results.getJSONArray(i);
                StatsSearchEngineTerm stat = new StatsSearchEngineTerm(mBlogID, date, result);
                ContentValues values = StatsSearchEngineTermsTable.getContentValues(stat);
                getContentResolver().insert(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI, values);

                ContentProviderOperation insert_op = ContentProviderOperation
                        .newInsert(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI).withValues(values).build();
                operations.add(insert_op);
            }

            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
        }

        @Override
        protected Uri getStatsContentProviderUpdateURI() {
            return StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI;
        }
    }

    private class ClicksListener extends AbsListener {

        ClicksListener(String blogID, String todayAPICallPath, String yesterdayAPICallPath) {
            super(blogID, todayAPICallPath, yesterdayAPICallPath);
        }

        @Override
        void parseResponse(final JSONObject response) throws JSONException, RemoteException,
                OperationApplicationException {
            super.parseResponse(response);
            getContentResolver().notifyChange(StatsContentProvider.STATS_CLICKS_URI, null);
        }

        @Override
        protected void parseSingleDayResponse(final JSONObject response) throws JSONException, RemoteException,
                OperationApplicationException {
            String date = response.getString("date");
            long dateMs = StatUtils.toMs(date);

            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

            // delete data with the same date, and data older than two days ago (keep yesterday's
            // data)
            ContentProviderOperation delete_group = ContentProviderOperation
                    .newDelete(StatsContentProvider.STATS_CLICK_GROUP_URI)
                    .withSelection("blogId=? AND (date=? OR date<=?)",
                            new String[] {
                                    mBlogID, dateMs + "", (dateMs - TWO_DAYS) + ""
                            }).build();
            ContentProviderOperation delete_child = ContentProviderOperation
                    .newDelete(StatsContentProvider.STATS_CLICKS_URI).withSelection("blogId=? AND (date=? OR date<=?)",
                            new String[] {
                            mBlogID, dateMs + "", (dateMs - TWO_DAYS) + ""
                            }).build();

            operations.add(delete_group);
            operations.add(delete_child);

            JSONArray groups = response.getJSONArray("clicks");

            // insert groups, limited to the number that can actually be displayed
            int groupsCount = Math.min(groups.length(), StatsActivity.STATS_GROUP_MAX_ITEMS);
            for (int i = 0; i < groupsCount; i++) {
                JSONObject group = groups.getJSONObject(i);
                StatsClickGroup statGroup = new StatsClickGroup(mBlogID, date, group);
                ContentValues values = StatsClickGroupsTable.getContentValues(statGroup);

                ContentProviderOperation insert_group = ContentProviderOperation
                        .newInsert(StatsContentProvider.STATS_CLICK_GROUP_URI).withValues(values).build();
                operations.add(insert_group);

                // insert children if there are any, limited to the number that can be displayed
                JSONArray clicks = group.getJSONArray("results");
                int childCount = Math.min(clicks.length(), StatsActivity.STATS_CHILD_MAX_ITEMS);
                if (childCount > 1) {
                    for (int j = 0; j < childCount; j++) {
                        StatsClick stat = new StatsClick(mBlogID, date, statGroup.getGroupId(), clicks.getJSONArray(j));
                        ContentValues v = StatsClicksTable.getContentValues(stat);
                        ContentProviderOperation insert_child = ContentProviderOperation
                                .newInsert(StatsContentProvider.STATS_CLICKS_URI).withValues(v).build();
                        operations.add(insert_child);
                    }
                }
            }

            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
        }

        @Override
        protected Uri getStatsContentProviderUpdateURI() {
            return StatsContentProvider.STATS_CLICK_GROUP_URI;
        }
    }

    private class ReferrersListener extends AbsListener {

        ReferrersListener(String blogID, String todayAPICallPath, String yesterdayAPICallPath) {
            super(blogID, todayAPICallPath, yesterdayAPICallPath);
        }

        @Override
        void parseResponse(final JSONObject response) throws JSONException, RemoteException,
                OperationApplicationException {
            super.parseResponse(response);
            getContentResolver().notifyChange(StatsContentProvider.STATS_REFERRERS_URI, null);
        }

        @Override
        protected void parseSingleDayResponse(final JSONObject response) throws JSONException, RemoteException,
                OperationApplicationException {
            String date = response.getString("date");
            long dateMs = StatUtils.toMs(date);

            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

            // delete data with the same date, and data older than two days ago (keep yesterday's
            // data)
            ContentProviderOperation delete_group_op = ContentProviderOperation
                    .newDelete(StatsContentProvider.STATS_REFERRER_GROUP_URI)
                    .withSelection("blogId=? AND (date=? OR date<=?)", new String[] {
                            mBlogID, dateMs + "", (dateMs - TWO_DAYS) + ""
                    }).build();
            operations.add(delete_group_op);

            ContentProviderOperation delete_op = ContentProviderOperation
                    .newDelete(StatsContentProvider.STATS_REFERRERS_URI)
                    .withSelection("blogId=? AND (date=? OR date<=?)", new String[] {
                            mBlogID, dateMs + "", (dateMs - TWO_DAYS) + ""
                    }).build();
            operations.add(delete_op);

            JSONArray groups = response.getJSONArray("referrers");
            int groupsCount = Math.min(groups.length(), StatsActivity.STATS_GROUP_MAX_ITEMS);

            // insert groups
            for (int i = 0; i < groupsCount; i++) {
                JSONObject group = groups.getJSONObject(i);
                StatsReferrerGroup statGroup = new StatsReferrerGroup(mBlogID, date, group);
                ContentValues values = StatsReferrerGroupsTable.getContentValues(statGroup);
                ContentProviderOperation insert_group_op = ContentProviderOperation
                        .newInsert(StatsContentProvider.STATS_REFERRER_GROUP_URI).withValues(values).build();
                operations.add(insert_group_op);

                // insert children, only if there is more than one entry
                JSONArray referrers = group.getJSONArray("results");
                int childCount = Math.min(referrers.length(), StatsActivity.STATS_CHILD_MAX_ITEMS);
                if (childCount > 1) {
                    for (int j = 0; j < childCount; j++) {
                        StatsReferrer stat = new StatsReferrer(mBlogID, date, statGroup.getGroupId(),
                                referrers.getJSONArray(j));
                        ContentValues v = StatsReferrersTable.getContentValues(stat);
                        ContentProviderOperation insert_child_op = ContentProviderOperation
                                .newInsert(StatsContentProvider.STATS_REFERRERS_URI).withValues(v).build();
                        operations.add(insert_child_op);
                    }
                }
            }

            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
        }

        @Override
        protected Uri getStatsContentProviderUpdateURI() {
            return StatsContentProvider.STATS_REFERRER_GROUP_URI;
        }
    }

    private class ViewsByCountryListener extends AbsListener {

        ViewsByCountryListener(String blogID, String todayAPICallPath, String yesterdayAPICallPath) {
            super(blogID, todayAPICallPath, yesterdayAPICallPath);
        }

        @Override
        protected void parseSingleDayResponse(final JSONObject response) throws JSONException, RemoteException,
                OperationApplicationException {
            if (!response.has("country-views")) {
                return;
            }

            JSONArray results = response.getJSONArray("country-views");
            int count = Math.min(results.length(), StatsActivity.STATS_GROUP_MAX_ITEMS);
            String date = response.getString("date");
            long dateMs = StatUtils.toMs(date);
            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

            if (count > 0) {
                // delete data with the same date, and data older than two days ago (keep
                // yesterday's data)
                ContentProviderOperation delete_op = ContentProviderOperation
                        .newDelete(StatsContentProvider.STATS_GEOVIEWS_URI)
                        .withSelection("blogId=? AND (date=? OR date<=?)", new String[] {
                                mBlogID, dateMs + "", (dateMs - TWO_DAYS) + ""
                        }).build();
                operations.add(delete_op);
            }

            for (int i = 0; i < count; i++) {
                JSONObject result = results.getJSONObject(i);
                StatsGeoview stat = new StatsGeoview(mBlogID, result);
                ContentValues values = StatsGeoviewsTable.getContentValues(stat);
                ContentProviderOperation op = ContentProviderOperation
                        .newInsert(StatsContentProvider.STATS_GEOVIEWS_URI).withValues(values).build();
                operations.add(op);
            }

            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
        }

        @Override
        protected Uri getStatsContentProviderUpdateURI() {
            return StatsContentProvider.STATS_GEOVIEWS_URI;
        }
    }

    private class TopPostAndPageListener extends AbsListener {

        TopPostAndPageListener(String blogID, String todayAPICallPath, String yesterdayAPICallPath) {
            super(blogID, todayAPICallPath, yesterdayAPICallPath);
        }

        @Override
        protected void parseSingleDayResponse(final JSONObject response) throws JSONException, RemoteException,
                OperationApplicationException {
            if (!response.has("top-posts")) {
                return;
            }

            JSONArray results = response.getJSONArray("top-posts");
            int count = Math.min(results.length(), StatsActivity.STATS_GROUP_MAX_ITEMS);

            String date = response.getString("date");
            long dateMs = StatUtils.toMs(date);

            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
            // delete data with the same date, and data older than two days ago (keep yesterday's data)
            ContentProviderOperation delete_op = ContentProviderOperation
                    .newDelete(StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI)
                    .withSelection("blogId=? AND (date=? OR date<=?)", new String[] {
                            mBlogID, dateMs + "", (dateMs - TWO_DAYS) + ""
                    }).build();
            operations.add(delete_op);

            for (int i = 0; i < count; i++) {
                JSONObject result = results.getJSONObject(i);
                StatsTopPostsAndPages stat = new StatsTopPostsAndPages(mBlogID, result);
                ContentValues values = StatsTopPostsAndPagesTable.getContentValues(stat);
                ContentProviderOperation op = ContentProviderOperation
                        .newInsert(StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI).withValues(values).build();
                operations.add(op);
            }

            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
        }

        @Override
        protected Uri getStatsContentProviderUpdateURI() {
            return StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI;
        }
    }

    private abstract class AbsListener implements RestRequest.Listener, RestRequest.ErrorListener {
        final String mTodayAPICallPath;
        final String mYesterdayAPICallPath;
        final String mBlogID;
        
        protected abstract Uri getStatsContentProviderUpdateURI();

        protected abstract void parseSingleDayResponse(final JSONObject response) throws JSONException,
                RemoteException, OperationApplicationException;

        AbsListener(String blogID, String todayAPICallPath, String yesterdayAPICallPath) {
            this.mBlogID = blogID;
            this.mTodayAPICallPath = todayAPICallPath;
            this.mYesterdayAPICallPath = yesterdayAPICallPath;
        }

        @Override
        public void onResponse(final JSONObject response) {
            AppLog.d(T.STATS, this.getClass().getName() + " responded OK");
            if (!updateUIExecutor.isShutdown() && !updateUIExecutor.isTerminated() && !updateUIExecutor.isTerminating())
                updateUIExecutor.submit(
                        new Thread() {
                            @Override
                            public void run() {
                                numberOfFinishedNetworkCalls++;
                                if (response != null) {
                                    try {
                                        parseResponse(response);
                                    } catch (JSONException e) {
                                        AppLog.e(AppLog.T.STATS, e);
                                    } catch (RemoteException e) {
                                        AppLog.e(AppLog.T.STATS, e);
                                    } catch (OperationApplicationException e) {
                                        AppLog.e(AppLog.T.STATS, e);
                                    }
                                }
                                notifyResponseReceived();
                                getContentResolver().notifyChange(getStatsContentProviderUpdateURI(), null);
                            }
                        });
        }

        void parseResponse(JSONObject response) throws JSONException, RemoteException,
                OperationApplicationException {
            if (response.has(mTodayAPICallPath)) {
                JSONObject todayJsonObject = response.getJSONObject(mTodayAPICallPath);
                parseSingleDayResponse(todayJsonObject);
            }
            if (response.has(mYesterdayAPICallPath)) {
                JSONObject yesterdayJsonObject = response.getJSONObject(mYesterdayAPICallPath);
                parseSingleDayResponse(yesterdayJsonObject);
            }
        }

        @Override
        public void onErrorResponse(final VolleyError volleyError) {
            AppLog.d(T.STATS, this.getClass().getName() + " responded with Error");
            if (!updateUIExecutor.isShutdown() && !updateUIExecutor.isTerminated() && !updateUIExecutor.isTerminating())
                updateUIExecutor.submit(new Thread() {
                    @Override
                    public void run() {
                        numberOfFinishedNetworkCalls++;
                        if (volleyError != null) {
                            AppLog.e(T.STATS, "Error while reading Stats - " + volleyError.getMessage(), volleyError);
                        }
                        notifyResponseReceived();
                        getContentResolver().notifyChange(getStatsContentProviderUpdateURI(), null);
                    }
                });
        }
    }

    private String getBarChartPath(String blogID, StatsBarChartUnit mBarChartUnit, int quantity) {
        String path = String.format("/sites/%s/stats/visits", blogID);
        String unit = mBarChartUnit.name().toLowerCase(Locale.ENGLISH);
        path += String.format("?unit=%s", unit);
        if (quantity > 0) {
            path += String.format("&quantity=%d", quantity);
        }
        return path;
    }

    private class BarChartListener extends AbsListener {

        private final StatsBarChartUnit mTodayBarChartUnit;
        private final StatsBarChartUnit mYesterdayBarChartUnit;
        private StatsBarChartUnit mCurrentBarChartUnit;

        BarChartListener(String blogID, String todayAPICallPath, StatsBarChartUnit todayBarChartUnit, 
                String yesterddayAPICallPath, StatsBarChartUnit yesterdayBarChartUnit) {
            super(blogID, todayAPICallPath, yesterddayAPICallPath);
            this.mTodayBarChartUnit = todayBarChartUnit;
            this.mYesterdayBarChartUnit = yesterdayBarChartUnit;
        }

        @Override
        protected Uri getStatsContentProviderUpdateURI() {
            return StatsContentProvider.STATS_BAR_CHART_DATA_URI;
        }

        @Override
        protected void parseResponse(JSONObject response) throws JSONException, RemoteException,
                OperationApplicationException {
            if (response.has(mTodayAPICallPath)) {
                JSONObject todayJsonObject = response.getJSONObject(mTodayAPICallPath);
                mCurrentBarChartUnit = mTodayBarChartUnit;
                parseSingleDayResponse(todayJsonObject);
            }
            if (response.has(mYesterdayAPICallPath)) {
                JSONObject yesterdayJsonObject = response.getJSONObject(mYesterdayAPICallPath);
                mCurrentBarChartUnit = mYesterdayBarChartUnit;
                parseSingleDayResponse(yesterdayJsonObject);
            }
        }

        @Override
        protected void parseSingleDayResponse(final JSONObject response) throws JSONException, RemoteException,
                OperationApplicationException {
            if (!response.has("data")) {
                return;
            }

            Uri uri = StatsContentProvider.STATS_BAR_CHART_DATA_URI;
            JSONArray results = response.getJSONArray("data");

            int count = results.length();

            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

            // delete old stats and insert new ones
            if (count > 0) {
                ContentProviderOperation op = ContentProviderOperation.newDelete(uri)
                        .withSelection("blogId=? AND unit=?", new String[] {
                                mBlogID, mCurrentBarChartUnit.name()
                        }).build();
                operations.add(op);
            }

            for (int i = 0; i < count; i++) {
                JSONArray result = results.getJSONArray(i);
                StatsBarChartData stat = new StatsBarChartData(mBlogID, mCurrentBarChartUnit, result);
                ContentValues values = StatsBarChartDataTable.getContentValues(stat);

                if (values != null && uri != null) {
                    ContentProviderOperation op = ContentProviderOperation.newInsert(uri).withValues(values).build();
                    operations.add(op);
                }
            }

            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
        }
    }

    
    private class SummaryRestListener implements RestRequest.Listener, RestRequest.ErrorListener {
        
        final String mBlogID;
        
        SummaryRestListener(String blogID) {
            this.mBlogID = blogID;
        }
        
        @Override
        public void onResponse(final JSONObject jsonObject) {
            if (!updateUIExecutor.isShutdown() && !updateUIExecutor.isTerminated() && !updateUIExecutor.isTerminating())
                updateUIExecutor.submit(new Thread() {
                    @Override
                    public void run() {
                        AppLog.d(T.STATS, "Stats Summary Call responded for blogID " + mBlogID);
                        numberOfFinishedNetworkCalls++;

                        try {
                            if (jsonObject == null)
                                return;

                            // save summary, then send broadcast that they've changed
                            StatUtils.saveSummary(mBlogID, jsonObject);
                            StatsSummary stats = StatUtils.getSummary(mBlogID);
                            if (stats != null) {
                                LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(WordPress.getContext());
                                Intent intent = new Intent(StatsService.ACTION_STATS_SUMMARY_UPDATED);
                                intent.putExtra(StatsService.STATS_SUMMARY_UPDATED_EXTRA, stats);
                                lbm.sendBroadcast(intent);
                            }
                        } finally {
                            notifyResponseReceived();
                        }
                    }
                });
        }
        
        @Override
        public void onErrorResponse(final VolleyError volleyError) {
            if (!updateUIExecutor.isShutdown() && !updateUIExecutor.isTerminated() && !updateUIExecutor.isTerminating())
                updateUIExecutor.submit(new Thread() {
                    @Override
                    public void run() {
                        numberOfFinishedNetworkCalls++;
                        if (volleyError != null) {
                            AppLog.e(T.STATS, "Error while reading Stats summary for " + mBlogID + " " + volleyError.getMessage(), volleyError);
                        }
                        notifyResponseReceived();
                    }
                });
        }
    }
    
    boolean isDone() {
        return numberOfFinishedNetworkCalls == numberOfNetworkCalls;
    }

    private void waitForResponse() {
        synchronized (mSyncObject) {
            try {
                mSyncObject.wait();
            } catch (InterruptedException e) {
                AppLog.w(T.STATS, "Orchestrator interrupted");
            }
        }
    }

    /*
     * called when either (a) the response has been received and parsed, or (b) the request failed
     */
    private void notifyResponseReceived() {
        synchronized (mSyncObject) {
            mSyncObject.notify();
        }
    }

    /*
     * broadcast that the update has started/ended - used by StatsActivity to animate refresh icon
     * while update is in progress
     */
    private void broadcastUpdate(boolean isUpdating) {
        Intent intent = new Intent()
                .setAction(ACTION_STATS_UPDATING)
                .putExtra(EXTRA_IS_UPDATING, isUpdating);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}