package org.wordpress.android.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;

import java.util.List;

import javax.inject.Inject;

import static org.wordpress.android.WordPress.getContext;

/**
 * An activity to handle deep linking and intercepting links like:
 * <p>
 * wordpress://viewpost?blogId={blogId}&postId={postId}
 * <p>
 * Redirects users to the reader activity along with IDs passed in the intent
 */
public class DeepLinkingIntentReceiverActivity extends LocaleAwareActivity {
    private static final String DEEP_LINK_HOST_NOTIFICATIONS = "notifications";
    private static final String DEEP_LINK_HOST_POST = "post";
    private static final String DEEP_LINK_HOST_STATS = "stats";
    private static final String DEEP_LINK_HOST_READ = "read";
    private static final String DEEP_LINK_HOST_VIEWPOST = "viewpost";
    private static final String HOST_WORDPRESS_COM = "wordpress.com";
    private static final String HOST_API_WORDPRESS_COM = "public-api.wordpress.com";
    private static final String MOBILE_TRACKING_PATH = "mbar";
    private static final String REGULAR_TRACKING_PATH = "bar";
    private static final String POST_PATH = "post";
    private static final String REDIRECT_TO_PARAM = "redirect_to";
    private static final String STATS_PATH = "stats";
    private static final String PAGES_PATH = "pages";

    private String mInterceptedUri;
    private String mBlogId;
    private String mPostId;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject PostStore mPostStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        String action = getIntent().getAction();
        Uri uri = getIntent().getData();
        String host = "";
        if (uri != null) {
            host = uri.getHost();
        }
        AnalyticsUtils.trackWithDeepLinkData(AnalyticsTracker.Stat.DEEP_LINKED, action, host, uri);

