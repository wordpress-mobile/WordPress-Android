package org.wordpress.android.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

import static org.wordpress.android.WordPress.getContext;

/**
 * An activity to handle deep linking and intercepting
 * <p>
 * wordpress://viewpost?blogId={blogId}&postId={postId}
 * <p>
 * Redirects users to the reader activity along with IDs passed in the intent
 */
public class DeepLinkingIntentReceiverActivity extends AppCompatActivity {
    public static final String DEEP_LINK_HOST_NOTIFICATIONS = "notifications";
    public static final String DEEP_LINK_HOST_POST = "post";
    public static final String DEEP_LINK_HOST_STATS = "stats";
    public static final String DEEP_LINK_HOST_READ = "read";

    private String mInterceptedUri;
    private String mBlogId;
    private String mPostId;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

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

            if (isFromAppBanner(host)) {
                handleAppBanner(host);
            } else {
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
        } else {
            finish();
        }
    }

    private void handleAppBanner(String host) {
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
}
