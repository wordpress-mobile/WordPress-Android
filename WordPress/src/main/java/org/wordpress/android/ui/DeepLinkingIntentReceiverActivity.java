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

import java.util.List;

/**
 * An activity to handle deep linking and intercepting
 *
 * wordpress://viewpost?blogId={blogId}&postId={postId}
 * http[s]://wordpress.com/read/feeds/{feedId}/posts/{feedItemId}
 *
 * Redirects users to the reader activity along with IDs passed in the intent
 */
public class DeepLinkingIntentReceiverActivity extends AppCompatActivity {
    private static final int INTENT_WELCOME = 0;

    private boolean mIsFeed;
    private String mBlogId;
    private String mPostId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String action = getIntent().getAction();
        Uri uri = getIntent().getData();

        // check if this intent is started via custom scheme link
        if (Intent.ACTION_VIEW.equals(action) && uri != null) {

            switch (uri.getScheme()) {
                case "wordpress":
                    mBlogId = uri.getQueryParameter("blogId");
                    mPostId = uri.getQueryParameter("postId");
                    break;
                case "http":
                case "https":
                    List<String> segments = uri.getPathSegments();

                    if (segments != null && segments.get(0).equals("read")) {
                        if (segments.size() > 2 && segments.get(1).equals("feeds")) {
                            mIsFeed = true;
                            mBlogId = segments.get(2);
                        }

                        if (segments.size() > 4 && segments.get(3).equals("posts")) {
                            mPostId = segments.get(4);
                        }
                    }
                    break;
            }

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
                ReaderActivityLauncher.showReaderPostDetail(this, mIsFeed, Long.parseLong(mBlogId),
                        Long.parseLong(mPostId), false);
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
