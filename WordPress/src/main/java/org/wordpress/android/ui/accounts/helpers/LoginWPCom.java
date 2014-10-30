package org.wordpress.android.ui.accounts.helpers;

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
import org.wordpress.android.*;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.VolleyUtils;

public class LoginWPCom extends LoginAbstract {
    public LoginWPCom(String username, String password) {
        super(username, password);
    }

    public static int restLoginErrorToMsgId(JSONObject errorObject) {
        // Default to generic error message
        int errorMsgId = org.wordpress.android.R.string.nux_cannot_log_in;

        // Map REST errors to local error codes
        if (errorObject != null) {
            try {
                String error = errorObject.getString("error");
                String errorDescription = errorObject.getString("error_description");
                if (error != null && error.equals("invalid_request")) {
                    if (errorDescription.contains("Incorrect username or password.")) {
                        errorMsgId = org.wordpress.android.R.string.username_or_password_incorrect;
                    }
                    if (errorDescription.contains("This account has two step authentication enabled.")) {
                        errorMsgId = org.wordpress.android.R.string.account_two_step_auth_enabled;
                    }
                }
            } catch (JSONException e) {
                AppLog.e(T.NUX, e);
            }
        }
        return errorMsgId;
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

    protected void login() {
        // Get OAuth token for the first time and check for errors
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
                editor.putString(WordPress.ACCESS_TOKEN_PREFERENCE, token.toString());
                editor.commit();

                mCallback.onSuccess();
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
