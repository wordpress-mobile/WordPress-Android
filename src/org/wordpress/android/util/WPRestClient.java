/**
 * Interface to the WordPress.com REST API.
 */
package org.wordpress.android.util;

import java.util.HashMap;
import java.util.Map;

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

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Note;

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
    
    
    public WPRestClient(RequestQueue queue, Authenticator authenticator, String accessToken){
        // load an existing access token from prefs if we have one
        mAuthenticator = authenticator;
        mRestClient = new RestClient(queue, accessToken);
        if (DeviceUtils.getInstance().isBlackBerry()) {
            mRestClient.setUserAgent(DeviceUtils.getBlackBerryUserAgent());
        } else {
            mRestClient.setUserAgent("wp-android/" + WordPress.versionName);
        }
    }
    /**
     * Remove the current access token
     */
    public void clearAccessToken(){
        mRestClient.setAccessToken(null);
    }
    /**
     * Sets the access token for all future requests
     */
    public void setAccessToken(String token){
        mRestClient.setAccessToken(token);
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
     * Get a single notification.
     * 
     * https://developer.wordpress.com/docs/api/1/get/notifications/
     */
    public void getNotification(String noteId, Listener listener, ErrorListener errorListener){
        get(String.format("notifications/%s", noteId), listener, errorListener);
    }
    /**
     * Mark a notification as read
     * 
     * https://developer.wordpress.com/docs/api/1/post/notifications/read/
     */
    public void markNoteAsRead(Note note, Listener listener, ErrorListener errorListener){
        String path = "notifications/read";
        Map<String, String> params = new HashMap<String, String>();
        params.put(String.format("counts[%s]", note.getId()), note.getUnreadCount());
        post(path, params, null, listener, errorListener);
    }
    /**
     * Get notifications with the provided params.
     * 
     * https://developer.wordpress.com/docs/api/1/get/notifications/
     */
    public void getNotifications(Map<String, String> params, Listener listener, ErrorListener errorListener){
        params.put("number", "40");
        params.put("num_note_items", "20");
        params.put("fields", NOTIFICATION_FIELDS);
        get("notifications", params, null, listener, errorListener);
    }
    /**
     * Get notifications with default params.
     * 
     * https://developer.wordpress.com/docs/api/1/get/notifications/
     */
    public void getNotifications(Listener listener, ErrorListener errorListener){
        getNotifications(new HashMap<String, String>(), listener, errorListener);
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
        
        RestRequest request = mRestClient.makeRequest(Method.GET, RestClient.getAbsoluteURL(path, params), null, listener, errorListener);
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
        final RestRequest request = mRestClient.makeRequest(Method.POST, RestClient.getAbsoluteURL(path), params, listener, errorListener);
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
        RestRequest mRequest;
        RestRequest.ErrorListener mListener;
        protected Request(RestRequest request, ErrorListener listener){
            mRequest = request;
            mListener = listener;
        }
        /**
         * Attempt to send the request, checks to see if we have an access token and if not
         * asks the Authenticator to authenticate the request.
         * 
         * If no Authenticator is provided the request is always sent.
         */
        public void send(){
            if (mRestClient.isAuthenticated() || mAuthenticator == null) {
                makeRequest();
            } else {
                mAuthenticator.authenticate(this);
            }
        }
        /**
         * Method to set the acces token for current and future requests
         */
        public void setAccessToken(Oauth.Token token){
            mRestClient.setAccessToken(token.toString());
            mRequest.setAccessToken(token.toString());
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
        /**
         * Implement this method to perform the request that should be authenticated
         */
        public void makeRequest(){
            mRestClient.send(mRequest);
        }
    }

}
