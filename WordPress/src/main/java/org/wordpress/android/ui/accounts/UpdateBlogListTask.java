package org.wordpress.android.ui.accounts;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.networking.LoginAndFetchBlogListAbstract.Callback;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class UpdateBlogListTask extends AsyncTask<Void, Void, List<Map<String, Object>>> {
    public static final int GET_BLOG_LIST_TIMEOUT = 30000;
    protected SetupBlog mSetupBlog;
    protected int mErrorMsgId;
    protected Context mContext;
    protected boolean mBlogListChanged;
    protected static List<Map<String, Object>> mUserBlogList;

    public UpdateBlogListTask(Context context) {
        mContext = context;
    }

    @Override
    protected void onPreExecute() {
        mSetupBlog = new SetupBlog();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        String username = settings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
        String password = WordPressDB.decryptPassword(settings.getString(WordPress.WPCOM_PASSWORD_PREFERENCE, null));
        mSetupBlog.setUsername(username);
        mSetupBlog.setPassword(password);
    }

    @Override
    protected List<Map<String, Object>> doInBackground(Void... args) {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        mSetupBlog.getBlogList(new Callback() {
            @Override
            public void onSuccess(List<Map<String, Object>> userBlogList) {
                mUserBlogList = userBlogList;
                if (mUserBlogList != null) {
                    mBlogListChanged = BlogUtils.syncBlogs(mContext, mUserBlogList, mSetupBlog.getUsername(),
                            mSetupBlog.getPassword(), null, null);
                }
                countDownLatch.countDown();
            }

            @Override
            public void onError(int messageId, boolean httpAuthRequired, boolean erroneousSslCertificate) {
                mErrorMsgId = mSetupBlog.getErrorMsgId();
                countDownLatch.countDown();
            }
        });
        try {
            countDownLatch.await(GET_BLOG_LIST_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            AppLog.e(T.NUX, e);
        }
        return mUserBlogList;
    }

    public static class GenericUpdateBlogListTask extends UpdateBlogListTask {
        public GenericUpdateBlogListTask(Context context) {
            super(context);
        }

        @Override
        protected void onPostExecute(final List<Map<String, Object>> userBlogList) {
            if (mBlogListChanged) {
                WordPress.sendLocalBroadcast(WordPress.getContext(), WordPress.BROADCAST_ACTION_BLOG_LIST_CHANGED);
            }
        }
    }
}
