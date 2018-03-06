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

    private final Activity mActivity;
    private final AccountStore mAccountStore;
    private final SiteModel mSiteModel;

    private String mRedirectPage;

    JetpackConnectionWebViewClient(Activity activity, AccountStore accountStore, SiteModel siteModel) {
        mActivity = activity;
        mAccountStore = accountStore;
        mSiteModel = siteModel;
    }

    private void loginToWPCom(WebView view, SiteModel site) {
        String authenticationURL = WPWebViewActivity.getSiteLoginUrl(site);
        String postData = WPWebViewActivity.getAuthenticationPostData(authenticationURL, mRedirectPage,
                                                                      site.getUsername(), site.getPassword(),
                                                                      mAccountStore.getAccessToken());
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
                mRedirectPage = extractRedirect(stringUrl);
                loginToWPCom(view, mSiteModel);
                return true;
            } else if (loadedHost.equals(currentSiteHost)
                       && loadedPath != null
                       && loadedPath.contains(ADMIN_PATH)
                       && mRedirectPage != null) {
                view.loadUrl(mRedirectPage);
                mRedirectPage = null;
                return true;
            } else if (loadedHost.equals(WORDPRESS_COM_HOST)
                    && loadedPath != null
                    && (loadedPath.equals(WPCOM_LOG_IN_PATH_1) || loadedPath.equals(WPCOM_LOG_IN_PATH_2))
                    && stringUrl.contains(REDIRECT_PARAMETER)) {
                mRedirectPage = extractRedirect(stringUrl);
                Intent loginIntent = new Intent(mActivity, LoginActivity.class);
                LoginMode.JETPACK_STATS.putInto(loginIntent);
                mActivity.startActivityForResult(loginIntent, JETPACK_LOGIN);
                return true;
            } else if (loadedHost.equals(JETPACK_DEEPLINK_URI.getHost())
                    && url.getScheme().equals(JETPACK_DEEPLINK_URI.getScheme())) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(url);
                intent.putExtra(SITE, mSiteModel);
                mActivity.startActivity(intent);
                mActivity.finish();
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
            JetpackConnectionWebViewActivity.openJetpackConnectionFlow(context, mRedirectPage, mSiteModel);
            mActivity.finish();
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putString(REDIRECT_PAGE_STATE_ITEM, mRedirectPage);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        mRedirectPage = savedInstanceState.getString(REDIRECT_PAGE_STATE_ITEM);
    }
}
