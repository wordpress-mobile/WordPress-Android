package org.wordpress.android.ui.reader.actions;

import android.os.Handler;
import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderLikeTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.datasets.ReaderUserTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostList;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.models.ReaderUserIdList;
import org.wordpress.android.models.ReaderUserList;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener;
import org.wordpress.android.ui.reader.actions.ReaderActions.RequestDataAction;
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult;
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResultListener;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.VolleyUtils;

import java.util.HashMap;
import java.util.Map;

public class ReaderPostActions {

    private ReaderPostActions() {
        throw new AssertionError();
    }

    /**
     * like/unlike the passed post
     */
    public static boolean performLikeAction(final ReaderPost post,
                                            final boolean isAskingToLike) {
        // do nothing if post's like state is same as passed
        boolean isCurrentlyLiked = ReaderPostTable.isPostLikedByCurrentUser(post);
        if (isCurrentlyLiked == isAskingToLike) {
            AppLog.w(T.READER, "post like unchanged");
            return false;
        }

        // update like status and like count in local db
        int newNumLikes = (isAskingToLike ? post.numLikes + 1 : post.numLikes - 1);
        ReaderPostTable.setLikesForPost(post, newNumLikes, isAskingToLike);
        ReaderLikeTable.setCurrentUserLikesPost(post, isAskingToLike);

        final String actionName = isAskingToLike ? "like" : "unlike";
        String path = "/sites/" + post.blogId + "/posts/" + post.postId + "/likes/";
        if (isAskingToLike) {
            path += "new";
        } else {
            path += "mine/delete";
        }

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                AppLog.d(T.READER, String.format("post %s succeeded", actionName));
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                String error = VolleyUtils.errStringFromVolleyError(volleyError);
                if (TextUtils.isEmpty(error)) {
                    AppLog.w(T.READER, String.format("post %s failed", actionName));
                } else {
                    AppLog.w(T.READER, String.format("post %s failed (%s)", actionName, error));
                }
                AppLog.e(T.READER, volleyError);
                ReaderPostTable.setLikesForPost(post, post.numLikes, post.isLikedByCurrentUser);
                ReaderLikeTable.setCurrentUserLikesPost(post, post.isLikedByCurrentUser);
            }
        };

        WordPress.getRestClientUtilsV1_1().post(path, listener, errorListener);
        return true;
    }

    /*
     * reblogs the passed post to the passed destination with optional comment
     * https://developer.wordpress.com/docs/api/1/post/sites/%24site/posts/%24post_ID/reblogs/new/
     */
    public static void reblogPost(final ReaderPost post,
                                  long destinationBlogId,
                                  final String optionalComment,
                                  final ActionListener actionListener) {
        if (post == null) {
            if (actionListener != null) {
                actionListener.onActionResult(false);
            }
            return;
        }

        Map<String, String> params = new HashMap<String, String>();
        params.put("destination_site_id", Long.toString(destinationBlogId));
        if (!TextUtils.isEmpty(optionalComment)) {
            params.put("note", optionalComment);
        }

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                boolean isReblogged = (jsonObject != null && JSONUtil.getBool(jsonObject, "is_reblogged"));
                if (isReblogged) {
                    ReaderPostTable.setPostReblogged(post, true);
                }
                if (actionListener != null) {
                    actionListener.onActionResult(isReblogged);
                }
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                if (actionListener != null) {
                    actionListener.onActionResult(false);
                }

            }
        };

        String path = "/sites/" + post.blogId
                    + "/posts/" + post.postId
                    + "/reblogs/new";
        WordPress.getRestClientUtilsV1_1().post(path, params, null, listener, errorListener);
    }

    /*
     * get the latest version of this post - note that the post is only considered changed if the
     * like/comment count has changed, or if the current user's like/follow status has changed
     */
    public static void updatePost(final ReaderPost originalPost,
                                  final UpdateResultListener resultListener) {
        String path = "/sites/" + originalPost.blogId + "/posts/" + originalPost.postId + "/?meta=site,likes";

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleUpdatePostResponse(originalPost, jsonObject, resultListener);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                if (resultListener != null) {
                    resultListener.onUpdateResult(UpdateResult.FAILED);
                }
            }
        };
        AppLog.d(T.READER, "updating post");
        WordPress.getRestClientUtilsV1_1().get(path, null, null, listener, errorListener);
    }

    private static void handleUpdatePostResponse(final ReaderPost originalPost,
                                                 final JSONObject jsonObject,
                                                 final UpdateResultListener resultListener) {
        if (jsonObject == null) {
            if (resultListener != null) {
                resultListener.onUpdateResult(UpdateResult.FAILED);
            }
            return;
        }

        final Handler handler = new Handler();

        new Thread() {
            @Override
            public void run() {
                ReaderPost updatedPost = ReaderPost.fromJson(jsonObject);
                boolean hasChanges = !originalPost.isSamePost(updatedPost);

                if (hasChanges) {
                    AppLog.d(T.READER, "post updated");
                    // set the featured image for the updated post to that of the original
                    // post - this should be done even if the updated post has a featured
                    // image since that may have been set by ReaderPost.findFeaturedImage()
                    if (originalPost.hasFeaturedImage()) {
                        updatedPost.setFeaturedImage(originalPost.getFeaturedImage());
                    }

                    // likewise for featured video
                    if (originalPost.hasFeaturedVideo()) {
                        updatedPost.setFeaturedVideo(originalPost.getFeaturedVideo());
                        updatedPost.isVideoPress = originalPost.isVideoPress;
                    }

                    // retain the pubDate and timestamp of the original post - this is important
                    // since these control how the post is sorted in the list view, and we don't
                    // want that sorting to change
                    updatedPost.timestamp = originalPost.timestamp;
                    updatedPost.setPublished(originalPost.getPublished());

                    ReaderPostTable.addOrUpdatePost(updatedPost);
                }

                // always update liking users regardless of whether changes were detected - this
                // ensures that the liking avatars are immediately available to post detail
                if (handlePostLikes(updatedPost, jsonObject)) {
                    hasChanges = true;
                }

                if (resultListener != null) {
                    final UpdateResult result = (hasChanges ? UpdateResult.CHANGED : UpdateResult.UNCHANGED);
                    handler.post(new Runnable() {
                        public void run() {
                            resultListener.onUpdateResult(result);
                        }
                    });
                }
            }
        }.start();
    }

    /*
     * updates local liking users based on the "likes" meta section of the post's json - requires
     * using the /sites/ endpoint with ?meta=likes - returns true if likes have changed
     */
    private static boolean handlePostLikes(final ReaderPost post, JSONObject jsonPost) {
        if (post == null || jsonPost == null) {
            return false;
        }

        JSONObject jsonLikes = JSONUtil.getJSONChild(jsonPost, "meta/data/likes");
        if (jsonLikes == null) {
            return false;
        }

        ReaderUserList likingUsers = ReaderUserList.fromJsonLikes(jsonLikes);
        ReaderUserIdList likingUserIds = likingUsers.getUserIds();

        ReaderUserIdList existingIds = ReaderLikeTable.getLikesForPost(post);
        if (likingUserIds.isSameList(existingIds)) {
            return false;
        }

        ReaderUserTable.addOrUpdateUsers(likingUsers);
        ReaderLikeTable.setLikesForPost(post, likingUserIds);
        return true;
    }

    /**
     * similar to updatePost, but used when post doesn't already exist in local db
     **/
    public static void requestPost(final long blogId, final long postId, final ActionListener actionListener) {
        String path = "/sites/" + blogId + "/posts/" + postId + "/?meta=site,likes";

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                ReaderPost post = ReaderPost.fromJson(jsonObject);
                // make sure the post has the passed blogId so it's saved correctly - necessary
                // since the /sites/ endpoints return site_id="1" for Jetpack-powered blogs
                post.blogId = blogId;
                ReaderPostTable.addOrUpdatePost(post);
                handlePostLikes(post, jsonObject);
                if (actionListener != null) {
                    actionListener.onActionResult(true);
                }
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                if (actionListener != null) {
                    actionListener.onActionResult(false);
                }
            }
        };
        AppLog.d(T.READER, "requesting post");
        WordPress.getRestClientUtilsV1_1().get(path, null, null, listener, errorListener);
    }

    /*
     * get the latest posts in the passed topic - note that this uses an UpdateResultAndCountListener
     * so the caller can be told how many new posts were added
     */
    public static void updatePostsInTag(final ReaderTag tag,
                                        final RequestDataAction updateAction,
                                        final UpdateResultListener resultListener) {
        String endpoint = getEndpointForTag(tag);
        if (TextUtils.isEmpty(endpoint)) {
            if (resultListener != null) {
                resultListener.onUpdateResult(UpdateResult.FAILED);
            }
            return;
        }

        StringBuilder sb = new StringBuilder(endpoint);

        // append #posts to retrieve
        sb.append("?number=").append(ReaderConstants.READER_MAX_POSTS_TO_REQUEST);

        // return newest posts first (this is the default, but make it explicit since it's important)
        sb.append("&order=DESC");

        // if older posts are being requested, add the &before param based on the oldest existing post
        if (updateAction == RequestDataAction.LOAD_OLDER) {
            String dateOldest = ReaderPostTable.getOldestPubDateWithTag(tag);
            if (!TextUtils.isEmpty(dateOldest)) {
                sb.append("&before=").append(UrlUtils.urlEncode(dateOldest));
            }
        }

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                // remember when this tag was updated if newer posts were requested
                if (updateAction == RequestDataAction.LOAD_NEWER) {
                    ReaderTagTable.setTagLastUpdated(tag);
                }
                handleUpdatePostsResponse(tag, jsonObject, resultListener);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                if (resultListener != null) {
                    resultListener.onUpdateResult(UpdateResult.FAILED);
                }
            }
        };

        WordPress.getRestClientUtilsV1_1().get(sb.toString(), null, null, listener, errorListener);
    }

    /*
     * get the latest posts in the passed blog
     */
    public static void requestPostsForBlog(final long blogId,
                                           final RequestDataAction updateAction,
                                           final UpdateResultListener resultListener) {
        String path = "/sites/" + blogId + "/posts/?meta=site,likes";

        // append the date of the oldest cached post in this blog when requesting older posts
        if (updateAction == RequestDataAction.LOAD_OLDER) {
            String dateOldest = ReaderPostTable.getOldestPubDateInBlog(blogId);
            if (!TextUtils.isEmpty(dateOldest)) {
                path += "&before=" + UrlUtils.urlEncode(dateOldest);
            }
        }
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleUpdatePostsResponse(null, jsonObject, resultListener);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                if (resultListener != null) {
                    resultListener.onUpdateResult(UpdateResult.FAILED);
                }
            }
        };
        AppLog.d(T.READER, "updating posts in blog " + blogId);
        WordPress.getRestClientUtilsV1_1().get(path, null, null, listener, errorListener);
    }

    public static void requestPostsForFeed(final long feedId,
                                           final RequestDataAction updateAction,
                                           final UpdateResultListener resultListener) {
        String path = "/read/feed/" + feedId + "/posts/?meta=site,likes";
        if (updateAction == RequestDataAction.LOAD_OLDER) {
            String dateOldest = ReaderPostTable.getOldestPubDateInFeed(feedId);
            if (!TextUtils.isEmpty(dateOldest)) {
                path += "&before=" + UrlUtils.urlEncode(dateOldest);
            }
        }

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleUpdatePostsResponse(null, jsonObject, resultListener);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                if (resultListener != null) {
                    resultListener.onUpdateResult(UpdateResult.FAILED);
                }
            }
        };

        AppLog.d(T.READER, "updating posts in feed " + feedId);
        WordPress.getRestClientUtilsV1_1().get(path, null, null, listener, errorListener);
    }

    /*
     * called after requesting posts with a specific tag or in a specific blog
     */
    private static void handleUpdatePostsResponse(final ReaderTag tag,
                                                  final JSONObject jsonObject,
                                                  final UpdateResultListener resultListener) {
        if (jsonObject == null) {
            if (resultListener != null) {
                resultListener.onUpdateResult(UpdateResult.FAILED);
            }
            return;
        }

        final Handler handler = new Handler();
        new Thread() {
            @Override
            public void run() {
                ReaderPostList serverPosts = ReaderPostList.fromJson(jsonObject);
                final UpdateResult updateResult = ReaderPostTable.comparePosts(serverPosts);
                if (updateResult.isNewOrChanged()) {
                    ReaderPostTable.addOrUpdatePosts(tag, serverPosts);
                }
                AppLog.d(T.READER, "requested posts response = " + updateResult.toString());

                if (resultListener != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            resultListener.onUpdateResult(updateResult);
                        }
                    });
                }
            }
        }.start();
    }

    /*
     * returns the endpoint to use for the passed tag - first gets it from local db, if not
     * there it generates it "by hand"
     */
    private static String getEndpointForTag(ReaderTag tag) {
        if (tag == null) {
            return null;
        }

        // if passed tag has an assigned endpoint, return it and be done
        if (!TextUtils.isEmpty(tag.getEndpoint())) {
            return getRelativeEndpoint(tag.getEndpoint());
        }

        // check the db for the endpoint
        String endpoint = ReaderTagTable.getEndpointForTag(tag);
        if (!TextUtils.isEmpty(endpoint)) {
            return getRelativeEndpoint(endpoint);
        }

        // never hand craft the endpoint for default tags, since these MUST be updated
        // using their stored endpoints
        if (tag.tagType == ReaderTagType.DEFAULT) {
            return null;
        }

        return String.format("/read/tags/%s/posts", ReaderUtils.sanitizeWithDashes(tag.getTagName()));
    }

    /*
     * returns the passed endpoint without the unnecessary path - this is
     * needed because as of 20-Feb-2015 the /read/menu/ call returns the
     * full path but we don't want to use the full path since it may change
     * between API versions (as it did when we moved from v1 to v1.1)
     *
     * ex: https://public-api.wordpress.com/rest/v1/read/tags/fitness/posts
     *     becomes just                            /read/tags/fitness/posts
     */
    private static String getRelativeEndpoint(final String endpoint) {
        if (endpoint != null && endpoint.startsWith("http")) {
            int pos = endpoint.indexOf("/read/");
            if (pos > -1) {
                return endpoint.substring(pos, endpoint.length());
            }
            pos = endpoint.indexOf("/v1/");
            if (pos > -1) {
                return endpoint.substring(pos + 3, endpoint.length());
            }
        }
        return StringUtils.notNullStr(endpoint);
    }

}
