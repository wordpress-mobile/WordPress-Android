package org.wordpress.android.util;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest.ErrorListener;
import com.wordpress.rest.RestRequest.Listener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.wordpress.android.WordPress;
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
import org.wordpress.android.ui.stats.StatsGeoviewsFragment;
import org.wordpress.android.ui.stats.StatsViewType;

public class StatsRestHelper {

    private static final String TAG = "WordPress";
    private static long TWO_DAYS = 2 * 24 * 60 * 60 * 1000;

    public static void getStats(StatsViewType type, String blogId) {
        
        String today = StatUtils.getCurrentDate();
        String yesterday = StatUtils.getYesterdaysDate();
        
        switch (type) {
            case CLICKS:
                getStatsClicks(blogId, today);
                getStatsClicks(blogId, yesterday);
                break;
            case COMMENTS:
                getStatsComments(blogId);
                break;
            case REFERRERS:
                getStatsReferrers(blogId, today);
                getStatsReferrers(blogId, yesterday);
                break;
            case SEARCH_ENGINE_TERMS:
                getStatsSearchEngineTerms(blogId, today);
                getStatsSearchEngineTerms(blogId, yesterday);
                break;
            case TAGS_AND_CATEGORIES:
                getStatsTagsAndCategories(blogId);
                break;
            case TOP_AUTHORS:
                getStatsTopAuthors(blogId);
                break;
            case TOP_POSTS_AND_PAGES:
                getStatsTopPostsAndPages(blogId);
                break;
            case TOTALS_FOLLOWERS_AND_SHARES:
                getStatsSummary(blogId);
                break;
            case VIDEO_PLAYS:
                getStatsVideoPlays(blogId, today);
                break;
            case VIEWS_BY_COUNTRY:
                getStatsViewsByCountry(blogId, today);
                break;
            case VISITORS_AND_VIEWS:
                getStatsSummary(blogId);
                break;
            
        }
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
                        Log.e(TAG, "Stats: Failed to fetch clicks");
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
                    
                    // delete data with the same date, and data older than two days ago (keep yesterday's data)
                    context.getContentResolver().delete(StatsContentProvider.STATS_CLICK_GROUP_URI, "blogId=? AND (date=? OR date<=?)", new String[] { blogId, dateMs + "", (dateMs - TWO_DAYS) + "" });
                    context.getContentResolver().delete(StatsContentProvider.STATS_CLICKS_URI, "blogId=? AND (date=? OR date<=?)", new String[] { blogId, dateMs + "", (dateMs - TWO_DAYS) + "" });
                    
                    JSONArray groups = response.getJSONArray("clicks");
                    int groupsCount = groups.length();
                    
                    // insert groups
                    for (int i = 0; i < groupsCount; i++ ) {
                        JSONObject group = groups.getJSONObject(i);
                        StatsClickGroup statGroup = new StatsClickGroup(blogId, date, group);
                        ContentValues values = StatsClickGroupsTable.getContentValues(statGroup);
                        context.getContentResolver().insert(StatsContentProvider.STATS_CLICK_GROUP_URI, values);
                        
                        // insert children, only if there is more than one entry
                        JSONArray clicks = group.getJSONArray("results");
                        int count = clicks.length();
                        if (count > 1) {

                            for (int j = 0; j < count; j++) {
                                StatsClick stat = new StatsClick(blogId, date, statGroup.getGroupId(), clicks.getJSONArray(j));
                                ContentValues v = StatsClicksTable.getContentValues(stat);
                                context.getContentResolver().insert(StatsContentProvider.STATS_CLICKS_URI, v);
                            }
                        }
                        
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                
            }
            
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
                        Log.e(TAG, "Stats: Failed to fetch most commented");
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
                        Log.e(TAG, "Stats: Failed to fetch top commenters");
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
                    
                    if (count > 0)
                        context.getContentResolver().delete(StatsContentProvider.STATS_MOST_COMMENTED_URI, "blogId=?", new String[] { blogId });
                    
                    for (int i = 0; i < count; i++ ) {
                        JSONObject result = results.getJSONObject(i);
                        StatsMostCommented stat = new StatsMostCommented(blogId, result);
                        ContentValues values = StatsMostCommentedTable.getContentValues(stat);
                        context.getContentResolver().insert(StatsContentProvider.STATS_MOST_COMMENTED_URI, values);
                    }
                    
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                
            }
            
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

