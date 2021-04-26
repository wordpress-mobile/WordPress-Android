package org.wordpress.android.ui.deeplinks;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UriWrapper;
import org.wordpress.android.util.analytics.AnalyticsUtils;

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
    private static final String DEEP_LINK_HOST_POST = "post";
    private static final String DEEP_LINK_HOST_VIEWPOST = "viewpost";
    private static final String HOST_WORDPRESS_COM = "wordpress.com";

    private String mInterceptedUri;
    private String mBlogId;
    private String mPostId;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject PostStore mPostStore;
    @Inject DeepLinkNavigator mDeeplinkNavigator;
    @Inject DeepLinkUriUtils mDeepLinkUriUtils;
    @Inject ViewModelProvider.Factory mViewModelFactory;
    private DeepLinkingIntentReceiverViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        mViewModel = new ViewModelProvider(this, mViewModelFactory).get(DeepLinkingIntentReceiverViewModel.class);

        String action = getIntent().getAction();
        Uri uri = getIntent().getData();
        String host = "";
        if (uri != null) {
            host = uri.getHost();
        }
        AnalyticsUtils.trackWithDeepLinkData(AnalyticsTracker.Stat.DEEP_LINKED, action, host, uri);

        setupObservers();

        // check if this intent is started via custom scheme link
        if (Intent.ACTION_VIEW.equals(action) && uri != null) {
            mInterceptedUri = uri.toString();
            UriWrapper uriWrapper = new UriWrapper(uri);
            boolean urlHandledInViewModel = mViewModel.handleUrl(uriWrapper);
            if (!urlHandledInViewModel) {
                if (shouldViewPost(host)) {
                    handleViewPost(uri);
                } else {
                    // not handled
                    finish();
                }
            }
        } else {
            finish();
        }
    }

    private void setupObservers() {
        mViewModel.getNavigateAction()
                  .observe(this, navigateActionEvent -> navigateActionEvent.applyIfNotHandled(navigateAction -> {
                      mDeeplinkNavigator.handleNavigationAction(navigateAction, this);
                      return null;
                  }));
        mViewModel.getToast().observe(this, toastEvent -> toastEvent.applyIfNotHandled(toastMessage -> {
            ToastUtils.showToast(getContext(), toastMessage);
            return null;
        }));
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
