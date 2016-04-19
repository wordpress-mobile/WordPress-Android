/**
 * Interface to the WordPress.com REST API.
 */
package org.wordpress.android.util;

import android.os.AsyncTask;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.wordpress.rest.Oauth;
import com.wordpress.rest.RestClient;
import com.wordpress.rest.RestRequest;
import com.wordpress.rest.RestRequest.ErrorListener;
import com.wordpress.rest.RestRequest.Listener;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Note;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class WPRestClient {
    
    private static final String NOTIFICATION_FIELDS="id,type,unread,body,subject,timestamp";
    private static final String COMMENT_REPLY_CONTENT_FIELD="content";
    
    private RestClient mRestClient;
    private Authenticator mAuthenticator;
    
    /** Socket timeout in milliseconds for rest requests */
    public static final int REST_TIMEOUT_MS = 30000;

    /** Default number of retries for POST rest requests */
    public static final int REST_MAX_RETRIES_POST = 0;

    /** Default number of retries for GET rest requests */
    public static final int REST_MAX_RETRIES_GET = 3;
    
    /** Default backoff multiplier for rest requests */
    public static final float REST_BACKOFF_MULT = 2f;
    
    public WPRestClient(RequestQueue queue, Authenticator authenticator){
        this(queue, authenticator, RestClient.REST_CLIENT_VERSIONS.V1);
    }

    public WPRestClient(RequestQueue queue, Authenticator authenticator, RestClient.REST_CLIENT_VERSIONS version){
        mAuthenticator = authenticator;
        mRestClient = new RestClient(queue, version);
        mRestClient.setUserAgent(WordPress.getUserAgent());
    }

    /**
     * Reply to a comment using a Note.Reply object.
     * 
     * https://developer.wordpress.com/docs/api/1/post/sites/%24site/posts/%24post_ID/replies/new/
     */
    public void replyToComment(Note.Reply reply, Listener listener, ErrorListener errorListener){
        Map<String, String> params = new HashMap<String, String>();
        params.put(COMMENT_REPLY_CONTENT_FIELD, reply.getContent());
        post(reply.getRestPath(), params, null, listener, errorListener);
    }
    /**
     * Reply to a comment.
     * 
     * https://developer.wordpress.com/docs/api/1/post/sites/%24site/posts/%24post_ID/replies/new/
     */
    public void replyToComment(String siteId, String commentId, String content, Listener listener, ErrorListener errorListener){
        Map<String, String> params = new HashMap<String, String>();
        params.put(COMMENT_REPLY_CONTENT_FIELD, content);
        String path = String.format("sites/%s/comments/%s/replies/new", siteId, commentId);
        post(path, params, null, listener, errorListener);
    }
    /**
     * Follow a site given an ID or domain
     * 
     * https://developer.wordpress.com/docs/api/1/post/sites/%24site/follows/new/
     */
    public void followSite(String siteId, Listener listener, ErrorListener errorListener){
        String path = String.format("sites/%s/follows/new", siteId);
        post(path, listener, errorListener);
    }
    /**
     * Unfollow a site given an ID or domain
     * 
     * https://developer.wordpress.com/docs/api/1/post/sites/%24site/follows/mine/delete/
     */
    public void unfollowSite(String siteId, Listener listener, ErrorListener errorListener){
        String path = String.format("sites/%s/follows/mine/delete", siteId);
        post(path, listener, errorListener);
    }

    /**
     * Update the seen timestamp.
     * 
     * https://developer.wordpress.com/docs/api/1/post/notifications/seen
     */
    public void markNotificationsSeen(String timestamp, Listener listener, ErrorListener errorListener){
        Map<String, String> params = new HashMap<String, String>();
        params.put("time", timestamp);
        post("notifications/seen", params, null, listener, errorListener);
    }
    /**
     * Moderate a comment.
     * 
     * http://developer.wordpress.com/docs/api/1/sites/%24site/comments/%24comment_ID/
     */
    public void moderateComment(String siteId, String commentId, String status, Listener listener, ErrorListener errorListener){
        Map<String, String> params = new HashMap<String, String>();
        params.put("status", status);
        String path = String.format("sites/%s/comments/%s/", siteId, commentId);
        post(path, params, null, listener, errorListener);
    }
    
    /**
     * Get all a site's themes
     */
    public void getThemes(String siteId, int limit, int offset, Listener listener, ErrorListener errorListener) {
        String path = String.format("sites/%s/themes?limit=%d&offset=%d", siteId, limit, offset);
        get(path, listener, errorListener);
    }
    
    /**
     * Set a site's theme
     */
    public void setTheme(String siteId, String themeId, Listener listener, ErrorListener errorListener) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("theme", themeId);
        String path = String.format("sites/%s/themes/mine", siteId);
        post(path, params, null, listener, errorListener);
    }
    
    /**
     * Get a site's current theme
     */
    public void getCurrentTheme(String siteId, Listener listener, ErrorListener errorListener) {
        String path = String.format("sites/%s/themes/mine", siteId);
        get(path, listener, errorListener);
    }
    
    /**
     * Get a site's stats for clicks
     */
    public void getStatsClicks(String siteId, String date, Listener listener, ErrorListener errorListener) {
        String path = String.format("sites/%s/stats/clicks?date=%s", siteId, date);
        get(path, listener, errorListener);
    }

    /**
     * Get a site's stats for geoviews (views by country)
     */
    public void getStatsGeoviews(String siteId, String date, Listener listener, ErrorListener errorListener) {
        String path = String.format("sites/%s/stats/country-views?date=%s", siteId, date);
        get(path, listener, errorListener);
    }

    /**
     * Get a site's stats for most commented posts
     */
    public void getStatsMostCommented(String siteId, Listener listener, ErrorListener errorListener) {
        String path = "stats/most_commented";
        Map<String, String> params = new HashMap<String, String>();
        params.put("blog", siteId);
        getXL(path, params, listener, errorListener);
    }

    /**
     * Get a site's stats for top commenters
     */
    public void getStatsTopCommenters(String siteId, Listener listener, ErrorListener errorListener) {
        String path = "stats/top_commenters";
        Map<String, String> params = new HashMap<String, String>();
        params.put("blog", siteId);
        getXL(path, params, listener, errorListener);
    }

    /**
     * Get a site's stats for referrers
     */
    public void getStatsReferrers(String siteId, String date, Listener listener, ErrorListener errorListener) {
        String path = String.format("sites/%s/stats/referrers?date=%s", siteId, date);
        get(path, listener, errorListener);
    }

    /**
     * Get a site's stats for search engine terms
     */
    public void getStatsSearchEngineTerms(String siteId, String date, Listener listener, ErrorListener errorListener) {
        String path = String.format("sites/%s/stats/search-terms?date=%s", siteId, date);
        get(path, listener, errorListener);
    }

    /**
     * Get a site's stats for tags and categories
     */
    public void getStatsTagsAndCategories(String siteId, Listener listener, ErrorListener errorListener) {
        String path = "stats/tags_and_categories";
        Map<String, String> params = new HashMap<String, String>();
        params.put("blog", siteId);
        getXL(path, params, listener, errorListener);
    }

    /**
     * Get a site's stats for top authors
     */
    public void getStatsTopAuthors(String siteId, Listener listener, ErrorListener errorListener) {
        String path = "stats/top_authors";
        Map<String, String> params = new HashMap<String, String>();
        params.put("blog", siteId);
        getXL(path, params, listener, errorListener);
    }

    /**
     * Get a site's stats for top posts and pages
     */
    public void getStatsTopPosts(String siteId, String date, Listener listener, ErrorListener errorListener) {
        String path = String.format("sites/%s/stats/top-posts?date=%s", siteId, date);
        get(path, listener, errorListener);
    }

    /**
     * Get a site's stats for video plays
     */
    public void getStatsVideoPlays(String siteId, Listener listener, ErrorListener errorListener) {
        String path = "stats/video_plays";
        Map<String, String> params = new HashMap<String, String>();
        params.put("blog", siteId);
        getXL(path, params, listener, errorListener);
    }
    
    /**
     * Get a site's stats summary
     */
    public void getStatsSummary(String siteId, Listener listener, ErrorListener errorListener) {
        String path = String.format("sites/%s/stats", siteId);
        get(path, listener, errorListener);
    }

    /**
     * Get a site's stats summary for videos
     */
    public void getStatsVideoSummary(String siteId, Listener listener, ErrorListener errorListener) {
        String path = "stats/video_summary";
        Map<String, String> params = new HashMap<String, String>();
        params.put("blog", siteId);
        getXL(path, params, listener, errorListener);
    }

    public void getSiteDescription(String siteId, Listener listener, ErrorListener errorListener) {
        String path = String.format("rest/v1.1/sites/%s", siteId);
        Map<String, String> params = new HashMap<String, String>();
        params.put("fields", "description");
        get(path, params, null, listener, errorListener);
    }

    /**
     * This method is for simulating stats APIs using the XL Studio API simulator. It should be removed once the other APIs are implemented. 
     **/
    public void getXL(String path, Map<String, String> params, final Listener listener, final ErrorListener errorListener) {
        
        path = "https://simulator.xlstudio.com/apis/32/" + path;

        final String url_path = path;
        
        new AsyncTask<Void, Void, JSONObject>() {

            @Override
            protected JSONObject doInBackground(Void... params) {

                URL url;
                HttpURLConnection conn;
                BufferedReader rd;
                String line;
                String result = "";
                
                try {
                    url = new URL(url_path);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.addRequestProperty("X-SIMULATOR-ACCESS-KEY", "bc88864498a705657486edb636196e31");
                    rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((line = rd.readLine()) != null) {
                       result += line;
                    }
                    rd.close();
                    
                    return new JSONObject(result);
                 } catch (JSONException e) {
                     e.printStackTrace();
                 } catch (Exception e) {
                    e.printStackTrace();
                 }
                return null;
            }
            
            protected void onPostExecute(JSONObject result) {
                if (result != null)
                    listener.onResponse(result);
                else
                    errorListener.onErrorResponse(new VolleyError("JSONObject null"));
            };
            
        }.execute();
        
    }
    
    /**
     * 
     * Make GET request
     */
    public void get(String path, Listener listener, ErrorListener errorListener){
        get(path, null, null, listener, errorListener);
    }
    /**
     * Make GET request with params
     */
    public void get(String path, Map<String, String> params, RetryPolicy retryPolicy, Listener listener, ErrorListener errorListener){
        // turn params into querystring
        
        RestRequest request = mRestClient.makeRequest(Method.GET, mRestClient.getAbsoluteURL(path, params), null, listener, errorListener);
        if(retryPolicy == null) {
            retryPolicy = new DefaultRetryPolicy(REST_TIMEOUT_MS, REST_MAX_RETRIES_GET, REST_BACKOFF_MULT);
        } 
        request.setRetryPolicy(retryPolicy);
        Request authCheck = new Request(request, errorListener);
        authCheck.send();
    }
    /**
     * Make POST request
     */
    public void post(String path, Listener listener, ErrorListener errorListener){
        post(path, null, null, listener, errorListener);
    }
    /**
     * Make POST request with params
     */
    public void post(final String path, Map<String, String> params, RetryPolicy retryPolicy, Listener listener, ErrorListener errorListener){
        final RestRequest request = mRestClient.makeRequest(Method.POST, mRestClient.getAbsoluteURL(path), params, listener, errorListener);
        if(retryPolicy == null) {
            retryPolicy = new DefaultRetryPolicy(REST_TIMEOUT_MS, REST_MAX_RETRIES_POST, REST_BACKOFF_MULT); //Do not retry on failure
        } 
        request.setRetryPolicy(retryPolicy);
        Request authCheck = new Request(request, errorListener);
        authCheck.send();
    }
    /**
     * Interface that provides a method that should perform the necessary task to make sure
     * the provided Request will be authenticated.
     * 
     * The Authenticator must call Request.send() when it has completed its operations. For
     * convenience the Request class provides Request.setAccessToken so the Authenticator can
     * easily update the access token.
     */
    public interface Authenticator {
        void authenticate(Request request);
    }

    /**
     * Encapsulates the behaviour for asking the Authenticator for an access token. This
     * allows the request maker to disregard the authentication state when making requests.
     */
    public class Request {

        static public final String SITE_PREFIX = "https://public-api.wordpress.com/rest/v1/sites/";
        RestRequest mRequest;
        RestRequest.ErrorListener mListener;

        protected Request(RestRequest request, ErrorListener listener){
            mRequest = request;
            mListener = listener;
        }

        public String getSiteId() {
            // parse out the site id from the url
            String url = mRequest.getUrl();

            if (url.startsWith(SITE_PREFIX) && !SITE_PREFIX.equals(url)) {
                int marker = SITE_PREFIX.length();
                if (url.indexOf("/", marker) < marker)
                    return null;
                return url.substring(marker, url.indexOf("/", marker));
            }
            // not a sites/$siteId request
            return null;
        }

        /**
         * Attempt to send the request, checks to see if we have an access token and if not
         * asks the Authenticator to authenticate the request.
         * 
         * If no Authenticator is provided the request is always sent.
         */
        protected void send(){
            if (mAuthenticator == null) {
                mRestClient.send(mRequest);
            } else {
                mAuthenticator.authenticate(this);
            }
        }

        public void sendWithAccessToken(String token){
            mRequest.setAccessToken(token);
            mRestClient.send(mRequest);
        }

        public void sendWithAccessToken(Oauth.Token token){
            sendWithAccessToken(token.toString());
        }

        /**
         * If an access token cannot be obtained the request can be aborted and the
         * handler's onFailure method is called
         */
        public void abort(VolleyError error){
            if (mListener != null) {
                mListener.onErrorResponse(error);
            }
        }

    }

}
