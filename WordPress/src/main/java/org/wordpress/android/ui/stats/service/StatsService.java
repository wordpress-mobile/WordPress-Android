package org.wordpress.android.ui.stats.service;

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
import org.wordpress.android.models.Blog;
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
import org.wordpress.android.ui.stats.StatsBarChartUnit;
import org.wordpress.android.ui.stats.StatsUIHelper;
import org.wordpress.android.ui.stats.StatsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Background service to retrieve latest stats - Uses a Thread to enqueue the network batch call in Volley;
 * Parsing of response data is done by using a ThreadPoolExecutor with a single thread.
 */

public class StatsService extends Service {
    public static final String ARG_BLOG_ID = "blog_id";

    // broadcast action to notify clients of update start/end
    public static final String ACTION_STATS_UPDATING = "wp-stats-updating";
    public static final String EXTRA_IS_UPDATING = "is-updating";
    public static final String EXTRA_IS_ERROR = "is-error";
    public static final String EXTRA_ERROR_OBJECT = "error-object";

    // broadcast action to notify clients when summary data has changed
    public static final String ACTION_STATS_SUMMARY_UPDATED = "STATS_SUMMARY_UPDATED";
    public static final String STATS_SUMMARY_UPDATED_EXTRA = "STATS_SUMMARY_UPDATED_EXTRA";

    private static final long TWO_DAYS = 2 * 24 * 60 * 60 * 1000;

    private String mServiceBlogId;
    private String mServiceBlogTimezone = null;
    private Object mServiceBlogIdMonitor = new Object();
    private int mServiceStartId;
    private Serializable mErrorObject = null;
    private Request<JSONObject> mCurrentStatsNetworkRequest = null;

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
        if (intent == null) {
            AppLog.d(T.STATS, "StatsService was killed and restarted with a null intent.");
            // if this service's process is killed while it is started (after returning from onStartCommand(Intent, int, int)),
            // then leave it in the started state but don't retain this delivered intent.
            // Later the system will try to re-create the service.
            // Because it is in the started state, it will guarantee to call onStartCommand(Intent, int, int) after creating the new service instance;
            // if there are not any pending start commands to be delivered to the service, it will be called with a null intent object.
            stopRefresh();
            return START_NOT_STICKY;
        }

        final String blogId = StringUtils.notNullStr(intent.getStringExtra(ARG_BLOG_ID));
        final String currentServiceBlogId = getServiceBlogId();

        if (currentServiceBlogId == null) {
            startTasks(blogId, startId);
        } else if (blogId.equals(currentServiceBlogId)) {
            // already running on the same blogID
            // Do nothing
            AppLog.i(T.STATS, "StatsService is already running on this blogID - " + currentServiceBlogId);
        } else {
            // stats is running on a different blogID
            stopRefresh();
            startTasks(blogId, startId);
        }
        // Always update the startId. Always.
        this.mServiceStartId = startId;

