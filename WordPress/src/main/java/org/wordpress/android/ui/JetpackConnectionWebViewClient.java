package org.wordpress.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.accounts.LoginActivity;
import org.wordpress.android.ui.accounts.LoginMode;
import org.wordpress.android.ui.prefs.AppPrefs;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

class JetpackConnectionWebViewClient extends WebViewClient {

    private Activity activity;
    private final String mAccessToken;
    private final SiteStore mSiteStore;

    private String redirectPage;
    private static final String LOGIN_PATH = "/wp-login.php";
    private static final String ADMIN_PATH = "/wp-admin/admin.php";
    private static final String REDIRECT_PARAMETER = "redirect_to=";
    private static final int REQUEST_CODE = 1;

    JetpackConnectionWebViewClient(Activity activity, String mAccessToken, SiteStore mSiteStore) {
        this.activity = activity;
        this.mAccessToken = mAccessToken;
        this.mSiteStore = mSiteStore;
    }

    @Override
    public void onPageStarted(WebView view, String stringUrl, Bitmap favicon) {
        super.onPageStarted(view, stringUrl, favicon);
        try {
            Uri url = Uri.parse(stringUrl);
            SiteModel site = mSiteStore.getSiteByLocalId(AppPrefs.getSelectedSite());
            if (url.getHost().equals(Uri.parse(site.getUrl()).getHost())
                    && url.getPath().equals(LOGIN_PATH)
                    && stringUrl.contains(REDIRECT_PARAMETER)) {
                redirectPage = extractRedirect(stringUrl);
                loginToWPCom(view, site);
            } else if (url.getHost().equals(Uri.parse(site.getUrl()).getHost()) && url.getPath().equals(ADMIN_PATH) && redirectPage != null) {
                view.loadUrl(redirectPage);
                redirectPage = null;
            } else if (url.getHost().equals("wordpress.com") && url.getPath().equals("/log-in")
                    && stringUrl.contains(REDIRECT_PARAMETER)) {
                redirectPage = extractRedirect(stringUrl);
                Intent loginIntent = new Intent(activity, LoginActivity.class);
                LoginMode.JETPACK_STATS.putInto(loginIntent);
                activity.startActivityForResult(loginIntent, REQUEST_CODE);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void loginToWPCom(WebView view, SiteModel site) {
        String authenticationURL = WPWebViewActivity.getSiteLoginUrl(site);
        String postData = WPWebViewActivity.getAuthenticationPostData(authenticationURL, redirectPage, site.getUsername(), site.getPassword(), mAccessToken);
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
        return URLDecoder.decode(redirectUrl, WPWebViewActivity.ENCODING_UTF8);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Uri parsedUrl = Uri.parse(url);
        Uri expectedUrl = Uri.parse(WPWebViewActivity.JETPACK_CONNECTION_DEEPLINK);
        if (parsedUrl.getScheme().equals(expectedUrl.getScheme()) && parsedUrl.getHost().equals(expectedUrl.getHost())) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(parsedUrl);
            view.getContext().startActivity(intent);
            return true;
        }
        return false;
    }

    public void loginFinished(WebView view) {
        loginToWPCom(view, mSiteStore.getSiteByLocalId(AppPrefs.getSelectedSite()));
        redirectPage = null;
    }
}
