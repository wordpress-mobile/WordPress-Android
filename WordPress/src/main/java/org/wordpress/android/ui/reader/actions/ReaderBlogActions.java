package org.wordpress.android.ui.reader.actions;

import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.wordpress.rest.RestRequest;

import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostList;
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener;
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateBlogInfoListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.VolleyUtils;

public class ReaderBlogActions {

    public static class BlockedBlogResult {
        public long blogId;
        public ReaderPostList deletedPosts;
        public boolean wasFollowing;
    }

    /*
     * follow/unfollow a blog - make sure to pass the blogId when known since following
     * solely by url may cause the blog to be followed as a feed
     */
    public static boolean performFollowAction(final long blogId,
                                               final String blogUrl,
                                               final boolean isAskingToFollow,
                                               final ActionListener actionListener) {
        // either blogId or blogUrl are required
        final boolean hasBlogId = (blogId != 0);
        final boolean hasBlogUrl = !TextUtils.isEmpty(blogUrl);
        if (!hasBlogId && !hasBlogUrl) {
            AppLog.w(T.READER, "follow action performed without blogId or blogUrl");
            if (actionListener != null) {
                actionListener.onActionResult(false);
            }
            return false;
        }

        // update local db
        ReaderBlogTable.setIsFollowedBlog(blogId, blogUrl, isAskingToFollow);
        ReaderPostTable.setFollowStatusForPostsInBlog(blogId, blogUrl, isAskingToFollow);

        if (isAskingToFollow) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.READER_FOLLOWED_SITE);
        }

        final String path = getFollowEndpoint(blogId, blogUrl, isAskingToFollow);
        final String actionName = (isAskingToFollow ? "follow" : "unfollow");

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                boolean success = isFollowActionSuccessful(jsonObject, isAskingToFollow);
                if (success) {
                    AppLog.d(T.READER, "blog " + actionName + " succeeded");
                } else {
                    AppLog.w(T.READER, "blog " + actionName + " failed");
                    localRevertFollowAction(blogId, blogUrl, isAskingToFollow);
                }
                if (actionListener != null) {
                    actionListener.onActionResult(success);
                }
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.w(T.READER, "blog " + actionName + " failed");
                AppLog.e(T.READER, volleyError);
                localRevertFollowAction(blogId, blogUrl, isAskingToFollow);
                if (actionListener != null) {
                    actionListener.onActionResult(false);
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
    public static boolean performFollowAction(ReaderPost post,
                                              boolean isAskingToFollow,
                                              ActionListener actionListener) {
        if (post == null) {
            return false;
        }
        // don't use the blogId if this is an external feed
        long blogId = (post.isExternal ? 0 : post.blogId);
        return performFollowAction(blogId, post.getBlogUrl(), isAskingToFollow, actionListener);
    }

    /*
     * called when a follow/unfollow fails, restores local data to previous state
     */
    private static void localRevertFollowAction(long blogId, String blogUrl, boolean isAskingToFollow) {
        if (blogId == 0 && TextUtils.isEmpty(blogUrl)) {
            return;
        }
        ReaderBlogTable.setIsFollowedBlog(blogId, blogUrl, !isAskingToFollow);
        ReaderPostTable.setFollowStatusForPostsInBlog(blogId, blogUrl, !isAskingToFollow);
    }

    /*
     * returns whether a follow/unfollow was successful based on the response to:
     *      read/follows/new
     *      read/follows/delete
     *      site/$site/follows/new
     *      site/$site/follows/mine/delete
     */
    private static boolean isFollowActionSuccessful(JSONObject json, boolean isAskingToFollow) {
        if (json == null) {
            return false;
        }

        final boolean isSubscribed;
        if (json.has("subscribed")) {
            // read/follows/
            isSubscribed = json.optBoolean("subscribed", false);
        } else if (json.has("is_following")) {
            // site/$site/follows/
            isSubscribed = json.optBoolean("is_following", false);
        } else {
            isSubscribed = false;
        }

        return (isSubscribed == isAskingToFollow);
    }

    /*
     * returns the endpoint path to use when following/unfollowing a blog
     */
    private static String getFollowEndpoint(long blogId, String blogUrl, boolean isAskingToFollow) {
        if (isAskingToFollow) {
            // if we have a blogId, use /sites/$siteId/follows/new - this is important
            // because /read/following/mine/new follows it as a feed rather than a blog,
            // so its posts show up without support for likes, comments, etc.
            if (blogId != 0) {
                return "/sites/" + blogId + "/follows/new";
            } else {
                AppLog.w(T.READER, "following blog by url rather than id");
                return "/read/following/mine/new?url=" + UrlUtils.urlEncode(blogUrl);
            }
        } else {
            if (blogId != 0) {
                return "/sites/" + blogId + "/follows/mine/delete";
            } else {
                AppLog.w(T.READER, "unfollowing blog by url rather than id");
                return "/read/following/mine/delete?url=" + UrlUtils.urlEncode(blogUrl);
            }
        }
    }

    /*
     * request info about a specific blog
     */
    public static void updateBlogInfo(long blogId,
                                      final String blogUrl,
                                      final UpdateBlogInfoListener infoListener) {
        // must pass either a valid id or url
        final boolean hasBlogId = (blogId != 0);
        final boolean hasBlogUrl = !TextUtils.isEmpty(blogUrl);
        if (!hasBlogId && !hasBlogUrl) {
            AppLog.w(T.READER, "cannot get blog info without either id or url");
            if (infoListener != null) {
                infoListener.onResult(null);
            }
            return;
        }

        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleUpdateBlogInfoResponse(jsonObject, infoListener);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                // authentication error may indicate that API access has been disabled for this blog
                int statusCode = VolleyUtils.statusCodeFromVolleyError(volleyError);
                boolean isAuthErr = (statusCode == HttpStatus.SC_FORBIDDEN);
                // if we failed to get the blog info using the id and this isn't an authentication
                // error, try again using just the domain
                if (!isAuthErr && hasBlogId && hasBlogUrl) {
                    AppLog.w(T.READER, "failed to get blog info by id, retrying with url");
                    updateBlogInfo(0, blogUrl, infoListener);
                } else {
                    AppLog.e(T.READER, volleyError);
                    if (infoListener != null) {
                        infoListener.onResult(null);
                    }
                }
            }
        };

        if (hasBlogId) {
            WordPress.getRestClientUtils().get("/sites/" + blogId, listener, errorListener);
        } else {
            WordPress.getRestClientUtils().get("/sites/" + UrlUtils.urlEncode(UrlUtils.getDomainFromUrl(blogUrl)), listener, errorListener);
        }
    }
    private static void handleUpdateBlogInfoResponse(JSONObject jsonObject, UpdateBlogInfoListener infoListener) {
        if (jsonObject == null) {
            if (infoListener != null) {
                infoListener.onResult(null);
            }
            return;
        }

        ReaderBlog blogInfo = ReaderBlog.fromJson(jsonObject);
        ReaderBlogTable.addOrUpdateBlog(blogInfo);

        if (infoListener != null) {
            infoListener.onResult(blogInfo);
        }
    }

    /*
     * tests whether the passed url can be reached - does NOT use authentication, and does not
     * account for 404 replacement pages used by ISPs such as Charter
     */
    public static void checkBlogUrlReachable(final String blogUrl, final ActionListener actionListener) {
        // ActionListener is required
        if (actionListener == null) {
            return;
        }
        if (TextUtils.isEmpty(blogUrl)) {
            actionListener.onActionResult(false);
            return;
        }

        Response.Listener<String> listener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                actionListener.onActionResult(true);
            }
        };
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                actionListener.onActionResult(false);
            }
        };

        // TODO: this should be a HEAD request, but even though Volley supposedly supports HEAD
        // using it results in "java.lang.IllegalStateException: Unknown method type"
        StringRequest request = new StringRequest(
                Request.Method.GET,
                blogUrl,
                listener,
                errorListener);
        WordPress.requestQueue.add(request);
    }

    /*
     * block a blog - result includes the list of posts that were deleted by the block so they
     * can be restored if the user undoes the block
     */
    public static BlockedBlogResult blockBlogFromReader(final long blogId, final ActionListener actionListener) {
        final BlockedBlogResult blockResult = new BlockedBlogResult();
        blockResult.blogId = blogId;
        blockResult.deletedPosts = ReaderPostTable.getPostsInBlog(blogId, 0, false);
        blockResult.wasFollowing = ReaderBlogTable.isFollowedBlog(blogId, null);

        ReaderPostTable.deletePostsInBlog(blogId);

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                boolean success = (jsonObject != null && jsonObject.optBoolean("success"));
                if (success) {
                    // blocking endpoint unfollows the blog, so do the same here
                    ReaderBlogTable.setIsFollowedBlogId(blogId, false);
                } else {
                    AppLog.w(T.READER, "failed to block blog " + blogId);
                    ReaderPostTable.addOrUpdatePosts(null, blockResult.deletedPosts);
                }
                if (actionListener != null) {
                    actionListener.onActionResult(success);
                }

            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                ReaderPostTable.addOrUpdatePosts(null, blockResult.deletedPosts);
                if (actionListener != null) {
                    actionListener.onActionResult(false);
                }
            }
        };

        AppLog.i(T.READER, "blocking blog " + blogId);
        String path = "/me/block/sites/" + Long.toString(blogId) + "/new";
        WordPress.getRestClientUtils().post(path, listener, errorListener);

        return blockResult;
    }

    public static void undoBlockBlogFromReader(final BlockedBlogResult blockResult) {
        if (blockResult == null) {
            return;
        }
        if (blockResult.deletedPosts != null) {
            ReaderPostTable.addOrUpdatePosts(null, blockResult.deletedPosts);
        }

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                boolean success = (jsonObject != null && jsonObject.optBoolean("success"));
                // re-follow the blog if it was being followed prior to the block
                if (success && blockResult.wasFollowing) {
                    performFollowAction(blockResult.blogId, null, true, null);
                } else if (!success) {
                    AppLog.w(T.READER, "failed to unblock blog " + blockResult.blogId);
                }

            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
            }
        };

        AppLog.i(T.READER, "unblocking blog " + blockResult.blogId);
        String path = "/me/block/sites/" + Long.toString(blockResult.blogId) + "/delete";
        WordPress.getRestClientUtils().post(path, listener, errorListener);
    }
}
