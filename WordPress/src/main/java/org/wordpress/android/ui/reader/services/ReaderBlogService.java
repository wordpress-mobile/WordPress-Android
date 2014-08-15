package org.wordpress.android.ui.reader.services;

import android.content.Context;
import android.content.Intent;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.models.ReaderBlogList;
import org.wordpress.android.models.ReaderRecommendBlogList;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.util.AppLog;

/*
 * background service for keeping list of followed and recommended blogs updated
 */

public class ReaderBlogService extends ReaderBaseService {

    public static void startService(Context context) {
        Intent intent = new Intent(context, ReaderBlogService.class);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(AppLog.T.READER, "reader blog service > created");
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.READER, "reader blog service > destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateFollowedBlogs();
        updateRecommendedBlogs();
        return START_NOT_STICKY;
    }

    /*
     * request the list of blogs the current user is following
     */
    public void updateFollowedBlogs() {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleFollowedBlogsResponse(jsonObject);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.READER, volleyError);
            }
        };
        // request using ?meta=site,feed to get extra info
        WordPress.getRestClientUtils().get("/read/following/mine?meta=site%2Cfeed", listener, errorListener);
    }
    private void handleFollowedBlogsResponse(final JSONObject jsonObject) {
        if (jsonObject == null) {
            return;
        }

        new Thread() {
            @Override
            public void run() {
                ReaderBlogList serverBlogs = ReaderBlogList.fromJson(jsonObject);
                ReaderBlogList localBlogs = ReaderBlogTable.getFollowedBlogs();

                if (!localBlogs.isSameList(serverBlogs)) {
                    ReaderBlogTable.setFollowedBlogs(serverBlogs);
                    AppLog.d(AppLog.T.READER, "reader blogs service > followed blogs changed");
                    sendLocalBroadcast(new Intent().setAction(ReaderServiceActions.ACTION_FOLLOWED_BLOGS_CHANGED));
                }
            }
        }.start();
    }

    /*
    * request the latest recommended blogs, replaces all local ones
    */
    public void updateRecommendedBlogs() {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleRecommendedBlogsResponse(jsonObject);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.READER, volleyError);
            }
        };

        String path = "/read/recommendations/mine/"
                    + "?source=mobile"
                    + "&number=" + Integer.toString(ReaderConstants.READER_MAX_RECOMMENDED_TO_REQUEST);
        WordPress.getRestClientUtils().get(path, listener, errorListener);
    }
    private void handleRecommendedBlogsResponse(final JSONObject jsonObject) {
        if (jsonObject == null) {
            return;
        }

        new Thread() {
            @Override
            public void run() {
                ReaderRecommendBlogList serverBlogs = ReaderRecommendBlogList.fromJson(jsonObject);
                ReaderRecommendBlogList localBlogs = ReaderBlogTable.getAllRecommendedBlogs();

                if (!localBlogs.isSameList(serverBlogs)) {
                    ReaderBlogTable.setRecommendedBlogs(serverBlogs);
                    sendLocalBroadcast(new Intent().setAction(ReaderServiceActions.ACTION_RECOMMENDED_BLOGS_CHANGED));
                }
            }
        }.start();
    }
}
