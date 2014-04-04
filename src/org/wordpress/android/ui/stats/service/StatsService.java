package org.wordpress.android.ui.stats.service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;

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
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.ui.stats.StatsBarChartUnit;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StatUtils;
import org.wordpress.android.util.StringUtils;

/**
 * Background service to retrieve latest stats - uses a ThreadPoolExecutor to
 * handle concurrent updating of the various stats tasks - see AbsStatsTask for
 * base implementation of an individual stats task
 */

public class StatsService extends Service {

    public static final String ARG_BLOG_ID = "blog_id";

    // broadcast action to notify clients of update start/end
    public static final String ACTION_STATS_UPDATING = "wp-stats-updating";
    public static final String EXTRA_IS_UPDATING = "is-updating";

    // broadcast action to notify clients when summary data has changed
    public static final String ACTION_STATS_SUMMARY_UPDATED = "STATS_SUMMARY_UPDATED";
    public static final String STATS_SUMMARY_UPDATED_EXTRA = "STATS_SUMMARY_UPDATED_EXTRA";
    
    protected static final long TWO_DAYS = 2 * 24 * 60 * 60 * 1000;
    
    private final Object mSyncObject = new Object();
    private LinkedList<Request<JSONObject>> statsNetworkRequests = new LinkedList<Request<JSONObject>>();

