package org.wordpress.android.networking;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.wordpress.rest.Oauth;
import com.wordpress.rest.Oauth.ErrorListener;
import com.wordpress.rest.Oauth.Listener;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.VolleyUtils;

public class LoginAndFetchBlogListWPCom extends LoginAndFetchBlogListAbstract {
    public LoginAndFetchBlogListWPCom(String username, String password) {
        super(username, password);
    }

    public static int restLoginErrorToMsgId(JSONObject errorObject) {
        // Default to generic error message
        int errorMsgId = R.string.nux_cannot_log_in;

        // Map REST errors to local error codes
        if (errorObject != null) {
            try {
                String error = (String) errorObject.get("error");
                String errorDescription = (String) errorObject.get("error_description");
                if (error != null && error.equals("invalid_request")) {
                    if (errorDescription.contains("Incorrect username or password.")) {
                        errorMsgId = R.string.username_or_password_incorrect;
                    }
                    if (errorDescription.contains("This account has two step authentication enabled.")) {
                        errorMsgId = R.string.account_two_step_auth_enabled;
                    }
                }
            } catch (JSONException e) {
                AppLog.e(T.NUX, e);
            }
        }
        return errorMsgId;
    }

    protected void init() {
        super.init();
        mSetupBlog.setSelfHostedURL(null);
    }

    private Request makeOAuthRequest(final String username, final String password, final Listener listener,
                                     final ErrorListener errorListener) {
        Oauth oauth = new Oauth(org.wordpress.android.BuildConfig.OAUTH_APP_ID,
                org.wordpress.android.BuildConfig.OAUTH_APP_SECRET,
                org.wordpress.android.BuildConfig.OAUTH_REDIRECT_URI);
        Request oauthRequest;
        oauthRequest = oauth.makeRequest(username, password, listener, errorListener);
        return oauthRequest;
    }

    protected void loginAndGetBlogList() {
        // Get OAuth token for the first time and check for errors, make it synch
        WordPress.requestQueue.add(makeOAuthRequest(mUsername, mPassword, new Oauth.Listener() {
            @SuppressLint("CommitPrefEdits")
            @Override
            public void onResponse(final Oauth.Token token) {
                // Once we have a token, start up Simperium
                SimperiumUtils.configureSimperium(WordPress.getContext(), token.toString());

                // login successful, store password
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(WordPress.WPCOM_USERNAME_PREFERENCE, mUsername);
                editor.putString(WordPress.WPCOM_PASSWORD_PREFERENCE, mPassword);
                editor.commit();

                new Thread() {
                    @Override
                    public void run() {
                        mSetupBlog.getBlogList(mCallback);
                    }
                }.start();
            }
        }, new Oauth.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                JSONObject errorObject = VolleyUtils.volleyErrorToJSON(volleyError);
                mCallback.onError(restLoginErrorToMsgId(errorObject), false, false);
            }
        }));
    }
}
