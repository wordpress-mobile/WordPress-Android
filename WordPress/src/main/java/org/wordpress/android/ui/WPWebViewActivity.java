package org.wordpress.android.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.URLFilteredWebViewClient;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.WPUrlUtils;
import org.wordpress.android.util.WPWebViewClient;
import org.wordpress.android.util.helpers.WPWebChromeClient;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/**
 * Activity for opening external WordPress links in a webview.
 * <p/>
 * Try to use one of the methods below to open the webview:
 * - openURL
 * - openUrlByUsingMainWPCOMCredentials
 * - openUrlByUsingWPCOMCredentials
 * - openUrlByUsingBlogCredentials (for self hosted sites)
 * <p/>
 * If you need to start the activity with delay, start activity with result, or none of the methods above are enough
 * for your needs,
 * you can start the activity by passing the required parameters, depending on what you need to do.
 * <p/>
 * 1. Load a simple URL (without any kind of authentication)
 * - Start the activity with the parameter URL_TO_LOAD set to the URL to load.
 * <p/>
 * 2. Load a WordPress.com URL
 * Start the activity with the following parameters:
 * - URL_TO_LOAD: target URL to load in the webview.
 * - AUTHENTICATION_URL: The address of the WordPress.com authentication endpoint. Please use WPCOM_LOGIN_URL.
 * - AUTHENTICATION_USER: username.
 * - AUTHENTICATION_PASSWD: password.
 * <p/>
 * 3. Load a WordPress.org URL with authentication
 * - URL_TO_LOAD: target URL to load in the webview.
 * - AUTHENTICATION_URL: The address of the authentication endpoint. Please use the value of getSiteLoginUrl()
 * to retrieve the correct address of the authentication endpoint.
 * - AUTHENTICATION_USER: username.
 * - AUTHENTICATION_PASSWD: password.
 * - LOCAL_BLOG_ID: local id of the blog in the app database. This is required since some blogs could have HTTP Auth,
 * or self-signed certs in place.
 * - REFERRER_URL: url to add as an HTTP referrer header, currently only used for non-authed reader posts
 */
