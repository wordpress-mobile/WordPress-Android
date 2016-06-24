package org.wordpress.android.ui.accounts.helpers;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.LanguageUtils;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.ui.accounts.AbstractFragment.ErrorListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.xmlpull.v1.XmlPullParser;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class CreateUserAndBlog {
    public static final int WORDPRESS_COM_API_BLOG_VISIBILITY_PUBLIC = 1;
    public static final int WORDPRESS_COM_API_BLOG_VISIBILITY_BLOCK_SEARCH_ENGINE = 0;
    public static final int WORDPRESS_COM_API_BLOG_VISIBILITY_PRIVATE = -1;
    private String mEmail;
    private String mUsername;
    private String mPassword;
    private String mSiteUrl;
    private String mSiteName;
    private String mLanguage;
    private Context mContext;
    private Callback mCallback;
    private ErrorListener mErrorListener;
    private RestClientUtils mRestClient;
    private ResponseHandler mResponseHandler;

    public CreateUserAndBlog(String email, String username, String password, String siteUrl, String siteName,
                             String language, RestClientUtils restClient, Context context,
                             ErrorListener errorListener, Callback callback) {
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
        mResponseHandler = new ResponseHandler();
    }

    public static String getDeviceLanguage(Context context) {
        Resources resources = context.getResources();
        XmlResourceParser parser = resources.getXml(R.xml.wpcom_languages);
        Hashtable<String, String> entries = new Hashtable<String, String>();
        String matchedDeviceLanguage = "en - English";
        try {
            int eventType = parser.getEventType();
            String deviceLanguageCode = LanguageUtils.getPatchedCurrentDeviceLanguage(context);

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    if (name.equals("language")) {
                        String currentID = null;
                        boolean currentLangIsDeviceLanguage = false;
                        int i = 0;
                        while (i < parser.getAttributeCount()) {
                            if (parser.getAttributeName(i).equals("id")) {
                                currentID = parser.getAttributeValue(i);
                            }
                            if (parser.getAttributeName(i).equals("code") &&
                                parser.getAttributeValue(i).equalsIgnoreCase(deviceLanguageCode)) {
                                currentLangIsDeviceLanguage = true;
                            }
                            i++;
                        }

                        while (eventType != XmlPullParser.END_TAG) {
                            if (eventType == XmlPullParser.TEXT) {
                                entries.put(parser.getText(), currentID);
                                if (currentLangIsDeviceLanguage) {
                                    matchedDeviceLanguage = parser.getText();
                                }
                            }
                            eventType = parser.next();
                        }
                    }
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            // do nothing
        }
        return matchedDeviceLanguage;
    }

    public void startCreateUserAndBlogProcess() {
        validateUser();
    }

    public void startCreateBlogProcess() {
        mResponseHandler.setMode(Mode.CREATE_BLOG_ONLY);
        validateSite();
    }

    private void validateUser() {
        String path = "users/new";
        Map<String, String> params = new HashMap<String, String>();
        params.put("username", mUsername);
        params.put("password", mPassword);
        params.put("email", mEmail);
        params.put("validate", "1");
        params.put("client_id", BuildConfig.OAUTH_APP_ID);
        params.put("client_secret", BuildConfig.OAUTH_APP_SECRET);
        mResponseHandler.setStep(Step.VALIDATE_USER);
        mRestClient.post(path, params, null, mResponseHandler, mErrorListener);
    }

    private void validateSite() {
        String path = "sites/new";
        Map<String, String> params = new HashMap<String, String>();
        params.put("blog_name", mSiteUrl);
        params.put("blog_title", mSiteName);
        params.put("lang_id", mLanguage);
        params.put("public", String.valueOf(WORDPRESS_COM_API_BLOG_VISIBILITY_PUBLIC));
        params.put("validate", "1");
        params.put("client_id", BuildConfig.OAUTH_APP_ID);
        params.put("client_secret", BuildConfig.OAUTH_APP_SECRET);
        mResponseHandler.setStep(Step.VALIDATE_SITE);
        mRestClient.post(path, params, null, mResponseHandler, mErrorListener);
    }

    private void createUser() {
        String path = "users/new";
        Map<String, String> params = new HashMap<String, String>();
        params.put("username", mUsername);
        params.put("password", mPassword);
        params.put("email", mEmail);
        params.put("validate", "0");
        params.put("client_id", BuildConfig.OAUTH_APP_ID);
        params.put("client_secret", BuildConfig.OAUTH_APP_SECRET);
        mResponseHandler.setStep(Step.CREATE_USER);
        mRestClient.post(path, params, null, mResponseHandler, mErrorListener);
    }

    private void authenticateUser() {
        LoginWPCom login = new LoginWPCom(mUsername, mPassword, null, false, null);
        login.execute(new LoginAbstract.Callback() {
            @Override
            public void onSuccess() {
                try {
                    mResponseHandler.nextStep(new JSONObject("{\"success\":true}"));
                } catch (JSONException e) {
                    AppLog.e(T.API, "Could not parse JSON in new user setup");
                }
            }

            @Override
            public void onError(int errorMessageId, boolean twoStepCodeRequired, boolean httpAuthRequired, boolean erroneousSslCertificate) {
                mErrorListener.onErrorResponse(new VolleyError("Sign in failed."));
            }
        });

        mResponseHandler.setStep(Step.AUTHENTICATE_USER);
    }

    private void createBlog() {
        String path = "sites/new";
        Map<String, String> params = new HashMap<String, String>();
        params.put("blog_name", mSiteUrl);
        params.put("blog_title", mSiteName);
        params.put("lang_id", mLanguage);
        params.put("public", String.valueOf(WORDPRESS_COM_API_BLOG_VISIBILITY_PUBLIC));
        params.put("validate", "0");
        params.put("client_id", BuildConfig.OAUTH_APP_ID);
        params.put("client_secret", BuildConfig.OAUTH_APP_SECRET);
        mResponseHandler.setStep(Step.CREATE_SITE);
        WordPress.getRestClientUtils().post(path, params, null, mResponseHandler, mErrorListener);
    }

    public enum Step {
        VALIDATE_USER, VALIDATE_SITE, CREATE_USER, AUTHENTICATE_USER, CREATE_SITE
    }

    private enum Mode {CREATE_USER_AND_BLOG, CREATE_BLOG_ONLY}

    public interface Callback {
        void onStepFinished(Step step);

        void onSuccess(JSONObject createSiteResponse);

        void onError(int messageId);
    }

    private class ResponseHandler implements RestRequest.Listener {
        public ResponseHandler() {
            super();
        }

        public void setMode(Mode mode) {
            mMode = mode;
        }

        private Step mStep = Step.VALIDATE_USER;

        public void setStep(Step step) {
            mStep = step;
        }

        private void nextStep(JSONObject response) {
            try {
                if (mStep == Step.AUTHENTICATE_USER) {
                    mCallback.onStepFinished(Step.AUTHENTICATE_USER);
                    createBlog();
                } else {
                    // steps VALIDATE_USER and VALIDATE_SITE could be run simultaneously in
                    // CREATE_USER_AND_BLOG mode
                    if (response.getBoolean("success")) {
                        switch (mStep) {
                            case VALIDATE_USER:
                                mCallback.onStepFinished(Step.VALIDATE_USER);
                                validateSite();
                                break;
                            case VALIDATE_SITE:
                                mCallback.onStepFinished(Step.VALIDATE_SITE);
                                if (mMode == Mode.CREATE_BLOG_ONLY) {
                                    createBlog();
                                } else {
                                    createUser();
                                }
                                break;
                            case CREATE_USER:
                                mCallback.onStepFinished(Step.CREATE_USER);
                                authenticateUser();
                                break;
                            case CREATE_SITE:
                                mCallback.onStepFinished(Step.CREATE_SITE);
                                mCallback.onSuccess(response);
                                break;
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

        private Mode mMode = Mode.CREATE_USER_AND_BLOG;

        @Override
        public void onResponse(JSONObject response) {
            AppLog.d(T.NUX, String.format("Create Account step %s", mStep.name()));
            nextStep(response);
        }
    }
}
