package org.wordpress.android.editor;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.WebView;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;
import org.wordpress.mobile.ReactNativeGutenbergBridge.GutenbergWebViewActivity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class WPGutenbergWebViewActivity extends GutenbergWebViewActivity {
    public static final String ENCODING_UTF8 = "UTF-8";
    public static final String WPCOM_LOGIN_URL = "https://wordpress.com/wp-login.php";

    public static final String ARG_USER_ID = "authenticated_user_id";
    public static final String ARG_AUTHENTICATION_USER = "authenticated_user";
    public static final String ARG_AUTHENTICATION_PASSWD = "authenticated_passwd";
    public static final String ARG_AUTHENTICATION_TOKEN = "authenticated_token";
    public static final String ARG_URL_TO_LOAD = "url_to_load";
    public static final String ARG_IS_SITE_PRIVATE = "is_site_private";
    public static final String ARG_USER_AGENT= "user_agent";

    public static final String ARG_BLOCK_ID = "block_id";
    public static final String ARG_BLOCK_NAME = "block_name";
    public static final String ARG_BLOCK_CONTENT = "block_content";

    @Override
    protected void loadUrl() {
        if (getIntent() != null
                && getIntent().getExtras() != null) {

            Bundle bundle = getIntent().getExtras();

            WebView.setWebContentsDebuggingEnabled(true);

            String siteUrl = bundle.getString(ARG_URL_TO_LOAD);
            String urlToLoad = siteUrl + "/wp-admin/post-new.php";
            String username = bundle.getString(ARG_AUTHENTICATION_USER);
            String password = bundle.getString(ARG_AUTHENTICATION_PASSWD);
            String token = bundle.getString(ARG_AUTHENTICATION_TOKEN);
            boolean isSitePrivate = bundle.getBoolean(ARG_IS_SITE_PRIVATE, false);
            String authenticationUrl = isSitePrivate ? siteUrl + "/wp-login.php" : WPCOM_LOGIN_URL;
            String userAgent = bundle.getString(ARG_USER_AGENT);

            // Request to login.php needs to carry the “User-Agent” along with the headers.
            // If this is not the case, this request might be blocked by the backend.
            mWebView.getSettings().setUserAgentString(userAgent);

            loadAuthenticatedUrl(authenticationUrl, urlToLoad, username, password, token);
        }
    }

    @Override
    protected String getToolbarTitle() {
        String blockName = getIntent().getExtras().getString(ARG_BLOCK_NAME);
        if (blockName != null) {
            return String.format(getString(R.string.menu_toolbar_title), blockName);
        }
        return "";
    }

    @Override
    protected void saveContent(String content) {
        Intent savedContentIntent = new Intent();
        String blockId = getIntent().getExtras().getString(ARG_BLOCK_ID);
        savedContentIntent.putExtra(ARG_BLOCK_ID, blockId);
        savedContentIntent.putExtra(ARG_BLOCK_CONTENT, content);
        setResult(RESULT_OK, savedContentIntent);
        finish();
    }

    /**
     * Login to the WordPress.com and load the specified URL.
     */
    protected void loadAuthenticatedUrl(String authenticationURL,
                                        String urlToLoad,
                                        String username,
                                        String password,
                                        String token) {
        String postData = getAuthenticationPostData(authenticationURL, urlToLoad, username, password,
                token);

        mWebView.postUrl(authenticationURL, postData.getBytes());
    }

    public static String getAuthenticationPostData(String authenticationUrl,
                                                   String urlToLoad,
                                                   String username,
                                                   String password,
                                                   String token) {
        if (TextUtils.isEmpty(authenticationUrl)) {
            return "";
        }

        try {
            String postData = String.format(
                    "log=%s&pwd=%s&redirect_to=%s",
                    URLEncoder.encode(StringUtils.notNullStr(username), ENCODING_UTF8),
                    URLEncoder.encode(StringUtils.notNullStr(password), ENCODING_UTF8),
                    URLEncoder.encode(StringUtils.notNullStr(urlToLoad), ENCODING_UTF8)
            );

            // Add token authorization when signing in to WP.com
            if (authenticationUrl.contains("wordpress.com/wp-login.php") && !TextUtils.isEmpty(token)) {
                postData += "&authorization=Bearer " + URLEncoder.encode(token, ENCODING_UTF8);
            }

            return postData;
        } catch (UnsupportedEncodingException e) {
            AppLog.e(AppLog.T.UTILS, e);
        }

        return "";
    }
}
