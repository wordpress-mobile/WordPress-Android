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
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderUrlList;
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateBlogInfoListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.UrlUtils;

public class ReaderBlogActions {

    /*
     * follow/unfollow a blog - make sure to pass the blogId when known
     * since following solely by url causes the blog to be followed as a feed
     */
    public static boolean performFollowAction(final long blogId,
                                              final String blogUrl,
                                              final boolean isAskingToFollow) {
        // either blogId or blogUrl are required
        final boolean hasBlogId = (blogId != 0);
        final boolean hasBlogUrl = !TextUtils.isEmpty(blogUrl);
        if (!hasBlogId && !hasBlogUrl) {
            AppLog.w(T.READER, "follow action performed without blogId or blogUrl");
            return false;
        }

        final String path;
        final String actionName = (isAskingToFollow ? "follow" : "unfollow");

        if (isAskingToFollow) {
            // if we have a blogId, use /sites/$siteId/follows/new - this is important
            // because /read/following/mine/new follows it as a feed rather than a blog,
            // so its posts show up without support for likes, comments, etc.
            if (hasBlogId) {
                path = "/sites/" + blogId + "/follows/new";
            } else {
                path = "/read/following/mine/new?url=" + UrlUtils.getDomainFromUrl(blogUrl);
                AppLog.w(T.READER, "following blog by url rather than id");
            }
        } else {
            if (hasBlogId) {
                path = "/sites/" + blogId + "/follows/mine/delete";
            } else {
                path = "/read/following/mine/delete?url=" + UrlUtils.getDomainFromUrl(blogUrl);
                AppLog.w(T.READER, "unfollowing blog by url rather than id");
            }
        }

        // update local db
        if (hasBlogUrl) {
            ReaderBlogTable.setIsFollowedBlogUrl(blogUrl, isAskingToFollow);
        }
        if (hasBlogId) {
            ReaderPostTable.setFollowStatusForPostsInBlog(blogId, isAskingToFollow);
        }

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                AppLog.d(T.READER, "blog " + actionName + " succeeded");
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.w(T.READER, "blog " + actionName + " failed");
                AppLog.e(T.READER, volleyError);
                // revert to original state
                if (hasBlogUrl) {
                    ReaderBlogTable.setIsFollowedBlogUrl(blogUrl, !isAskingToFollow);
                }
                if (hasBlogId) {
                    ReaderPostTable.setFollowStatusForPostsInBlog(blogId, !isAskingToFollow);
                }
            }
        };
        WordPress.getRestClientUtils().post(path, listener, errorListener);

        // return before API call completes
        return true;
    }

    /*
     * helper routine when following a blog from a post view
     */
    public static boolean performFollowAction(ReaderPost post, boolean isAskingToFollow) {
        if (post == null) {
            return false;
        }
        return performFollowAction(post.blogId, post.getBlogUrl(), isAskingToFollow);
    }

    /*
     * request the list of blogs the current user is following
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
                if (jsonBlogs != null) {
                    for (int i=0; i < jsonBlogs.length(); i++) {
                        urls.add(JSONUtil.getString(jsonBlogs.optJSONObject(i), "URL"));
                    }
                }
                ReaderBlogTable.setFollowedBlogUrls(urls);
            }
        }.start();
    }

    /*
     * request info about a specific blog
     */
    public static void updateBlogInfo(long blogId,
                                      String blogUrl,
                                      final UpdateBlogInfoListener infoListener) {
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
                if (infoListener != null) {
                    infoListener.onResult(null);
                }
            }
        };


        final String path;
        if (blogId == 0 && !TextUtils.isEmpty(blogUrl)) {
            path = "/sites/" + UrlUtils.getDomainFromUrl(blogUrl);
        } else {
            path = "/sites/" + blogId;
        }
        WordPress.getRestClientUtils().get(path, listener, errorListener);
    }
    private static void handleUpdateBlogInfoResponse(JSONObject jsonObject, UpdateBlogInfoListener infoListener) {
        if (jsonObject == null) {
            if (infoListener != null) {
                infoListener.onResult(null);
            }
            return;
        }

        ReaderBlogInfo blogInfo = ReaderBlogInfo.fromJson(jsonObject);
        ReaderBlogTable.setBlogInfo(blogInfo);

        if (infoListener != null) {
            infoListener.onResult(blogInfo);
        }
    }

}
