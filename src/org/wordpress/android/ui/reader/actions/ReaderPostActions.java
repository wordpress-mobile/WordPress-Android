package org.wordpress.android.ui.reader.actions;

import android.os.Handler;
import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.Constants;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.datasets.ReaderLikeTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.datasets.ReaderUserTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostList;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderUserList;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.VolleyUtils;
import org.wordpress.android.util.stats.AnalyticsTracker;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ReaderPostActions {

    public enum PostAction {TOGGLE_LIKE, TOGGLE_FOLLOW}

    private ReaderPostActions() {
        throw new AssertionError();
    }

    /**
     * perform the passed action on the passed post both locally and via API - this is
     * optimistic in that changes to the passed post are made locally before calling API.
     * this routines is also non-blocking - it returns before the API call completes, so
     * callers that want to be alerted when the API call completes should pass a listener
     **/
    public static boolean performPostAction(final PostAction action,
                                            final ReaderPost post,
                                            final ReaderActions.ActionListener actionListener) {
        // get post BEFORE we make changes so we can revert on error
        final ReaderPost originalPost = ReaderPostTable.getPost(post.blogId, post.postId);

        // change local post and determine API endpoint
        String path;
        switch (action) {
            case TOGGLE_LIKE:
                boolean isLiking = !post.isLikedByCurrentUser;
                post.isLikedByCurrentUser = isLiking;
                if (isLiking) {
                    post.numLikes++;
                } else if (!isLiking && post.numLikes > 0) {
                    post.numLikes--;
                }
                ReaderPostTable.addOrUpdatePost(post);
                ReaderLikeTable.setCurrentUserLikesPost(post, isLiking);
                path = "sites/" + post.blogId + "/posts/" + post.postId + "/likes/";
                if (isLiking) {
                    path += "new";
                } else {
                    path += "mine/delete";
                }
                break;

            case TOGGLE_FOLLOW :
                boolean isAskingToFollow = !post.isFollowedByCurrentUser;
                post.isFollowedByCurrentUser = isAskingToFollow;
                ReaderPostTable.addOrUpdatePost(post);
                ReaderPostTable.setBlogPostsFollowStatus(post.blogId, isAskingToFollow);
                path = "sites/" + post.blogId + "/follows/";
                if (isAskingToFollow) {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.READER_FOLLOWED_SITE);
                    path += "new";
                } else {
                    path += "mine/delete";
                }
                if (post.hasBlogUrl())
                    ReaderBlogTable.setIsFollowedBlogUrl(post.getBlogUrl(), isAskingToFollow);
                break;

            default :
                if (actionListener != null)
                    actionListener.onActionResult(false);
                return false;
        }

        // make API call, and revert to the original post on error
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                AppLog.d(T.READER, "post action " + action.name() + " succeeded");
                if (actionListener != null)
                    actionListener.onActionResult(true);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                String error = VolleyUtils.errStringFromVolleyError(volleyError);
                if (TextUtils.isEmpty(error)) {
                    AppLog.w(T.READER, String.format("post action %s failed", action.name()));
                } else {
                    AppLog.w(T.READER, String.format("post action %s failed (%s)", action.name(), error));
                }
                AppLog.e(T.READER, volleyError);
                // revert to original post
                if (originalPost!=null) {
                    ReaderPostTable.addOrUpdatePost(originalPost);
                    switch (action) {
                        case TOGGLE_LIKE:
                            ReaderLikeTable.setCurrentUserLikesPost(post, originalPost.isLikedByCurrentUser);
                            break;
                        case TOGGLE_FOLLOW :
                            ReaderPostTable.setBlogPostsFollowStatus(originalPost.blogId, originalPost.isFollowedByCurrentUser);
                            if (originalPost.hasBlogUrl())
                                ReaderBlogTable.setIsFollowedBlogUrl(post.getBlogUrl(), originalPost.isFollowedByCurrentUser);
                           break;
                    }
                }
                if (actionListener != null)
                    actionListener.onActionResult(false);
            }
        };
        WordPress.getRestClientUtils().post(path, listener, errorListener);

        return true;
    }

    /*
     * reblogs the passed post to the passed destination with optional comment
     * https://developer.wordpress.com/docs/api/1/post/sites/%24site/posts/%24post_ID/reblogs/new/
     */
    public static void reblogPost(final ReaderPost post,
                                  long destinationBlogId,
                                  final String optionalComment,
                                  final ReaderActions.ActionListener actionListener) {
        if (post == null) {
            if (actionListener != null)
                actionListener.onActionResult(false);
            return;
        }

        Map<String, String> params = new HashMap<String, String>();
        params.put("destination_site_id", Long.toString(destinationBlogId));
        if (!TextUtils.isEmpty(optionalComment))
            params.put("note", optionalComment);

        StringBuilder sb = new StringBuilder("/sites/")
                .append(post.blogId)
                .append("/posts/")
                .append(post.postId)
                .append("/reblogs/new");

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                boolean isReblogged = (jsonObject!=null ? JSONUtil.getBool(jsonObject, "is_reblogged") : false);
                //boolean success = (jsonObject!=null ? JSONUtil.getBool(jsonObject, "success") : false);
                if (isReblogged)
                    ReaderPostTable.setPostReblogged(post, true);
                if (actionListener != null)
                    actionListener.onActionResult(isReblogged);
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

        WordPress.getRestClientUtils().post(sb.toString(), params, null, listener, errorListener);
    }

    /*
     * get the latest version of this post - note that the post is only considered changed if the
     * like/comment count has changed, or if the current user's like/follow status has changed
     */
    public static void updatePost(final ReaderPost post, final ReaderActions.UpdateResultListener resultListener) {
        String path = "sites/" + post.blogId + "/posts/" + post.postId + "/?meta=site,likes";

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleUpdatePostResponse(post, jsonObject, resultListener);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                if (resultListener!=null)
                    resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);

            }
        };
        AppLog.d(T.READER, "updating post");
        WordPress.getRestClientUtils().get(path, null, null, listener, errorListener);
    }

    private static void handleUpdatePostResponse(final ReaderPost post,
                                                 final JSONObject jsonObject,
                                                 final ReaderActions.UpdateResultListener resultListener) {
        if (jsonObject == null) {
            if (resultListener != null)
                resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);
            return;
        }

        final Handler handler = new Handler();

        new Thread() {
            @Override
            public void run() {
                ReaderPost updatedPost = ReaderPost.fromJson(jsonObject);
                final boolean hasChanges = (updatedPost.numReplies != post.numReplies
                                         || updatedPost.numLikes != post.numLikes
                                         || updatedPost.isCommentsOpen != post.isCommentsOpen
                                         || updatedPost.isLikedByCurrentUser != post.isLikedByCurrentUser
                                         || updatedPost.isFollowedByCurrentUser != post.isFollowedByCurrentUser);

                if (hasChanges) {
                    AppLog.d(T.READER, "post updated");
                    // the endpoint for requesting a single post doesn't support featured images,
                    // so if the original post had a featured image, set the featured image for
                    // the updated post to that of the original post - this should be done even
                    // if the updated post has a featured image since that was most likely
                    // assigned by ReaderPost.findFeaturedImage()
                    if (post.hasFeaturedImage()) {
                        updatedPost.setFeaturedImage(post.getFeaturedImage());
                    }
                    // likewise for featured video
                    if (post.hasFeaturedVideo()) {
                        updatedPost.setFeaturedVideo(post.getFeaturedVideo());
                        updatedPost.isVideoPress = post.isVideoPress;
                    }
                    ReaderPostTable.addOrUpdatePost(updatedPost);
                }

                // always update liking users regardless of whether changes were detected - this
                // ensures that the liking avatars are immediately available to post detail
                handlePostLikes(updatedPost, jsonObject);

                if (resultListener != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            resultListener.onUpdateResult(hasChanges ? ReaderActions.UpdateResult.CHANGED : ReaderActions.UpdateResult.UNCHANGED);
                        }
                    });
                }
            }
        }.start();
    }

    /*
     * updates local liking users based on the "likes" meta section of the post's json - requires
     * using the /sites/ endpoint with ?meta=likes
     */
    private static void handlePostLikes(final ReaderPost post, JSONObject jsonPost) {
        if (post == null || jsonPost == null)
            return;

        JSONObject jsonLikes = JSONUtil.getJSONChild(jsonPost, "meta/data/likes");
        if (jsonLikes == null)
            return;

        ReaderUserList likingUsers = ReaderUserList.fromJsonLikes(jsonLikes);
        ReaderUserTable.addOrUpdateUsers(likingUsers);
        ReaderLikeTable.setLikesForPost(post, likingUsers.getUserIds());
    }

    /**
     * similar to updatePost, but used when post doesn't already exist in local db
     **/
    public static void requestPost(final long blogId, final long postId, final ReaderActions.ActionListener actionListener) {
        String path = "sites/" + blogId + "/posts/" + postId + "/?meta=site,likes";

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                ReaderPost post = ReaderPost.fromJson(jsonObject);
                // make sure the post has the passed blogId so it's saved correctly - necessary
                // since the /sites/ endpoints return site_id="1" for Jetpack-powered blogs
                post.blogId = blogId;
                ReaderPostTable.addOrUpdatePost(post);
                handlePostLikes(post, jsonObject);
                if (actionListener != null)
                    actionListener.onActionResult(true);
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
        AppLog.d(T.READER, "requesting post");
        WordPress.getRestClientUtils().get(path, null, null, listener, errorListener);
    }

    /*
     * get the latest posts in the passed topic - note that this uses an UpdateResultAndCountListener
     * so the caller can be told how many new posts were added
     */
    public static void updatePostsWithTag(final String tagName,
                                          final ReaderActions.RequestDataAction updateAction,
                                          final ReaderActions.UpdateResultAndCountListener resultListener) {
//Debug.startMethodTracing("WordPress");
        final ReaderTag topic = ReaderTagTable.getTag(tagName);
        if (topic == null) {
            if (resultListener != null)
                resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED, -1);
            return;
        }

        StringBuilder sb = new StringBuilder(topic.getEndpoint());

        // append #posts to retrieve
        sb.append("?number=").append(Constants.READER_MAX_POSTS_TO_REQUEST);

        // return newest posts first (this is the default, but make it explicit since it's important)
        sb.append("&order=DESC");

        // apply the after/before to limit results based on previous update, but only if there are
        // existing posts in this topic
        if (ReaderPostTable.hasPostsWithTag(tagName)) {
            switch (updateAction) {
                case LOAD_NEWER:
                    String dateNewest = ReaderTagTable.getTagNewestDate(tagName);
                    if (!TextUtils.isEmpty(dateNewest)) {
                        sb.append("&after=").append(UrlUtils.urlEncode(dateNewest));
                        AppLog.d(T.READER, String.format("requesting newer posts in topic %s (%s)", tagName, dateNewest));
                    }
                    break;

                case LOAD_OLDER:
                    String dateOldest = ReaderTagTable.getTagOldestDate(tagName);
                    // if oldest date isn't stored, it means we haven't requested older posts until
                    // now, so use the date of the oldest stored post
                    if (TextUtils.isEmpty(dateOldest))
                        dateOldest = ReaderPostTable.getOldestPubDateWithTag(tagName);
                    if (!TextUtils.isEmpty(dateOldest)) {
                        sb.append("&before=").append(UrlUtils.urlEncode(dateOldest));
                        AppLog.d(T.READER, String.format("requesting older posts in topic %s (%s)", tagName, dateOldest));
                    }
                    break;
            }
        } else {
            AppLog.d(T.READER, String.format("requesting posts in empty topic %s", tagName));
        }

        String endpoint = sb.toString();

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleUpdatePostsWithTagResponse(tagName, updateAction, jsonObject, resultListener);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                if (resultListener!=null)
                    resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED, -1);
            }
        };

        WordPress.getRestClientUtils().get(endpoint, null, null, listener, errorListener);
    }
    private static void handleUpdatePostsWithTagResponse(final String tagName,
                                                         final ReaderActions.RequestDataAction updateAction,
                                                         final JSONObject jsonObject,
                                                         final ReaderActions.UpdateResultAndCountListener resultListener) {
        if (jsonObject == null) {
            if (resultListener != null)
                resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED, -1);
            return;
        }
        final Handler handler = new Handler();

        new Thread() {
            @Override
            public void run() {
                ReaderPostList serverPosts = ReaderPostList.fromJson(jsonObject);
                final boolean responseHasPosts = (serverPosts.size() > 0);

                // remember when this topic was updated if newer posts were requested, regardless of
                // whether the response contained any posts
                if (updateAction == ReaderActions.RequestDataAction.LOAD_NEWER)
                    ReaderTagTable.setTagLastUpdated(tagName, DateTimeUtils.javaDateToIso8601(new Date()));

                // json "date_range" tells the the range of dates in the response, which we want to
                // store for use the next time we request newer/older if this response contained any
                // posts - note that freshly-pressed uses "newest" and "oldest" but other endpoints
                // use "after" and "before"
                JSONObject jsonDateRange = jsonObject.optJSONObject("date_range");

                if (responseHasPosts && jsonDateRange != null) {
                    switch (updateAction) {
                        case LOAD_NEWER:
                            String newest = jsonDateRange.has("before") ? JSONUtil.getString(jsonDateRange, "before") : JSONUtil.getString(jsonDateRange, "newest");
                            if (!TextUtils.isEmpty(newest))
                                ReaderTagTable.setTagNewestDate(tagName, newest);
                            break;
                        case LOAD_OLDER:
                            String oldest = jsonDateRange.has("after") ? JSONUtil.getString(jsonDateRange, "after") : JSONUtil.getString(jsonDateRange, "oldest");
                            if (!TextUtils.isEmpty(oldest))
                                ReaderTagTable.setTagOldestDate(tagName, oldest);
                            break;
                    }
                }

                if (!responseHasPosts) {
                    AppLog.d(T.READER, String.format("no new posts in topic %s", tagName));
                    if (resultListener != null) {
                        handler.post(new Runnable() {
                            public void run() {
                                resultListener.onUpdateResult(ReaderActions.UpdateResult.UNCHANGED, 0);
                            }
                        });
                    }
                    return;
                }

                // determine how many of the downloaded posts are new (response will contain both
                // new posts and posts updated since the last call), then save the posts even if
                // none are new in order to update comment counts, likes, etc., on existing posts
                final int numNewPosts = ReaderPostTable.getNumNewPostsWithTag(tagName, serverPosts);
                ReaderPostTable.addOrUpdatePosts(tagName, serverPosts);

                AppLog.d(T.READER, String.format("retrieved %d posts (%d new) in topic %s", serverPosts.size(), numNewPosts, tagName));

                if (resultListener != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            // always pass CHANGED as the result even if there are no new posts (since if
                            // get this far, it means there are changed - updated - posts)
                            resultListener.onUpdateResult(ReaderActions.UpdateResult.CHANGED, numNewPosts);
                        }
                    });
                }
//Debug.stopMethodTracing();
            }
        }.start();
    }
}
