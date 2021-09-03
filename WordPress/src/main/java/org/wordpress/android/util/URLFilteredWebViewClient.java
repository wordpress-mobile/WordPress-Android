package org.wordpress.android.util;

import android.content.Context;
import android.net.Uri;
import android.webkit.WebView;
import android.widget.Toast;

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

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        // Found a bug on some pages where there is an incorrect
        // auto-redirect to file:///android_asset/webkit/.
        if (url.equals("file:///android_asset/webkit/")) {
            return true;
        }

        if (isAllURLsAllowed() || mAllowedURLs.contains(url)
            || isHttpUrlAllowed(url) // Allow https redirection
            // If a url is allowed without the trailing `/`, it should be allowed with it as well
            || mAllowedURLs.contains(StringUtils.removeTrailingSlash(url))) {
            boolean isComingFromLoginUrl =
                    view.getUrl().endsWith(WP_LOGIN_URL_SUFFIX) || view.getUrl().endsWith(REMOTE_LOGIN_URL_SUFFIX);

            boolean isRemoteLoginUrl = url.endsWith(REMOTE_LOGIN_URL_SUFFIX);
            boolean isLoginUrl = url.endsWith(WP_LOGIN_URL_SUFFIX);

            Uri currentUri = Uri.parse((view.getUrl()));
            Uri incomingUri = Uri.parse(url);

            boolean newUrlIsOnTheSameHost =
                    currentUri.getHost() != null && currentUri.getHost().equals(incomingUri.getHost());

            boolean openInExternalBrowser =
                    !isRemoteLoginUrl && !isLoginUrl && !isComingFromLoginUrl && !newUrlIsOnTheSameHost;

            if (openInExternalBrowser) {
                ReaderActivityLauncher.openUrl(view.getContext(), url, ReaderActivityLauncher.OpenUrlType.EXTERNAL);
                return true;
            }

            return false;
        } else {
            // show "links are disabled" message.
            Context ctx = WordPress.getContext();
            int linksDisabledMessageResId = org.wordpress.android.R.string.preview_screen_links_disabled;
            Toast.makeText(ctx, ctx.getText(linksDisabledMessageResId), Toast.LENGTH_SHORT).show();
        }
        return true;
    }
}
