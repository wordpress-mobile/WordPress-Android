package org.wordpress.android.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import org.wordpress.android.R;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ToastUtils;

/**
 * An activity to handle deep linking.
 *
 * wordpress://viewpost?blogId={blogId}&postId={postId}
 *
 * Redirects users to the reader activity along with IDs passed in the intent
 */
public class DeepLinkingIntentReceiverActivity extends AppCompatActivity {
    private static final int INTENT_WELCOME = 0;

    private String mBlogId;
    private String mPostId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String action = getIntent().getAction();
        Uri uri = getIntent().getData();

        // check if this intent is started via custom scheme link
        if (Intent.ACTION_VIEW.equals(action) && uri != null) {
            mBlogId = uri.getQueryParameter("blogId");
            mPostId = uri.getQueryParameter("postId");

            // if user is logged in, show the post right away - otherwise show welcome activity
            // and then show the post once the user has logged in
            if (AccountHelper.isSignedInWordPressDotCom()) {
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
                ReaderActivityLauncher.showReaderPostDetail(this, Long.parseLong(mBlogId), Long.parseLong(mPostId));
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
