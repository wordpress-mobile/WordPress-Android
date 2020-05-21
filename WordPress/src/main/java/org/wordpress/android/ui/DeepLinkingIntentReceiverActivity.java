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
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
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

    private static final String TAG = DeepLinkingIntentReceiverActivity.class.getSimpleName();

    private String mInterceptedUri;
    private String mBlogId;
    private String mPostId;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

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
            } else if (shouldHandleTrackingUrl(uri)) {
                // There is only one handled tracking URL for now (open editor)
                handleOpenEditorFromTrackingUrl(uri);
            } else if (isFromAppBanner(host)) {
                handleAppBanner(host);
            } else if (shouldViewPost(host)) {
                handleViewPost(uri);
            } else if (shouldShowStats(uri)) {
                handleShowStats(uri);
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
        return StringUtils.equals(uri.getHost(), HOST_WORDPRESS_COM)
               && (!uri.getPathSegments().isEmpty() && StringUtils.equals(uri.getPathSegments().get(0), POST_PATH));
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

    private void handleOpenEditor(@NonNull Uri uri) {
        openEditorForSite(extractTargetHost(uri));
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
        return StringUtils.equals(uri.getHost(), HOST_WORDPRESS_COM)
               && (!uri.getPathSegments().isEmpty() && StringUtils.equals(uri.getPathSegments().get(0), STATS_PATH));
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
    }

    private void handleAppBanner(@NonNull String host) {
        switch (host) {
            case DEEP_LINK_HOST_NOTIFICATIONS:
                ActivityLauncher.viewNotificationsInNewStack(getContext());
                break;
            case DEEP_LINK_HOST_POST:
                ActivityLauncher.openEditorInNewStack(getContext());
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
                    || host.equals(DEEP_LINK_HOST_POST)
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

                ReaderActivityLauncher.showReaderPostDetail(this, false, blogId, postId, null, 0, false,
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
}
