package org.wordpress.android.util;

import android.content.Context;
import android.net.Uri;
import android.webkit.WebView;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * WebViewClient that adds the ability of restrict URL loading (navigation) to a list of allowed URLs.
 * Generally used to disable links and navigation in admin pages.
 */
public class URLFilteredWebViewClient extends ErrorManagedWebViewClient {
    private Set<String> mAllowedURLs = new LinkedHashSet<>();

    private static final String WP_LOGIN_URL_SUFFIX = "wp-login.php";
    private static final String REMOTE_LOGIN_URL_SUFFIX = "remote-login.php";
    private static final String BLOB_URL_PREFIX = "blob:";

    public URLFilteredWebViewClient(String url, ErrorManagedWebViewClientListener listener) {
        super(listener);
        mAllowedURLs.add(url);
    }

    public URLFilteredWebViewClient(Collection<String> urls, ErrorManagedWebViewClientListener listener) {
        super(listener);
        if (urls == null || urls.size() == 0) {
            AppLog.w(AppLog.T.UTILS, "No valid URLs passed to URLFilteredWebViewClient! HTTP Links in the"
                                     + " page are NOT disabled, and ALL URLs could be loaded by the user!!");
            return;
        }
        mAllowedURLs.addAll(urls);
    }


    private boolean isAllURLsAllowed() {
        return mAllowedURLs.size() == 0;
    }

    private boolean isHttpUrlAllowed(String url) {
        return mAllowedURLs.contains(url.replace("https://", "http://"))
               || mAllowedURLs.contains(StringUtils.removeTrailingSlash(url)
                                                   .replace("https://", "http://"));
    }

    private static boolean isLoginPath(String url, String loginPage) {
        if (url == null) {
            return false;
        }
        String path = Uri.parse(url).getPath();
        return path != null && path.endsWith("/" + loginPage);
    }

    private boolean isLoginPageOnAllowedHost(String url) {
        Uri uri = Uri.parse(url);
        if (uri.getHost() == null || !isLoginPath(url, WP_LOGIN_URL_SUFFIX)) {
            return false;
        }
        for (String allowedURL : mAllowedURLs) {
            if (uri.getHost().equals(Uri.parse(allowedURL).getHost())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        // Found a bug on some pages where there is an incorrect
        // auto-redirect to file:///android_asset/webkit/.
        if (url.equals("file:///android_asset/webkit/")) {
            return true;
        }

        if (isAllURLsAllowed() || mAllowedURLs.contains(url)
            || isHttpUrlAllowed(url) // Allow https redirection
            || isLoginPageOnAllowedHost(url) // Allow every step of logging in, e.g. confirming the admin email
            // If a url is allowed without the trailing `/`, it should be allowed with it as well
            || mAllowedURLs.contains(StringUtils.removeTrailingSlash(url))) {
            // Compare paths rather than whole URLs, since login pages often carry a query (e.g. `redirect_to`)
            boolean isComingFromLoginUrl = isLoginPath(view.getUrl(), WP_LOGIN_URL_SUFFIX)
                                           || isLoginPath(view.getUrl(), REMOTE_LOGIN_URL_SUFFIX);

            boolean isRemoteLoginUrl = isLoginPath(url, REMOTE_LOGIN_URL_SUFFIX);
            boolean isLoginUrl = isLoginPath(url, WP_LOGIN_URL_SUFFIX);
            boolean isBlobUrl = url.startsWith(BLOB_URL_PREFIX);

            Uri currentUri = Uri.parse((view.getUrl()));
            Uri incomingUri = Uri.parse(url);

            boolean newUrlIsOnTheSameHost =
                    currentUri.getHost() != null && currentUri.getHost().equals(incomingUri.getHost());

            boolean openInExternalBrowser = !isRemoteLoginUrl && !isLoginUrl && !isComingFromLoginUrl
                                            && !newUrlIsOnTheSameHost && !isBlobUrl;

            if (openInExternalBrowser) {
                ReaderActivityLauncher.openUrl(view.getContext(), url, ReaderActivityLauncher.OpenUrlType.EXTERNAL);
                return true;
            }

            return false;
        } else {
            // show "links are disabled" message.
            Context ctx = WordPress.getContext();
            int linksDisabledMessageResId = R.string.preview_screen_links_disabled;
            Toast.makeText(ctx, ctx.getText(linksDisabledMessageResId), Toast.LENGTH_SHORT).show();
        }
        return true;
    }
}
