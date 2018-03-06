package org.wordpress.android.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.login.LoginMode;
import org.wordpress.android.ui.accounts.LoginActivity;
import org.wordpress.android.util.AppLog;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import static org.wordpress.android.WordPress.SITE;
import static org.wordpress.android.ui.RequestCodes.JETPACK_LOGIN;

/**
 * Activity that opens the Jetpack login flow and returns to StatsActivity when finished.
 * Use one of the static factory methods to start the flow.
 */
public class JetpackConnectionWebViewActivity extends WPWebViewActivity {
    private static final String LOGIN_PATH = "/wp-login.php";
    private static final String REDIRECT_PARAMETER = "redirect_to=";
    private static final String WORDPRESS_COM_HOST = "wordpress.com";
    private static final String WPCOM_LOG_IN_PATH_1 = "/log-in";
    private static final String WPCOM_LOG_IN_PATH_2 = "/log-in/jetpack";
    private static final String JETPACK_PATH = "/jetpack";
    private static final String WORDPRESS_COM_PREFIX = "https://wordpress.com";
    private static final String JETPACK_CONNECTION_DEEPLINK = "wordpress://jetpack-connection";
    private static final Uri JETPACK_DEEPLINK_URI = Uri.parse(JETPACK_CONNECTION_DEEPLINK);
    private static final String REDIRECT_PAGE_STATE_ITEM = "redirectPage";

    public enum Source {
        STATS("stats"), NOTIFICATIONS("notifications");
        private final String mValue;

        Source(String value) {
            mValue = value;
        }

        @Nullable
        public static Source fromString(String value) {
            if (STATS.mValue.equals(value)) {
                return STATS;
            } else if (NOTIFICATIONS.mValue.equals(value)) {
                return NOTIFICATIONS;
            } else {
                return null;
            }
        }

        @Override
        public String toString() {
            return mValue;
        }
    }

    private SiteModel mSite;
    private String mRedirectPage;

    public static void startJetpackConnectionFlow(Context context, Source source, SiteModel site, boolean authorized) {
        String url = "https://wordpress.com/jetpack/connect?"
                     + "url=" + site.getUrl()
                     + "&mobile_redirect=" + JETPACK_CONNECTION_DEEPLINK
                     + "?source=" + source.toString();
        startJetpackConnectionFlow(context, url, site, authorized);
    }

    private static void startJetpackConnectionFlow(Context context, String url, SiteModel site, boolean authorized) {
        if (!checkContextAndUrl(context, url)) {
            return;
        }

        Intent intent = new Intent(context, JetpackConnectionWebViewActivity.class);
        intent.putExtra(WPWebViewActivity.URL_TO_LOAD, url);
        if (authorized) {
            intent.putExtra(WPWebViewActivity.USE_GLOBAL_WPCOM_USER, true);
            intent.putExtra(WPWebViewActivity.AUTHENTICATION_URL, WPCOM_LOGIN_URL);
        }
        if (site != null) {
            intent.putExtra(WordPress.SITE, site);
        }
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
        // We need to get the site before calling super since it'll create the web client
        super.onCreate(savedInstanceState);
    }

    @Override
    protected WebViewClient createWebViewClient(List<String> allowedURL) {
        return new JetpackConnectionWebViewClient();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == JETPACK_LOGIN && resultCode == RESULT_OK) {
            JetpackConnectionWebViewActivity
                    .startJetpackConnectionFlow(this, mRedirectPage, mSite, mAccountStore.hasAccessToken());
        }
        finish();
    }

    @Override
    protected void cancel() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_CANCELED_INSTALL_JETPACK);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(REDIRECT_PAGE_STATE_ITEM, mRedirectPage);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mRedirectPage = savedInstanceState.getString(REDIRECT_PAGE_STATE_ITEM);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    private void extractRedirect(String stringUrl) throws UnsupportedEncodingException {
        int from = stringUrl.indexOf(REDIRECT_PARAMETER) + REDIRECT_PARAMETER.length();
        int to = stringUrl.indexOf("&", from);
        String redirectUrl;
        if (to > from) {
            redirectUrl = stringUrl.substring(from, to);
        } else {
            redirectUrl = stringUrl.substring(from);
        }
        if (redirectUrl.startsWith(JETPACK_PATH)) {
            redirectUrl = WORDPRESS_COM_PREFIX + redirectUrl;
        }
        mRedirectPage = URLDecoder.decode(redirectUrl, WPWebViewActivity.ENCODING_UTF8);
    }

    private void loginToJetpackInApp() {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        LoginMode.JETPACK_STATS.putInto(loginIntent);
        startActivityForResult(loginIntent, JETPACK_LOGIN);
    }

    private void jetpackSuccessfullyConnected(Uri uri) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_COMPLETED_INSTALL_JETPACK);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        intent.putExtra(SITE, mSite);
        startActivity(intent);
        finish();
    }

    class JetpackConnectionWebViewClient extends WebViewClient {
        private void loginToWPComInWebClient(WebView view) {
            String authenticationURL = WPWebViewActivity.getSiteLoginUrl(mSite);
            String postData = WPWebViewActivity.getAuthenticationPostData(authenticationURL, mRedirectPage,
                                                                          mSite.getUsername(), mSite.getPassword(),
                                                                          mAccountStore.getAccessToken());
            view.postUrl(authenticationURL, postData.getBytes());
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String stringUrl) {
            try {
                final Uri uri = Uri.parse(stringUrl);
                final String loadedHost = uri.getHost();
                if (loadedHost == null) {
                    return false;
                }
                final String loadedPath = uri.getPath();
                final String currentSiteHost = Uri.parse(mSite.getUrl()).getHost();
                if (loadedHost.equals(currentSiteHost)
                    && loadedPath != null
                    && loadedPath.contains(LOGIN_PATH)
                    && stringUrl.contains(REDIRECT_PARAMETER)) {
                    extractRedirect(stringUrl);
                    loginToWPComInWebClient(view);
                    return true;
                } else if (loadedHost.equals(WORDPRESS_COM_HOST)
                           && loadedPath != null
                           && (loadedPath.equals(WPCOM_LOG_IN_PATH_1) || loadedPath.equals(WPCOM_LOG_IN_PATH_2))
                           && stringUrl.contains(REDIRECT_PARAMETER)) {
                    extractRedirect(stringUrl);
                    loginToJetpackInApp();
                    return true;
                } else if (loadedHost.equals(JETPACK_DEEPLINK_URI.getHost())
                           && uri.getScheme().equals(JETPACK_DEEPLINK_URI.getScheme())) {
                    jetpackSuccessfullyConnected(uri);
                    return true;
                }
            } catch (UnsupportedEncodingException e) {
                AppLog.e(AppLog.T.API, "Unexpected URL encoding in Jetpack connection flow.", e);
            }
            return false;
        }
    }
}
