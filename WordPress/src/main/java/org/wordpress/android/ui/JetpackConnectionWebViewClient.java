package org.wordpress.android.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.login.LoginMode;
import org.wordpress.android.ui.accounts.LoginActivity;
import org.wordpress.android.util.AppLog;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import static org.wordpress.android.WordPress.SITE;
import static org.wordpress.android.ui.RequestCodes.JETPACK_LOGIN;

class JetpackConnectionWebViewClient extends WebViewClient {
    private static final String LOGIN_PATH = "/wp-login.php";
    private static final String ADMIN_PATH = "/wp-admin/admin.php";
    private static final String REDIRECT_PARAMETER = "redirect_to=";
    private static final String WORDPRESS_COM_HOST = "wordpress.com";
    private static final String WPCOM_LOG_IN_PATH_1 = "/log-in";
    private static final String WPCOM_LOG_IN_PATH_2 = "/log-in/jetpack";
    private static final String JETPACK_PATH = "/jetpack";
    private static final String WORDPRESS_COM_PREFIX = "https://wordpress.com";
    private static final Uri JETPACK_DEEPLINK_URI =
            Uri.parse(JetpackConnectionWebViewActivity.JETPACK_CONNECTION_DEEPLINK);
    private static final String REDIRECT_PAGE_STATE_ITEM = "redirectPage";
    private static final String FLOW_FINISHED = "FLOW_FINISHED";

    private Activity activity;
    private final AccountStore accountStore;
    private final SiteModel mSiteModel;

    private String redirectPage;
    private boolean flowFinished = false;

    JetpackConnectionWebViewClient(Activity activity, AccountStore accountStore, SiteModel mSiteModel) {
        this.activity = activity;
        this.accountStore = accountStore;
        this.mSiteModel = mSiteModel;
    }

    private void loginToWPCom(WebView view, SiteModel site) {
        String authenticationURL = WPWebViewActivity.getSiteLoginUrl(site);
        String postData = WPWebViewActivity.getAuthenticationPostData(authenticationURL, redirectPage,
                                                                      site.getUsername(), site.getPassword(),
                                                                      accountStore.getAccessToken());
        view.postUrl(authenticationURL, postData.getBytes());
    }

    private String extractRedirect(String stringUrl) throws UnsupportedEncodingException {
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
        return URLDecoder.decode(redirectUrl, WPWebViewActivity.ENCODING_UTF8);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String stringUrl) {
        try {
            final Uri url = Uri.parse(stringUrl);
            final String loadedHost = url.getHost();
            if (loadedHost == null) {
                return false;
            }
            final String loadedPath = url.getPath();
            final String currentSiteHost = Uri.parse(mSiteModel.getUrl()).getHost();
            if (loadedHost.equals(currentSiteHost)
                    && loadedPath != null
                    && loadedPath.contains(LOGIN_PATH)
                    && stringUrl.contains(REDIRECT_PARAMETER)) {
                redirectPage = extractRedirect(stringUrl);
                loginToWPCom(view, mSiteModel);
                return true;
            } else if (loadedHost.equals(currentSiteHost)
                    && loadedPath != null
                    && loadedPath.contains(ADMIN_PATH)
                    && redirectPage != null) {
                view.loadUrl(redirectPage);
                redirectPage = null;
                return true;
            } else if (loadedHost.equals(WORDPRESS_COM_HOST)
                    && loadedPath != null
                    && (loadedPath.equals(WPCOM_LOG_IN_PATH_1) || loadedPath.equals(WPCOM_LOG_IN_PATH_2))
                    && stringUrl.contains(REDIRECT_PARAMETER)) {
                redirectPage = extractRedirect(stringUrl);
                Intent loginIntent = new Intent(activity, LoginActivity.class);
                LoginMode.JETPACK_STATS.putInto(loginIntent);
                activity.startActivityForResult(loginIntent, JETPACK_LOGIN);
                return true;
            } else if (loadedHost.equals(JETPACK_DEEPLINK_URI.getHost())
                    && url.getScheme().equals(JETPACK_DEEPLINK_URI.getScheme())) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(url);
                intent.putExtra(SITE, mSiteModel);
                activity.startActivity(intent);
                activity.finish();
                flowFinished = true;
                AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_COMPLETED_INSTALL_JETPACK);
                return true;
            }
        } catch (UnsupportedEncodingException e) {
            AppLog.e(AppLog.T.API, "Unexpected URL encoding in Jetpack connection flow.", e);
        }
        return false;
    }

    void activityResult(Context context, int requestCode) {
        if (requestCode == JETPACK_LOGIN) {
            JetpackConnectionWebViewActivity.openJetpackConnectionFlow(context, redirectPage, mSiteModel);
            activity.finish();
        }
    }

    public void cancel() {
        if (!flowFinished) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_CANCELED_INSTALL_JETPACK);
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putString(REDIRECT_PAGE_STATE_ITEM, redirectPage);
        outState.putBoolean(FLOW_FINISHED, flowFinished);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        redirectPage = savedInstanceState.getString(REDIRECT_PAGE_STATE_ITEM);
        flowFinished = savedInstanceState.getBoolean(FLOW_FINISHED);
    }
}
