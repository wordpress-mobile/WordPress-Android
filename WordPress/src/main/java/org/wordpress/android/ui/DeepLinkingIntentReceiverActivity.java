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
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

/**
 * An activity to handle deep linking and intercepting
 * <p>
 * wordpress://viewpost?blogId={blogId}&postId={postId}
 * <p>
 * Redirects users to the reader activity along with IDs passed in the intent
 */
public class DeepLinkingIntentReceiverActivity extends AppCompatActivity {
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

        AnalyticsUtils.trackWithDeepLinkData(AnalyticsTracker.Stat.DEEP_LINKED, action, uri);

        // check if this intent is started via custom scheme link
        if (Intent.ACTION_VIEW.equals(action) && uri != null) {
            if (isLoggedIn()) {
                mInterceptedUri = uri.toString();

                String host = StringUtils.notNullStr(uri.getHost());

                switch (host) {
                    case "stats":
                        showStats();
                        break;
                    case "reader":

                        break;
                    case "post":
                        showEditor();
                        break;
                    case "notifications":
                        showNotifications();
                        break;
                }

                finish();
            } else {
                ActivityLauncher.loginForDeeplink(this);
            }
        } else {
            finish();
        }
    }

    private void showNotifications() {
        int i = 0;
    }

    private void showStats() {
        long siteId = mAccountStore.getAccount().getPrimarySiteId();
        SiteModel siteModel = mSiteStore.getSiteBySiteId(siteId);
        ActivityLauncher.viewBlogStats(this, siteModel);
    }

    private void showEditor() {

        ActivityLauncher.addNewPostOrPageForResult();
    }

    private void showPost(Uri uri) {
        mBlogId = uri.getQueryParameter("blogId");
        mPostId = uri.getQueryParameter("postId");
        showPost();
    }

    private boolean isLoggedIn() {
        return mAccountStore.hasAccessToken();
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
