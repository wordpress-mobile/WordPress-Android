package org.wordpress.android.ui.reader_native.actions;

import android.content.Context;
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
import org.wordpress.android.datasets.ReaderTopicTable;
import org.wordpress.android.datasets.ReaderUserTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostList;
import org.wordpress.android.models.ReaderTopic;
import org.wordpress.android.models.ReaderUserIdList;
import org.wordpress.android.models.ReaderUserList;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.ReaderLog;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.VolleyUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nbradbury on 8/13/13.
 */
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
    public static boolean performPostAction(Context context,
                                            final PostAction action,
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
                path = "sites/" + post.blogId + "/follows/";
                if (isAskingToFollow) {
                    path += "new";
                } else {
                    path += "mine/delete";
                }
                if (post.hasBlogUrl())
                    ReaderBlogTable.setIsFollowedBlogUrl(post.getBlogUrl(), isAskingToFollow);
                break;

            default :
                //ToastUtils.notImplemented(context);
                if (actionListener!=null)
                    actionListener.onActionResult(false);
                return false;
        }

        // make API call, and revert to the original post on error
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                ReaderLog.d("post action " + action.name() + " succeeded");
                if (actionListener!=null)
                    actionListener.onActionResult(true);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                String error = VolleyUtils.errStringFromVolleyError(volleyError);
                if (TextUtils.isEmpty(error)) {
                    ReaderLog.w(String.format("post action %s failed", action.name()));
                } else {
                    ReaderLog.w(String.format("post action %s failed (%s)", action.name(), error));
                }
                ReaderLog.e(volleyError);
                // revert to original post
                if (originalPost!=null) {
                    ReaderPostTable.addOrUpdatePost(originalPost);
                    switch (action) {
                        case TOGGLE_LIKE:
                            ReaderLikeTable.setCurrentUserLikesPost(post, originalPost.isLikedByCurrentUser);
                            break;
                        case TOGGLE_FOLLOW :
                            if (originalPost.hasBlogUrl())
                                ReaderBlogTable.setIsFollowedBlogUrl(post.getBlogUrl(), originalPost.isFollowedByCurrentUser);
                           break;
                    }
                }
                if (actionListener!=null)
                    actionListener.onActionResult(false);
            }
        };
        WordPress.restClient.post(path, listener, errorListener);

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
        if (post==null) {
            if (actionListener!=null)
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
                ReaderLog.e(volleyError);
                if (actionListener != null)
                    actionListener.onActionResult(false);

            }
        };

        WordPress.restClient.post(sb.toString(), params, null, listener, errorListener);
    }

    /*
     * get the latest version of this post
     */
    public static void updatePost(final ReaderPost post, final ReaderActions.UpdateResultListener resultListener) {
        if (post.blogId==0)
            ReaderLog.w("updating post with no blogId");

        String path = "sites/" + post.blogId + "/posts/" + post.postId;

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleUpdatePostResponse(post, jsonObject, resultListener);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                ReaderLog.e(volleyError);
                if (resultListener!=null)
                    resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);

            }
        };
        ReaderLog.d("updating post");
        WordPress.restClient.get(path, null, null, listener, errorListener);
    }
    private static void handleUpdatePostResponse(final ReaderPost post,
                                                 final JSONObject jsonObject,
                                                 final ReaderActions.UpdateResultListener resultListener) {
        if (jsonObject==null) {
            if (resultListener!=null)
                resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);
            return;
        }

        final Handler handler = new Handler();

        new Thread() {
            @Override
            public void run() {
                // IMPORTANT: this API call returns the post in a different format, so skip parsing
                // the entire post and just check for the specific changes we care about here
                int numReplies = jsonObject.optInt("comment_count");
                int numLikes = jsonObject.optInt("like_count");
                boolean isLiked = JSONUtil.getBool(jsonObject, "i_like");
                boolean isCommentsOpen = JSONUtil.getBool(jsonObject, "comments_open");
                boolean isFollowed = JSONUtil.getBool(jsonObject, "is_following");

                final boolean hasChanges = (numReplies != post.numReplies
                                         || numLikes != post.numLikes
                                         || isLiked  != post.isLikedByCurrentUser
                                         || isCommentsOpen != post.isCommentsOpen
                                         || isFollowed != post.isFollowedByCurrentUser);

                if (hasChanges) {
                    ReaderLog.d("post updated");
                    post.numLikes = numLikes;
                    post.numReplies = numReplies;
                    post.isCommentsOpen = isCommentsOpen;
                    post.isLikedByCurrentUser = isLiked;
                    post.isFollowedByCurrentUser = isFollowed;
                    ReaderPostTable.addOrUpdatePost(post);
                }

                if (resultListener!=null) {
                    handler.post(new Runnable() {
                        public void run() {
                            resultListener.onUpdateResult(hasChanges ? ReaderActions.UpdateResult.CHANGED : ReaderActions.UpdateResult.UNCHANGED);
                        }
                    });
                }
            }
        }.start();
    }

    /**
     * get the latest likes for this post
     **/
    public static void updateLikesForPost(final ReaderPost post, final ReaderActions.UpdateResultListener resultListener) {
        String path = "sites/" + post.blogId + "/posts/" + post.postId + "/likes/";
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleUpdateLikesResponse(jsonObject, post, resultListener);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                ReaderLog.e(volleyError);
                if (resultListener!=null)
                    resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);

            }
        };
        ReaderLog.d("updating likes");
        WordPress.restClient.get(path, null, null, listener, errorListener);
    }
    private static void handleUpdateLikesResponse(final JSONObject jsonObject, final ReaderPost post, final ReaderActions.UpdateResultListener resultListener) {
        if (jsonObject==null)
            return;

        final Handler handler = new Handler();
        new Thread() {
            @Override
            public void run() {
                ReaderUserList serverUsers = ReaderUserList.fromJsonLikes(jsonObject);
                ReaderUserIdList localLikeIDs = ReaderLikeTable.getLikesForPost(post);
                ReaderUserIdList serverLikeIDs = serverUsers.getUserIds();

                final boolean hasChanges = !localLikeIDs.isSameList(serverLikeIDs);
                if (hasChanges) {
                    ReaderLog.d("new likes found");
                    post.numLikes = jsonObject.optInt("found");
                    post.isLikedByCurrentUser = JSONUtil.getBool(jsonObject, "i_like");
                    ReaderPostTable.addOrUpdatePost(post);
                    ReaderUserTable.addOrUpdateUsers(serverUsers);
                    ReaderLikeTable.setLikesForPost(post, serverLikeIDs);
                }

                if (resultListener!=null) {
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
     * get the latest posts in the passed topic - note that this uses an UpdateResultAndCountListener
     * so the caller can be told how many new posts were added
     */
    public static void updatePostsInTopic(final String topicName,
                                          final ReaderActions.RequestDataAction updateAction,
                                          final ReaderActions.UpdateResultAndCountListener resultListener) {
        final ReaderTopic topic = ReaderTopicTable.getTopic(topicName);
        if (topic==null) {
            if (resultListener!=null)
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
        if (ReaderPostTable.hasPostsInTopic(topicName)) {
            switch (updateAction) {
                case LOAD_NEWER:
                    String dateNewest = ReaderTopicTable.getTopicNewestDate(topicName);
                    if (!TextUtils.isEmpty(dateNewest)) {
                        sb.append("&after=").append(UrlUtils.urlEncode(dateNewest));
                        ReaderLog.d(String.format("requesting newer posts in topic %s (%s)", topicName, dateNewest));
                    }
                    break;

                case LOAD_OLDER:
                    String dateOldest = ReaderTopicTable.getTopicOldestDate(topicName);
                    // if oldest date isn't stored, it means we haven't requested older posts until
                    // now, so use the date of the oldest stored post
                    if (TextUtils.isEmpty(dateOldest))
                        dateOldest = ReaderPostTable.getOldestPubDateInTopic(topicName);
                    if (!TextUtils.isEmpty(dateOldest)) {
                        sb.append("&before=").append(UrlUtils.urlEncode(dateOldest));
                        ReaderLog.d(String.format("requesting older posts in topic %s (%s)", topicName, dateOldest));
                    }
                    break;
            }
        } else {
            ReaderLog.d(String.format("requesting posts in empty topic %s", topicName));
        }

        String endpoint = sb.toString();

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleUpdatePostsInTopicResponse(topicName, updateAction, jsonObject, resultListener);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                ReaderLog.e(volleyError);
                if (resultListener!=null)
                    resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED, -1);
            }
        };

        WordPress.restClient.get(endpoint, null, null, listener, errorListener);
    }
    private static void handleUpdatePostsInTopicResponse(final String topicName,
                                                         final ReaderActions.RequestDataAction updateAction,
                                                         final JSONObject jsonObject,
                                                         final ReaderActions.UpdateResultAndCountListener resultListener) {
        if (jsonObject==null) {
            if (resultListener!=null)
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
                if (updateAction==ReaderActions.RequestDataAction.LOAD_NEWER)
                    ReaderTopicTable.setTopicLastUpdated(topicName, DateTimeUtils.javaDateToIso8601(new Date()));

                // json "date_range" tells the the range of dates in the response, which we want to
                // store for use the next time we request newer/older if this response contained any
                // posts - note that freshly-pressed uses "newest" and "oldest" but other endpoints
                // use "after" and "before"
                JSONObject jsonDateRange = jsonObject.optJSONObject("date_range");

                if (responseHasPosts && jsonDateRange!=null) {
                    switch (updateAction) {
                        case LOAD_NEWER:
                            String newest = jsonDateRange.has("before") ? JSONUtil.getString(jsonDateRange, "before") : JSONUtil.getString(jsonDateRange, "newest");
                            if (!TextUtils.isEmpty(newest))
                                ReaderTopicTable.setTopicNewestDate(topicName, newest);
                            break;
                        case LOAD_OLDER:
                            String oldest = jsonDateRange.has("after") ? JSONUtil.getString(jsonDateRange, "after") : JSONUtil.getString(jsonDateRange, "oldest");
                            if (!TextUtils.isEmpty(oldest))
                                ReaderTopicTable.setTopicOldestDate(topicName, oldest);
                            break;
                    }
                }

                if (!responseHasPosts) {
                    if (resultListener!=null) {
                        handler.post(new Runnable() {
                            public void run() {
                                resultListener.onUpdateResult(ReaderActions.UpdateResult.UNCHANGED, 0);
                            }
                        });
                    }
                    return;
                }

                // determine how many of the downloaded posts are new (response will contain both
                // new posts and posts updated since the last call)
                final int numNewPosts = ReaderPostTable.getNumNewPostsInTopic(topicName, serverPosts);

                ReaderLog.d(String.format("retrieved %d posts (%d new) in topic %s", serverPosts.size(), numNewPosts, topicName));

                // save the posts even if none are new in order to update comment counts, likes, etc., on existing posts
                ReaderPostTable.addOrUpdatePosts(topicName, serverPosts);

                if (resultListener!=null) {
                    handler.post(new Runnable() {
                        public void run() {
                            // always pass CHANGED as the result even if there are no new posts (since if
                            // get this far, it means there are changed - updated - posts)
                            resultListener.onUpdateResult(ReaderActions.UpdateResult.CHANGED, numNewPosts);
                        }
                    });
                }
            }
        }.start();
    }
}
