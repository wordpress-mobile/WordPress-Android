package org.wordpress.android.ui.reader.actions;

import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.ReaderBlogInfo;
import org.wordpress.android.models.ReaderUrlList;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.UrlUtils;

public class ReaderBlogActions {

    public enum FollowAction {FOLLOW, UNFOLLOW}

    public static boolean performFollowAction(final FollowAction action,
                                              final long blogId,
                                              final String blogUrl) {

        if (TextUtils.isEmpty(blogUrl))
            return false;

        final boolean isCurrentlyFollowing = ReaderBlogTable.isFollowedBlogUrl(blogUrl);
        final boolean isAskingToFollow = (action == FollowAction.FOLLOW);

        if (isCurrentlyFollowing == isAskingToFollow) {
            return true;
        }

        final String path;
        final String domain = UrlUtils.getDomainFromUrl(blogUrl);

        switch (action) {
            case FOLLOW:
                path = "/read/following/mine/new?url=" + domain;
                break;

            case UNFOLLOW:
                path = "/read/following/mine/delete?url=" + domain;
                break;

            default :
                return false;
        }

        ReaderBlogTable.setIsFollowedBlogUrl(blogUrl, isAskingToFollow);
        ReaderPostTable.setFollowStatusForPostsInBlog(blogId, isAskingToFollow);

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                AppLog.d(T.READER, "blog action " + action.name() + " succeeded");
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.w(T.READER, "blog action " + action.name() + " failed");
                AppLog.e(T.READER, volleyError);
                // revert to original state
                ReaderBlogTable.setIsFollowedBlogUrl(blogUrl, isCurrentlyFollowing);
                ReaderPostTable.setFollowStatusForPostsInBlog(blogId, isCurrentlyFollowing);
            }
        };
        WordPress.getRestClientUtils().post(path, listener, errorListener);

        return true;
    }

    /*
     * get the latest server info on blogs the current user is following
     */
    public static void updateFollowedBlogs() {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleFollowedBlogsResponse(jsonObject);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
            }
        };
        WordPress.getRestClientUtils().get("/read/following/mine", listener, errorListener);
    }
    private static void handleFollowedBlogsResponse(final JSONObject jsonObject) {
        if (jsonObject == null) {
            return;
        }

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

    /*
     * requests info about a specific blog
     */
    public static void updateBlogInfo(long blogId, final ReaderActions.RequestBlogInfoListener infoListener) {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleUpdateBlogInfoResponse(jsonObject, infoListener);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                if (infoListener != null)
                    infoListener.onResult(null);
            }
        };
        WordPress.getRestClientUtils().get("/sites/" + blogId, listener, errorListener);
    }

    private static void handleUpdateBlogInfoResponse(JSONObject jsonObject, ReaderActions.RequestBlogInfoListener infoListener) {
        if (jsonObject == null) {
            if (infoListener != null)
                infoListener.onResult(null);
            return;
        }

        ReaderBlogInfo blogInfo = ReaderBlogInfo.fromJson(jsonObject);
        ReaderBlogTable.setBlogInfo(blogInfo);
        if (infoListener != null)
            infoListener.onResult(blogInfo);
    }

}
