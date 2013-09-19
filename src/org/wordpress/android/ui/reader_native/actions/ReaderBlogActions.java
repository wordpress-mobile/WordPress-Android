package org.wordpress.android.ui.reader_native.actions;

import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.datasets.ReaderUserTable;
import org.wordpress.android.models.ReaderUrlList;
import org.wordpress.android.models.ReaderUser;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.ReaderLog;
import org.wordpress.android.util.UrlUtils;

/**
 * Created by nbradbury on 9/2/13.
 */
public class ReaderBlogActions {

    public enum BlogAction {FOLLOW, UNFOLLOW}

    public static boolean performBlogAction(final BlogAction action,
                                            final String blogUrl) {

        if (TextUtils.isEmpty(blogUrl))
            return false;

        final boolean isCurrentlyFollowing = ReaderBlogTable.isFollowedBlogUrl(blogUrl);
        final boolean isAskingToFollow = (action== BlogAction.FOLLOW ? true : false);

        if (isCurrentlyFollowing==isAskingToFollow)
            return true;

        final String path;

        switch (action) {
            case FOLLOW:
                ReaderBlogTable.setIsFollowedBlogUrl(blogUrl, true);
                path = "/read/following/mine/new?url=" + UrlUtils.urlEncode(blogUrl);
                break;

            case UNFOLLOW:
                ReaderBlogTable.setIsFollowedBlogUrl(blogUrl, false);
                path = "/read/following/mine/delete?url=" + UrlUtils.urlEncode(blogUrl);
                break;

            default :
                return false;
        }

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                ReaderLog.d("blog action " + action.name() + " succeeded");
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                ReaderLog.w("blog action " + action.name() + " failed");
                ReaderLog.e(volleyError);
                // revert to original state
                ReaderBlogTable.setIsFollowedBlogUrl(blogUrl, isCurrentlyFollowing);
            }
        };
        WordPress.restClient.post(path, listener, errorListener);

        return true;
    }

    /*
     * get the latest server info on blogs the current user is following
     */
    public static void updateFollowedBlogs() {
        final ReaderUser localUser = ReaderUserTable.getCurrentUser();

        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleFollowedBlogsResponse(jsonObject);
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                ReaderLog.e(volleyError);
            }
        };

        WordPress.restClient.get("/read/following/mine", listener, errorListener);
    }
    private static void handleFollowedBlogsResponse(final JSONObject jsonObject) {
        if (jsonObject==null)
            return;

        new Thread() {
            @Override
            public void run() {
                ReaderUrlList urls = new ReaderUrlList();
                JSONArray jsonBlogs = jsonObject.optJSONArray("subscriptions");
                if (jsonBlogs!=null) {
                    for (int i=0; i < jsonBlogs.length(); i++)
                        urls.add(JSONUtil.getString(jsonBlogs.optJSONObject(i), "URL"));
                }
                ReaderBlogTable.setFollowedBlogUrls(urls);
            }
        }.start();
    }
}
