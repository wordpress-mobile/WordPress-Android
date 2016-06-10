package org.wordpress.android.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.WPMeShortlinks;
import org.wordpress.android.util.WPUrlUtils;
import org.wordpress.android.util.WPWebViewClient;
import org.wordpress.android.util.helpers.WPWebChromeClient;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Activity for opening external WordPress links in a webview.
 *
 * Try to use one of the methods below to open the webview:
 * - openURL
 * - openUrlByUsingMainWPCOMCredentials
 * - openUrlByUsingWPCOMCredentials
 * - openUrlByUsingBlogCredentials (for self hosted sites)
 *
 * If you need to start the activity with delay, start activity with result, or none of the methods above are enough for your needs,
 * you can start the activity by passing the required parameters, depending on what you need to do.
 *
 * 1. Load a simple URL (without any kind of authentication)
 * - Start the activity with the parameter URL_TO_LOAD set to the URL to load.
 *
 * 2. Load a WordPress.com URL
 * Start the activity with the following parameters:
 * - URL_TO_LOAD: target URL to load in the webview.
 * - AUTHENTICATION_URL: The address of the WordPress.com authentication endpoint. Please use WPCOM_LOGIN_URL.
 * - AUTHENTICATION_USER: username.
 * - AUTHENTICATION_PASSWD: password.
 *
 * 3. Load a WordPress.org URL with authentication
 * - URL_TO_LOAD: target URL to load in the webview.
 * - AUTHENTICATION_URL: The address of the authentication endpoint. Please use the value of getBlogLoginUrl()
 * to retrieve the correct address of the authentication endpoint.
 * - AUTHENTICATION_USER: username.
 * - AUTHENTICATION_PASSWD: password.
 * - LOCAL_BLOG_ID: local id of the blog in the app database. This is required since some blogs could have HTTP Auth,
 * or self-signed certs in place.
 * - REFERRER_URL: url to add as an HTTP referrer header, currently only used for non-authed reader posts
 *
 */
public class WPWebViewActivity extends WebViewActivity {
    private static final String NON_AMP_URL = "non_amp_url";

    public static final String AUTHENTICATION_URL = "authenticated_url";
    public static final String AUTHENTICATION_USER = "authenticated_user";
    public static final String AUTHENTICATION_PASSWD = "authenticated_passwd";
    public static final String URL_TO_LOAD = "url_to_load";
    public static final String WPCOM_LOGIN_URL = "https://wordpress.com/wp-login.php";
    public static final String LOCAL_BLOG_ID = "local_blog_id";
    public static final String SHARABLE_URL = "sharable_url";
    public static final String REFERRER_URL = "referrer_url";
    public static final String USE_AMP_IF_ENABLED = "use_amp_if_enabled";

    private static final String ENCODING_UTF8 = "UTF-8";

