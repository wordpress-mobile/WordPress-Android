package org.wordpress.android.editor;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.helpers.WPWebChromeClient;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


public class GutenbergWebViewActivity extends AppCompatActivity {

    public static final String ENCODING_UTF8 = "UTF-8";
    public static final String WPCOM_LOGIN_URL = "https://wordpress.com/wp-login.php";

    public static final String ARG_USER_ID = "authenticated_user_id";
    public static final String ARG_AUTHENTICATION_USER = "authenticated_user";
    public static final String ARG_AUTHENTICATION_PASSWD = "authenticated_passwd";
    public static final String ARG_AUTHENTICATION_TOKEN = "authenticated_token";
    public static final String ARG_URL_TO_LOAD = "url_to_load";
    public static final String ARG_IS_SITE_PRIVATE = "is_site_private";

    public static final String ARG_BLOCK_ID = "block_id";
    public static final String ARG_BLOCK_NAME = "block_name";
    public static final String ARG_BLOCK_CONTENT = "block_content";

    public static final String RESULT_SAVED_CONTENT = "saved_content";
    public static final String RESULT_BLOCK_ID = "block_id";

    private static final String INJECT_LOCAL_STORAGE_SCRIPT_TEMPLATE = "localStorage.setItem('WP_DATA_USER_%d','%s')";
    private static final String INJECT_GET_HTML_POST_CONTENT_SCRIPT = "window.getHTMLPostContent();";
    private static final String JAVA_SCRIPT_INTERFACE_NAME = "wpwebkit";

    private WebView mWebView;

    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gutenberg_web_view);

        setupToolbar();

        mWebView = findViewById(R.id.gutenberg_web_view);

        // Set settings
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptThirdPartyCookies(mWebView, true);

        // Add javascript interface
        mWebView.addJavascriptInterface(new WPWebKit(), JAVA_SCRIPT_INTERFACE_NAME);

        // Setup WebView client
        setupWebViewClient();
        mWebView.setWebChromeClient(new WPWebChromeClient(null, findViewById(R.id.progress_bar)));

        if (getIntent() != null
            && getIntent().getExtras() != null) {
            String siteUrl = getIntent().getExtras().getString(ARG_URL_TO_LOAD);
            String urlToLoad = siteUrl + "/wp-admin/post-new.php";
            String username = getIntent().getExtras().getString(ARG_AUTHENTICATION_USER);
            String password = getIntent().getExtras().getString(ARG_AUTHENTICATION_PASSWD);
            String token = getIntent().getExtras().getString(ARG_AUTHENTICATION_TOKEN);
            boolean isSitePrivate = getIntent().getExtras().getBoolean(ARG_IS_SITE_PRIVATE, false);
            String authenticationUrl = isSitePrivate ? siteUrl + "/wp-login.php" : WPCOM_LOGIN_URL;

            loadAuthenticatedUrl(authenticationUrl, urlToLoad, username, password, token);
        }
    }

    private void setupToolbar() {
        setTitle("");

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setSubtitle("");
                String blockName = getIntent().getExtras().getString(ARG_BLOCK_NAME);
                if (blockName != null) {
                    actionBar.setTitle(String.format(getString(R.string.menu_toolbar_title), blockName));
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_gutenber_webview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (mWebView == null) {
            return false;
        }

        int itemID = item.getItemId();

        if (itemID == android.R.id.home) {
            finish();
        } else if (itemID == R.id.menu_save) {
            saveAction();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveAction() {
        mWebView.clearFocus();
        mWebView.evaluateJavascript(INJECT_GET_HTML_POST_CONTENT_SCRIPT,
                value -> AppLog.e(AppLog.T.EDITOR, value));
    }

    private void saveContent(String content) {
        Intent savedContentIntent = new Intent();
        String blockId = getIntent().getExtras().getString(ARG_BLOCK_ID);
        savedContentIntent.putExtra(ARG_BLOCK_ID, blockId);
        savedContentIntent.putExtra(RESULT_SAVED_CONTENT, content);
        setResult(RESULT_OK, savedContentIntent);
        finish();
    }

    private void setupWebViewClient() {
        mWebView.setWebViewClient(new WebViewClient() {

            private boolean mIsRedirected;

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Set if page is redirected
                if (!mIsRedirected) {
                    mIsRedirected = true;
                }

                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                String contentFunctions = Utils.getHtmlFromFile(GutenbergWebViewActivity.this, "gutenberg/content-functions.js");
                evaluateJavaScript(contentFunctions);

                String injectLocalStorageScript = Utils.getHtmlFromFile(GutenbergWebViewActivity.this, "gutenberg/local-storage-overrides.json");
                String trimmed = injectLocalStorageScript.replace("\n", "").replace("\r", "").replaceAll("\\s+","");
                long userId = getIntent().getExtras().getLong(ARG_USER_ID);
                evaluateJavaScript(String.format(INJECT_LOCAL_STORAGE_SCRIPT_TEMPLATE, userId, trimmed));

                super.onPageCommitVisible(view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!mIsRedirected) {
                    final Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        String preventAutosaves = Utils.getHtmlFromFile(GutenbergWebViewActivity.this, "gutenberg/prevent-autosaves.js");
                        evaluateJavaScript(preventAutosaves);

                        String insertBlock = Utils.getHtmlFromFile(GutenbergWebViewActivity.this, "gutenberg/insert-block.js");
                        String content = getIntent().getExtras().getString(ARG_BLOCK_CONTENT).replace("\n", "").replace("\r", "");
                        String block = String.format(insertBlock, content);
                        evaluateJavaScript(block);
                    }, 2000);

                } else {
                    mIsRedirected = false;

                    String injectCssScript = Utils.getHtmlFromFile(GutenbergWebViewActivity.this, "gutenberg/inject-css.js");
                    evaluateJavaScript(injectCssScript);

                    String editorStyle = Utils.getHtmlFromFile(GutenbergWebViewActivity.this, "gutenberg/editor-style-overrides.css");
                    String trimmed = editorStyle.replace("\n", "").replace("\r", "").replaceAll("\\s+","");
                    evaluateJavaScript(String.format("window.injectCss('%s')", trimmed));

                    String injectWPBarsCssScript = Utils.getHtmlFromFile(GutenbergWebViewActivity.this, "gutenberg/wp-bar-override.css");
                    trimmed =  injectWPBarsCssScript.replace("\n", "").replace("\r", "").replaceAll("\\s+","");
                    evaluateJavaScript(String.format("window.injectCss('%s')", trimmed));
                }

                super.onPageFinished(view, url);
            }

            private void evaluateJavaScript(String script) {
                mWebView.evaluateJavascript(script, value ->
                    AppLog.e(AppLog.T.EDITOR, value));
            }
        });


    }

    /**
     * Login to the WordPress.com and load the specified URL.
     */
    protected void loadAuthenticatedUrl(String authenticationURL, String urlToLoad, String username, String password, String token) {
        String postData = getAuthenticationPostData(authenticationURL, urlToLoad, username, password,
                token);

        mWebView.postUrl(authenticationURL, postData.getBytes());
    }

    public static String getAuthenticationPostData(String authenticationUrl, String urlToLoad, String username,
                                                   String password, String token) {
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

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void finish() {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                mWebView.removeJavascriptInterface(JAVA_SCRIPT_INTERFACE_NAME);
                mWebView.clearHistory();
                mWebView.clearFormData();
                mWebView.clearCache(true);
            }
        });

        super.finish();
    }

    public class WPWebKit {

        @JavascriptInterface
        public void postMessage(String content) {
            if (content != null && content.length() > 0) {
                saveContent(content);
            }
        }
    }
}
