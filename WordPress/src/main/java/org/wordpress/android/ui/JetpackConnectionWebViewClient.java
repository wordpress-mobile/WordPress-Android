package org.wordpress.android.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.prefs.AppPrefs;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

class JetpackConnectionWebViewClient extends WebViewClient {

    private final String mAccessToken;
    private final SiteStore mSiteStore;

    private String redirectPage;
    private static final String LOGIN_PATH = "/wp-login.php";
    private static final String ADMIN_PATH = "/wp-admin/admin.php";
    private static final String REDIRECT_PARAMETER = "redirect_to=";

    JetpackConnectionWebViewClient(String mAccessToken, SiteStore mSiteStore) {
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
                int from = stringUrl.indexOf(REDIRECT_PARAMETER) + REDIRECT_PARAMETER.length();
                int to = stringUrl.indexOf("&", from);
                String redirectUrl = stringUrl.substring(from, to);
                redirectPage = URLDecoder.decode(redirectUrl, WPWebViewActivity.ENCODING_UTF8);
                String authenticationURL = WPWebViewActivity.getSiteLoginUrl(site);
                String postData = WPWebViewActivity.getAuthenticationPostData(authenticationURL, redirectPage, site.getUsername(), site.getPassword(), mAccessToken);
                view.postUrl(authenticationURL, postData.getBytes());
            } else if (url.getHost().equals(Uri.parse(site.getUrl()).getHost()) && url.getPath().equals(ADMIN_PATH) && redirectPage != null) {
                view.loadUrl(redirectPage);
                redirectPage = null;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
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
}