    private String mNonAmpUrl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mNonAmpUrl = savedInstanceState.getString(NON_AMP_URL);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(NON_AMP_URL, mNonAmpUrl);
    }

    private static boolean isAmpCapableServer(String addressToLoad) {
        // add more AMP-capable servers here
        return WPUrlUtils.isWordPressCom(addressToLoad);
    }

    public static void openUrlByUsingWPCOMCredentials(Context context, String url, String user) {
        openWPCOMURL(context, url, user);
    }

    public static void openUrlByUsingBlogCredentials(Context context, Blog blog, Post post, String url) {
        if (context == null) {
            AppLog.e(AppLog.T.UTILS, "Context is null");
            return;
        }

        if (blog == null) {
            AppLog.e(AppLog.T.UTILS, "Blog obj is null");
            return;
        }

        if (TextUtils.isEmpty(url)) {
            AppLog.e(AppLog.T.UTILS, "Empty or null URL");
            Toast.makeText(context, context.getResources().getText(R.string.invalid_site_url_message),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String authURL = WPWebViewActivity.getBlogLoginUrl(blog);
        Intent intent = new Intent(context, WPWebViewActivity.class);
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_USER, blog.getUsername());
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_PASSWD, blog.getPassword());
        intent.putExtra(WPWebViewActivity.URL_TO_LOAD, url);
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_URL, authURL);
        intent.putExtra(WPWebViewActivity.LOCAL_BLOG_ID, blog.getLocalTableBlogId());
        if (post != null) {
            intent.putExtra(WPWebViewActivity.SHARABLE_URL, WPMeShortlinks.getPostShortlink(blog, post));

            if (!post.isPage() && isAmpCapableServer(url)) {
                intent.putExtra(WPWebViewActivity.USE_AMP_IF_ENABLED, true);
            }
        }
        context.startActivity(intent);
    }

    public static void openURL(Context context, String url) {
        openURL(context, url, null);
    }
    public static void openURL(Context context, String url, String referrer) {
        if (context == null) {
            AppLog.e(AppLog.T.UTILS, "Context is null");
            return;
        }

        if (TextUtils.isEmpty(url)) {
            AppLog.e(AppLog.T.UTILS, "Empty or null URL");
            Toast.makeText(context, context.getResources().getText(R.string.invalid_site_url_message),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(context, WPWebViewActivity.class);
        intent.putExtra(WPWebViewActivity.URL_TO_LOAD, url);
        if (!TextUtils.isEmpty(referrer)) {
            intent.putExtra(REFERRER_URL, referrer);
        }
        context.startActivity(intent);
    }

    private static void openWPCOMURL(Context context, String url, String user) {
        if (context == null) {
            AppLog.e(AppLog.T.UTILS, "Context is null");
            return;
        }

        if (TextUtils.isEmpty(url)) {
            AppLog.e(AppLog.T.UTILS, "Empty or null URL passed to openUrlByUsingMainWPCOMCredentials");
            Toast.makeText(context, context.getResources().getText(R.string.invalid_site_url_message),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(user)) {
            AppLog.e(AppLog.T.UTILS, "Username empty/null");
            return;
        }

        Intent intent = new Intent(context, WPWebViewActivity.class);
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_USER, user);
        intent.putExtra(WPWebViewActivity.URL_TO_LOAD, url);
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_URL, WPCOM_LOGIN_URL);
        context.startActivity(intent);
    }


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void configureWebView() {
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);

        WebViewClient webViewClient;

        if (getIntent().hasExtra(LOCAL_BLOG_ID)) {
            Blog blog = WordPress.getBlog(getIntent().getIntExtra(LOCAL_BLOG_ID, -1));
            if (blog == null) {
                AppLog.e(AppLog.T.UTILS, "No valid blog passed to WPWebViewActivity");
                finish();
            }
            webViewClient = new WPWebViewClient(blog);
        } else {
            webViewClient = new WebViewClient();
        }

        mWebView.setWebViewClient(webViewClient);
        mWebView.setWebChromeClient(new WPWebChromeClient(this, (ProgressBar) findViewById(R.id.progress_bar)));
    }

    @Override
    protected void loadContent() {
        Bundle extras = getIntent().getExtras();

        if (extras == null) {
            AppLog.e(AppLog.T.UTILS, "No valid parameters passed to WPWebViewActivity");
            finish();
            return;
        }

        String addressToLoad = extras.getString(URL_TO_LOAD);
        String username = extras.getString(AUTHENTICATION_USER, "");
        String password = extras.getString(AUTHENTICATION_PASSWD, "");
        String authURL = extras.getString(AUTHENTICATION_URL);
        boolean useAmpIfEnabled = extras.getBoolean(USE_AMP_IF_ENABLED);

        if (TextUtils.isEmpty(addressToLoad) || !UrlUtils.isValidUrlAndHostNotNull(addressToLoad)) {
            AppLog.e(AppLog.T.UTILS, "Empty or null or invalid URL passed to WPWebViewActivity");
            Toast.makeText(this, getText(R.string.invalid_site_url_message),
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        mNonAmpUrl = null;

        if (useAmpIfEnabled) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            final boolean isAmpEnabled = prefs.getBoolean(getString(R.string.pref_key_amp_enabled), false);

            if (isAmpEnabled) {
                mNonAmpUrl = addressToLoad;

                addressToLoad = Uri.parse(addressToLoad).buildUpon().appendPath("amp").build().toString();
                AppLog.v(AppLog.T.POSTS, "Using AMP to open " + addressToLoad);
                AnalyticsTracker.track(AnalyticsTracker.Stat.AMP_POST_OPEN);
            }
        }

        if (TextUtils.isEmpty(authURL) && TextUtils.isEmpty(username) && TextUtils.isEmpty(password)) {
            // Only the URL to load is passed to this activity. Use the normal un-authenticated
            // loader, optionally with our referrer header
            String referrerUrl = extras.getString(REFERRER_URL);
            if (!TextUtils.isEmpty(referrerUrl)) {
                Map<String, String> headers = new HashMap<>();
                headers.put("Referer", referrerUrl);
                loadUrl(addressToLoad, headers);
            } else {
                loadUrl(addressToLoad);
            }
        } else {
            if (TextUtils.isEmpty(authURL) || !UrlUtils.isValidUrlAndHostNotNull(authURL)) {
                AppLog.e(AppLog.T.UTILS, "Empty or null or invalid auth URL passed to WPWebViewActivity");
                Toast.makeText(this, getText(R.string.invalid_site_url_message),
                        Toast.LENGTH_SHORT).show();
                finish();
            }

            if (TextUtils.isEmpty(username)) {
                AppLog.e(AppLog.T.UTILS, "Username empty/null");
                Toast.makeText(this, getText(R.string.incorrect_credentials), Toast.LENGTH_SHORT).show();
                finish();
            }

            loadAuthenticatedUrl(authURL, addressToLoad, username, password);
        }
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
    }

    /**
     * Login to the WordPress.com and load the specified URL.
     *
     */
    protected void loadAuthenticatedUrl(String authenticationURL, String urlToLoad, String username, String password) {
        String postData = getAuthenticationPostData(authenticationURL, urlToLoad, username, password,
                AccountHelper.getDefaultAccount().getAccessToken());

        mWebView.postUrl(authenticationURL, postData.getBytes());
    }

    public static String getAuthenticationPostData(String authenticationUrl, String urlToLoad, String username, String password, String token) {
        if (TextUtils.isEmpty(authenticationUrl)) return "";

        try {
            String postData = String.format("log=%s&pwd=%s&redirect_to=%s",
                    URLEncoder.encode(StringUtils.notNullStr(username), ENCODING_UTF8),
                    URLEncoder.encode(StringUtils.notNullStr(password), ENCODING_UTF8),
                    URLEncoder.encode(StringUtils.notNullStr(urlToLoad), ENCODING_UTF8)
            );

            // Add token authorization when signing in to WP.com
            if (WPUrlUtils.safeToAddWordPressComAuthToken(authenticationUrl)
                    && authenticationUrl.contains("wordpress.com/wp-login.php") && !TextUtils.isEmpty(token)) {
                postData += "&authorization=Bearer " + URLEncoder.encode(token, ENCODING_UTF8);
            }

            return postData;
        } catch (UnsupportedEncodingException e) {
            AppLog.e(AppLog.T.UTILS, e);
        }

        return "";
    }

    /**
     * Get the URL of the WordPress login page.
     *
     * @return URL of the login page.
     */
    public static String getBlogLoginUrl(Blog blog) {
        String loginURL = null;
        Gson gson = new Gson();
        Type type = new TypeToken<Map<?, ?>>() {}.getType();
        Map<?, ?> blogOptions = gson.fromJson(blog.getBlogOptions(), type);
        if (blogOptions != null) {
            Map<?, ?> homeURLMap = (Map<?, ?>) blogOptions.get("login_url");
            if (homeURLMap != null) {
                loginURL = homeURLMap.get("value").toString();
            }
        }
        // Try to guess the login URL if blogOptions is null (blog not added to the app), or WP version is < 3.6
        if (loginURL == null) {
            if (blog.getUrl().lastIndexOf("/") != -1) {
                return blog.getUrl().substring(0, blog.getUrl().lastIndexOf("/"))
                        + "/wp-login.php";
            } else {
                return blog.getUrl().replace("xmlrpc.php", "wp-login.php");
            }
        }

        return loginURL;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.webview, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem noAmpOpen = menu.findItem(R.id.menu_no_amp);
        if (noAmpOpen != null) {
            noAmpOpen.setVisible(mNonAmpUrl != null);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (mWebView == null) {
            return false;
        }

        int itemID = item.getItemId();
        if (itemID == R.id.menu_refresh) {
            mWebView.reload();
            return true;
        } else if (itemID == R.id.menu_share) {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            // Use the preferred sharable URL or the default webview URL
            Bundle extras = getIntent().getExtras();
            String sharableUrl = extras.getString(SHARABLE_URL, null);
            if (sharableUrl == null) {
                sharableUrl = mWebView.getUrl();
            }
            share.putExtra(Intent.EXTRA_TEXT, sharableUrl);
            startActivity(Intent.createChooser(share, getText(R.string.share_link)));
            return true;
        } else if (itemID == R.id.menu_browser) {
            ReaderActivityLauncher.openUrl(this, mWebView.getUrl(), ReaderActivityLauncher.OpenUrlType.EXTERNAL);
            return true;
        } else if (itemID == R.id.menu_no_amp) {
            ReaderActivityLauncher.openUrl(this, mNonAmpUrl, ReaderActivityLauncher.OpenUrlType.EXTERNAL);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
