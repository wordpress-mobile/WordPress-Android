/**
 * Wraps the Oauth and RestClient classes and configures them with application specific settings
 */
package org.wordpress.android.util;

import com.wordpress.rest.Oauth;
import com.wordpress.rest.OauthToken;
import com.wordpress.rest.OauthTokenResponseHandler;
import com.wordpress.rest.RestClient;

import android.content.SharedPreferences;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.util.Properties;

import org.json.JSONObject;

import org.wordpress.android.models.Note;

public class WPRestClient {
    
    private static final String ACCESS_TOKEN_PREFERNCE="wpcom-access-token";
    private static final String APP_ID_PROPERTY="oauth.app_id";
    private static final String APP_SECRET_PROPERTY="oauth.app_secret";
    private static final String APP_REDIRECT_PROPERTY="oauth.redirect_uri";
    
    private static final String NOTIFICATION_FIELDS="id,type,unread,body,subject,timestamp";
    private static final String COMMENT_REPLY_CONTENT_FIELD="content";
    
    private Oauth mOauth;
    private RestClient mRestClient;
    private SharedPreferences mPrefs;
    
    public WPRestClient(Properties config, SharedPreferences prefs){
        mPrefs = prefs;
        // configure Oauth with app credentials
        mOauth = new Oauth(config.getProperty(APP_ID_PROPERTY),
                           config.getProperty(APP_SECRET_PROPERTY),
                           config.getProperty(APP_REDIRECT_PROPERTY));
        // load an existing access token from prefs if we have one
        mRestClient = new RestClient(prefs.getString(ACCESS_TOKEN_PREFERNCE, null));
    }
    /**
     * Authenticate the user using WordPress.com Oauth
     */
    public void requestAccessToken(String username, String password, final OauthTokenResponseHandler handler){
        mOauth.requestAccessToken(username, password, new OauthTokenResponseHandler() {
            @Override
            public void onStart(){
                handler.onStart();
            }
            /**
             * Save the token to a preference
             */
            @Override
            public void onSuccess(OauthToken token){
                mRestClient.setAccessToken(token.toString());
                handler.onSuccess(token);
            }
            
            @Override
            public void onFailure(Throwable e, JSONObject response){
                handler.onFailure(e, response);
            }
            
            @Override
            public void onFinish(){
                handler.onFinish();
            }
        });
    }
    public void replyToComment(Note.Reply reply, AsyncHttpResponseHandler handler){
        replyToComment(reply.getSiteId(), reply.getCommentId(), reply.getContent(), handler);
    }
    public void replyToComment(String siteId, String commentId, String content, AsyncHttpResponseHandler h){
        RequestParams params = new RequestParams();
        params.put(COMMENT_REPLY_CONTENT_FIELD, content);
        String path = String.format("sites/%s/comments/%s/replies/new", siteId, commentId);
        post(path, params, h);
    }
    /**
     * Follow a site given an ID or domain
     */
    public void followSite(String siteId, AsyncHttpResponseHandler handler){
        String path = String.format("sites/%s/follows/new", siteId);
        post(path, handler);
    }
    /**
     * Unfollow a site given an ID or domain
     */
    public void unfollowSite(String siteId, AsyncHttpResponseHandler handler){
        String path = String.format("sites/%s/follows/mine/delete", siteId);
        post(path, handler);
    }
    /**
     * Get a single notification
     */
    public void getNotification(String noteId, AsyncHttpResponseHandler handler){
        get(String.format("notifications/%s", noteId), handler);
    }
    /**
     * Mark a notification as read
     */
    public void markNoteAsRead(Note note, AsyncHttpResponseHandler handler){
        String path = "notifications/read";
        RequestParams params = new RequestParams();
        params.put(String.format("counts[%s]", note.getId()), note.getUnreadCount());
        post(path, params, handler);
    }
    /**
     * Get notifications
     */
    public void getNotifications(RequestParams params, AsyncHttpResponseHandler handler){
        params.put("number", "20");
        params.put("fields", NOTIFICATION_FIELDS);
        get("notifications", params, handler);
    }
    public void getNotifications(AsyncHttpResponseHandler handler){
        getNotifications(new RequestParams(), handler);
    }
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
    public void get(String path, RequestParams params, AsyncHttpResponseHandler handler){
        mRestClient.get(path, params, handler);
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
    public void post(String path, RequestParams params, AsyncHttpResponseHandler h){
        mRestClient.post(path, params, h);
    }
}