public class WPWebViewActivity extends WebViewActivity {
    public static final String AUTHENTICATION_URL = "authenticated_url";
    public static final String AUTHENTICATION_USER = "authenticated_user";
    public static final String AUTHENTICATION_PASSWD = "authenticated_passwd";
    public static final String USE_GLOBAL_WPCOM_USER = "USE_GLOBAL_WPCOM_USER";
    public static final String URL_TO_LOAD = "url_to_load";
    public static final String WPCOM_LOGIN_URL = "https://wordpress.com/wp-login.php";
    public static final String LOCAL_BLOG_ID = "local_blog_id";
    public static final String SHAREABLE_URL = "shareable_url";
    public static final String SHARE_SUBJECT = "share_subject";
    public static final String REFERRER_URL = "referrer_url";
    public static final String DISABLE_LINKS_ON_PAGE = "DISABLE_LINKS_ON_PAGE";
    public static final String ALLOWED_URLS = "allowed_urls";
    public static final String ENCODING_UTF8 = "UTF-8";

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ((WordPress) getApplication()).component().inject(this);
        super.onCreate(savedInstanceState);
    }

    public static void openUrlByUsingGlobalWPCOMCredentials(Context context, String url) {
        openWPCOMURL(context, url, null, null);
    }

    public static void openPostUrlByUsingGlobalWPCOMCredentials(Context context, String url, String shareableUrl,
                                                                String shareSubject) {
        openWPCOMURL(context, url, shareableUrl, shareSubject);
    }

    // frameNonce is used to show drafts, without it "no page found" error would be thrown
    public static void openJetpackBlogPostPreview(Context context, String url, String shareableUrl, String shareSubject,
                                                  String frameNonce) {
        if (!TextUtils.isEmpty(frameNonce)) {
            url += "&frame-nonce=" + frameNonce;
        }
        Intent intent = new Intent(context, WPWebViewActivity.class);
        intent.putExtra(WPWebViewActivity.URL_TO_LOAD, url);
        intent.putExtra(WPWebViewActivity.DISABLE_LINKS_ON_PAGE, false);
        if (!TextUtils.isEmpty(shareableUrl)) {
            intent.putExtra(WPWebViewActivity.SHAREABLE_URL, shareableUrl);
        }
        if (!TextUtils.isEmpty(shareSubject)) {
            intent.putExtra(WPWebViewActivity.SHARE_SUBJECT, shareSubject);
        }
        context.startActivity(intent);
    }

    // Note: The webview has links disabled (excepted for urls in the whitelist: listOfAllowedURLs)
    public static void openUrlByUsingBlogCredentials(Context context, SiteModel site, PostModel post, String url,
                                                     String[] listOfAllowedURLs) {
        if (context == null) {
            AppLog.e(AppLog.T.UTILS, "Context is null");
            return;
        }

        if (site == null) {
            AppLog.e(AppLog.T.UTILS, "Site is null");
            return;
        }

        if (TextUtils.isEmpty(url)) {
            AppLog.e(AppLog.T.UTILS, "Empty or null URL");
            ToastUtils.showToast(context, R.string.invalid_site_url_message, ToastUtils.Duration.SHORT);
            return;
        }

        String authURL = WPWebViewActivity.getSiteLoginUrl(site);
        Intent intent = new Intent(context, WPWebViewActivity.class);
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_USER, site.getUsername());
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_PASSWD, site.getPassword());
        intent.putExtra(WPWebViewActivity.URL_TO_LOAD, url);
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_URL, authURL);
        intent.putExtra(WPWebViewActivity.LOCAL_BLOG_ID, site.getId());
        intent.putExtra(WPWebViewActivity.DISABLE_LINKS_ON_PAGE, true);
        intent.putExtra(ALLOWED_URLS, listOfAllowedURLs);
        if (post != null) {
            intent.putExtra(WPWebViewActivity.SHAREABLE_URL, post.getLink());
            if (!TextUtils.isEmpty(post.getTitle())) {
                intent.putExtra(WPWebViewActivity.SHARE_SUBJECT, post.getTitle());
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
            ToastUtils.showToast(context, R.string.invalid_site_url_message, ToastUtils.Duration.SHORT);
            return;
        }

        Intent intent = new Intent(context, WPWebViewActivity.class);
        intent.putExtra(WPWebViewActivity.URL_TO_LOAD, url);
        if (!TextUtils.isEmpty(referrer)) {
            intent.putExtra(REFERRER_URL, referrer);
        }
        context.startActivity(intent);
    }

    protected static boolean checkContextAndUrl(Context context, String url) {
        if (context == null) {
            AppLog.e(AppLog.T.UTILS, "Context is null");
            return false;
        }

        if (TextUtils.isEmpty(url)) {
            AppLog.e(AppLog.T.UTILS, "Empty or null URL passed to openUrlByUsingMainWPCOMCredentials");
            ToastUtils.showToast(context, R.string.invalid_site_url_message, ToastUtils.Duration.SHORT);
            return false;
        }
        return true;
    }

    private static void openWPCOMURL(Context context, String url, String shareableUrl, String shareSubject) {
        if (!checkContextAndUrl(context, url)) {
            return;
        }

        Intent intent = new Intent(context, WPWebViewActivity.class);
        intent.putExtra(WPWebViewActivity.USE_GLOBAL_WPCOM_USER, true);
        intent.putExtra(WPWebViewActivity.URL_TO_LOAD, url);
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_URL, WPCOM_LOGIN_URL);
        if (!TextUtils.isEmpty(shareableUrl)) {
            intent.putExtra(WPWebViewActivity.SHAREABLE_URL, shareableUrl);
        }
        if (!TextUtils.isEmpty(shareSubject)) {
            intent.putExtra(WPWebViewActivity.SHARE_SUBJECT, shareSubject);
        }
        context.startActivity(intent);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void configureWebView() {
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);

        final Bundle extras = getIntent().getExtras();

        // Configure the allowed URLs if available
        ArrayList<String> allowedURL = null;
        if (extras != null && extras.getBoolean(DISABLE_LINKS_ON_PAGE, false)) {
            String addressToLoad = extras.getString(URL_TO_LOAD);
            String authURL = extras.getString(AUTHENTICATION_URL);
            allowedURL = new ArrayList<>();
            if (!TextUtils.isEmpty(addressToLoad)) {
                allowedURL.add(addressToLoad);
            }
            if (!TextUtils.isEmpty(authURL)) {
                allowedURL.add(authURL);
            }

            if (extras.getStringArray(ALLOWED_URLS) != null) {
                String[] urls = extras.getStringArray(ALLOWED_URLS);
                for (String currentURL : urls) {
                    allowedURL.add(currentURL);
                }
            }
        }

        WebViewClient webViewClient = createWebViewClient(allowedURL);

        mWebView.setWebViewClient(webViewClient);
        mWebView.setWebChromeClient(new WPWebChromeClient(this, (ProgressBar) findViewById(R.id.progress_bar)));
    }

    protected WebViewClient createWebViewClient(List<String> allowedURL) {
        if (getIntent().hasExtra(LOCAL_BLOG_ID)) {
            SiteModel site = mSiteStore.getSiteByLocalId(getIntent().getIntExtra(LOCAL_BLOG_ID, -1));
            if (site == null) {
                AppLog.e(AppLog.T.UTILS, "No valid blog passed to WPWebViewActivity");
                finish();
            }
            return new WPWebViewClient(site, mAccountStore.getAccessToken(), allowedURL);
        } else {
            return new URLFilteredWebViewClient(allowedURL);
        }
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

        if (TextUtils.isEmpty(addressToLoad) || !UrlUtils.isValidUrlAndHostNotNull(addressToLoad)) {
            AppLog.e(AppLog.T.UTILS, "Empty or null or invalid URL passed to WPWebViewActivity");
            ToastUtils.showToast(this, R.string.invalid_site_url_message, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        if (extras.getBoolean(USE_GLOBAL_WPCOM_USER, false)) {
            username = mAccountStore.getAccount().getUserName();

            // Custom domains are not properly authenticated due to a server side(?) issue, so this gets around that
            if (!addressToLoad.contains(".wordpress.com")) {
                List<SiteModel> wpComSites = mSiteStore.getWPComSites();
                for (SiteModel siteModel : wpComSites) {
                    // Only replace the url if we know the unmapped url and if it's a custom domain
                    if (!TextUtils.isEmpty(siteModel.getUnmappedUrl())
                        && !siteModel.getUrl().contains(".wordpress.com")) {
                        addressToLoad = addressToLoad.replace(siteModel.getUrl(), siteModel.getUnmappedUrl());
                    }
                }
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
                ToastUtils.showToast(this, R.string.invalid_site_url_message, ToastUtils.Duration.SHORT);
                finish();
            }

            if (TextUtils.isEmpty(username)) {
                AppLog.e(AppLog.T.UTILS, "Username empty/null");
                ToastUtils.showToast(this, R.string.incorrect_credentials, ToastUtils.Duration.SHORT);
                finish();
            }

            loadAuthenticatedUrl(authURL, addressToLoad, username, password);
        }
    }

    /**
     * Login to the WordPress.com and load the specified URL.
     */
    protected void loadAuthenticatedUrl(String authenticationURL, String urlToLoad, String username, String password) {
        String postData = getAuthenticationPostData(authenticationURL, urlToLoad, username, password,
                                                    mAccountStore.getAccessToken());

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
    public static String getSiteLoginUrl(SiteModel site) {
        String loginURL = site.getLoginUrl();

        // Try to guess the login URL if blogOptions is null (blog not added to the app), or WP version is < 3.6
        if (loginURL == null) {
            if (site.getUrl() != null) {
                return site.getUrl() + "/wp-login.php";
            } else {
                return site.getXmlRpcUrl().replace("xmlrpc.php", "wp-login.php");
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
            // Use the preferred shareable URL or the default webview URL
            Bundle extras = getIntent().getExtras();
            String shareableUrl = extras.getString(SHAREABLE_URL, null);
            if (TextUtils.isEmpty(shareableUrl)) {
                shareableUrl = mWebView.getUrl();
            }
            share.putExtra(Intent.EXTRA_TEXT, shareableUrl);
            String shareSubject = extras.getString(SHARE_SUBJECT, null);
            if (!TextUtils.isEmpty(shareSubject)) {
                share.putExtra(Intent.EXTRA_SUBJECT, shareSubject);
            }
            startActivity(Intent.createChooser(share, getText(R.string.share_link)));
            return true;
        } else if (itemID == R.id.menu_browser) {
            ReaderActivityLauncher.openUrl(this, mWebView.getUrl(), ReaderActivityLauncher.OpenUrlType.EXTERNAL);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
