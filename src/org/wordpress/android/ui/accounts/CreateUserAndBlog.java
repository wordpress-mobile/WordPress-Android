package org.wordpress.android.ui.accounts;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.Config;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.WPRestClient;

import java.util.HashMap;
import java.util.Map;


public class CreateUserAndBlog {
    public enum Step {
        VALIDATE_USER, VALIDATE_SITE, CREATE_USER, AUTHENTICATE_USER, CREATE_SITE
    }

    final public static int WORDPRESS_COM_API_BLOG_VISIBILITY_PUBLIC = 0;
    final public static int WORDPRESS_COM_API_BLOG_VISIBILITY_PRIVATE = 1;
    final public static int WORDPRESS_COM_API_BLOG_VISIBILITY_HIDDEN = 2;

    private String mEmail;
    private String mUsername;
    private String mPassword;
    private String mSiteUrl;
    private String mSiteName;
    private String mLanguage;
    private Context mContext;
    private Callback mCallback;
    private NewAccountAbstractPageFragment.ErrorListener mErrorListener;
    private WPRestClient mRestClient;

    public interface Callback {
        void onStepFinished(Step step);
        void onSuccess();
        void onError(int messageId);
    }

    private class ResponseHandler implements RestRequest.Listener {
        private Step mCurrentStep = Step.VALIDATE_USER;

        public ResponseHandler(Step step) {
            super();
            mCurrentStep = step;
        }

        private void nextStep(JSONObject response) {
            try {
                if (mCurrentStep == Step.AUTHENTICATE_USER) {
                    mCallback.onStepFinished(Step.AUTHENTICATE_USER);
                    createBlog();
                } else {
                    // Note: steps VALIDATE_USER and VALIDATE_SITE could be run simultaneously
                    if (response.getBoolean("success")) {
                        switch (mCurrentStep) {
                            case VALIDATE_USER:
                                mCallback.onStepFinished(Step.VALIDATE_USER);
                                validateSite();
                                break;
                            case VALIDATE_SITE:
                                mCallback.onStepFinished(Step.VALIDATE_SITE);
                                createUser();
                                break;
                            case CREATE_USER:
                                mCallback.onStepFinished(Step.CREATE_USER);
                                authenticateUser();
                                break;
                            case CREATE_SITE:
                                mCallback.onStepFinished(Step.CREATE_SITE);
                                mCallback.onSuccess();
                                break;
                            case AUTHENTICATE_USER:
                            default:
                                break;
                        }
                    } else {
                        mCallback.onError(R.string.error_generic);
                    }
                }
            } catch (JSONException e) {
                mCallback.onError(R.string.error_generic);
            }
        }

        @Override
        public void onResponse(JSONObject response) {
            Log.d("REST Response", String.format("Create Account step %s", mCurrentStep.name()));
            Log.d("REST Response", String.format("OK %s", response.toString()));
            nextStep(response);
        }
    }

    public CreateUserAndBlog(String email, String username, String password, String siteUrl,
                             String siteName, String language, WPRestClient restClient,
                             Context context,
                             NewAccountAbstractPageFragment.ErrorListener errorListener,
                             Callback callback) {
        mEmail = email;
        mUsername = username;
        mPassword = password;
        mSiteUrl = siteUrl;
        mSiteName = siteName;
        mLanguage = language;
        mCallback = callback;
        mContext = context;
        mErrorListener = errorListener;
        mRestClient = restClient;
    }

    public void startCreateUserAndBlogProcess() {
        validateUser();
    }

    private void validateUser() {
        String path = "users/new";
        Map<String, String> params = new HashMap<String, String>();
        params.put("username", mUsername);
        params.put("password", mPassword);
        params.put("email", mEmail);
        params.put("validate", "1");
        params.put("client_id", Config.OAUTH_APP_ID);
        params.put("client_secret", Config.OAUTH_APP_SECRET);
        mRestClient.post(path, params, null,
                new ResponseHandler(Step.VALIDATE_USER), mErrorListener);
    }

    private void validateSite() {
        String path = "sites/new";
        Map<String, String> params = new HashMap<String, String>();
        params.put("blog_name", mSiteUrl);
        params.put("blog_title", mSiteName);
        params.put("lang_id", mLanguage);
        params.put("public", String.valueOf(WORDPRESS_COM_API_BLOG_VISIBILITY_PUBLIC));
        params.put("validate", "1");
        params.put("client_id", Config.OAUTH_APP_ID);
        params.put("client_secret", Config.OAUTH_APP_SECRET);
        mRestClient.post(path, params, null,
                new ResponseHandler(Step.VALIDATE_SITE), mErrorListener);
    }

    private void createUser() {
        String path = "users/new";
        Map<String, String> params = new HashMap<String, String>();
        params.put("username", mUsername);
        params.put("password", mPassword);
        params.put("email", mEmail);
        params.put("validate", "0");
        params.put("client_id", Config.OAUTH_APP_ID);
        params.put("client_secret", Config.OAUTH_APP_SECRET);
        mRestClient.post(path, params, null,
                new ResponseHandler(Step.CREATE_USER), mErrorListener);
    }

    private void authenticateUser() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(WordPress.WPCOM_USERNAME_PREFERENCE, mUsername);
        editor.putString(WordPress.WPCOM_PASSWORD_PREFERENCE, mPassword);
        editor.commit();
        // fire off a request to get an access token
        WordPress.restClient.get("me", new ResponseHandler(Step.AUTHENTICATE_USER),
                mErrorListener);
    }

    private void createBlog() {
        String path = "sites/new";
        Map<String, String> params = new HashMap<String, String>();
        params.put("blog_name", mSiteUrl);
        params.put("blog_title", mSiteName);
        params.put("lang_id", mLanguage);
        params.put("public", String.valueOf(WORDPRESS_COM_API_BLOG_VISIBILITY_PUBLIC));
        params.put("validate", "0");
        params.put("client_id", Config.OAUTH_APP_ID);
        params.put("client_secret", Config.OAUTH_APP_SECRET);
        WordPress.restClient.post(path, params, null,
                new ResponseHandler(Step.CREATE_SITE), mErrorListener);
    }
}
