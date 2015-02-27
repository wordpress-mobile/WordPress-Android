package org.wordpress.android.ui.accounts.helpers;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.wordpress.rest.Oauth;
import com.wordpress.rest.Oauth.ErrorListener;
import com.wordpress.rest.Oauth.Listener;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.*;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.VolleyUtils;

public class LoginWPCom extends LoginAbstract {

    private String mTwoStepCode;
    private boolean mShouldSendTwoStepSMS;
    private Blog mJetpackBlog;

    public LoginWPCom(String username, String password, String twoStepCode, boolean shouldSendTwoStepSMS, Blog blog) {
        super(username, password);
        mTwoStepCode = twoStepCode;
        mShouldSendTwoStepSMS = shouldSendTwoStepSMS;
        mJetpackBlog = blog;
    }

    public static int restLoginErrorToMsgId(JSONObject errorObject) {
        // Default to generic error message
        int errorMsgId = org.wordpress.android.R.string.nux_cannot_log_in;

        // Map REST errors to local error codes
        if (errorObject != null) {
            try {
                String error = errorObject.optString("error", "");
                String errorDescription = errorObject.getString("error_description");
                if (error.equals("invalid_request")) {
                    if (errorDescription.contains("Incorrect username or password.")) {
                        errorMsgId = org.wordpress.android.R.string.username_or_password_incorrect;
                    }
                } else if (error.equals("needs_2fa")) {
                    errorMsgId = org.wordpress.android.R.string.account_two_step_auth_enabled;
                } else if (error.equals("invalid_otp")) {
                    errorMsgId = org.wordpress.android.R.string.invalid_verification_code;
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
        oauthRequest = oauth.makeRequest(username, password, mTwoStepCode, mShouldSendTwoStepSMS, listener, errorListener);
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

                if (mJetpackBlog != null) {
                    // Store token in blog object for Jetpack sites
                    mJetpackBlog.setApi_key(token.toString());
                    WordPress.wpDB.saveBlog(mJetpackBlog);
                }

                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
                if (mJetpackBlog == null || TextUtils.isEmpty(settings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null))) {
                    // Store token in global account
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString(WordPress.WPCOM_USERNAME_PREFERENCE, mUsername);
                    editor.putString(WordPress.ACCESS_TOKEN_PREFERENCE, token.toString());
                    editor.commit();
                }

                mCallback.onSuccess();
            }
        }, new Oauth.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                JSONObject errorObject = VolleyUtils.volleyErrorToJSON(volleyError);
                int errorMsgId = restLoginErrorToMsgId(errorObject);
                mCallback.onError(errorMsgId, errorMsgId == R.string.account_two_step_auth_enabled, false, false);
            }
        }));
    }
}
