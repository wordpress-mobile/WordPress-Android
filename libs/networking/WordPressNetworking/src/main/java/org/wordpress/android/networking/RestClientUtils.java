/**
 * Interface to the WordPress.com REST API.
 */
package org.wordpress.android.networking;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.RequestFuture;
import com.wordpress.rest.JsonRestRequest;
import com.wordpress.rest.RestClient;
import com.wordpress.rest.RestRequest;
import com.wordpress.rest.RestRequest.ErrorListener;
import com.wordpress.rest.RestRequest.Listener;

import org.json.JSONObject;
import org.wordpress.android.util.LanguageUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class RestClientUtils {
    private static final String NOTIFICATION_FIELDS = "id,type,unread,body,subject,timestamp,meta";
    private static final String COMMENT_REPLY_CONTENT_FIELD = "content";
    private static String sUserAgent = "WordPress Networking Android";

    private RestClient mRestClient;
    private Authenticator mAuthenticator;
    private Context mContext;

    /**
     * Socket timeout in milliseconds for rest requests
     */
    public static final int REST_TIMEOUT_MS = 30000;

    /**
     * Default number of retries for POST rest requests
     */
    public static final int REST_MAX_RETRIES_POST = 0;

    /**
     * Default number of retries for GET rest requests
     */
    public static final int REST_MAX_RETRIES_GET = 3;

    /**
     * Default backoff multiplier for rest requests
     */
    public static final float REST_BACKOFF_MULT = 2f;

    public static void setUserAgent(String userAgent) {
        sUserAgent = userAgent;
    }

    public RestClientUtils(Context context, RequestQueue queue, Authenticator authenticator, RestRequest.OnAuthFailedListener onAuthFailedListener) {
        this(context, queue, authenticator, onAuthFailedListener, RestClient.REST_CLIENT_VERSIONS.V1);
    }

    public RestClientUtils(Context context, RequestQueue queue, Authenticator authenticator, RestRequest.OnAuthFailedListener onAuthFailedListener, RestClient.REST_CLIENT_VERSIONS version) {
        // load an existing access token from prefs if we have one
        mContext = context;
        mAuthenticator = authenticator;
        mRestClient = RestClientFactory.instantiate(queue, version);
        if (onAuthFailedListener != null) {
            mRestClient.setOnAuthFailedListener(onAuthFailedListener);
        }
        mRestClient.setUserAgent(sUserAgent);
    }

    public Authenticator getAuthenticator() {
        return mAuthenticator;
    }

    public RestClient getRestClient() {
        return mRestClient;
    }

    public void getCategories(String siteId, Listener listener, ErrorListener errorListener) {
        String path = String.format("sites/%s/categories", siteId);
        get(path, null, null, listener, errorListener);
    }

    /**
     * get a list of recent comments
     * <p/>
     * https://developer.wordpress.com/docs/api/1.1/get/sites/%24site/comments/
     */
    public void getComments(String siteId, Map<String, String> params, final Listener listener, ErrorListener errorListener) {
        String path = String.format("sites/%s/comments", siteId);
        get(path, params, null, listener, errorListener);
    }

    /**
     * Reply to a comment
     * <p/>
     * https://developer.wordpress.com/docs/api/1/post/sites/%24site/posts/%24post_ID/replies/new/
     */
    public void replyToComment(String reply, String path, Listener listener, ErrorListener errorListener) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(COMMENT_REPLY_CONTENT_FIELD, reply);
        post(path, params, null, listener, errorListener);
    }

    /**
     * Reply to a comment.
     * <p/>
     * https://developer.wordpress.com/docs/api/1/post/sites/%24site/posts/%24post_ID/replies/new/
     */
    public void replyToComment(long siteId, long commentId, String content, Listener listener,
                               ErrorListener errorListener) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(COMMENT_REPLY_CONTENT_FIELD, content);
        String path = String.format("sites/%d/comments/%d/replies/new", siteId, commentId);
        post(path, params, null, listener, errorListener);
    }

    /**
     * Follow a site given an ID or domain
     * <p/>
     * https://developer.wordpress.com/docs/api/1/post/sites/%24site/follows/new/
     */
    public void followSite(String siteId, Listener listener, ErrorListener errorListener) {
        String path = String.format("sites/%s/follows/new", siteId);
        post(path, listener, errorListener);
    }

    /**
     * Unfollow a site given an ID or domain
     * <p/>
     * https://developer.wordpress.com/docs/api/1/post/sites/%24site/follows/mine/delete/
     */
    public void unfollowSite(String siteId, Listener listener, ErrorListener errorListener) {
        String path = String.format("sites/%s/follows/mine/delete", siteId);
        post(path, listener, errorListener);
    }

    /**
     * Get notifications with the provided params.
     * <p/>
     * https://developer.wordpress.com/docs/api/1/get/notifications/
     */
    public void getNotifications(Map<String, String> params, Listener listener, ErrorListener errorListener) {
        params.put("number", "40");
        params.put("num_note_items", "20");
        params.put("fields", NOTIFICATION_FIELDS);
        get("notifications", params, null, listener, errorListener);
    }

    /**
     * Get notifications with default params.
     * <p/>
     * https://developer.wordpress.com/docs/api/1/get/notifications/
     */
    public void getNotifications(Listener listener, ErrorListener errorListener) {
        getNotifications(new HashMap<String, String>(), listener, errorListener);
    }

    /**
     * Update the seen timestamp.
     * <p/>
     * https://developer.wordpress.com/docs/api/1/post/notifications/seen
     */
    public void markNotificationsSeen(String timestamp, Listener listener, ErrorListener errorListener) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("time", timestamp);
        post("notifications/seen", params, null, listener, errorListener);
    }

    /**
     * Moderate a comment.
     * <p/>
     * http://developer.wordpress.com/docs/api/1/sites/%24site/comments/%24comment_ID/
     */
    public void moderateComment(String siteId, String commentId, String status, Listener listener,
                                ErrorListener errorListener) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("status", status);
        String path = String.format("sites/%s/comments/%s/", siteId, commentId);
        post(path, params, null, listener, errorListener);
    }

    /**
     * Edit the content of a comment
     */
    public void editCommentContent(long siteId, long commentId, String content, Listener listener,
                                ErrorListener errorListener) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("content", content);
        String path = String.format("sites/%d/comments/%d/", siteId, commentId);
        post(path, params, null, listener, errorListener);
    }

    /**
     * Like or unlike a comment.
     */
    public void likeComment(String siteId, String commentId, boolean isLiked, Listener listener,
                                ErrorListener errorListener) {
        Map<String, String> params = new HashMap<String, String>();
        String path = String.format("sites/%s/comments/%s/likes/", siteId, commentId);

        if (!isLiked) {
            path += "mine/delete";
        } else {
            path += "new";
        }

        post(path, params, null, listener, errorListener);
    }

    public void getFreeSearchThemes(String siteId, int limit, int offset, String searchTerm, Listener listener, ErrorListener errorListener) {
        getSearchThemes("free", siteId, limit, offset, searchTerm, listener, errorListener);
    }

    public void getSearchThemes(String tier, String siteId, int limit, int offset, String searchTerm, Listener listener, ErrorListener errorListener) {
        String path = String.format("sites/%s/themes?tier=" + tier + "&number=%d&offset=%d&search=%s", siteId, limit, offset, searchTerm);
        get(path, listener, errorListener);
    }

    public void getFreeThemes(String siteId, int limit, int offset, Listener listener, ErrorListener errorListener) {
        getThemes("free", siteId, limit, offset, listener, errorListener);
    }

    public void getPurchasedThemes(String siteId, Listener listener, ErrorListener errorListener) {
        String path = String.format("sites/%s/themes/purchased", siteId);
        get(path, listener, errorListener);
    }

    /**
     * Get all a site's themes
     */
    public void getThemes(String tier, String siteId, int limit, int offset, Listener listener, ErrorListener errorListener) {
        String path = String.format("sites/%s/themes/?tier=" + tier + "&number=%d&offset=%d", siteId, limit, offset);
        get(path, listener, errorListener);
    }

    /**
     * Set a site's theme
     */
    public void setTheme(String siteId, String themeId, Listener listener, ErrorListener errorListener) {
        Map<String, String> params = new HashMap<>();
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

    public void getGeneralSettings(String siteId, Listener listener, ErrorListener errorListener) {
        String path = String.format("sites/%s/settings", siteId);
        Map<String, String> params = new HashMap<String, String>();
        get(path, params, null, listener, errorListener);
    }

    public void setGeneralSiteSettings(String siteId, Listener listener, ErrorListener errorListener,
                                       Map<String, String> params) {
        String path = String.format("sites/%s/settings", siteId);
        post(path, params, null, listener, errorListener);
    }

    /**
     * Delete a site
     */
    public void deleteSite(String siteId, Listener listener, ErrorListener errorListener) {
        String path = String.format("sites/%s/delete", siteId);
        post(path, listener, errorListener);
    }

    public void getSitePurchases(String siteId, Listener listener, ErrorListener errorListener) {
        String path = String.format("sites/%s/purchases", siteId);
        get(path, listener, errorListener);
    }

    public void exportContentAll(String siteId, Listener listener, ErrorListener errorListener) {
        String path = String.format("sites/%s/exports/start", siteId);
        post(path, listener, errorListener);
    }

    public void sendLoginEmail(Map<String, String> params, Listener listener, ErrorListener errorListener) {
        post("auth/send-login-email", params, null, listener, errorListener);
    }

    public void isAvailable(String email, Listener listener, ErrorListener errorListener) {
        String path = String.format("is-available/email?q=%s", email);
        get(path, listener, errorListener);
    }

    /**
     * Make GET request
     */
    public Request<JSONObject> get(String path, Listener listener, ErrorListener errorListener) {
        return get(path, null, null, listener, errorListener);
    }

    /**
     * Make GET request with params
     */
    public Request<JSONObject> get(String path, Map<String, String> params, RetryPolicy retryPolicy, Listener listener,
                    ErrorListener errorListener) {
        // turn params into querystring
        HashMap<String, String> paramsWithLocale = getRestLocaleParams(mContext);
        if (params != null) {
            paramsWithLocale.putAll(params);
        }

        String realPath = getSanitizedPath(path);
        if (TextUtils.isEmpty(realPath)) {
            realPath = path;
        }
        paramsWithLocale.putAll(getSanitizedParameters(path));

        RestRequest request = mRestClient.makeRequest(Method.GET, mRestClient.getAbsoluteURL(realPath, paramsWithLocale), null,
                listener, errorListener);

        if (retryPolicy == null) {
            retryPolicy = new DefaultRetryPolicy(REST_TIMEOUT_MS, REST_MAX_RETRIES_GET, REST_BACKOFF_MULT);
        }
        request.setRetryPolicy(retryPolicy);
        AuthenticatorRequest authCheck = new AuthenticatorRequest(request, errorListener, mRestClient, mAuthenticator);
        authCheck.send();
        return request;
    }

    /**
     * Make Synchronous GET request
     *
     * @throws TimeoutException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public JSONObject getSynchronous(String path) throws InterruptedException, ExecutionException, TimeoutException {
        return getSynchronous(path, null, null);
    }

    /**
     * Make Synchronous GET request with params
     *
     * @throws TimeoutException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public JSONObject getSynchronous(String path, Map<String, String> params, RetryPolicy retryPolicy)
            throws InterruptedException, ExecutionException, TimeoutException {
        RequestFuture<JSONObject> future = RequestFuture.newFuture();

        HashMap<String, String> paramsWithLocale = getRestLocaleParams(mContext);
        if (params != null) {
            paramsWithLocale.putAll(params);
        }

        String realPath = getSanitizedPath(path);
        if (TextUtils.isEmpty(realPath)) {
            realPath = path;
        }
        paramsWithLocale.putAll(getSanitizedParameters(path));

        RestRequest request = mRestClient.makeRequest(Method.GET, mRestClient.getAbsoluteURL(realPath, paramsWithLocale), null, future, future);

        if (retryPolicy == null) {
            retryPolicy = new DefaultRetryPolicy(REST_TIMEOUT_MS, REST_MAX_RETRIES_GET, REST_BACKOFF_MULT);
        }
        request.setRetryPolicy(retryPolicy);

        AuthenticatorRequest authCheck = new AuthenticatorRequest(request, null, mRestClient, mAuthenticator);
        authCheck.send(); //this insert the request into the queue. //TODO: Verify that everything is OK on REST calls without a valid token
        JSONObject response = future.get();
        return response;
    }

    /**
     * Make POST request
     */
    public void post(String path, Listener listener, ErrorListener errorListener) {
        Map<String, String> params = null;
        post(path, params, null, listener, errorListener);
    }

    /**
     * Make POST request with params
     */
    public void post(final String path, Map<String, String> params, RetryPolicy retryPolicy, Listener listener,
                     ErrorListener errorListener) {
        final RestRequest request = mRestClient.makeRequest(Method.POST, mRestClient.getAbsoluteURL(path, getRestLocaleParams(mContext)), params,
                listener, errorListener);
        if (retryPolicy == null) {
            retryPolicy = new DefaultRetryPolicy(REST_TIMEOUT_MS, REST_MAX_RETRIES_POST,
                    REST_BACKOFF_MULT); //Do not retry on failure
        }
        request.setRetryPolicy(retryPolicy);
        AuthenticatorRequest authCheck = new AuthenticatorRequest(request, errorListener, mRestClient, mAuthenticator);
        authCheck.send();
    }


    /**
     * Make a JSON POST request
     */
    public void post(final String path, JSONObject params, RetryPolicy retryPolicy, Listener listener,
                     ErrorListener errorListener) {
        final JsonRestRequest request = mRestClient.makeRequest(mRestClient.getAbsoluteURL(path, getRestLocaleParams(mContext)), params,
                listener, errorListener);
        if (retryPolicy == null) {
            retryPolicy = new DefaultRetryPolicy(REST_TIMEOUT_MS, REST_MAX_RETRIES_POST,
                    REST_BACKOFF_MULT); //Do not retry on failure
        }
        request.setRetryPolicy(retryPolicy);
        AuthenticatorRequest authCheck = new AuthenticatorRequest(request, errorListener, mRestClient, mAuthenticator);
        authCheck.send();
    }

    /**
     * Takes a URL and returns the path within, or an empty string (not null)
     */
    public static String getSanitizedPath(String unsanitizedPath){
        if (unsanitizedPath != null) {
            int qmarkPos = unsanitizedPath.indexOf('?');
            if (qmarkPos > -1) { //strip any querystring params off this to obtain the path
                return unsanitizedPath.substring(0, qmarkPos+1);
            } else {
                // return the string as is, consider the whole string as the path
                return unsanitizedPath;
            }
        }
        return "";
    }

    /**
     * Takes a URL with query strings and returns a Map of query string values.
     */
    public static HashMap<String, String> getSanitizedParameters(String unsanitizedPath){
        HashMap<String, String> queryParams = new HashMap<>();

        Uri uri = Uri.parse(unsanitizedPath);

        if (uri.getQueryParameterNames() != null ) {
            Iterator iter = uri.getQueryParameterNames().iterator();
            while (iter.hasNext()) {
                String name = (String)iter.next();
                String value = uri.getQueryParameter(name);
                queryParams.put(name, value);
            }
        }

        return queryParams;
    }

    /**
     * Returns locale parameter used in REST calls which require the response to be localized
     */
    public static HashMap<String, String> getRestLocaleParams(Context context) {
        HashMap<String, String> params = new HashMap<>();
        String deviceLanguageCode = LanguageUtils.getCurrentDeviceLanguageCode(context);
        if (!TextUtils.isEmpty(deviceLanguageCode)) {
            //patch locale if it's any of the deprecated codes as can be read in Locale.java source code:
            deviceLanguageCode = LanguageUtils.patchDeviceLanguageCode(deviceLanguageCode);
            params.put("locale", deviceLanguageCode);
        }
        return params;
    }

}
