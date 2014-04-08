package org.wordpress.android.ui.reader.actions;

import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.models.ReaderBlogInfo;
import org.wordpress.android.models.ReaderUrlList;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.UrlUtils;

public class ReaderBlogActions {

    public enum BlogAction {FOLLOW, UNFOLLOW}

    public static boolean performBlogAction(final BlogAction action,
                                            final String blogUrl) {

        if (TextUtils.isEmpty(blogUrl))
            return false;

        final boolean isCurrentlyFollowing = ReaderBlogTable.isFollowedBlogUrl(blogUrl);
        final boolean isAskingToFollow = (action == BlogAction.FOLLOW);

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

    /*
     * requests info about a specific blog
     */
    /*public static void updateBlogInfo(final long blogId, final ReaderActions.ActionListener actionListener) {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleUpdateBlogInfoResponse(jsonObject, actionListener);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                if (actionListener != null)
                    actionListener.onActionResult(false);
            }
        };
        WordPress.getRestClientUtils().get("/sites/" + blogId, listener, errorListener);
    }*/
    public static void updateBlogInfo(final String blogUrl, final ReaderActions.ActionListener actionListener) {
        if (TextUtils.isEmpty(blogUrl)) {
            if (actionListener != null)
                actionListener.onActionResult(false);
            return;
        }

        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleUpdateBlogInfoResponse(jsonObject, actionListener);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                if (actionListener != null)
                    actionListener.onActionResult(false);
            }
        };
        final String domain = UrlUtils.removeProtocol(blogUrl);
        WordPress.getRestClientUtils().get("/sites/" + domain, listener, errorListener);
    }
    private static void handleUpdateBlogInfoResponse(JSONObject jsonObject, ReaderActions.ActionListener actionListener) {
        if (jsonObject == null) {
            if (actionListener != null)
                actionListener.onActionResult(false);
            return;
        }

        ReaderBlogTable.setBlogInfo(ReaderBlogInfo.fromJson(jsonObject));
        if (actionListener != null)
            actionListener.onActionResult(true);
    }

}