    private String mBlogId;
    private static final int NUMBER_OF_NETWORK_CALLS = 13; //The number of networks calls made by stats. 
    private int numberOfReturnedCall = 0;
    
    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(T.STATS, "service created");
    }

    @Override
    public void onDestroy() {
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
        this.mBlogId = blogId;
        startTasks(blogId, startId);
        return START_NOT_STICKY;
    }

    private void startTasks(final String blogId, final int startId) {

        new Thread() {
            @Override
            public void run() {
                final String today = StatUtils.getCurrentDate();
                final String yesterday = StatUtils.getYesterdaysDate();
            
     
                AppLog.i(T.STATS, "update started");
                broadcastUpdate(true);
                
                // visitors and views
                String path = String.format("sites/%s/stats", blogId);
                statsNetworkRequests.add(WordPress.getRestClientUtils().get(path, statsSummaryRestListener, restErrListener));
                
                path = getBarChartPath(StatsBarChartUnit.WEEK, 30);
                statsNetworkRequests.add(WordPress.getRestClientUtils().get(path, new BarChartRestListener(StatsBarChartUnit.WEEK), restErrListener));
                path = getBarChartPath(StatsBarChartUnit.MONTH, 30);
                statsNetworkRequests.add(WordPress.getRestClientUtils().get(path, new BarChartRestListener(StatsBarChartUnit.MONTH), restErrListener));
                
                // top posts and pages
                path = String.format("sites/%s/stats/top-posts?date=%s", mBlogId, today);
                statsNetworkRequests.add(WordPress.getRestClientUtils().get(path, new TopPostAndPageRestListener(today), restErrListener));
                path = String.format("sites/%s/stats/top-posts?date=%s", mBlogId, yesterday);
                statsNetworkRequests.add(WordPress.getRestClientUtils().get(path, new TopPostAndPageRestListener(yesterday), restErrListener));
                
                // referrers
                path = String.format("sites/%s/stats/referrers?date=%s", mBlogId, today);
                statsNetworkRequests.add(WordPress.getRestClientUtils().get(path, new ReferrersListener(today), restErrListener));
                path = String.format("sites/%s/stats/referrers?date=%s", mBlogId, yesterday);
                statsNetworkRequests.add(WordPress.getRestClientUtils().get(path, new ReferrersListener(yesterday), restErrListener));
                
                // clicks
                path = String.format("sites/%s/stats/clicks?date=%s", mBlogId, today);
                statsNetworkRequests.add(WordPress.getRestClientUtils().get(path, new ClicksListener(today), restErrListener));
                path = String.format("sites/%s/stats/clicks?date=%s", mBlogId, yesterday);
                statsNetworkRequests.add(WordPress.getRestClientUtils().get(path, new ClicksListener(yesterday), restErrListener));

                // search engine terms
                path = String.format("sites/%s/stats/search-terms?date=%s", mBlogId, today);
                statsNetworkRequests.add(WordPress.getRestClientUtils().get(path, new SearchEngineTermsListener(today), restErrListener));
                path = String.format("sites/%s/stats/search-terms?date=%s", mBlogId, yesterday);
                statsNetworkRequests.add(WordPress.getRestClientUtils().get(path, new SearchEngineTermsListener(yesterday), restErrListener));

                // views by country - put at the end since this will start other networks calls on finish
                path = String.format("sites/%s/stats/country-views?date=%s", mBlogId, today);
                statsNetworkRequests.add(WordPress.getRestClientUtils().get(path, new ViewsByCountryTask(today), restErrListener));
                path = String.format("sites/%s/stats/country-views?date=%s", mBlogId, yesterday);
                statsNetworkRequests.add(WordPress.getRestClientUtils().get(path, new ViewsByCountryTask(yesterday), restErrListener));
                
                while (!isDone()) {
                    AppLog.w(T.STATS, "sto per andare a dormire");
                    waitForResponse();
                    AppLog.w(T.STATS, "mi sono svegliato!");
                }

                broadcastUpdate(false);
                AppLog.w(T.STATS, "spegniamo tutto!");
                stopSelf(startId);               
            } //end run
        }.start(); 
    }

    private class SearchEngineTermsListener implements RestRequest.Listener {

        private String date;

        SearchEngineTermsListener(String date){
            this.date = date;
        }

        @Override
        public void onResponse(final JSONObject response) {
            numberOfReturnedCall++;
            AppLog.d(T.STATS, "Search Engine Terms Call " + date + " responded");
            if (response == null) {
                notifyResponseReceived();
                return;
            }

            try {
                String date = response.getString("date");
                long dateMs = StatUtils.toMs(date);

                ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

                ContentProviderOperation delete_op = ContentProviderOperation.newDelete(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI).withSelection("blogId=? AND (date=? OR date<=?)",
                        new String[] { mBlogId, dateMs + "", (dateMs - TWO_DAYS) + "" }).build();

                operations.add(delete_op);

                JSONArray results = response.getJSONArray("search-terms");

                int count = Math.min(results.length(), StatsActivity.STATS_GROUP_MAX_ITEMS);
                for (int i = 0; i < count; i++ ) {
                    JSONArray result = results.getJSONArray(i);
                    StatsSearchEngineTerm stat = new StatsSearchEngineTerm(mBlogId, date, result);
                    ContentValues values = StatsSearchEngineTermsTable.getContentValues(stat);
                    getContentResolver().insert(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI, values);

                    ContentProviderOperation insert_op = ContentProviderOperation.newInsert(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI).withValues(values).build();
                    operations.add(insert_op);
                }

                getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                getContentResolver().notifyChange(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI, null);

            } catch (JSONException e) {
                AppLog.e(AppLog.T.STATS, e);
            } catch (RemoteException e) {
                AppLog.e(AppLog.T.STATS, e);
            } catch (OperationApplicationException e) {
                AppLog.e(AppLog.T.STATS, e);
            }
            
            notifyResponseReceived();
        }
    };
    
    
    private class ClicksListener implements RestRequest.Listener {

        private String date;

        ClicksListener(String date){
            this.date = date;
        }

        @Override
        public void onResponse(final JSONObject response) {
            numberOfReturnedCall++;
            AppLog.d(T.STATS, "ClicksTask Call " + date + " responded");
            if (response == null) {
                notifyResponseReceived();
                return;
            }

            try {
                String date = response.getString("date");
                long dateMs = StatUtils.toMs(date);

                ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

                // delete data with the same date, and data older than two days ago (keep yesterday's data)
                ContentProviderOperation delete_group = ContentProviderOperation.newDelete(StatsContentProvider.STATS_CLICK_GROUP_URI).withSelection("blogId=? AND (date=? OR date<=?)",
                        new String[] { mBlogId, dateMs + "", (dateMs - TWO_DAYS) + "" }).build();
                ContentProviderOperation delete_child = ContentProviderOperation.newDelete(StatsContentProvider.STATS_CLICKS_URI).withSelection("blogId=? AND (date=? OR date<=?)",
                        new String[] { mBlogId, dateMs + "", (dateMs - TWO_DAYS) + "" }).build();

                operations.add(delete_group);
                operations.add(delete_child);


                JSONArray groups = response.getJSONArray("clicks");

                 // insert groups, limited to the number that can actually be displayed
                int groupsCount = Math.min(groups.length(), StatsActivity.STATS_GROUP_MAX_ITEMS);
                for (int i = 0; i < groupsCount; i++ ) {
                    JSONObject group = groups.getJSONObject(i);
                    StatsClickGroup statGroup = new StatsClickGroup(mBlogId, date, group);
                    ContentValues values = StatsClickGroupsTable.getContentValues(statGroup);

                    ContentProviderOperation insert_group = ContentProviderOperation.newInsert(StatsContentProvider.STATS_CLICK_GROUP_URI).withValues(values).build();
                    operations.add(insert_group);

                    // insert children if there are any, limited to the number that can be displayed
                    JSONArray clicks = group.getJSONArray("results");
                    int childCount = Math.min(clicks.length(), StatsActivity.STATS_CHILD_MAX_ITEMS);
                    if (childCount > 1) {
                        for (int j = 0; j < childCount; j++) {
                            StatsClick stat = new StatsClick(mBlogId, date, statGroup.getGroupId(), clicks.getJSONArray(j));
                            ContentValues v = StatsClicksTable.getContentValues(stat);
                            ContentProviderOperation insert_child = ContentProviderOperation.newInsert(StatsContentProvider.STATS_CLICKS_URI).withValues(v).build();
                            operations.add(insert_child);
                        }
                    }
                }

                getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                getContentResolver().notifyChange(StatsContentProvider.STATS_CLICK_GROUP_URI, null);
                getContentResolver().notifyChange(StatsContentProvider.STATS_CLICKS_URI, null);

            } catch (JSONException e) {
                AppLog.e(AppLog.T.STATS, e);
            } catch (RemoteException e) {
                AppLog.e(AppLog.T.STATS, e);
            } catch (OperationApplicationException e) {
                AppLog.e(AppLog.T.STATS, e);
            }
            
            notifyResponseReceived();
        }
    };
    
    private class ReferrersListener implements RestRequest.Listener {

        private String date;

        ReferrersListener(String date){
            this.date = date;
        }

        @Override
        public void onResponse(final JSONObject response) {
            numberOfReturnedCall++;
            AppLog.d(T.STATS, "Referrers Call " + date + " responded");
            if (response == null) {
                notifyResponseReceived();
                return;
            }

            try {
                String date = response.getString("date");
                long dateMs = StatUtils.toMs(date);

                ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

                // delete data with the same date, and data older than two days ago (keep yesterday's data)
                ContentProviderOperation delete_group_op = ContentProviderOperation.newDelete(StatsContentProvider.STATS_REFERRER_GROUP_URI)
                        .withSelection("blogId=? AND (date=? OR date<=?)", new String[] { mBlogId, dateMs + "", (dateMs - TWO_DAYS) + "" }).build();
                operations.add(delete_group_op);

                ContentProviderOperation delete_op = ContentProviderOperation.newDelete(StatsContentProvider.STATS_REFERRERS_URI)
                        .withSelection("blogId=? AND (date=? OR date<=?)", new String[] { mBlogId, dateMs + "", (dateMs - TWO_DAYS) + "" }).build();
                operations.add(delete_op);

                JSONArray groups = response.getJSONArray("referrers");
                int groupsCount = Math.min(groups.length(), StatsActivity.STATS_GROUP_MAX_ITEMS);

                // insert groups
                for (int i = 0; i < groupsCount; i++ ) {
                    JSONObject group = groups.getJSONObject(i);
                    StatsReferrerGroup statGroup = new StatsReferrerGroup(mBlogId, date, group);
                    ContentValues values = StatsReferrerGroupsTable.getContentValues(statGroup);
                    ContentProviderOperation insert_group_op = ContentProviderOperation.newInsert(StatsContentProvider.STATS_REFERRER_GROUP_URI).withValues(values).build();
                    operations.add(insert_group_op);

                    // insert children, only if there is more than one entry
                    JSONArray referrers = group.getJSONArray("results");
                    int childCount = Math.min(referrers.length(), StatsActivity.STATS_CHILD_MAX_ITEMS);
                    if (childCount > 1) {
                        for (int j = 0; j < childCount; j++) {
                            StatsReferrer stat = new StatsReferrer(mBlogId, date, statGroup.getGroupId(), referrers.getJSONArray(j));
                            ContentValues v = StatsReferrersTable.getContentValues(stat);
                            ContentProviderOperation insert_child_op = ContentProviderOperation.newInsert(StatsContentProvider.STATS_REFERRERS_URI).withValues(v).build();
                            operations.add(insert_child_op);
                        }
                    }
                }

                getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                getContentResolver().notifyChange(StatsContentProvider.STATS_REFERRER_GROUP_URI, null);
                getContentResolver().notifyChange(StatsContentProvider.STATS_REFERRERS_URI, null);

            } catch (JSONException e) {
                AppLog.e(AppLog.T.STATS, e);
            } catch (RemoteException e) {
                AppLog.e(AppLog.T.STATS, e);
            } catch (OperationApplicationException e) {
                AppLog.e(AppLog.T.STATS, e);
            }
            
            notifyResponseReceived();
        }
    };
    
    private class ViewsByCountryTask implements RestRequest.Listener {

        private String date;

        ViewsByCountryTask(String date){
            this.date = date;
        }

        @Override
        public void onResponse(final JSONObject response) {
            numberOfReturnedCall++;
            AppLog.d(T.STATS, "ViewsByCountry Call " + date + " responded");
            if (response == null || !response.has("country")) {
                notifyResponseReceived();
                return;
            }

            try {
                JSONArray results = response.getJSONArray("country-views");
                int count = Math.min(results.length(), StatsActivity.STATS_GROUP_MAX_ITEMS);
                String date = response.getString("date");
                long dateMs = StatUtils.toMs(date);
                ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

                if (count > 0) {
                    // delete data with the same date, and data older than two days ago (keep yesterday's data)
                    ContentProviderOperation delete_op = ContentProviderOperation.newDelete(StatsContentProvider.STATS_GEOVIEWS_URI)
                            .withSelection("blogId=? AND (date=? OR date<=?)", new String[]{mBlogId, dateMs + "", (dateMs - TWO_DAYS) + ""}).build();
                    operations.add(delete_op);
                }

                for (int i = 0; i < count; i++ ) {
                    JSONObject result = results.getJSONObject(i);
                    StatsGeoview stat = new StatsGeoview(mBlogId, result);
                    ContentValues values = StatsGeoviewsTable.getContentValues(stat);
                    ContentProviderOperation op = ContentProviderOperation.newInsert(StatsContentProvider.STATS_GEOVIEWS_URI).withValues(values).build();
                    operations.add(op);
                }

                getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                getContentResolver().notifyChange(StatsContentProvider.STATS_GEOVIEWS_URI, null);

            } catch (JSONException e) {
                AppLog.e(AppLog.T.STATS, e);
            } catch (RemoteException e) {
                AppLog.e(AppLog.T.STATS, e);
            } catch (OperationApplicationException e) {
                AppLog.e(AppLog.T.STATS, e);
            }
            
            notifyResponseReceived();
        }
    };
    
    private class TopPostAndPageRestListener implements RestRequest.Listener {

        private String date;

        TopPostAndPageRestListener(String date){
            this.date = date;
        }

        @Override
        public void onResponse(final JSONObject response) {
            AppLog.d(T.STATS, "TopPostsAndPages Call " + date + " responded");
            numberOfReturnedCall++;
            if (response == null || !response.has("top-posts")) {
                notifyResponseReceived();
                return;
            }

            try {
                JSONArray results = response.getJSONArray("top-posts");
                int count = Math.min(results.length(), StatsActivity.STATS_GROUP_MAX_ITEMS);

                String date = response.getString("date");
                long dateMs = StatUtils.toMs(date);

                ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
                // delete data with the same date, and data older than two days ago (keep yesterday's data)
                ContentProviderOperation delete_op = ContentProviderOperation.newDelete(StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI)
                        .withSelection("blogId=? AND (date=? OR date<=?)", new String[] { mBlogId, dateMs + "", (dateMs - TWO_DAYS) + "" }).build();
                operations.add(delete_op);

                for (int i = 0; i < count; i++ ) {
                    JSONObject result = results.getJSONObject(i);
                    StatsTopPostsAndPages stat = new StatsTopPostsAndPages(mBlogId, result);
                    ContentValues values = StatsTopPostsAndPagesTable.getContentValues(stat);
                    ContentProviderOperation op = ContentProviderOperation.newInsert(StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI).withValues(values).build();
                    operations.add(op);
                }

                getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                getContentResolver().notifyChange(StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI, null);

            } catch (JSONException e) {
                AppLog.e(AppLog.T.STATS, e);
            } catch (RemoteException e) {
                AppLog.e(AppLog.T.STATS, e);
            } catch (OperationApplicationException e) {
                AppLog.e(AppLog.T.STATS, e);
            }
            
            notifyResponseReceived();
        }
    };
    
    
    private String getBarChartPath(StatsBarChartUnit mBarChartUnit, int quantity) {
        String path = String.format("sites/%s/stats/visits", mBlogId);
        String unit = mBarChartUnit.name().toLowerCase(Locale.ENGLISH);
        path += String.format("?unit=%s", unit);
        if (quantity > 0) {
            path += String.format("&quantity=%d", quantity);
        }
        return path;
    }
    
    private class BarChartRestListener implements RestRequest.Listener {

        private StatsBarChartUnit mBarChartUnit;
        BarChartRestListener(StatsBarChartUnit barChartUnit){
            this.mBarChartUnit = barChartUnit;
        }

        @Override
        public void onResponse(final JSONObject response) {
            numberOfReturnedCall++;
            AppLog.d(T.STATS, "BarChartRest Call " + mBarChartUnit.name() + " responded");
            if (response == null || !response.has("data")) {
                notifyResponseReceived();
                return;
            }

            Uri uri = StatsContentProvider.STATS_BAR_CHART_DATA_URI;
            try {
                JSONArray results = response.getJSONArray("data");

                int count = results.length();

                ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

                // delete old stats and insert new ones
                if (count > 0) {
                    ContentProviderOperation op = ContentProviderOperation.newDelete(uri).withSelection("blogId=? AND unit=?", new String[] { mBlogId, mBarChartUnit.name() }).build();
                    operations.add(op);
                }

                for (int i = 0; i < count; i++ ) {
                    JSONArray result = results.getJSONArray(i);
                    StatsBarChartData stat = new StatsBarChartData(mBlogId, mBarChartUnit, result);
                    ContentValues values = StatsBarChartDataTable.getContentValues(stat);

                    if (values != null && uri != null) {
                        ContentProviderOperation op = ContentProviderOperation.newInsert(uri).withValues(values).build();
                        operations.add(op);
                    }
                }

                getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                getContentResolver().notifyChange(uri, null);
            } catch (JSONException e) {
                AppLog.e(AppLog.T.STATS, e);
            } catch (RemoteException e) {
                AppLog.e(AppLog.T.STATS, e);
            } catch (OperationApplicationException e) {
                AppLog.e(AppLog.T.STATS, e);
            }
            
            notifyResponseReceived();
        }
    };
    
    RestRequest.Listener statsSummaryRestListener = new RestRequest.Listener() {
        @Override
        public void onResponse(final JSONObject jsonObject) {
            AppLog.d(T.STATS, "Stats Summary Call responded");
            numberOfReturnedCall++;
            new Thread() {
                @Override
                public void run() {
                    try{
                        if (jsonObject == null)
                            return;

                        // save summary, then send broadcast that they've changed
                        StatUtils.saveSummary(mBlogId, jsonObject);
                        StatsSummary stats = StatUtils.getSummary(mBlogId);
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
            }.start();
        }
    };
    
    RestRequest.ErrorListener restErrListener = new RestRequest.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            numberOfReturnedCall++;
            if (volleyError != null)
                AppLog.e(T.STATS, "Error while reading Stats - " + volleyError.getMessage(), volleyError);
            notifyResponseReceived();
        }
    };
    
    protected boolean isDone() {
        return numberOfReturnedCall == NUMBER_OF_NETWORK_CALLS;
    }
    
    private void waitForResponse() {
        synchronized (mSyncObject) {
            try {
                mSyncObject.wait();
            } catch (InterruptedException e) {
                AppLog.w(T.STATS, " interrupted");
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
     * broadcast that the update has started/ended - used by StatsActivity to animate refresh
     * icon while update is in progress
     */
    private void broadcastUpdate(boolean isUpdating) {
        Intent intent = new Intent()
                .setAction(ACTION_STATS_UPDATING)
                .putExtra(EXTRA_IS_UPDATING, isUpdating);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
