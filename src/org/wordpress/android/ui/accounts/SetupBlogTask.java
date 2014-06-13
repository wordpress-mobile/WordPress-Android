package org.wordpress.android.ui.accounts;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;

import java.util.List;
import java.util.Map;

public class SetupBlogTask extends AsyncTask<Void, Void, List<Map<String, Object>>> {
    protected SetupBlog mSetupBlog;
    protected int mErrorMsgId;
    protected Context mContext;

    public SetupBlogTask(Context context) {
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
        List<Map<String, Object>> userBlogList = mSetupBlog.getBlogList();
        mErrorMsgId = mSetupBlog.getErrorMsgId();
        if (userBlogList != null) {
            mSetupBlog.syncBlogs(mContext, userBlogList);
        }
        return userBlogList;
    }
}