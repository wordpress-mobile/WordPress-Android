package org.wordpress.android.editor;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;

import org.wordpress.android.editor.gutenberg.GutenbergWebViewAuthorizationData;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;
import org.wordpress.mobile.ReactNativeGutenbergBridge.GutenbergWebViewActivity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

public class WPGutenbergWebViewActivity extends GutenbergWebViewActivity {
    public static final String ENCODING_UTF8 = "UTF-8";
    public static final String WPCOM_LOGIN_URL = "https://wordpress.com/wp-login.php";

    public static final String ARG_GUTENBERG_WEB_VIEW_AUTH_DATA = "param_gutenberg_web_view_auth_data";

    public static final String ARG_USER_ID = "authenticated_user_id";
    public static final String ARG_BLOCK_ID = "block_id";
    public static final String ARG_BLOCK_TITLE = "block_title";
    public static final String ARG_BLOCK_CONTENT = "block_content";

    private boolean mIsJetpackSsoEnabled;
    private boolean mIsJetpackSsoRedirected;
    private String mUrlToLoad;
    private long mUserId;

    @Override
    protected void loadUrl() {
        if (getIntent() != null
                && getIntent().getExtras() != null) {
            Bundle bundle = getIntent().getExtras();

            GutenbergWebViewAuthorizationData gutenbergWebViewAuthorizationData =
                    bundle.getParcelable(ARG_GUTENBERG_WEB_VIEW_AUTH_DATA);

            String siteUrl = gutenbergWebViewAuthorizationData.getSiteUrl();
            String urlToLoad = siteUrl + "/wp-admin/post-new.php";

            boolean isSelfHosted = !gutenbergWebViewAuthorizationData.isWPCom();
            String authenticationUrl = isSelfHosted ? siteUrl + "/wp-login.php" : WPCOM_LOGIN_URL;
            String userAgent = gutenbergWebViewAuthorizationData.getWordPressUserAgent();

            // Request to login.php needs to carry the “User-Agent” along with the headers.
            // If this is not the case, this request might be blocked by the backend.
            mWebView.getSettings().setUserAgentString(userAgent);

            String username = gutenbergWebViewAuthorizationData.getWPComAccountUsername();
            String password = gutenbergWebViewAuthorizationData.getWPComAccountPassword();
            String token = gutenbergWebViewAuthorizationData.getWPComAccountToken();

            mUserId = isSelfHosted ? gutenbergWebViewAuthorizationData.getSelfHostedSiteId()
                    : gutenbergWebViewAuthorizationData.getWPComAccountUserId();

            if (isSelfHosted && gutenbergWebViewAuthorizationData.isJetpackSsoEnabled()) {
                mIsJetpackSsoEnabled = true;
                mUrlToLoad = urlToLoad;
                mUserId = gutenbergWebViewAuthorizationData.getWPComAccountUserId();
                loadAuthenticatedUrl(WPCOM_LOGIN_URL, "", username, password, token);
                return;
            }

            loadAuthenticatedUrl(authenticationUrl, urlToLoad, username, password, token);
        }
    }

    @Override
    protected String getToolbarTitle() {
        String blockTitle = getIntent().getExtras().getString(ARG_BLOCK_TITLE);
        if (blockTitle != null) {
            return String.format(getString(R.string.menu_toolbar_title), blockTitle);
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
    protected String getOnGutenbergReadyExternalStyles() {
        String injectExternalCssScript = getFileContentFromAssets("external-style-overrides.css");
        return injectExternalCssScript;
    }

    @Override
    protected List<String> getOnGutenbergReadyExternalSources() {
        String file = getFileContentFromAssets("remove-nux.js");
        return Arrays.asList(file);
    }

    @Override protected List<String> getOnPageLoadExternalSources() {
        long userId = getIntent().getExtras().getLong(ARG_USER_ID, 0);
        String file = getFileContentFromAssets("extra-localstorage-entries.js")
                .replace("%@", Long.toString(userId));
        return Arrays.asList(file);
    }

    protected boolean isUrlOverridden(WebView view, String url) {
        if (mIsJetpackSsoEnabled) {
            if (!mIsJetpackSsoRedirected) {
                mForegroundView.setVisibility(View.VISIBLE);
                mIsJetpackSsoRedirected = true;
                view.loadUrl(mUrlToLoad);
                return true;
            }

            if (url.contains(mUrlToLoad)) {
                mForegroundView.setVisibility(View.VISIBLE);
                mIsRedirected = true;
            } else {
                mForegroundView.setVisibility(View.INVISIBLE);
            }

            return false;
        } else {
            return super.isUrlOverridden(view, url);
        }
    }

    @Override public long getUserId() {
        return mUserId;
    }
}