        // check if this intent is started via custom scheme link
        if (Intent.ACTION_VIEW.equals(action) && uri != null) {
            mInterceptedUri = uri.toString();
            if (shouldOpenEditor(uri)) {
                handleOpenEditor(uri);
            } else if (shouldOpenEditorFromDeepLink(host)) {
                handleOpenEditorFromDeepLink(uri);
            } else if (shouldHandleTrackingUrl(uri)) {
                // There is only one handled tracking URL for now (open editor)
                handleOpenEditorFromTrackingUrl(uri);
            } else if (isFromAppBanner(host)) {
                handleAppBanner(host);
            } else if (shouldViewPost(host)) {
                handleViewPost(uri);
            } else if (shouldShowStats(uri)) {
                handleShowStats(uri);
            } else if (shouldShowPages(uri)) {
                handleShowPages(uri);
            } else {
                // not handled
                finish();
            }
        } else {
            finish();
        }
    }

    private boolean shouldOpenEditor(@NonNull Uri uri) {
        // Match: https://wordpress.com/post/
        return shouldShow(uri, POST_PATH);
    }

    private boolean shouldOpenEditorFromDeepLink(String host) {
        // Match: wordpress://post/...
        return host != null && host.equals(DEEP_LINK_HOST_POST);
    }

    private @Nullable Uri getRedirectUri(@NonNull Uri uri) {
        String redirectTo = uri.getQueryParameter(REDIRECT_TO_PARAM);
        if (redirectTo == null) {
            return null;
        }
        return Uri.parse(redirectTo);
    }

    private boolean shouldHandleTrackingUrl(@NonNull Uri uri) {
        // https://public-api.wordpress.com/mbar/
        return StringUtils.equals(uri.getHost(), HOST_API_WORDPRESS_COM)
               && (!uri.getPathSegments().isEmpty()
                   && StringUtils.equals(uri.getPathSegments().get(0), MOBILE_TRACKING_PATH));
    }

    private void handleOpenEditorFromTrackingUrl(@NonNull Uri uri) {
        Uri redirectUri = getRedirectUri(uri);
        if (redirectUri == null || !shouldOpenEditor(redirectUri)) {
            // Replace host to redirect to the browser
            Uri newUri = (new Uri.Builder())
                    .scheme(uri.getScheme())
                    .path(REGULAR_TRACKING_PATH)
                    .query(uri.getQuery())
                    .fragment(uri.getFragment())
                    .authority(uri.getAuthority())
                    .build();
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, newUri);
            startActivity(browserIntent);
            finish();
            return;
        }
        handleOpenEditor(redirectUri);
    }

    /**
     * Opens post editor for provided uri. If uri contains a site and a postId
     * (e.g. https://wordpress.com/example.com/1231/), opens the post for editing, if available.
     * If the uri only contains a site (e.g. https://wordpress.com/example.com/ ), opens a new post
     * editor for that site, if available.
     * Else opens the new post editor for currently selected site.
     */
    private void handleOpenEditor(@NonNull Uri uri) {
        List<String> pathSegments = uri.getPathSegments();

        if (pathSegments.size() < 3) {
            // No postId in path, open new post editor for site
            openEditorForSite(extractTargetHost(uri));
            return;
        }

        // Match: https://wordpress.com/post/blogNameOrUrl/postId
        String targetHost = pathSegments.get(1);
        String targetPostId = pathSegments.get(2);
        openEditorForSiteAndPost(targetHost, targetPostId);
    }

    /**
     * Opens post editor for provided uri. If uri contains a site and a postId
     * (e.g. wordpress/post?blogId=798&postId=1231), opens the post for editing, if available.
     * If the uri only contains a site (e.g. wordpress/post?blogId=798 ), opens a new post
     * editor for that site, if available.
     * Else opens the new post editor for currently selected site.
     */
    private void handleOpenEditorFromDeepLink(@NonNull Uri uri) {
        String blogId = uri.getQueryParameter("blogId");
        String postId = uri.getQueryParameter("postId");

        if (blogId == null) {
            // No blogId provided. Follow default behaviour: open a blank editor with the current selected site
            ActivityLauncher.openEditorInNewStack(getContext());
            return;
        }

        SiteModel site;

        Long siteId = parseAsLongOrNull(blogId);
        if (siteId != null) {
            // Blog id is a number so we check for it as site id
            site = mSiteStore.getSiteBySiteId(siteId);
        } else {
            // Blog id is not a number so we check for it as blog name or url
            List<SiteModel> matchedSites = mSiteStore.getSitesByNameOrUrlMatching(blogId);
            site = matchedSites.isEmpty() ? null : matchedSites.get(0);
        }

        if (site == null) {
            // Site not found. Open a blank editor with the current selected site
            ToastUtils.showToast(getContext(), R.string.blog_not_found);
            ActivityLauncher.openEditorInNewStack(getContext());
            return;
        }

        Long remotePostId = parseAsLongOrNull(postId);

        if (remotePostId == null) {
            // Open new post editor for given site
            ActivityLauncher.openEditorForSiteInNewStack(getContext(), site);
            return;
        }

        // Check if post is available for opening
        PostModel post = mPostStore.getPostByRemotePostId(remotePostId, site);

        if (post == null) {
            ToastUtils.showToast(getContext(), R.string.post_not_found);
            // Post not found. Open new post editor for given site.
            ActivityLauncher.openEditorForSiteInNewStack(getContext(), site);
            return;
        }

        // Open editor with post
        ActivityLauncher.openEditorForPostInNewStack(getContext(), site, post.getId());
    }

    private void openEditorForSite(@NonNull String targetHost) {
        SiteModel site = extractSiteModelFromTargetHost(targetHost);
        String host = extractHostFromSite(site);
        if (site != null && host != null && StringUtils.equals(host, targetHost)) {
            // if we found the site with the matching url, open the editor for this site.
            ActivityLauncher.openEditorForSiteInNewStack(getContext(), site);
        } else {
            // In other cases, open the editor with the current selected site.
            ActivityLauncher.openEditorInNewStack(getContext());
        }
    }

    private void openEditorForSiteAndPost(@NonNull String targetHost, @NonNull String targetPostId) {
        // Check if a site is available with given targetHost
        SiteModel site = extractSiteModelFromTargetHost(targetHost);
        String host = extractHostFromSite(site);
        if (site == null || host == null || !StringUtils.equals(host, targetHost)) {
            // Site not found, or host of site doesn't match the host in url
            ToastUtils.showToast(getContext(), R.string.blog_not_found);
            // Open a new post editor with current selected site
            ActivityLauncher.openEditorInNewStack(getContext());
            return;
        }

        Long remotePostId = parseAsLongOrNull(targetPostId);

        if (remotePostId == null) {
            // No post id provided; open new post editor for given site
            ActivityLauncher.openEditorForSiteInNewStack(getContext(), site);
            return;
        }

        // Check if post with given id is available for opening
        PostModel post = mPostStore.getPostByRemotePostId(remotePostId, site);

        if (post == null) {
            // Post not found
            ToastUtils.showToast(getContext(), R.string.post_not_found);
            // Open new post editor for given site
            ActivityLauncher.openEditorForSiteInNewStack(getContext(), site);
            return;
        }

        // Open editor with post
        ActivityLauncher.openEditorForPostInNewStack(getContext(), site, post.getId());
    }

    private boolean shouldViewPost(String host) {
        return StringUtils.equals(host, DEEP_LINK_HOST_VIEWPOST);
    }

    private void handleViewPost(@NonNull Uri uri) {
        mBlogId = uri.getQueryParameter("blogId");
        mPostId = uri.getQueryParameter("postId");

        // if user is signed in wpcom show the post right away - otherwise show welcome activity
        // and then show the post once the user has signed in
        if (mAccountStore.hasAccessToken()) {
            showPost();
            finish();
        } else {
            ActivityLauncher.loginForDeeplink(this);
        }
    }

    private boolean shouldShowStats(@NonNull Uri uri) {
        // Match: https://wordpress.com/stats/
        return shouldShow(uri, STATS_PATH);
    }

    private void handleShowStats(@NonNull Uri uri) {
        String targetHost = extractTargetHost(uri);
        SiteModel site = extractSiteModelFromTargetHost(targetHost);
        String host = extractHostFromSite(site);
        if (site != null && host != null && StringUtils.equals(host, targetHost)) {
            ActivityLauncher.viewStatsInNewStack(getContext(), site);
        } else {
            // In other cases, launch stats with the current selected site.
            ActivityLauncher.viewStatsInNewStack(getContext());
        }
        finish();
    }

    private boolean shouldShowPages(@NonNull Uri uri) {
        // Match: https://wordpress.com/pages/
        return shouldShow(uri, PAGES_PATH);
    }

    private void handleShowPages(@NonNull Uri uri) {
        String targetHost = extractTargetHost(uri);
        SiteModel site = extractSiteModelFromTargetHost(targetHost);
        String host = extractHostFromSite(site);
        if (site != null && host != null && StringUtils.equals(host, targetHost)) {
            ActivityLauncher.viewPagesInNewStack(getContext(), site);
        } else {
            // In other cases, launch pages with the current selected site.
            ActivityLauncher.viewPagesInNewStack(getContext());
        }
        finish();
    }

    private void handleAppBanner(@NonNull String host) {
        switch (host) {
            case DEEP_LINK_HOST_NOTIFICATIONS:
                ActivityLauncher.viewNotificationsInNewStack(getContext());
                break;
            case DEEP_LINK_HOST_STATS:
                long primarySiteId = mAccountStore.getAccount().getPrimarySiteId();
                SiteModel siteModel = mSiteStore.getSiteBySiteId(primarySiteId);
                ActivityLauncher.viewStatsInNewStack(getContext(), siteModel);
                break;
            case DEEP_LINK_HOST_READ:
                ActivityLauncher.viewReaderInNewStack(getContext());
                break;
        }
    }

    private boolean isFromAppBanner(String host) {
        return (host != null
                && (host.equals(DEEP_LINK_HOST_NOTIFICATIONS)
                    || host.equals(DEEP_LINK_HOST_READ)
                    || host.equals(DEEP_LINK_HOST_STATS)));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // show the post if user is returning from successful login
        if (requestCode == RequestCodes.DO_LOGIN && resultCode == RESULT_OK) {
            showPost();
        }

        finish();
    }

    private void showPost() {
        if (!TextUtils.isEmpty(mBlogId) && !TextUtils.isEmpty(mPostId)) {
            try {
                final long blogId = Long.parseLong(mBlogId);
                final long postId = Long.parseLong(mPostId);

                AnalyticsUtils.trackWithBlogPostDetails(AnalyticsTracker.Stat.READER_VIEWPOST_INTERCEPTED,
                        blogId, postId);

                ReaderActivityLauncher.showReaderPostDetail(this, false, blogId, postId, null, 0,
                        mInterceptedUri);
            } catch (NumberFormatException e) {
                AppLog.e(T.READER, e);
            }
        } else {
            ToastUtils.showToast(this, R.string.error_generic);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    // Helper Methods
    private String extractTargetHost(@NonNull Uri uri) {
        return uri.getLastPathSegment() == null ? "" : uri.getLastPathSegment();
    }

    private @Nullable SiteModel extractSiteModelFromTargetHost(String host) {
        List<SiteModel> matchedSites = mSiteStore.getSitesByNameOrUrlMatching(host);
        return matchedSites.isEmpty() ? null : matchedSites.get(0);
    }

    private @Nullable String extractHostFromSite(SiteModel site) {
        if (site != null && site.getUrl() != null) {
            return Uri.parse(site.getUrl()).getHost();
        }
        return null;
    }

    private boolean shouldShow(@NonNull Uri uri, @NonNull String path) {
        return StringUtils.equals(uri.getHost(), HOST_WORDPRESS_COM)
               && (!uri.getPathSegments().isEmpty() && StringUtils.equals(uri.getPathSegments().get(0), path));
    }

    private Long parseAsLongOrNull(String longAsString) {
        if (longAsString == null || longAsString.isEmpty()) {
            return null;
        }

        try {
            return Long.valueOf(longAsString);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }
}