                    if (count > 0)
                        context.getContentResolver().delete(StatsContentProvider.STATS_TOP_COMMENTERS_URI, "blogId=?", new String[] { blogId });
                    
                    for (int i = 0; i < count; i++ ) {
                        JSONObject result = results.getJSONObject(i);
                        StatsTopCommenter stat = new StatsTopCommenter(blogId, result);
                        ContentValues values = StatsTopCommentersTable.getContentValues(stat);
                        context.getContentResolver().insert(StatsContentProvider.STATS_TOP_COMMENTERS_URI, values);
                    }
                    
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                
            }
            
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
                        Log.e(TAG, "Stats: Failed to fetch referrers");
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
                    
                    // delete data with the same date, and data older than two days ago (keep yesterday's data)
                    context.getContentResolver().delete(StatsContentProvider.STATS_REFERRER_GROUP_URI, "blogId=? AND (date=? OR date<=?)", new String[] { blogId, dateMs + "", (dateMs - TWO_DAYS) + "" });
                    context.getContentResolver().delete(StatsContentProvider.STATS_REFERRERS_URI, "blogId=? AND (date=? OR date<=?)", new String[] { blogId, dateMs + "", (dateMs - TWO_DAYS) + "" });
                    
                    JSONArray groups = response.getJSONArray("referrers");
                    int groupsCount = groups.length();
                    
                    // insert groups
                    for (int i = 0; i < groupsCount; i++ ) {
                        JSONObject group = groups.getJSONObject(i);
                        StatsReferrerGroup statGroup = new StatsReferrerGroup(blogId, date, group);
                        ContentValues values = StatsReferrerGroupsTable.getContentValues(statGroup);
                        context.getContentResolver().insert(StatsContentProvider.STATS_REFERRER_GROUP_URI, values);

                        
                        // insert children, only if there is more than one entry
                        JSONArray referrers = group.getJSONArray("results");
                        int count = referrers.length();
                        if (count > 1) {
                            
                            for (int j = 0; j < count; j++) {
                                StatsReferrer stat = new StatsReferrer(blogId, date, statGroup.getGroupId(), referrers.getJSONArray(j));
                                ContentValues v = StatsReferrersTable.getContentValues(stat);
                                context.getContentResolver().insert(StatsContentProvider.STATS_REFERRERS_URI, v);
                            }
                        }
                        
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                
            }
            
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
                        Log.e(TAG, "Stats: Failed to fetch search engine terms");
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

                    // delete data with the same date, and data older than two days ago (keep yesterday's data)
                    context.getContentResolver().delete(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI, "blogId=? AND (date=? OR date<=?)", new String[] { blogId, dateMs + "", (dateMs - TWO_DAYS) + "" });
                    
                    JSONArray results = response.getJSONArray("search-terms");

                    int count = results.length();
                    for (int i = 0; i < count; i++ ) {
                        JSONArray result = results.getJSONArray(i);
                        StatsSearchEngineTerm stat = new StatsSearchEngineTerm(blogId, date, result);
                        ContentValues values = StatsSearchEngineTermsTable.getContentValues(stat);
                        context.getContentResolver().insert(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI, values);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                
            }

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
                        Log.e(TAG, "Stats: Failed to fetch tags and categories");
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
                    
                    if (count > 0)
                        context.getContentResolver().delete(StatsContentProvider.STATS_TAGS_AND_CATEGORIES_URI, "blogId=?", new String[] { blogId });
                    
                    for (int i = 0; i < count; i++ ) {
                        JSONObject result = results.getJSONObject(i);
                        StatsTagsandCategories stat = new StatsTagsandCategories(blogId, result);
                        ContentValues values = StatsTagsAndCategoriesTable.getContentValues(stat);
                        context.getContentResolver().insert(StatsContentProvider.STATS_TAGS_AND_CATEGORIES_URI, values);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                
            }
            
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
                        Log.e(TAG, "Stats: Failed to fetch top authors");
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
                    
                    if (count > 0)
                        context.getContentResolver().delete(StatsContentProvider.STATS_TOP_AUTHORS_URI, "blogId=?", new String[] { blogId });
                    
                    for (int i = 0; i < count; i++ ) {
                        JSONObject result = results.getJSONObject(i);
                        StatsTopAuthor stat = new StatsTopAuthor(blogId, result);
                        ContentValues values = StatsTopAuthorsTable.getContentValues(stat);
                        context.getContentResolver().insert(StatsContentProvider.STATS_TOP_AUTHORS_URI, values);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                
            }
            
            return null;

        }        
    }

    private static void getStatsTopPostsAndPages(final String blogId) {

        WordPress.restClient.getStatsTopPosts(blogId, 
                new Listener() {
                    
                    @Override
                    public void onResponse(JSONObject response) {
                        new ParseTopPostsAndPagesTask().execute(blogId, response);
                    }
                }, 
                new ErrorListener() {
                    
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Stats: Failed to fetch top posts and pages");
                    }
                });
    }

    private static class ParseTopPostsAndPagesTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            String blogId = (String) params[0];
            JSONObject response = (JSONObject) params[1];
            
            Context context = WordPress.getContext();
            
            if (response != null && response.has("result")) {
                try {
                    JSONArray results = response.getJSONArray("result");
                    int count = results.length();

                    if (count > 0)
                        context.getContentResolver().delete(StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI, "blogId=?", new String[] { blogId });
                    
                    for (int i = 0; i < count; i++ ) {
                        JSONObject result = results.getJSONObject(i);
                        StatsTopPostsAndPages stat = new StatsTopPostsAndPages(blogId, result);
                        ContentValues values = StatsTopPostsAndPagesTable.getContentValues(stat);
                        context.getApplicationContext().getContentResolver().insert(StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI, values);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                
            }

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
                        Log.e(TAG, "Stats: Failed to fetch video plays");
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

                    if (count > 0)
                        context.getContentResolver().delete(StatsContentProvider.STATS_VIDEOS_URI, "blogId=?", new String[] { blogId });
                    
                    for (int i = 0; i < count; i++ ) {
                        JSONObject result = results.getJSONObject(i);
                        StatsVideo stat = new StatsVideo(blogId, result);
                        ContentValues values = StatsVideosTable.getContentValues(stat);
                        context.getContentResolver().insert(StatsContentProvider.STATS_VIDEOS_URI, values);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                
            }

            return null;
        }        
    }


    private static void getStatsViewsByCountry(final String blogId, String date) {
        WordPress.restClient.getStatsGeoviews(blogId, 
                new Listener() {
                    
                    @Override
                    public void onResponse(JSONObject response) {
                        new ParseGeoViewsTask().execute(blogId, response);
                    }
                }, 
                new ErrorListener() {
                    
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("WordPress Stats", StatsGeoviewsFragment.class.getSimpleName() + ": " + error.toString());
                    }
                });
    }

    private static class ParseGeoViewsTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            String blogId = (String) params[0];
            JSONObject response = (JSONObject) params[1];
            
            Context context = WordPress.getContext();
            
            if (response != null && response.has("result")) {
                try {
                    JSONArray results = response.getJSONArray("result");
                    int count = results.length();

                    if (count > 0)
                        context.getContentResolver().delete(StatsContentProvider.STATS_GEOVIEWS_URI, "blogId=?", new String[] { blogId });
                    
                    for (int i = 0; i < count; i++ ) {
                        JSONObject result = results.getJSONObject(i);
                        StatsGeoview stat = new StatsGeoview(blogId, result);
                        ContentValues values = StatsGeoviewsTable.getContentValues(stat);
                        context.getContentResolver().insert(StatsContentProvider.STATS_GEOVIEWS_URI, values);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                
            }
            
            return null;

        }        
    }
    
    public static void getStatsSummary(final String blogId) {
        WordPress.restClient.getStatsSummary(blogId, 
                new Listener() {
                    
                    @Override
                    public void onResponse(JSONObject response) {
                        StatUtils.saveSummary(blogId, response);
                        StatsSummary stats = StatUtils.getSummary(blogId);
                        StatUtils.broadcastSummaryUpdated(stats);
                    }
                }, 
                new ErrorListener() {
                    
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Stats: Failed to get summary");
                    }
                });
    }
    
    
}
