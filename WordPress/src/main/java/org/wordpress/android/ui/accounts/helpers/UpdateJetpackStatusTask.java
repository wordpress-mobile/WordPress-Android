package org.wordpress.android.ui.accounts.helpers;

import android.content.Context;
import android.os.AsyncTask;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.VolleyUtils;

import java.util.Locale;

public class UpdateJetpackStatusTask extends AsyncTask<Void, Void, Void> {
    protected Context mContext;
    private Blog mBlog;

    public UpdateJetpackStatusTask(Context context, Blog blog) {
        if (blog == null || !blog.isJetpackPowered()) {
            cancel(true);
            return;
        }
        mBlog = blog;
        mContext = context;
    }

    @Override
    protected Void doInBackground(Void... args) {
        String path = String.format(Locale.US, "sites/%s/jetpack/modules", mBlog.getDotComBlogId());
        WordPress.getRestClientUtils().get(path, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                boolean isModified;
                if (response == null) {
                    AppLog.e(AppLog.T.MAIN, "Response from the server was 200, but no data received, or received a malformed response.");
                    mBlog.setJetpackModulesInfo("{}");
                    isModified = true;
                } else {
                    isModified = mBlog.bsetJetpackModulesInfo(response.toString());
                }

                if (isModified) {
                    WordPress.wpDB.saveBlog(mBlog);
                }
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                AppLog.e(AppLog.T.MAIN, VolleyUtils.errStringFromVolleyError(error));
                AppLog.e(AppLog.T.MAIN, VolleyUtils.messageStringFromVolleyError(error));
            }
        });
        return null;
    }
}