        return START_NOT_STICKY;
    }

    /**
     * Returns a copy of the current mServiceBlogId value, or null.
     */
    private String getServiceBlogId() {
        synchronized (mServiceBlogIdMonitor) {
            if (mServiceBlogId == null) {
                return null;
            }
            return new String(mServiceBlogId);
        }
    }

    private void setServiceBlogIdAndTimeZone(String value) {
        synchronized (mServiceBlogIdMonitor) {
            mServiceBlogId = value;
            String timezone = null;
            if (mServiceBlogId != null) {
                Blog blog = WordPress.wpDB.getBlogForDotComBlogId(value);
                timezone = StatsUtils.getBlogTimezone(blog);
            }
            if (timezone != null) {
                mServiceBlogTimezone = timezone;
            } else {
                mServiceBlogTimezone = null;
            }
        }
    }

    private void stopRefresh() {
        if (mCurrentStatsNetworkRequest != null && !mCurrentStatsNetworkRequest.hasHadResponseDelivered()
                && !mCurrentStatsNetworkRequest.isCanceled()) {
            mCurrentStatsNetworkRequest.cancel();
        }
        setServiceBlogIdAndTimeZone(null);
        this.mErrorObject = null;
        this.mServiceStartId = 0;
    }

    private void startTasks(final String blogId, final int startId) {
        setServiceBlogIdAndTimeZone(blogId);
        this.mServiceStartId = startId;
        this.mErrorObject = null;

        new Thread() {
            @Override
            public void run() {
                final RestClientUtils restClientUtils = WordPress.getRestClientUtils();

                // Calculate the correct today and yesterday values by using the blog time_zone offset
                // fallback to device settings if option is null
                final String today = (mServiceBlogTimezone != null) ?
                        StatsUtils.getCurrentDateTZ(mServiceBlogTimezone) : StatsUtils.getCurrentDate();
                final String yesterday = (mServiceBlogTimezone != null) ?
                        StatsUtils.getYesterdaysDateTZ(mServiceBlogTimezone) : StatsUtils.getYesterdaysDate();

                AppLog.i(T.STATS, "Update started for blogID - " + blogId);
                broadcastUpdate(true);

                // visitors and views
                final String summaryPath = String.format("/sites/%s/stats", blogId);
                // bar charts
                final String barChartWeekPath = getBarChartPath(blogId, StatsBarChartUnit.WEEK, 30);
                final String barChartMonthPath = getBarChartPath(blogId, StatsBarChartUnit.MONTH, 30);
                // top posts and pages
                final String topPostsAndPagesTodayPath = String.format(
                        "/sites/%s/stats/top-posts?date=%s", blogId, today);
                final String topPostsAndPagesYesterdayPath = String.format(
                        "/sites/%s/stats/top-posts?date=%s", blogId, yesterday);
                // referrers
                final String referrersTodayPath = String.format(
                        "/sites/%s/stats/referrers?date=%s", blogId, today);
                final String referrersYesterdayPath = String.format(
                        "/sites/%s/stats/referrers?date=%s", blogId, yesterday);
                // clicks
                final String clicksTodayPath = String.format(
                        "/sites/%s/stats/clicks?date=%s", blogId, today);
                final String clicksYesterdayPath = String.format(
                        "/sites/%s/stats/clicks?date=%s", blogId, yesterday);
                // search engine terms
                final String searchEngineTermsTodayPath = String.format(
                        "/sites/%s/stats/search-terms?date=%s", blogId, today);
                final String searchEngineTermsYesterdayPath = String.format(
                        "/sites/%s/stats/search-terms?date=%s", blogId, yesterday);
                // Views by country
                final String viewByCountryTodayPath = String.format(
                        "/sites/%s/stats/country-views?date=%s", blogId, today);
                final String viewByCountryYesterdayPath = String.format(
                        "/sites/%s/stats/country-views?date=%s", blogId, yesterday);

                final String parametersSepator = "&urls%5B%5D=";

                String path = new StringBuilder("batch/?urls%5B%5D=")
                        .append(Uri.encode(summaryPath))
                        .append(parametersSepator)
                        .append(Uri.encode(barChartWeekPath))
                        .append(parametersSepator)
                        .append(Uri.encode(barChartMonthPath))
                        .append(parametersSepator)
                        .append(Uri.encode(topPostsAndPagesTodayPath))
                        .append(parametersSepator)
                        .append(Uri.encode(topPostsAndPagesYesterdayPath))
                        .append(parametersSepator)
                        .append(Uri.encode(referrersTodayPath))
                        .append(parametersSepator)
                        .append(Uri.encode(referrersYesterdayPath))
                        .append(parametersSepator)
                        .append(Uri.encode(clicksTodayPath))
                        .append(parametersSepator)
                        .append(Uri.encode(clicksYesterdayPath))
                        .append(parametersSepator)
                        .append(Uri.encode(searchEngineTermsTodayPath))
                        .append(parametersSepator)
                        .append(Uri.encode(searchEngineTermsYesterdayPath))
                        .append(parametersSepator)
                        .append(Uri.encode(viewByCountryTodayPath))
                        .append(parametersSepator)
                        .append(Uri.encode(viewByCountryYesterdayPath))
                        .toString();

                RestBatchCallListener restAPIListener = new RestBatchCallListener(blogId,
                        summaryPath,
                        barChartWeekPath, barChartMonthPath,
                        topPostsAndPagesTodayPath, topPostsAndPagesYesterdayPath,
                        referrersTodayPath, referrersYesterdayPath,
                        clicksTodayPath, clicksYesterdayPath,
                        searchEngineTermsTodayPath, searchEngineTermsYesterdayPath,
                        viewByCountryTodayPath, viewByCountryYesterdayPath);

                AppLog.i(T.STATS, "Enqueuing the following Stats request " + path);
                restClientUtils.get(path, restAPIListener, restAPIListener);
            } // end run
        } .start();
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

    private class RestBatchCallListener implements RestRequest.Listener, RestRequest.ErrorListener {
        final String mRequestBlogId, mSummaryAPICallPath, mBarChartWeekPath, mBarChartMonthPath,
        mTopPostsAndPagesTodayPath, mTopPostsAndPagesYesterdayPath, mReferrersTodayPath,
        mReferrersYesterdayPath, mClicksTodayPath, mClicksYesterdayPath,
        mSearchEngineTermsTodayPath, mSearchEngineTermsYesterdayPath,
        mViewByCountryTodayPath, mViewByCountryYesterdayPath;

        RestBatchCallListener(String blogId, String summaryAPICallPath,
                String barChartWeekPath, String barChartMonthPath,
                String topPostsAndPagesTodayPath, String topPostsAndPagesYesterdayPath,
                String referrersTodayPath, String referrersYesterdayPath,
                String clicksTodayPath, String clicksYesterdayPath,
                String searchEngineTermsTodayPath, String searchEngineTermsYesterdayPath,
                String viewByCountryTodayPath, String viewByCountryYesterdayPath) {
            this.mRequestBlogId = blogId;
            this.mSummaryAPICallPath = summaryAPICallPath;
            this.mBarChartWeekPath = barChartWeekPath;
            this.mBarChartMonthPath = barChartMonthPath;
            this.mTopPostsAndPagesTodayPath = topPostsAndPagesTodayPath;
            this.mTopPostsAndPagesYesterdayPath = topPostsAndPagesYesterdayPath;
            this.mReferrersTodayPath = referrersTodayPath;
            this.mReferrersYesterdayPath = referrersYesterdayPath;
            this.mClicksTodayPath = clicksTodayPath;
            this.mClicksYesterdayPath = clicksYesterdayPath;
            this.mSearchEngineTermsTodayPath = searchEngineTermsTodayPath;
            this.mSearchEngineTermsYesterdayPath = searchEngineTermsYesterdayPath;
            this.mViewByCountryTodayPath = viewByCountryTodayPath;
            this.mViewByCountryYesterdayPath = viewByCountryYesterdayPath;
        }

        @Override
        public void onResponse(final JSONObject response) {
            // single thread
            ThreadPoolExecutor parseResponseExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
            parseResponseExecutor.submit(new Thread() {
                @Override
                public void run() {
                    parseSummaryResponse(response);
                    parseBarChartResponse(response);
                    parseTopPostsAndPagesResponse(response);
                    parseReferrersResponse(response);
                    parseClicksResponse(response);
                    parseSearchEngineTermsResponse(response);
                    parseViewsByCountryResponse(response);

                    // Stop the service if this is the current response, or mServiceBlogId is null
                    String currentServiceBlogId = getServiceBlogId();
                    if (currentServiceBlogId == null || currentServiceBlogId.equals(mRequestBlogId)) {
                        stopService();
                    }
                }
            });
        }

        private void parseViewsByCountryResponse(final JSONObject response) {
            String currentServiceBlogId = getServiceBlogId();
            if (currentServiceBlogId == null || !currentServiceBlogId.equals(mRequestBlogId)) {
                return;
            }
            String[] viewsByCountryPaths = {mViewByCountryTodayPath, mViewByCountryYesterdayPath};
            for (String currentViewsByCountryPath : viewsByCountryPaths) {
                if (response.has(currentViewsByCountryPath)) {
                    try {
                        final JSONObject currentViewsByCountryJsonObject =
                                response.getJSONObject(currentViewsByCountryPath);
                        if (!isSingleCallResponseError(currentViewsByCountryPath, currentViewsByCountryJsonObject)) {
                            if (!currentViewsByCountryJsonObject.has("country-views")) {
                                return;
                            }

                            JSONArray results = currentViewsByCountryJsonObject.getJSONArray("country-views");
                            int count = Math.min(results.length(), StatsUIHelper.STATS_GROUP_MAX_ITEMS);
                            String date = currentViewsByCountryJsonObject.getString("date");
                            long dateMs = StatsUtils.toMs(date);
                            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

                            if (count > 0) {
                                // delete data with the same date, and data older than two days ago (keep
                                // yesterday's data)
                                ContentProviderOperation deleteOp = ContentProviderOperation
                                        .newDelete(StatsContentProvider.STATS_GEOVIEWS_URI)
                                        .withSelection("blogId=? AND (date=? OR date<=?)", new String[] {
                                                mRequestBlogId, dateMs + "", (dateMs - TWO_DAYS) + ""
                                        }).build();
                                operations.add(deleteOp);
                            }

                            for (int i = 0; i < count; i++) {
                                JSONObject result = results.getJSONObject(i);
                                StatsGeoview stat = new StatsGeoview(mRequestBlogId, result);
                                ContentValues values = StatsGeoviewsTable.getContentValues(stat);
                                ContentProviderOperation op = ContentProviderOperation
                                        .newInsert(StatsContentProvider.STATS_GEOVIEWS_URI).withValues(values).build();
                                operations.add(op);
                            }

                            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                        }
                    } catch (RemoteException e) {
                        logSingleCallError(currentViewsByCountryPath, e);
                    } catch (OperationApplicationException e) {
                        logSingleCallError(currentViewsByCountryPath, e);
                    } catch (JSONException e) {
                        logSingleCallError(currentViewsByCountryPath, e);
                    }
                }
            }
            getContentResolver().notifyChange(StatsContentProvider.STATS_GEOVIEWS_URI, null);
        }

        private void parseSearchEngineTermsResponse(final JSONObject response) {
            String currentServiceBlogId = getServiceBlogId();
            if (currentServiceBlogId == null || !currentServiceBlogId.equals(mRequestBlogId)) {
                return;
            }
            String[] searchEngineTermsPaths = {mSearchEngineTermsTodayPath, mSearchEngineTermsYesterdayPath};
            for (String currentSearchEngineTermsPath : searchEngineTermsPaths) {
                if (response.has(currentSearchEngineTermsPath)) {
                    try {
                        final JSONObject currentSearchEngineTermsJsonObject =
                                response.getJSONObject(currentSearchEngineTermsPath);
                        if (!isSingleCallResponseError(currentSearchEngineTermsPath,
                                currentSearchEngineTermsJsonObject)) {
                            String date = currentSearchEngineTermsJsonObject.getString("date");
                            long dateMs = StatsUtils.toMs(date);

                            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

                            ContentProviderOperation deleteOp = ContentProviderOperation
                                    .newDelete(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI)
                                    .withSelection("blogId=? AND (date=? OR date<=?)",
                                            new String[] {
                                            mRequestBlogId, dateMs + "", (dateMs - TWO_DAYS) + ""
                                    }).build();

                            operations.add(deleteOp);

                            JSONArray results = currentSearchEngineTermsJsonObject.getJSONArray("search-terms");

                            int count = Math.min(results.length(), StatsUIHelper.STATS_GROUP_MAX_ITEMS);
                            for (int i = 0; i < count; i++) {
                                JSONArray result = results.getJSONArray(i);
                                StatsSearchEngineTerm stat = new StatsSearchEngineTerm(mRequestBlogId, date, result);
                                ContentValues values = StatsSearchEngineTermsTable.getContentValues(stat);
                                getContentResolver()
                                    .insert(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI, values);

                                ContentProviderOperation insertOp = ContentProviderOperation
                                        .newInsert(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI)
                                        .withValues(values)
                                        .build();
                                operations.add(insertOp);
                            }

                            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                        }
                    } catch (RemoteException e) {
                        logSingleCallError(currentSearchEngineTermsPath, e);
                    } catch (OperationApplicationException e) {
                        logSingleCallError(currentSearchEngineTermsPath, e);
                    } catch (JSONException e) {
                        logSingleCallError(currentSearchEngineTermsPath, e);
                    }
                }
            }
            getContentResolver().notifyChange(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI, null);
        }

        private void parseClicksResponse(final JSONObject response) {
            String currentServiceBlogId = getServiceBlogId();
            if (currentServiceBlogId == null || !currentServiceBlogId.equals(mRequestBlogId)) {
                return;
            }
            String[] clicksPaths = {mClicksTodayPath, mClicksYesterdayPath};
            for (String currentClickPath : clicksPaths) {
                if (response.has(currentClickPath)) {
                    try {
                        final JSONObject currentClicksJsonObject =
                                response.getJSONObject(currentClickPath);
                        if (!isSingleCallResponseError(currentClickPath, currentClicksJsonObject)) {
                            String date = currentClicksJsonObject.getString("date");
                            long dateMs = StatsUtils.toMs(date);

                            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

                            // delete data with the same date, and data older than two days ago (keep yesterday's
                            // data)
                            ContentProviderOperation deleteGroup = ContentProviderOperation
                                    .newDelete(StatsContentProvider.STATS_CLICK_GROUP_URI)
                                    .withSelection("blogId=? AND (date=? OR date<=?)",
                                            new String[] {
                                            mRequestBlogId, dateMs + "", (dateMs - TWO_DAYS) + ""
                                    }).build();
                            ContentProviderOperation deleteChildOp = ContentProviderOperation
                                    .newDelete(StatsContentProvider.STATS_CLICKS_URI)
                                    .withSelection("blogId=? AND (date=? OR date<=?)",
                                            new String[] {mRequestBlogId, dateMs + "", (dateMs - TWO_DAYS) + ""}
                                    )
                                    .build();

                            operations.add(deleteGroup);
                            operations.add(deleteChildOp);

                            JSONArray groups = currentClicksJsonObject.getJSONArray("clicks");

                            // insert groups, limited to the number that can actually be displayed
                            int groupsCount = Math.min(groups.length(), StatsUIHelper.STATS_GROUP_MAX_ITEMS);
                            for (int i = 0; i < groupsCount; i++) {
                                JSONObject group = groups.getJSONObject(i);
                                StatsClickGroup statGroup = new StatsClickGroup(mRequestBlogId, date, group);
                                ContentValues values = StatsClickGroupsTable.getContentValues(statGroup);

                                ContentProviderOperation insertGroupOp = ContentProviderOperation
                                        .newInsert(StatsContentProvider.STATS_CLICK_GROUP_URI).withValues(values)
                                        .build();
                                operations.add(insertGroupOp);

                                // insert children if there are any, limited to the number that can be displayed
                                JSONArray clicks = group.getJSONArray("results");
                                int childCount = Math.min(clicks.length(), StatsUIHelper.STATS_CHILD_MAX_ITEMS);
                                if (childCount > 1) {
                                    for (int j = 0; j < childCount; j++) {
                                        StatsClick stat = new StatsClick(mRequestBlogId, date,
                                                statGroup.getGroupId(), clicks.getJSONArray(j));
                                        ContentValues v = StatsClicksTable.getContentValues(stat);
                                        ContentProviderOperation insertChildOp = ContentProviderOperation
                                                .newInsert(StatsContentProvider.STATS_CLICKS_URI).withValues(v).build();
                                        operations.add(insertChildOp);
                                    }
                                }
                            }

                            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                        }
                    } catch (RemoteException e) {
                        logSingleCallError(currentClickPath, e);
                    } catch (OperationApplicationException e) {
                        logSingleCallError(currentClickPath, e);
                    } catch (JSONException e) {
                        logSingleCallError(currentClickPath, e);
                    }
                }
            }
            getContentResolver().notifyChange(StatsContentProvider.STATS_CLICK_GROUP_URI, null);
            getContentResolver().notifyChange(StatsContentProvider.STATS_CLICKS_URI, null);
        }

        private void parseReferrersResponse(final JSONObject response) {
            String currentServiceBlogId = getServiceBlogId();
            if (currentServiceBlogId == null || !currentServiceBlogId.equals(mRequestBlogId)) {
                return;
            }
            String[] referrersPaths = {mReferrersTodayPath, mReferrersYesterdayPath};
            for (String currentReferrerPath : referrersPaths) {
                if (response.has(currentReferrerPath)) {
                    try {
                        final JSONObject currentReferrersJsonObject =
                                response.getJSONObject(currentReferrerPath);
                        if (!isSingleCallResponseError(currentReferrerPath, currentReferrersJsonObject)) {
                            String date = currentReferrersJsonObject.getString("date");
                            long dateMs = StatsUtils.toMs(date);

                            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

                            // delete data with the same date, and data older than two days ago (keep yesterday's
                            // data)
                            ContentProviderOperation deleteGroupOp = ContentProviderOperation
                                    .newDelete(StatsContentProvider.STATS_REFERRER_GROUP_URI)
                                    .withSelection("blogId=? AND (date=? OR date<=?)", new String[] {
                                            mRequestBlogId, dateMs + "", (dateMs - TWO_DAYS) + ""
                                    }).build();
                            operations.add(deleteGroupOp);

                            ContentProviderOperation deleteOp = ContentProviderOperation
                                    .newDelete(StatsContentProvider.STATS_REFERRERS_URI)
                                    .withSelection("blogId=? AND (date=? OR date<=?)", new String[] {
                                            mRequestBlogId, dateMs + "", (dateMs - TWO_DAYS) + ""
                                    }).build();
                            operations.add(deleteOp);

                            JSONArray groups = currentReferrersJsonObject.getJSONArray("referrers");
                            int groupsCount = Math.min(groups.length(), StatsUIHelper.STATS_GROUP_MAX_ITEMS);

                            // insert groups
                            for (int i = 0; i < groupsCount; i++) {
                                JSONObject group = groups.getJSONObject(i);
                                StatsReferrerGroup statGroup = new StatsReferrerGroup(mRequestBlogId, date, group);
                                ContentValues values = StatsReferrerGroupsTable.getContentValues(statGroup);
                                ContentProviderOperation insertGroupOp = ContentProviderOperation
                                        .newInsert(StatsContentProvider.STATS_REFERRER_GROUP_URI).withValues(values)
                                        .build();
                                operations.add(insertGroupOp);

                                // insert children, only if there is more than one entry
                                JSONArray referrers = group.getJSONArray("results");
                                int childCount = Math.min(referrers.length(), StatsUIHelper.STATS_CHILD_MAX_ITEMS);
                                if (childCount > 1) {
                                    for (int j = 0; j < childCount; j++) {
                                        StatsReferrer stat = new StatsReferrer(mRequestBlogId, date,
                                                statGroup.getGroupId(), referrers.getJSONArray(j));
                                        ContentValues v = StatsReferrersTable.getContentValues(stat);
                                        ContentProviderOperation insertChildOp = ContentProviderOperation
                                                .newInsert(StatsContentProvider.STATS_REFERRERS_URI).withValues(v)
                                                .build();
                                        operations.add(insertChildOp);
                                    }
                                }
                            }
                            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                        }
                    } catch (RemoteException e) {
                        logSingleCallError(currentReferrerPath, e);
                    } catch (OperationApplicationException e) {
                        logSingleCallError(currentReferrerPath, e);
                    } catch (JSONException e) {
                        logSingleCallError(currentReferrerPath, e);
                    }
                }
            }
            getContentResolver().notifyChange(StatsContentProvider.STATS_REFERRER_GROUP_URI, null);
            getContentResolver().notifyChange(StatsContentProvider.STATS_REFERRERS_URI, null);
        }

        private void parseTopPostsAndPagesResponse(final JSONObject response) {
            String currentServiceBlogId = getServiceBlogId();
            if (currentServiceBlogId == null || !currentServiceBlogId.equals(mRequestBlogId)) {
                return;
            }
            String[] topPostsAndPagesPaths = {mTopPostsAndPagesTodayPath, mTopPostsAndPagesYesterdayPath};
            for (String currentTopPostsAndPagesPath : topPostsAndPagesPaths) {
                if (response.has(currentTopPostsAndPagesPath)) {
                    try {
                        final JSONObject currentTopPostsAndPagesJsonObject =
                                response.getJSONObject(currentTopPostsAndPagesPath);
                        if (!isSingleCallResponseError(
                                currentTopPostsAndPagesPath, currentTopPostsAndPagesJsonObject)) {
                            if (!currentTopPostsAndPagesJsonObject.has("top-posts")) {
                                return;
                            }

                            JSONArray results = currentTopPostsAndPagesJsonObject.getJSONArray("top-posts");
                            int count = Math.min(results.length(), StatsUIHelper.STATS_GROUP_MAX_ITEMS);

                            String date = currentTopPostsAndPagesJsonObject.getString("date");
                            long dateMs = StatsUtils.toMs(date);

                            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
                            // delete data with the same date, and data older than two days ago (keep yesterday's data)
                            ContentProviderOperation deleteOp = ContentProviderOperation
                                    .newDelete(StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI)
                                    .withSelection("blogId=? AND (date=? OR date<=?)", new String[] {
                                            mRequestBlogId, dateMs + "", (dateMs - TWO_DAYS) + ""
                                    }).build();
                            operations.add(deleteOp);

                            for (int i = 0; i < count; i++) {
                                JSONObject result = results.getJSONObject(i);
                                StatsTopPostsAndPages stat = new StatsTopPostsAndPages(mRequestBlogId, result);
                                ContentValues values = StatsTopPostsAndPagesTable.getContentValues(stat);
                                ContentProviderOperation op = ContentProviderOperation
                                        .newInsert(StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI)
                                        .withValues(values)
                                        .build();
                                operations.add(op);
                            }

                            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                        }
                    } catch (RemoteException e) {
                        logSingleCallError(currentTopPostsAndPagesPath, e);
                    } catch (OperationApplicationException e) {
                        logSingleCallError(currentTopPostsAndPagesPath, e);
                    } catch (JSONException e) {
                        logSingleCallError(currentTopPostsAndPagesPath, e);
                    }
                }
            }
            getContentResolver().notifyChange(StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI, null);
        }

        private void parseBarChartResponse(final JSONObject response) {
            String currentServiceBlogId = getServiceBlogId();
            if (currentServiceBlogId == null || !currentServiceBlogId.equals(mRequestBlogId)) {
                return;
            }
            String[] barChartPaths = {mBarChartWeekPath, mBarChartMonthPath};
            for (String currentBarChartPath : barChartPaths) {
                if (response.has(currentBarChartPath)) {
                    try {
                        final JSONObject barChartJsonObject = response.getJSONObject(currentBarChartPath);
                        final StatsBarChartUnit currentUnit = currentBarChartPath.equals(mBarChartWeekPath)
                               ? StatsBarChartUnit.WEEK : StatsBarChartUnit.MONTH;
                        if (!isSingleCallResponseError(currentBarChartPath, barChartJsonObject)) {
                            if (!barChartJsonObject.has("data")) {
                                return;
                            }

                            Uri uri = StatsContentProvider.STATS_BAR_CHART_DATA_URI;
                            JSONArray results = barChartJsonObject.getJSONArray("data");

                            int count = results.length();

                            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

                            // delete old stats and insert new ones
                            if (count > 0) {
                                ContentProviderOperation op = ContentProviderOperation.newDelete(uri)
                                        .withSelection("blogId=? AND unit=?", new String[] {
                                                mRequestBlogId, currentUnit.name()
                                        }).build();
                                operations.add(op);
                            }

                            for (int i = 0; i < count; i++) {
                                JSONArray result = results.getJSONArray(i);
                                StatsBarChartData stat = new StatsBarChartData(mRequestBlogId, currentUnit, result);
                                ContentValues values = StatsBarChartDataTable.getContentValues(stat);

                                if (values != null && uri != null) {
                                    ContentProviderOperation op = ContentProviderOperation.newInsert(uri)
                                            .withValues(values).build();
                                    operations.add(op);
                                }
                            }

                            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                        }
                    } catch (RemoteException e) {
                        logSingleCallError(currentBarChartPath, e);
                    } catch (OperationApplicationException e) {
                        logSingleCallError(currentBarChartPath, e);
                    } catch (JSONException e) {
                        logSingleCallError(currentBarChartPath, e);
                    }
                }
            }
            getContentResolver().notifyChange(StatsContentProvider.STATS_BAR_CHART_DATA_URI, null);
        }

        private void parseSummaryResponse(final JSONObject response) {
            String currentServiceBlogId = getServiceBlogId();
            if (currentServiceBlogId == null || !currentServiceBlogId.equals(mRequestBlogId)) {
                return;
            }
            final String currentPath = mSummaryAPICallPath;
            if (response.has(currentPath)) {
                try {
                    JSONObject summaryJsonObject = response.getJSONObject(currentPath);
                    if (!isSingleCallResponseError(currentPath, summaryJsonObject)) {
                        if (summaryJsonObject == null) {
                            return;
                        }
                        // save summary, then send broadcast that they've changed
                        StatsUtils.saveSummary(mRequestBlogId, summaryJsonObject);
                        StatsSummary stats = StatsUtils.getSummary(mRequestBlogId);
                        if (stats != null) {
                            LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(WordPress.getContext());
                            Intent intent = new Intent(StatsService.ACTION_STATS_SUMMARY_UPDATED);
                            intent.putExtra(StatsService.STATS_SUMMARY_UPDATED_EXTRA, stats);
                            lbm.sendBroadcast(intent);
                        }
                    }
                } catch (JSONException e) {
                    logSingleCallError(mSummaryAPICallPath, e);
                }
            }
        }

        private boolean isSingleCallResponseError(String restCallPATH, final JSONObject response) {
            if (response.has("errors")) {
                mErrorObject = response.toString();
                AppLog.e(AppLog.T.STATS, "The single call " + restCallPATH
                        + " failed with the following response: " + response.toString());
                return true;
            }

            return false;
        }

        private void logSingleCallError(String restCallPATH, Exception e) {
            AppLog.e(AppLog.T.STATS, "Single call failed " + restCallPATH, e);
        }

        private void stopService() {
            broadcastUpdate(false);
            stopSelf(mServiceStartId);
        }

        @Override
        public void onErrorResponse(final VolleyError volleyError) {
            if (volleyError != null) {
                mErrorObject = volleyError;
                AppLog.e(T.STATS, "Error while reading Stats summary for " + mRequestBlogId + " "
                + volleyError.getMessage(), volleyError);
            }
            stopService();
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
        if (mErrorObject != null) {
            intent.putExtra(EXTRA_IS_ERROR, true);
            intent.putExtra(EXTRA_ERROR_OBJECT, mErrorObject);
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
