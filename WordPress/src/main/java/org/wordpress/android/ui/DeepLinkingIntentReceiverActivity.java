package org.wordpress.android.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;
/**
 * An activity to handle deep linking and intercepting
 *
 * wordpress://viewpost?blogId={blogId}&postId={postId}
 *
 * Redirects users to the reader activity along with IDs passed in the intent
 */
public class DeepLinkingIntentReceiverActivity extends AppCompatActivity {
    private static final int INTENT_WELCOME = 0;

    private String mInterceptedUri;
    private String mBlogId;
    private String mPostId;

    @Inject AccountStore mAccountStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        String action = getIntent().getAction();
        Uri uri = getIntent().getData();

        AnalyticsUtils.trackWithDeepLinkData(AnalyticsTracker.Stat.DEEP_LINKED, action, uri);

        // check if this intent is started via custom scheme link
        if (Intent.ACTION_VIEW.equals(action) && uri != null) {
            mInterceptedUri = uri.toString();

            mBlogId = uri.getQueryParameter("blogId");
            mPostId = uri.getQueryParameter("postId");

            // if user is signed in wpcom show the post right away - otherwise show welcome activity
            // and then show the post once the user has signed in
            if (mAccountStore.hasAccessToken()) {
                showPost();
            } else {
                Intent intent = new Intent(this, SignInActivity.class);
                startActivityForResult(intent, INTENT_WELCOME);
            }
        } else {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // show the post if user is returning from successful login
        if (requestCode == INTENT_WELCOME && resultCode == RESULT_OK)
            showPost();
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

        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
