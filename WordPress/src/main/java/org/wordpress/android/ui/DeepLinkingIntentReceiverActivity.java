package org.wordpress.android.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.Account;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.accounts.BlogUtils;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.ui.accounts.helpers.FetchBlogListAbstract;
import org.wordpress.android.ui.accounts.helpers.FetchBlogListWPCom;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.ui.reader.services.ReaderUpdateService;
import org.wordpress.android.ui.stats.StatsWidgetProvider;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.xmlrpc.android.ApiHelper;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        if (Intent.ACTION_VIEW.equals(action) && uri != null) {
            if (uri.equals("viewpost")) {
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
            } else if (uri.getHost().contains("magic-login")) {
                attemptLoginWithMagicLink(uri);
            } else {
                finish();
            }
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

    private void attemptLoginWithMagicLink(Uri uri) {
        // handle magic link url
        String token = uri.getQueryParameter("token");
        if (token != null && !token.isEmpty()) {
            final Account account = AccountHelper.getDefaultAccount();
            account.setAccessToken(token);
            account.save();

            account.fetchAccountDetails();
            SimperiumUtils.configureSimperium(WordPress.getContext(), token);
            FetchBlogListWPCom listWPCom = new FetchBlogListWPCom();
            listWPCom.execute(new FetchBlogListAbstract.Callback() {
                @Override
                public void onSuccess(List<Map<String, Object>> userBlogList) {
                    if (userBlogList != null) {
                        BlogUtils.addBlogs(userBlogList, account.getUserName());

                        // refresh the first 5 blogs
                        refreshFirstFiveBlogsContent();
                    }

                    trackAnalyticsSignIn();

                    // get reader tags so they're available as soon as the Reader is accessed - done for
                    // both wp.com and self-hosted (self-hosted = "logged out" reader) - note that this
                    // uses the application context since the activity is finished immediately below
                    ReaderUpdateService.startService(getApplicationContext(),
                            EnumSet.of(ReaderUpdateService.UpdateTask.TAGS));

                        //Update previous stats widgets
                    StatsWidgetProvider.updateWidgetsOnLogin(getApplicationContext());

                    // Fire off a synchronous request to get the primary blog
                    WordPress.getRestClientUtils().get("me", new RestRequest.Listener() {
                        @Override
                        public void onResponse(JSONObject jsonObject) {
                            // Set primary blog
                            setPrimaryBlog(jsonObject);
                            Intent intent = new Intent(getApplicationContext(), WPMainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }, null);
                }

                @Override
                public void onError(int errorMessageId, boolean twoStepCodeRequired, boolean httpAuthRequired, boolean erroneousSslCertificate, String clientResponse) {
                    String yup = "";
                }
            });
        }
    }

    private void setPrimaryBlog(JSONObject jsonObject) {
        try {
            String primaryBlogId = jsonObject.getString("primary_blog");
            // Look for a visible blog with this id in the DB
            List<Map<String, Object>> blogs = WordPress.wpDB.getBlogsBy("isHidden = 0 AND blogId = " + primaryBlogId,
                    null, 1, true);
            if (blogs != null && !blogs.isEmpty()) {
                Map<String, Object> primaryBlog = blogs.get(0);
                // Ask for a refresh and select it
                refreshBlogContent(primaryBlog);
                WordPress.setCurrentBlog((Integer) primaryBlog.get("id"));
            }
        } catch (JSONException e) {
            AppLog.e(T.NUX, e);
        }
    }

    private void trackAnalyticsSignIn() {
        AnalyticsUtils.refreshMetadata();
        Map<String, Boolean> properties = new HashMap<String, Boolean>();
        properties.put("dotcom_user", true);
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNED_IN, properties);
    }

    private void refreshFirstFiveBlogsContent() {
        List<Map<String, Object>> visibleBlogs = WordPress.wpDB.getBlogsBy("isHidden = 0", null, 5, true);
        if (visibleBlogs != null && !visibleBlogs.isEmpty()) {
            int numberOfBlogsBeingRefreshed = Math.min(5, visibleBlogs.size());
            for (int i = 0; i < numberOfBlogsBeingRefreshed; i++) {
                Map<String, Object> currentBlog = visibleBlogs.get(i);
                refreshBlogContent(currentBlog);
            }
        }
    }

    private void refreshBlogContent(Map<String, Object> blogMap) {
        String blogId = blogMap.get("blogId").toString();
        String xmlRpcUrl = blogMap.get("url").toString();
        int intBlogId = StringUtils.stringToInt(blogId, -1);
        if (intBlogId == -1) {
            AppLog.e(T.NUX, "Can't refresh blog content - invalid blogId: " + blogId);
            return;
        }
        int blogLocalId = WordPress.wpDB.getLocalTableBlogIdForRemoteBlogIdAndXmlRpcUrl(intBlogId, xmlRpcUrl);
        Blog firstBlog = WordPress.wpDB.instantiateBlogByLocalId(blogLocalId);
        new ApiHelper.RefreshBlogContentTask(firstBlog, null).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, false);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
