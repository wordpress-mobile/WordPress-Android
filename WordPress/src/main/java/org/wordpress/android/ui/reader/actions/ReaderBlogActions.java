package org.wordpress.android.ui.reader.actions;

import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.wordpress.rest.RestRequest;

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
import org.wordpress.android.util.analytics.AnalyticsUtils;

import java.net.HttpURLConnection;

public class ReaderBlogActions {
    public static class BlockedBlogResult {
        public long blogId;
        public ReaderPostList deletedPosts;
        public boolean wasFollowing;
    }

    private static String jsonToString(JSONObject json) {
        return (json != null ? json.toString() : "");
    }

    public static boolean followBlogById(final long blogId,
                                         final boolean isAskingToFollow,
                                         final ActionListener actionListener) {
        if (blogId == 0) {
            ReaderActions.callActionListener(actionListener, false);
            return false;
        }

        ReaderBlogTable.setIsFollowedBlogId(blogId, isAskingToFollow);
        ReaderPostTable.setFollowStatusForPostsInBlog(blogId, isAskingToFollow);

        if (isAskingToFollow) {
            AnalyticsUtils.trackWithSiteId(AnalyticsTracker.Stat.READER_BLOG_FOLLOWED, blogId);
        } else {
            AnalyticsUtils.trackWithSiteId(AnalyticsTracker.Stat.READER_BLOG_UNFOLLOWED, blogId);
        }

        final String actionName = (isAskingToFollow ? "follow" : "unfollow");
        final String path = "sites/" + blogId + "/follows/" + (isAskingToFollow ? "new?source=android" : "mine/delete");

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                boolean success = isFollowActionSuccessful(jsonObject, isAskingToFollow);
                if (success) {
                    AppLog.d(T.READER, "blog " + actionName + " succeeded");
                } else {
                    AppLog.w(T.READER, "blog " + actionName + " failed - " + jsonToString(jsonObject) + " - " + path);
                    localRevertFollowBlogId(blogId, isAskingToFollow);
                }
                ReaderActions.callActionListener(actionListener, success);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.w(T.READER, "blog " + actionName + " failed with error");
                AppLog.e(T.READER, volleyError);
                // check if we get a 403 when unfollowing - this will happen when we attempt
                // to unfollow a blog that no longer exists - the workaround is to unfollow
                // by url - note that the v1.2 endpoint will return a 404 in this situation
                int status = VolleyUtils.statusCodeFromVolleyError(volleyError);
                if (status == 403 && !isAskingToFollow) {
                    internalUnfollowBlogByUrl(blogId, actionListener);
                } else {
                    localRevertFollowBlogId(blogId, isAskingToFollow);
                    ReaderActions.callActionListener(actionListener, false);
                }
            }
        };
        WordPress.getRestClientUtilsV1_1().post(path, listener, errorListener);

        return true;
    }

    private static void internalUnfollowBlogByUrl(long blogId,
                                                  final ActionListener actionListener) {
        String blogUrl = ReaderBlogTable.getBlogUrl(blogId);
        if (TextUtils.isEmpty(blogUrl)) {
            AppLog.w(T.READER, "URL not found for blogId " + blogId);
            ReaderActions.callActionListener(actionListener, false);
            return;
        }

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                ReaderActions.callActionListener(actionListener, true);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                AppLog.e(T.READER, error);
                ReaderActions.callActionListener(actionListener, false);
            }
        };

        String path = "/read/following/mine/delete?url=" + UrlUtils.urlEncode(blogUrl);
        WordPress.getRestClientUtilsV1_1().post(path, listener, errorListener);
    }

    public static boolean followFeedById(final long feedId,
                                         final boolean isAskingToFollow,
                                         final ActionListener actionListener) {
        ReaderBlog blogInfo = ReaderBlogTable.getFeedInfo(feedId);
        if (blogInfo != null) {
            return internalFollowFeed(blogInfo.feedId, blogInfo.getFeedUrl(), isAskingToFollow, actionListener);
        }

        updateFeedInfo(feedId, null, new UpdateBlogInfoListener() {
            @Override
            public void onResult(ReaderBlog blogInfo) {
                if (blogInfo != null) {
                    internalFollowFeed(
                            blogInfo.feedId,
                            blogInfo.getFeedUrl(),
                            isAskingToFollow,
                            actionListener);
                } else {
                    ReaderActions.callActionListener(actionListener, false);
                }
            }
        });

        return true;
    }

    public static void followFeedByUrl(final String feedUrl,
                                       final ActionListener actionListener) {
        if (TextUtils.isEmpty(feedUrl)) {
            ReaderActions.callActionListener(actionListener, false);
            return;
        }

        // use existing blog info if we can
        ReaderBlog blogInfo = ReaderBlogTable.getFeedInfo(ReaderBlogTable.getFeedIdFromUrl(feedUrl));
        if (blogInfo != null) {
            internalFollowFeed(blogInfo.feedId, blogInfo.getFeedUrl(), true, actionListener);
            return;
        }

        // otherwise, look it up via the endpoint
        updateFeedInfo(0, feedUrl, new UpdateBlogInfoListener() {
            @Override
            public void onResult(ReaderBlog blogInfo) {
                // note we attempt to follow even when the look up fails (blogInfo = null) because that
                // endpoint doesn't perform feed discovery, whereas the endpoint to follow a feed does
                long feedIdToFollow = blogInfo != null ? blogInfo.feedId : 0;
                String feedUrlToFollow = (blogInfo != null && blogInfo.hasFeedUrl()) ? blogInfo.getFeedUrl() : feedUrl;
                internalFollowFeed(
                        feedIdToFollow,
                        feedUrlToFollow,
                        true,
                        actionListener);
            }
        });
    }

    private static boolean internalFollowFeed(
            final long feedId,
            final String feedUrl,
            final boolean isAskingToFollow,
            final ActionListener actionListener) {
        // feedUrl is required
        if (TextUtils.isEmpty(feedUrl)) {
            ReaderActions.callActionListener(actionListener, false);
            return false;
        }

        if (feedId != 0) {
            ReaderBlogTable.setIsFollowedFeedId(feedId, isAskingToFollow);
            ReaderPostTable.setFollowStatusForPostsInFeed(feedId, isAskingToFollow);
        }

        if (isAskingToFollow) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.READER_BLOG_FOLLOWED);
        } else {
            AnalyticsTracker.track(AnalyticsTracker.Stat.READER_BLOG_UNFOLLOWED);
        }

        final String actionName = (isAskingToFollow ? "follow" : "unfollow");
        final String path = "read/following/mine/"
                            + (isAskingToFollow ? "new?source=android&url=" : "delete?url=")
                            + UrlUtils.urlEncode(feedUrl);

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                boolean success = isFollowActionSuccessful(jsonObject, isAskingToFollow);
                if (success) {
                    AppLog.d(T.READER, "feed " + actionName + " succeeded");
                } else {
                    AppLog.w(T.READER, "feed " + actionName + " failed - " + jsonToString(jsonObject) + " - " + path);
                    localRevertFollowFeedId(feedId, isAskingToFollow);
                }
                ReaderActions.callActionListener(actionListener, success);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.w(T.READER, "feed " + actionName + " failed with error");
                AppLog.e(T.READER, volleyError);
                localRevertFollowFeedId(feedId, isAskingToFollow);
                ReaderActions.callActionListener(actionListener, false);
            }
        };
        WordPress.getRestClientUtilsV1_1().post(path, listener, errorListener);

        return true;
    }

    /*
     * helper routine when following a blog from a post view
     */
    public static boolean followBlogForPost(ReaderPost post,
                                            boolean isAskingToFollow,
                                            ActionListener actionListener) {
        if (post == null) {
            AppLog.w(T.READER, "follow action performed with null post");
            ReaderActions.callActionListener(actionListener, false);
            return false;
        }
        if (post.feedId != 0) {
            return followFeedById(post.feedId, isAskingToFollow, actionListener);
        } else {
            return followBlogById(post.blogId, isAskingToFollow, actionListener);
        }
    }

    /*
     * called when a follow/unfollow fails, restores local data to previous state
     */
    private static void localRevertFollowBlogId(long blogId, boolean isAskingToFollow) {
        ReaderBlogTable.setIsFollowedBlogId(blogId, !isAskingToFollow);
        ReaderPostTable.setFollowStatusForPostsInBlog(blogId, !isAskingToFollow);
    }

    private static void localRevertFollowFeedId(long feedId, boolean isAskingToFollow) {
        ReaderBlogTable.setIsFollowedFeedId(feedId, !isAskingToFollow);
        ReaderPostTable.setFollowStatusForPostsInFeed(feedId, !isAskingToFollow);
    }

    /*
     * returns whether a follow/unfollow was successful based on the response to:
     * read/follows/new
     * read/follows/delete
     * site/$site/follows/new
     * site/$site/follows/mine/delete
     */
    private static boolean isFollowActionSuccessful(JSONObject json, boolean isAskingToFollow) {
        if (json == null) {
            return false;
        }

        boolean isSubscribed;
        if (json.has("subscribed")) {
            // read/follows/
            isSubscribed = json.optBoolean("subscribed", false);
        } else {
            // site/$site/follows/
            isSubscribed = json.has("is_following") && json.optBoolean("is_following", false);
        }
        return (isSubscribed == isAskingToFollow);
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
                boolean isAuthErr = (statusCode == HttpURLConnection.HTTP_FORBIDDEN);
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
            WordPress.getRestClientUtilsV1_1().get("read/sites/" + blogId, listener, errorListener);
        } else {
            WordPress.getRestClientUtilsV1_1()
                     .get("read/sites/" + UrlUtils.urlEncode(UrlUtils.getHost(blogUrl)), listener, errorListener);
        }
    }

    public static void updateFeedInfo(long feedId, String feedUrl, final UpdateBlogInfoListener infoListener) {
        // must pass either a valid id or url
        final boolean hasFeedId = (feedId != 0);
        final boolean hasFeedUrl = !TextUtils.isEmpty(feedUrl);
        if (!hasFeedId && !hasFeedUrl) {
            AppLog.w(T.READER, "cannot update Feed info without either id or url");
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
                AppLog.e(T.READER, volleyError);
                if (infoListener != null) {
                    infoListener.onResult(null);
                }
            }
        };
        String path;
        if (feedId != 0) {
            path = "read/feed/" + feedId;
        } else {
            path = "read/feed/" + UrlUtils.urlEncode(feedUrl);
        }
        WordPress.getRestClientUtilsV1_1().get(path, listener, errorListener);
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
    public static void checkUrlReachable(final String blogUrl,
                                         final ReaderActions.OnRequestListener requestListener) {
        // listener is required
        if (requestListener == null) {
            return;
        }

        Response.Listener<String> listener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                requestListener.onSuccess();
            }
        };
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                int statusCode;
                // check specifically for auth failure class rather than relying on status code
                // since a redirect to an unauthorized url may return a 301 rather than a 401
                if (volleyError instanceof com.android.volley.AuthFailureError) {
                    statusCode = 401;
                } else {
                    statusCode = VolleyUtils.statusCodeFromVolleyError(volleyError);
                }
                // Volley treats a 301 redirect as a failure here, we should treat it as
                // success since it means the blog url is reachable
                if (statusCode == 301) {
                    requestListener.onSuccess();
                } else {
                    requestListener.onFailure(statusCode);
                }
            }
        };

        // TODO: this should be a HEAD request, but even though Volley supposedly supports HEAD
        // using it results in "java.lang.IllegalStateException: Unknown method type"
        StringRequest request = new StringRequest(
                Request.Method.GET,
                blogUrl,
                listener,
                errorListener);
        WordPress.sRequestQueue.add(request);
    }

    /*
     * block a blog - result includes the list of posts that were deleted by the block so they
     * can be restored if the user undoes the block
     */
    public static BlockedBlogResult blockBlogFromReader(final long blogId, final ActionListener actionListener) {
        final BlockedBlogResult blockResult = new BlockedBlogResult();
        blockResult.blogId = blogId;
        blockResult.deletedPosts = ReaderPostTable.getPostsInBlog(blogId, 0, false);
        blockResult.wasFollowing = ReaderBlogTable.isFollowedBlog(blogId);

        ReaderPostTable.deletePostsInBlog(blogId);
        ReaderBlogTable.setIsFollowedBlogId(blogId, false);

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                ReaderActions.callActionListener(actionListener, true);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                ReaderPostTable.addOrUpdatePosts(null, blockResult.deletedPosts);
                if (blockResult.wasFollowing) {
                    ReaderBlogTable.setIsFollowedBlogId(blogId, true);
                }
                ReaderActions.callActionListener(actionListener, false);
            }
        };

        AppLog.i(T.READER, "blocking blog " + blogId);
        String path = "me/block/sites/" + Long.toString(blogId) + "/new";
        WordPress.getRestClientUtilsV1_1().post(path, listener, errorListener);

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
                    followBlogById(blockResult.blogId, true, null);
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
        String path = "me/block/sites/" + Long.toString(blockResult.blogId) + "/delete";
        WordPress.getRestClientUtilsV1_1().post(path, listener, errorListener);
    }

    /**
     * Updates the imageUrl for a feed. Since the /read/feed/{feedId} endpoint does not have this data,
     * the request is made to the /read/sites/{blogId} endpoint.
     * @param blogId
     * @param blogUrl
     * @param feedId
     * @param infoListener
     */
    public static void updateImageForFeedByBlog(long blogId,
                                      final String blogUrl,
                                      final long feedId,
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
                updateImageForFeedInfoResponse(feedId, jsonObject, infoListener);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                // authentication error may indicate that API access has been disabled for this blog
                int statusCode = VolleyUtils.statusCodeFromVolleyError(volleyError);
                boolean isAuthErr = (statusCode == HttpURLConnection.HTTP_FORBIDDEN);
                // if we failed to get the blog info using the id and this isn't an authentication
                // error, try again using just the domain
                if (!isAuthErr && hasBlogId && hasBlogUrl) {
                    AppLog.w(T.READER, "failed to get blog info by id, retrying with url");
                    updateImageForFeedByBlog(0, blogUrl, feedId, infoListener);
                } else {
                    AppLog.e(T.READER, volleyError);
                    if (infoListener != null) {
                        infoListener.onResult(null);
                    }
                }
            }
        };

        if (hasBlogId) {
            WordPress.getRestClientUtilsV1_1().get("read/sites/" + blogId, listener, errorListener);
        } else {
            WordPress.getRestClientUtilsV1_1()
                     .get("read/sites/" + UrlUtils.urlEncode(UrlUtils.getHost(blogUrl)), listener, errorListener);
        }
    }

    /**
     * updateImageForFeedInfoResponse is executed on return of updateImageForFeedByBlog. It sends a request
     * to the readerBlogTable to update the entry using feedId
     * @param feedId
     * @param jsonObject
     * @param infoListener
     */
    private static void updateImageForFeedInfoResponse(
            long feedId, JSONObject jsonObject, UpdateBlogInfoListener infoListener) {
        if (jsonObject == null) {
            if (infoListener != null) {
                infoListener.onResult(null);
            }
            return;
        }

        ReaderBlog blogInfo = ReaderBlog.fromJson(jsonObject);
        if (blogInfo.hasImageUrl()) {
            ReaderBlogTable.updateImageByFeedId(feedId, blogInfo.getImageUrl());
        }

        if (infoListener != null) {
            infoListener.onResult(blogInfo);
        }
    }
}
