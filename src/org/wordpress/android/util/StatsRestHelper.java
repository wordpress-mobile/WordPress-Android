package org.wordpress.android.util;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest.ErrorListener;
import com.wordpress.rest.RestRequest.Listener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.StatsBarChartDataTable;
import org.wordpress.android.datasets.StatsClickGroupsTable;
import org.wordpress.android.datasets.StatsClicksTable;
import org.wordpress.android.datasets.StatsGeoviewsTable;
import org.wordpress.android.datasets.StatsMostCommentedTable;
import org.wordpress.android.datasets.StatsReferrerGroupsTable;
import org.wordpress.android.datasets.StatsReferrersTable;
import org.wordpress.android.datasets.StatsSearchEngineTermsTable;
import org.wordpress.android.datasets.StatsTagsAndCategoriesTable;
import org.wordpress.android.datasets.StatsTopAuthorsTable;
import org.wordpress.android.datasets.StatsTopCommentersTable;
import org.wordpress.android.datasets.StatsTopPostsAndPagesTable;
import org.wordpress.android.datasets.StatsVideosTable;
import org.wordpress.android.models.StatsBarChartData;
import org.wordpress.android.models.StatsClick;
import org.wordpress.android.models.StatsClickGroup;
import org.wordpress.android.models.StatsGeoview;
import org.wordpress.android.models.StatsMostCommented;
import org.wordpress.android.models.StatsReferrer;
import org.wordpress.android.models.StatsReferrerGroup;
import org.wordpress.android.models.StatsSearchEngineTerm;
import org.wordpress.android.models.StatsSummary;
import org.wordpress.android.models.StatsTagsandCategories;
import org.wordpress.android.models.StatsTopAuthor;
import org.wordpress.android.models.StatsTopCommenter;
import org.wordpress.android.models.StatsTopPostsAndPages;
import org.wordpress.android.models.StatsVideo;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.stats.StatsBarChartUnit;
import org.wordpress.android.ui.stats.StatsViewType;
import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A utility class to help with parsing json from the stats api.
 * Deletes and inserts are done on the database in batches, by adding each operation into a list of {@link ContentProviderOperation}s.
 * Once ready, a {@link ContentResolver} is used to apply the batch with the {@link StatsContentProvider#AUTHORITY} as the authority.
 * The relevant URIs are then notified once the operation is complete.  
 */
public class StatsRestHelper {

    private static final String TAG = "WordPress";
    private static long TWO_DAYS = 2 * 24 * 60 * 60 * 1000;

    // A map to keep track of the number of pending calls for each viewtype.
    // A counter for each type is incremented before a call is executed. 
    // When the result comes back, regardless of success or failure, the map should be decremented 
    private static Map<StatsViewType, Integer> sRefreshMap;
    
    // Listen to this intent for updates on the rest call
    public static final String REFRESH_VIEW_TYPE = "REFRESH_VIEW_TYPE";
    public static final String REFRESH_VIEW_TYPE_STARTED = "REFRESH_VIEW_TYPE_STARTED";
    public static final String REFRESH_VIEW_TYPE_ORDINAL = "REFRESH_VIEW_TYPE_ORDINAL";
    
    public static void getStats(StatsViewType type, String blogId) {
        
        if (sRefreshMap == null) {
            initRefreshMap();
        }
        
        String today = StatUtils.getCurrentDate();
        String yesterday = StatUtils.getYesterdaysDate();
        
        switch (type) {
            case CLICKS:
                // update the refresh map with the number of calls being executed 
                updateRefreshMap(type, 2);
                getStatsClicks(blogId, today);
                getStatsClicks(blogId, yesterday);
                break;
            case COMMENTS:
                updateRefreshMap(type, 1);
                getStatsComments(blogId);
                break;
            case REFERRERS:
                updateRefreshMap(type, 2);
                getStatsReferrers(blogId, today);
                getStatsReferrers(blogId, yesterday);
                break;
            case SEARCH_ENGINE_TERMS:
                updateRefreshMap(type, 2);
                getStatsSearchEngineTerms(blogId, today);
                getStatsSearchEngineTerms(blogId, yesterday);
                break;
            case TAGS_AND_CATEGORIES:
                updateRefreshMap(type, 1);
                getStatsTagsAndCategories(blogId);
                break;
            case TOP_AUTHORS:
                updateRefreshMap(type, 1);
                getStatsTopAuthors(blogId);
                break;
            case TOP_POSTS_AND_PAGES:
                updateRefreshMap(type, 2);
                getStatsTopPostsAndPages(blogId, today);
                getStatsTopPostsAndPages(blogId, yesterday);
                break;
            case TOTALS_FOLLOWERS_AND_SHARES:
                updateRefreshMap(type, 1);
                getStatsTotalsFollowersAndShares(blogId);
                break;
            case VIDEO_PLAYS:
                updateRefreshMap(type, 1);
                getStatsVideoPlays(blogId, today);
                break;
            case VIEWS_BY_COUNTRY:
                updateRefreshMap(type, 2);
                getStatsViewsByCountry(blogId, today);
                getStatsViewsByCountry(blogId, yesterday);
                break;
            case VISITORS_AND_VIEWS:
                updateRefreshMap(type, 3);
                getStatsVisitorsAndViews(blogId); // this has bar chart data for days
                getStatsBarChart(blogId, StatsBarChartUnit.WEEK);
                getStatsBarChart(blogId, StatsBarChartUnit.MONTH);
                break;
            
        }
    }

    private static void initRefreshMap() {
        sRefreshMap = new HashMap<StatsViewType, Integer>(StatsViewType.values().length);
        for (int i = 0; i < StatsViewType.values().length; i++) {
            sRefreshMap.put(StatsViewType.values()[i], 0);
        }
    }

    private static void updateRefreshMap(StatsViewType type, int i) {
        int count = sRefreshMap.get(type) + i;
        sRefreshMap.put(type, count);
        
        int total = 0;
        for (StatsViewType viewtype : sRefreshMap.keySet()) {
            total += sRefreshMap.get(viewtype);
        }
        
        Intent intent = new Intent(REFRESH_VIEW_TYPE);
        
        if (total == 0) {
            intent.putExtra(REFRESH_VIEW_TYPE_STARTED, false);
        } else {
            intent.putExtra(REFRESH_VIEW_TYPE_STARTED, count > 0);
            intent.putExtra(REFRESH_VIEW_TYPE_ORDINAL, type.ordinal());    
        }

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(WordPress.getContext());
        lbm.sendBroadcast(intent);
    }

    private static void getStatsClicks(final String blogId, final String date) {
        WordPress.restClient.getStatsClicks(blogId, date, 
                new Listener() {
                    
                    @Override
                    public void onResponse(JSONObject response) {
                        new ParseClicksTask().execute(blogId, response);
                    }
                }, 
                new ErrorListener() {
                    
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(T.STATS, "Stats: Failed to fetch clicks");
                        updateRefreshMap(StatsViewType.CLICKS, -1);
                    }
                });
    }

    private static class ParseClicksTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            String blogId = (String) params[0];
            JSONObject response = (JSONObject) params[1];
            
            Context context = WordPress.getContext();
            
            if (response != null) {
                try {
                    String date = response.getString("date");
                    long dateMs = StatUtils.toMs(date);

                    ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

                    // delete data with the same date, and data older than two days ago (keep yesterday's data)
                    ContentProviderOperation delete_group = ContentProviderOperation.newDelete(StatsContentProvider.STATS_CLICK_GROUP_URI).withSelection("blogId=? AND (date=? OR date<=?)", 
                            new String[] { blogId, dateMs + "", (dateMs - TWO_DAYS) + "" }).build();
                    ContentProviderOperation delete_child = ContentProviderOperation.newDelete(StatsContentProvider.STATS_CLICKS_URI).withSelection("blogId=? AND (date=? OR date<=?)", 
                            new String[] { blogId, dateMs + "", (dateMs - TWO_DAYS) + "" }).build();
                    
                    operations.add(delete_group);
                    operations.add(delete_child);
                    
                    
                    JSONArray groups = response.getJSONArray("clicks");
                    int groupsCount = groups.length();

                    // insert groups
                    for (int i = 0; i < groupsCount; i++ ) {
                        JSONObject group = groups.getJSONObject(i);
                        StatsClickGroup statGroup = new StatsClickGroup(blogId, date, group);
                        ContentValues values = StatsClickGroupsTable.getContentValues(statGroup);
                        
                        ContentProviderOperation insert_group = ContentProviderOperation.newInsert(StatsContentProvider.STATS_CLICK_GROUP_URI).withValues(values).build();
                        operations.add(insert_group);
                        
                        // insert children, only if there is more than one entry
                        JSONArray clicks = group.getJSONArray("results");
                        int count = clicks.length();
                        if (count > 1) {

                            for (int j = 0; j < count; j++) {
                                StatsClick stat = new StatsClick(blogId, date, statGroup.getGroupId(), clicks.getJSONArray(j));
                                ContentValues v = StatsClicksTable.getContentValues(stat);
                                ContentProviderOperation insert_child = ContentProviderOperation.newInsert(StatsContentProvider.STATS_CLICKS_URI).withValues(v).build();
                                operations.add(insert_child);
                            }
                        }
                        
                    }
                    
                    ContentResolver resolver = context.getContentResolver();
                    resolver.applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                    resolver.notifyChange(StatsContentProvider.STATS_CLICK_GROUP_URI, null);
                    resolver.notifyChange(StatsContentProvider.STATS_CLICKS_URI, null);
                    
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (OperationApplicationException e) {
                    e.printStackTrace();
                }
                
            }

            updateRefreshMap(StatsViewType.CLICKS, -1);
            return null;
        }        
    }
    
    private static void getStatsComments(final String blogId) {
    
        WordPress.restClient.getStatsMostCommented(blogId, 
                new Listener() {
                    
                    @Override
                    public void onResponse(JSONObject response) {
                        new ParseMostCommentedTask().execute(blogId, response);
                    }
                }, 
                new ErrorListener() {
                    
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        updateRefreshMap(StatsViewType.COMMENTS, -1);
                        AppLog.e(T.STATS, "Stats: Failed to fetch most commented");
                    }
                });
        
        WordPress.restClient.getStatsTopCommenters(blogId, 
                new Listener() {
                    
                    @Override
                    public void onResponse(JSONObject response) {
                        new ParseTopCommentersTask().execute(blogId, response);
                    }
                }, 
                new ErrorListener() {
                    
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        updateRefreshMap(StatsViewType.COMMENTS, -1);
                        AppLog.e(T.STATS, "Stats: Failed to fetch top commenters");
                    }
                });
        
    }
    
    private static class ParseMostCommentedTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            String blogId = (String) params[0];
            JSONObject response = (JSONObject) params[1];
            
            Context context = WordPress.getContext();
            
            if (response != null && response.has("result")) {
                try {
                    JSONArray results = response.getJSONArray("result");
                    int count = results.length();

                    ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
                    
                    if (count > 0) {
                        ContentProviderOperation op = ContentProviderOperation.newDelete(StatsContentProvider.STATS_MOST_COMMENTED_URI).withSelection("blogId=?", new String[] { blogId }).build();
                        operations.add(op);
                    }
                    
                    for (int i = 0; i < count; i++ ) {
                        JSONObject result = results.getJSONObject(i);
                        StatsMostCommented stat = new StatsMostCommented(blogId, result);
                        ContentValues values = StatsMostCommentedTable.getContentValues(stat);
                        ContentProviderOperation op = ContentProviderOperation.newInsert(StatsContentProvider.STATS_MOST_COMMENTED_URI).withValues(values).build();
                        operations.add(op);
                    }

                    ContentResolver resolver = context.getContentResolver();
                    resolver.applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                    resolver.notifyChange(StatsContentProvider.STATS_MOST_COMMENTED_URI, null);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (OperationApplicationException e) {
                    e.printStackTrace();
                }
                
            }

            updateRefreshMap(StatsViewType.COMMENTS, -1);
            return null;
        }
    }
    
    private static class ParseTopCommentersTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            String blogId = (String) params[0];
            JSONObject response = (JSONObject) params[1];

            Context context = WordPress.getContext();
            
            if (response != null && response.has("result")) {
                try {
                    JSONArray results = response.getJSONArray("result");
                    int count = results.length();

                    ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
                    
                    if (count > 0) {
                        ContentProviderOperation op = ContentProviderOperation.newDelete(StatsContentProvider.STATS_TOP_COMMENTERS_URI).withSelection("blogId=?", new String[] { blogId }).build();
                        operations.add(op);
                    }
                    
                    for (int i = 0; i < count; i++ ) {
                        JSONObject result = results.getJSONObject(i);
                        StatsTopCommenter stat = new StatsTopCommenter(blogId, result);
                        ContentValues values = StatsTopCommentersTable.getContentValues(stat);
                        ContentProviderOperation op = ContentProviderOperation.newInsert(StatsContentProvider.STATS_TOP_COMMENTERS_URI).withValues(values).build();
                        operations.add(op);
                    }

                    ContentResolver resolver = context.getContentResolver();
                    resolver.applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                    resolver.notifyChange(StatsContentProvider.STATS_TOP_COMMENTERS_URI, null);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (OperationApplicationException e) {
                    e.printStackTrace();
                }
                
            }

            updateRefreshMap(StatsViewType.COMMENTS, -1);
            return null;
        }
        
    }
    
    private static void getStatsReferrers(final String blogId, final String date) {
        WordPress.restClient.getStatsReferrers(blogId, date,
                new Listener() {
                    
                    @Override
                    public void onResponse(JSONObject response) {
                        new ParseReferrersTask().execute(blogId, response);
                    }
                }, 
                new ErrorListener() {
                    
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        updateRefreshMap(StatsViewType.REFERRERS, -1);
                        AppLog.e(T.STATS, "Stats: Failed to fetch referrers");
                    }
                });
    }
    
    private static class ParseReferrersTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            String blogId = (String) params[0];
            JSONObject response = (JSONObject) params[1];
            
            Context context = WordPress.getContext();
            
            if (response != null) {
                try {
                    String date = response.getString("date");
                    long dateMs = StatUtils.toMs(date);

                    ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
                    
                    // delete data with the same date, and data older than two days ago (keep yesterday's data)
                    ContentProviderOperation delete_group_op = ContentProviderOperation.newDelete(StatsContentProvider.STATS_REFERRER_GROUP_URI)
                            .withSelection("blogId=? AND (date=? OR date<=?)", new String[] { blogId, dateMs + "", (dateMs - TWO_DAYS) + "" }).build();
                    operations.add(delete_group_op);
                    
                    ContentProviderOperation delete_op = ContentProviderOperation.newDelete(StatsContentProvider.STATS_REFERRERS_URI)
                            .withSelection("blogId=? AND (date=? OR date<=?)", new String[] { blogId, dateMs + "", (dateMs - TWO_DAYS) + "" }).build();
                    operations.add(delete_op);

                    
                    JSONArray groups = response.getJSONArray("referrers");
                    int groupsCount = groups.length();
                    
                    // insert groups
                    for (int i = 0; i < groupsCount; i++ ) {
                        JSONObject group = groups.getJSONObject(i);
                        StatsReferrerGroup statGroup = new StatsReferrerGroup(blogId, date, group);
                        ContentValues values = StatsReferrerGroupsTable.getContentValues(statGroup);
                        ContentProviderOperation insert_group_op = ContentProviderOperation.newInsert(StatsContentProvider.STATS_REFERRER_GROUP_URI).withValues(values).build();
                        operations.add(insert_group_op);
                        
                        // insert children, only if there is more than one entry
                        JSONArray referrers = group.getJSONArray("results");
                        int count = referrers.length();
                        if (count > 1) {
                            
                            for (int j = 0; j < count; j++) {
                                StatsReferrer stat = new StatsReferrer(blogId, date, statGroup.getGroupId(), referrers.getJSONArray(j));
                                ContentValues v = StatsReferrersTable.getContentValues(stat);
                                ContentProviderOperation insert_child_op = ContentProviderOperation.newInsert(StatsContentProvider.STATS_REFERRERS_URI).withValues(v).build();
                                operations.add(insert_child_op);
                            }
                        }
                        
                    }

                    ContentResolver resolver = context.getContentResolver();
                    resolver.applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                    resolver.notifyChange(StatsContentProvider.STATS_REFERRER_GROUP_URI, null);
                    resolver.notifyChange(StatsContentProvider.STATS_REFERRERS_URI, null);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (OperationApplicationException e) {
                    e.printStackTrace();
                }
                
            }

            updateRefreshMap(StatsViewType.REFERRERS, -1);
            return null;
        }        
    }

    private static void getStatsSearchEngineTerms(final String blogId, final String date) {

        WordPress.restClient.getStatsSearchEngineTerms(blogId, date, 
                new Listener() {
                    
                    @Override
                    public void onResponse(JSONObject response) {
                        new ParseSearchEngineTermsTask().execute(blogId, response);
                    }
                }, 
                new ErrorListener() {
                    
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        updateRefreshMap(StatsViewType.SEARCH_ENGINE_TERMS, -1);
                        AppLog.e(T.STATS, "Stats: Failed to fetch search engine terms");
                    }
                });
        
    }

    private static class ParseSearchEngineTermsTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            String blogId = (String) params[0];
            JSONObject response = (JSONObject) params[1];
            
            Context context = WordPress.getContext();
            
            if (response != null) {
                try {
                    String date = response.getString("date");
                    long dateMs = StatUtils.toMs(date);
                    
                    ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
                    
                    ContentProviderOperation delete_op = ContentProviderOperation.newDelete(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI).withSelection("blogId=? AND (date=? OR date<=?)", 
                            new String[] { blogId, dateMs + "", (dateMs - TWO_DAYS) + "" }).build();

                    operations.add(delete_op);
                    
                    JSONArray results = response.getJSONArray("search-terms");

                    int count = results.length();
                    for (int i = 0; i < count; i++ ) {
                        JSONArray result = results.getJSONArray(i);
                        StatsSearchEngineTerm stat = new StatsSearchEngineTerm(blogId, date, result);
                        ContentValues values = StatsSearchEngineTermsTable.getContentValues(stat);
                        context.getContentResolver().insert(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI, values);
                        
                        ContentProviderOperation insert_op = ContentProviderOperation.newInsert(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI).withValues(values).build();
                        operations.add(insert_op);
                    }
                    
                    ContentResolver resolver = context.getContentResolver();
                    resolver.applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                    resolver.notifyChange(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI, null);
                    
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (OperationApplicationException e) {
                    e.printStackTrace();
                }
                
            }
  
            updateRefreshMap(StatsViewType.SEARCH_ENGINE_TERMS, -1);
            return null;
        }        
    }

    private static void getStatsTagsAndCategories(final String blogId) {
        
        WordPress.restClient.getStatsTagsAndCategories(blogId, 
                new Listener() {
                    
                    @Override
                    public void onResponse(JSONObject response) {
                        new ParseTagsAndCategoriesTask().execute(blogId, response);
                    }
                }, 
                new ErrorListener() {
                    
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        updateRefreshMap(StatsViewType.TAGS_AND_CATEGORIES, -1);
                        AppLog.e(T.STATS, "Stats: Failed to fetch tags and categories");
                    }
                });
    }

    private static class ParseTagsAndCategoriesTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            String blogId = (String) params[0];
            JSONObject response = (JSONObject) params[1];
            
            Context context = WordPress.getContext();
            
            if (response != null && response.has("result")) {
                try {
                    JSONArray results = response.getJSONArray("result");
                    int count = results.length();
                    
                    ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
                    
                    if (count > 0) {
                        ContentProviderOperation op = ContentProviderOperation.newDelete(StatsContentProvider.STATS_TAGS_AND_CATEGORIES_URI).withSelection("blogId=?", new String[] { blogId }).build();
                        operations.add(op);
                    }
                    
                    for (int i = 0; i < count; i++ ) {
                        JSONObject result = results.getJSONObject(i);
                        StatsTagsandCategories stat = new StatsTagsandCategories(blogId, result);
                        ContentValues values = StatsTagsAndCategoriesTable.getContentValues(stat);
                        ContentProviderOperation op = ContentProviderOperation.newInsert(StatsContentProvider.STATS_TAGS_AND_CATEGORIES_URI).withValues(values).build();
                        operations.add(op);
                    }
                    
                    ContentResolver resolver = context.getContentResolver();
                    resolver.applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                    resolver.notifyChange(StatsContentProvider.STATS_TAGS_AND_CATEGORIES_URI, null);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (OperationApplicationException e) {
                    e.printStackTrace();
                }
                
            }

            updateRefreshMap(StatsViewType.TAGS_AND_CATEGORIES, -1);
            return null;
            
        }        
    }

    private static void getStatsTopAuthors(final String blogId) {
        
        WordPress.restClient.getStatsTopAuthors(blogId, 
                new Listener() {
                    
                    @Override
                    public void onResponse(JSONObject response) {
                        new ParseTopAuthorsTask().execute(blogId, response);
                    }
                }, 
                new ErrorListener() {
                    
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        updateRefreshMap(StatsViewType.TOP_AUTHORS, -1);
                        AppLog.e(T.STATS, "Stats: Failed to fetch top authors");
                    }
                });
    }

    private static class ParseTopAuthorsTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            String blogId = (String) params[0];
            JSONObject response = (JSONObject) params[1];
            
            Context context = WordPress.getContext();
            
            if (response != null && response.has("result")) {
                try {
                    JSONArray results = response.getJSONArray("result");
                    int count = results.length();
                    
                    ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
                    
                    if (count > 0) {
                        ContentProviderOperation op = ContentProviderOperation.newDelete(StatsContentProvider.STATS_TOP_AUTHORS_URI).withSelection("blogId=?", new String[] { blogId }).build();
                        operations.add(op);
                    }
                    
                    for (int i = 0; i < count; i++ ) {
                        JSONObject result = results.getJSONObject(i);
                        StatsTopAuthor stat = new StatsTopAuthor(blogId, result);
                        ContentValues values = StatsTopAuthorsTable.getContentValues(stat);
                        ContentProviderOperation op = ContentProviderOperation.newInsert(StatsContentProvider.STATS_TOP_AUTHORS_URI).withValues(values).build();
                        operations.add(op);
                    }
                    
                    ContentResolver resolver = context.getContentResolver();
                    resolver.applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                    resolver.notifyChange(StatsContentProvider.STATS_TOP_AUTHORS_URI, null);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (OperationApplicationException e) {
                    e.printStackTrace();
                }
                
            }

            updateRefreshMap(StatsViewType.TOP_AUTHORS, -1);
            return null;

        }        
    }

    private static void getStatsTopPostsAndPages(final String blogId, final String date) {

        WordPress.restClient.getStatsTopPosts(blogId, date,
                new Listener() {
                    
                    @Override
                    public void onResponse(JSONObject response) {
                        new ParseTopPostsAndPagesTask().execute(blogId, response);
                    }
                },
                new ErrorListener() {
                    
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        updateRefreshMap(StatsViewType.TOP_POSTS_AND_PAGES, -1);
                        AppLog.e(T.STATS, "Stats: Failed to fetch top posts and pages");
                    }
                });
    }

    private static class ParseTopPostsAndPagesTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            String blogId = (String) params[0];
            JSONObject response = (JSONObject) params[1];
            
            Context context = WordPress.getContext();
            
            if (response != null && response.has("top-posts")) {
                try {
                    JSONArray results = response.getJSONArray("top-posts");
                    int count = results.length();

                    String date = response.getString("date");
                    long dateMs = StatUtils.toMs(date);

                    ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
                    // delete data with the same date, and data older than two days ago (keep yesterday's data)
                    ContentProviderOperation delete_op = ContentProviderOperation.newDelete(StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI)
                            .withSelection("blogId=? AND (date=? OR date<=?)", new String[] { blogId, dateMs + "", (dateMs - TWO_DAYS) + "" }).build();
                    operations.add(delete_op);
                    
                    for (int i = 0; i < count; i++ ) {
                        JSONObject result = results.getJSONObject(i);
                        StatsTopPostsAndPages stat = new StatsTopPostsAndPages(blogId, result);
                        ContentValues values = StatsTopPostsAndPagesTable.getContentValues(stat);
                        ContentProviderOperation op = ContentProviderOperation.newInsert(StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI).withValues(values).build();
                        operations.add(op);
                    }
                    
                    ContentResolver resolver = context.getContentResolver();
                    resolver.applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                    resolver.notifyChange(StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI, null);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (OperationApplicationException e) {
                    e.printStackTrace();
                }
                
            }

            updateRefreshMap(StatsViewType.TOP_POSTS_AND_PAGES, -1);
            return null;
        }        
    }

    private static void getStatsVideoPlays(final String blogId, String date) {
        WordPress.restClient.getStatsVideoPlays(blogId, 
                new Listener() {
                    
                    @Override
                    public void onResponse(JSONObject response) {
                        new ParseVideoPlaysTask().execute(blogId, response);
                    }
                }, 
                new ErrorListener() {
                    
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        updateRefreshMap(StatsViewType.VIDEO_PLAYS, -1);
                        AppLog.e(T.STATS, "Stats: Failed to fetch video plays");
                    }
                });
    }
    
    private static class ParseVideoPlaysTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            String blogId = (String) params[0];
            JSONObject response = (JSONObject) params[1];
            
            Context context = WordPress.getContext();
            
            if (response != null && response.has("result")) {
                try {
                    JSONArray results = response.getJSONArray("result");
                    int count = results.length();

                    ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
                    
                    if (count > 0) {
                        ContentProviderOperation op = ContentProviderOperation.newDelete(StatsContentProvider.STATS_VIDEOS_URI).withSelection("blogId=?", new String[] { blogId }).build();
                        operations.add(op);
                    }
                    
                    for (int i = 0; i < count; i++ ) {
                        JSONObject result = results.getJSONObject(i);
                        StatsVideo stat = new StatsVideo(blogId, result);
                        ContentValues values = StatsVideosTable.getContentValues(stat);
                        ContentProviderOperation op = ContentProviderOperation.newInsert(StatsContentProvider.STATS_VIDEOS_URI).withValues(values).build();
                        operations.add(op);
                    }
                    
                    ContentResolver resolver = context.getContentResolver();
                    resolver.applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                    resolver.notifyChange(StatsContentProvider.STATS_VIDEOS_URI, null);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (OperationApplicationException e) {
                    e.printStackTrace();
                }
                
            }

            updateRefreshMap(StatsViewType.VIDEO_PLAYS, -1);
            return null;
        }        
    }

    private static void getStatsViewsByCountry(final String blogId, String date) {
        WordPress.restClient.getStatsGeoviews(blogId, date,
                new Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        new ParseGeoViewsTask().execute(blogId, response);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        updateRefreshMap(StatsViewType.VIEWS_BY_COUNTRY, -1);
                        AppLog.e(T.STATS, error);
                    }
                }
        );
    }

    private static class ParseGeoViewsTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            String blogId = (String) params[0];
            JSONObject response = (JSONObject) params[1];
            
            Context context = WordPress.getContext();
            
            if (response != null && response.has("country-views")) {
                try {
                    JSONArray results = response.getJSONArray("country-views");
                    int count = results.length();
                    String date = response.getString("date");
                    long dateMs = StatUtils.toMs(date);
                    ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
                    
                    if (count > 0) {
                        // delete data with the same date, and data older than two days ago (keep yesterday's data)
                        ContentProviderOperation delete_op = ContentProviderOperation.newDelete(StatsContentProvider.STATS_GEOVIEWS_URI)
                                .withSelection("blogId=? AND (date=? OR date<=?)", new String[] { blogId, dateMs + "", (dateMs - TWO_DAYS) + "" }).build();
                        operations.add(delete_op);
                    }
                    
                    for (int i = 0; i < count; i++ ) {
                        JSONObject result = results.getJSONObject(i);
                        StatsGeoview stat = new StatsGeoview(blogId, result);
                        ContentValues values = StatsGeoviewsTable.getContentValues(stat);
                        ContentProviderOperation op = ContentProviderOperation.newInsert(StatsContentProvider.STATS_GEOVIEWS_URI).withValues(values).build();
                        operations.add(op);
                    }
                    
                    ContentResolver resolver = context.getContentResolver();
                    resolver.applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                    resolver.notifyChange(StatsContentProvider.STATS_GEOVIEWS_URI, null);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (OperationApplicationException e) {
                    e.printStackTrace();
                }
                
            }

            updateRefreshMap(StatsViewType.VIEWS_BY_COUNTRY, -1);
            return null;

        }        
    }
    
    public static void getStatsTotalsFollowersAndShares(final String blogId) {
        getStatsSummary(blogId, new StatsSummaryInterface() {
            
            @Override
            public void onSuccess() {
                updateRefreshMap(StatsViewType.TOTALS_FOLLOWERS_AND_SHARES, -1);
            }
            
            @Override
            public void onFailure(VolleyError error) {
                updateRefreshMap(StatsViewType.TOTALS_FOLLOWERS_AND_SHARES, -1);
            }
        });
    }
    
    public static void getStatsVisitorsAndViews(final String blogId) {
        getStatsSummary(blogId, new StatsSummaryInterface() {
            
            @Override
            public void onSuccess() {
                updateRefreshMap(StatsViewType.VISITORS_AND_VIEWS, -1);
            }
            
            @Override
            public void onFailure(VolleyError error) {
                updateRefreshMap(StatsViewType.VISITORS_AND_VIEWS, -1);
            }
        });
    }
    
    public static void getStatsSummary(final String blogId) {
        getStatsSummary(blogId, null);
    }
    
    public static interface StatsSummaryInterface {
        void onSuccess();
        void onFailure(VolleyError error);
    }
    
    public static void getStatsSummary(final String blogId, final StatsSummaryInterface callback) {
        WordPress.restClient.getStatsSummary(blogId, 
                new Listener() {
                    
                    @Override
                    public void onResponse(final JSONObject response) {
                        new AsyncTask<Void, Void, StatsSummary>() {

                            @Override
                            protected StatsSummary doInBackground(Void... params) {
                                StatUtils.saveSummary(blogId, response);
                                return StatUtils.getSummary(blogId);
                            }
                            
                            protected void onPostExecute(StatsSummary result) {
                                if (callback != null)
                                    callback.onSuccess();
                                StatUtils.broadcastSummaryUpdated(result);
                            };
                            
                        }.execute();
                    }
                }, 
                new ErrorListener() {
                    
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (callback != null)
                            callback.onFailure(error);
                        AppLog.e(T.STATS, "Stats: Failed to get summary", error);
                    }
                });
    }

    public static void getStatsBarChart(final String blogId, final StatsBarChartUnit barChartUnit) {
        

        Listener listener = new Listener() {
            
            @Override
            public void onResponse(JSONObject response) {
                new ParseBarChartTask().execute(blogId, response, barChartUnit.ordinal());
            }
        }; 
        
        ErrorListener errorListener = new ErrorListener() {
            
            @Override
            public void onErrorResponse(VolleyError error) {
                updateRefreshMap(StatsViewType.VISITORS_AND_VIEWS, -1);
                AppLog.e(T.STATS, "Stats: Failed to get bar chart data");
            }
        };
        
        WordPress.restClient.getStatsBarChartData(blogId, barChartUnit, 30, listener, errorListener);
        
    }
    
    private static class ParseBarChartTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            String blogId = (String) params[0];
            JSONObject response = (JSONObject) params[1];
            StatsBarChartUnit unit = StatsBarChartUnit.values()[(Integer) params[2]];
            
            Context context = WordPress.getContext();
            Uri uri = StatsContentProvider.STATS_BAR_CHART_DATA_URI;
            
            if (response != null && response.has("data")) {
                try {
                    JSONArray results = response.getJSONArray("data");
                    
                    int count = results.length();

                    ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
                    
                    // delete old stats and insert new ones
                    if (count > 0) {
                        ContentProviderOperation op = ContentProviderOperation.newDelete(uri).withSelection("blogId=? AND unit=?", new String[] { blogId, unit.name() }).build();
                        operations.add(op);
                    }

                    for (int i = 0; i < count; i++ ) {
                        JSONArray result = results.getJSONArray(i);
                        StatsBarChartData stat = new StatsBarChartData(blogId, unit, result);
                        ContentValues values = StatsBarChartDataTable.getContentValues(stat);
                        
                        if (values != null && uri != null) {
                            ContentProviderOperation op = ContentProviderOperation.newInsert(uri).withValues(values).build();
                            operations.add(op);
                        }
                    }
                    
                    ContentResolver resolver = context.getContentResolver();
                    resolver.applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                    resolver.notifyChange(uri, null);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (OperationApplicationException e) {
                    e.printStackTrace();
                }
                
            }
            
            updateRefreshMap(StatsViewType.VISITORS_AND_VIEWS, -1);
            return null;
        }        
    }
    
    
}
