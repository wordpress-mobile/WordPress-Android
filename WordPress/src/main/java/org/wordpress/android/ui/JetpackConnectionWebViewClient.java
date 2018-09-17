package org.wordpress.android.ui;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.wordpress.android.util.AppLog;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

class JetpackConnectionWebViewClient extends WebViewClient {
    interface JetpackConnectionWebViewClientListener {
        void onRequiresWPComLogin(WebView webView, String redirectPage);
        void onRequiresJetpackLogin();
        void onJetpackSuccessfullyConnected(Uri uri);
    }

    private static final String LOGIN_PATH = "/wp-login.php";
    private static final String ADMIN_PATH = "/wp-admin/admin.php";
    private static final String REDIRECT_PARAMETER = "redirect_to=";
    private static final String WORDPRESS_COM_HOST = "wordpress.com";
    private static final String WPCOM_LOG_IN_PATH_1 = "/log-in";
    private static final String WPCOM_LOG_IN_PATH_2 = "/log-in/jetpack";
    private static final String JETPACK_PATH = "/jetpack";
    private static final String WORDPRESS_COM_PREFIX = "https://wordpress.com";
    static final String JETPACK_CONNECTION_DEEPLINK = "wordpress://jetpack-connection";
    private static final Uri JETPACK_DEEPLINK_URI = Uri.parse(JETPACK_CONNECTION_DEEPLINK);

    private final @NonNull JetpackConnectionWebViewClientListener mListener;
    private final String mSiteUrl;
    private String mRedirectPage;

    JetpackConnectionWebViewClient(@NonNull JetpackConnectionWebViewClientListener listener, String siteUrl) {
        mListener = listener;
        mSiteUrl = siteUrl;
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
            final String currentSiteHost = Uri.parse(mSiteUrl).getHost();
            if (loadedHost.equals(currentSiteHost)
                && loadedPath != null
                && loadedPath.contains(LOGIN_PATH)
                && stringUrl.contains(REDIRECT_PARAMETER)) {
                extractRedirect(stringUrl);
                mListener.onRequiresWPComLogin(view, mRedirectPage);
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
                extractRedirect(stringUrl);
                mListener.onRequiresJetpackLogin();
                return true;
            } else if (loadedHost.equals(JETPACK_DEEPLINK_URI.getHost())
                       && uri.getScheme().equals(JETPACK_DEEPLINK_URI.getScheme())) {
                mListener.onJetpackSuccessfullyConnected(uri);
                return true;
            }
        } catch (UnsupportedEncodingException e) {
            AppLog.e(AppLog.T.API, "Unexpected URL encoding in Jetpack connection flow.", e);
        }
        return false;
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
        String decodedUrl = URLDecoder.decode(redirectUrl, WPWebViewActivity.ENCODING_UTF8);
        if (decodedUrl.startsWith(JETPACK_PATH)) {
            decodedUrl = WORDPRESS_COM_PREFIX + decodedUrl;
        }
        mRedirectPage = decodedUrl;
    }

    String getRedirectPage() {
        return mRedirectPage;
    }

    void setRedirectPage(String redirectPage) {
        mRedirectPage = redirectPage;
    }
}
