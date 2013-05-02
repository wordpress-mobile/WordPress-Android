/**
 * Interface to the WordPress.com REST API.
 */
package org.wordpress.android.util;

import com.wordpress.rest.Oauth;
import com.wordpress.rest.OauthToken;
import com.wordpress.rest.OauthTokenResponseHandler;
import com.wordpress.rest.RestClient;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import org.wordpress.android.models.Note;

public class WPRestClient {
    
    private static final String NOTIFICATION_FIELDS="id,type,unread,body,subject,timestamp";
    private static final String COMMENT_REPLY_CONTENT_FIELD="content";
    
    private RestClient mRestClient;
    private Authenticator mAuthenticator;
    
    public WPRestClient(Authenticator authenticator, String accessToken){
        // load an existing access token from prefs if we have one
        mAuthenticator = authenticator;
        mRestClient = new RestClient(accessToken);
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
    public void replyToComment(Note.Reply reply, AsyncHttpResponseHandler handler){
        replyToComment(reply.getSiteId(), reply.getCommentId(), reply.getContent(), handler);
    }
    /**
     * Reply to a comment.
     * 
     * https://developer.wordpress.com/docs/api/1/post/sites/%24site/posts/%24post_ID/replies/new/
     */
    public void replyToComment(String siteId, String commentId, String content, AsyncHttpResponseHandler h){
        RequestParams params = new RequestParams();
        params.put(COMMENT_REPLY_CONTENT_FIELD, content);
        String path = String.format("sites/%s/comments/%s/replies/new", siteId, commentId);
        post(path, params, h);
    }
    /**
     * Follow a site given an ID or domain
     * 
     * https://developer.wordpress.com/docs/api/1/post/sites/%24site/follows/new/
     */
    public void followSite(String siteId, AsyncHttpResponseHandler handler){
        String path = String.format("sites/%s/follows/new", siteId);
        post(path, handler);
    }
    /**
     * Unfollow a site given an ID or domain
     * 
     * https://developer.wordpress.com/docs/api/1/post/sites/%24site/follows/mine/delete/
     */
    public void unfollowSite(String siteId, AsyncHttpResponseHandler handler){
        String path = String.format("sites/%s/follows/mine/delete", siteId);
        post(path, handler);
    }
    /**
     * Get a single notification.
     * 
     * https://developer.wordpress.com/docs/api/1/get/notifications/
     */
    public void getNotification(String noteId, AsyncHttpResponseHandler handler){
        get(String.format("notifications/%s", noteId), handler);
    }
    /**
     * Mark a notification as read
     * 
     * https://developer.wordpress.com/docs/api/1/post/notifications/read/
     */
    public void markNoteAsRead(Note note, AsyncHttpResponseHandler handler){
        String path = "notifications/read";
        RequestParams params = new RequestParams();
        params.put(String.format("counts[%s]", note.getId()), note.getUnreadCount());
        post(path, params, handler);
    }
    /**
     * Get notifications with the provided params.
     * 
     * https://developer.wordpress.com/docs/api/1/get/notifications/
     */
    public void getNotifications(RequestParams params, AsyncHttpResponseHandler handler){
        params.put("number", "20");
        params.put("fields", NOTIFICATION_FIELDS);
        get("notifications", params, handler);
    }
    /**
     * Get notifications with default params.
     * 
     * https://developer.wordpress.com/docs/api/1/get/notifications/
     */
    public void getNotifications(AsyncHttpResponseHandler handler){
        getNotifications(new RequestParams(), handler);
    }
    /**
     * Update the seen timestamp.
     * 
     * https://developer.wordpress.com/docs/api/1/post/notifications/seen
     */
    public void markNotificationsSeen(String timestamp, AsyncHttpResponseHandler handler){
        RequestParams params = new RequestParams();
        params.put("time", timestamp);
        post("notifications/seen", params, handler);
    }
    /**
     * Make GET request
     */
    public void get(String path, AsyncHttpResponseHandler handler){
        get(path, null, handler);
    }
    /**
     * Make GET request with params
     */
    public void get(final String path, final RequestParams params, final AsyncHttpResponseHandler handler){
        Request request = new Request(handler){
            @Override
            public void makeRequest(){
                mRestClient.get(path, params, handler);
            }
        };
        request.send();
    }
    /**
     * Make POST request
     */
    public void post(String path, AsyncHttpResponseHandler handler){
        post(path, null, handler);
    }
    /**
     * Make POST request with params
     */
    public void post(final String path, final RequestParams params, final AsyncHttpResponseHandler handler){
        Request request = new Request(handler){
            @Override
            public void makeRequest(){
                mRestClient.post(path, params, handler);
            }
        };
        request.send();
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
    abstract public class Request {
        private AsyncHttpResponseHandler mHandler;
        protected Request(AsyncHttpResponseHandler handler){
            mHandler = handler;
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
        public void setAccessToken(OauthToken token){
            mRestClient.setAccessToken(token.toString());
        }
        /**
         * If an access token cannot be obtained the request can be aborted and the
         * handler's onFailure method is called
         */
        public void abort(Throwable e, String body){
            mHandler.onFailure(e, body);
        }
        /**
         * Implement this method to perform the request that should be authenticated
         */
        abstract public void makeRequest();
    }

}
