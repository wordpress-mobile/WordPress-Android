package org.wordpress.android.editor;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import org.wordpress.android.editor.gutenberg.GutenbergWebViewAuthorizationData;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;
import org.wordpress.mobile.ReactNativeGutenbergBridge.GutenbergWebViewActivity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class WPGutenbergWebViewActivity extends GutenbergWebViewActivity {
    public static final String ENCODING_UTF8 = "UTF-8";
    public static final String WPCOM_LOGIN_URL = "https://wordpress.com/wp-login.php";

    public static final String ARG_GUTENBERG_WEB_VIEW_AUTH_DATA = "param_gutenberg_web_view_auth_data";

    public static final String ARG_BLOCK_ID = "block_id";
    public static final String ARG_BLOCK_NAME = "block_name";
    public static final String ARG_BLOCK_CONTENT = "block_content";

    private boolean mIsJetpackSsoEnabled;
    private String mUrlToLoad;

    @Override
    protected void loadUrl() {
        if (getIntent() != null
                && getIntent().getExtras() != null) {
            Bundle bundle = getIntent().getExtras();

            GutenbergWebViewAuthorizationData gutenbergWebViewAuthorizationData =
                    bundle.getParcelable(ARG_GUTENBERG_WEB_VIEW_AUTH_DATA);

            String siteUrl = gutenbergWebViewAuthorizationData.getSiteUrl();
            String urlToLoad = siteUrl + "/wp-admin/post-new.php";

            boolean isSitePrivate = !gutenbergWebViewAuthorizationData.isWPCom();
            String authenticationUrl = isSitePrivate ? siteUrl + "/wp-login.php" : WPCOM_LOGIN_URL;
            String userAgent = gutenbergWebViewAuthorizationData.getWordPressUserAgent();

            // Request to login.php needs to carry the “User-Agent” along with the headers.
            // If this is not the case, this request might be blocked by the backend.
            mWebView.getSettings().setUserAgentString(userAgent);

            String username = gutenbergWebViewAuthorizationData.getWPComAccountUsername();
            String password = gutenbergWebViewAuthorizationData.getWPComAccountPassword();
            String token = gutenbergWebViewAuthorizationData.getWPComAccountToken();

            if (isSitePrivate && gutenbergWebViewAuthorizationData.isJetpackSsoEnabled()) {
                mIsJetpackSsoEnabled = true;
                mUrlToLoad = urlToLoad;
                loadAuthenticatedUrl(WPCOM_LOGIN_URL, "", username, password, token);
                return;
            }

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
    private void loadAuthenticatedUrl(String authenticationURL,
                                        String urlToLoad,
                                        String username,
                                        String password,
                                        String token) {
        String postData = getAuthenticationPostData(authenticationURL, urlToLoad, username, password,
                token);

        mWebView.postUrl(authenticationURL, postData.getBytes());
    }

    private String getAuthenticationPostData(String authenticationUrl,
                                                   String urlToLoad,
                                                   String username,
                                                   String password,
                                                   String token) {
        if (TextUtils.isEmpty(authenticationUrl)) {
            return "";
        }

        try {
            String postData = String.format(
                    "log=%s&pwd=%s",
                    URLEncoder.encode(StringUtils.notNullStr(username), ENCODING_UTF8),
                    URLEncoder.encode(StringUtils.notNullStr(password), ENCODING_UTF8)
            );

            // Add url to load if needed
            if (!TextUtils.isEmpty(urlToLoad)) {
                postData += "&redirect_to=" + URLEncoder.encode(urlToLoad, ENCODING_UTF8);
            }

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

    @Override
    protected boolean isJetpackSsoEnabled() {
        return mIsJetpackSsoEnabled;
    }

    @Override
    protected String urlToLoad() {
        return mUrlToLoad;
    }
}
