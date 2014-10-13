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
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.VolleyUtils;

import java.util.Date;
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
        String path = "sites/" + post.blogId + "/posts/" + post.postId + "/likes/";
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
        WordPress.getRestClientUtils().post(path, params, null, listener, errorListener);
    }

    /*
     * get the latest version of this post - note that the post is only considered changed if the
     * like/comment count has changed, or if the current user's like/follow status has changed
     */
    public static void updatePost(final ReaderPost originalPost,
                                  final ReaderActions.UpdateResultListener resultListener) {
        String path = "sites/" + originalPost.blogId + "/posts/" + originalPost.postId + "/?meta=site,likes";

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
                    resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);
                }
            }
        };
        AppLog.d(T.READER, "updating post");
        WordPress.getRestClientUtils().get(path, null, null, listener, errorListener);
    }

    private static void handleUpdatePostResponse(final ReaderPost originalPost,
                                                 final JSONObject jsonObject,
                                                 final ReaderActions.UpdateResultListener resultListener) {
        if (jsonObject == null) {
            if (resultListener != null) {
                resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);
            }
            return;
        }

        final Handler handler = new Handler();

        new Thread() {
            @Override
            public void run() {
                ReaderPost updatedPost = ReaderPost.fromJson(jsonObject);
                boolean hasChanges =
                         ( updatedPost.numReplies != originalPost.numReplies
                        || updatedPost.numLikes != originalPost.numLikes
                        || updatedPost.isCommentsOpen != originalPost.isCommentsOpen
                        || updatedPost.isLikedByCurrentUser != originalPost.isLikedByCurrentUser
                        || updatedPost.isFollowedByCurrentUser != originalPost.isFollowedByCurrentUser);

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
                    final ReaderActions.UpdateResult result =
                            (hasChanges ? ReaderActions.UpdateResult.CHANGED : ReaderActions.UpdateResult.UNCHANGED);
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
        WordPress.getRestClientUtils().get(path, null, null, listener, errorListener);
    }

    /*
     * get the latest posts in the passed topic - note that this uses an UpdateResultAndCountListener
     * so the caller can be told how many new posts were added - use the second method which accepts
     * a backfillListener to request new posts and backfill missing posts - note that a backfill
     * will NOT occur unless a backfillListener is passed
     */
    public static void updatePostsInTag(final ReaderTag tag,
                                        final ReaderActions.RequestDataAction updateAction,
                                        final ReaderActions.UpdateResultAndCountListener resultListener) {
        updatePostsInTag(tag, updateAction, resultListener, null);
    }
    public static void updatePostsInTagWithBackfill(final ReaderTag tag,
                                                    final ReaderActions.UpdateResultAndCountListener resultListener,
                                                    final ReaderActions.PostBackfillListener backfillListener) {
        updatePostsInTag(tag, ReaderActions.RequestDataAction.LOAD_NEWER, resultListener, backfillListener);
    }
    private static void updatePostsInTag(final ReaderTag tag,
                                         final ReaderActions.RequestDataAction updateAction,
                                         final ReaderActions.UpdateResultAndCountListener resultListener,
                                         final ReaderActions.PostBackfillListener backfillListener) {

        String endpoint = getEndpointForTag(tag);
        if (TextUtils.isEmpty(endpoint)) {
            if (resultListener != null) {
                resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED, -1);
            }
            return;
        }

        StringBuilder sb = new StringBuilder(endpoint);

        // append #posts to retrieve
        sb.append("?number=").append(ReaderConstants.READER_MAX_POSTS_TO_REQUEST);

        // return newest posts first (this is the default, but make it explicit since it's important)
        sb.append("&order=DESC");

        // apply the after/before to limit results based on previous update, but only if there are
        // existing posts in this topic
        if (ReaderPostTable.hasPostsWithTag(tag)) {
            switch (updateAction) {
                case LOAD_NEWER:
                    String dateNewest = ReaderTagTable.getTagNewestDate(tag);
                    if (!TextUtils.isEmpty(dateNewest)) {
                        sb.append("&after=").append(UrlUtils.urlEncode(dateNewest));
                        AppLog.d(T.READER, String.format("requesting newer posts in tag %s (%s)", tag.getTagNameForLog(), dateNewest));
                    }
                    break;

                case LOAD_OLDER:
                    String dateOldest = ReaderTagTable.getTagOldestDate(tag);
                    // if oldest date isn't stored, it means we haven't requested older posts until
                    // now, so use the date of the oldest stored post
                    if (TextUtils.isEmpty(dateOldest)) {
                        dateOldest = ReaderPostTable.getOldestPubDateWithTag(tag);
                    }
                    if (!TextUtils.isEmpty(dateOldest)) {
                        sb.append("&before=").append(UrlUtils.urlEncode(dateOldest));
                        AppLog.d(T.READER, String.format("requesting older posts in tag %s (%s)", tag.getTagNameForLog(), dateOldest));
                    }
                    break;
            }
        } else {
            AppLog.d(T.READER, "requesting posts in empty tag " + tag.getTagNameForLog());
        }

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleUpdatePostsWithTagResponse(tag, updateAction, jsonObject, resultListener, backfillListener);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                if (resultListener != null) {
                    resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED, -1);
                }
            }
        };

        WordPress.getRestClientUtils().get(sb.toString(), null, null, listener, errorListener);
    }

    private static void handleUpdatePostsWithTagResponse(final ReaderTag tag,
                                                         final ReaderActions.RequestDataAction updateAction,
                                                         final JSONObject jsonObject,
                                                         final ReaderActions.UpdateResultAndCountListener resultListener,
                                                         final ReaderActions.PostBackfillListener backfillListener) {
        if (jsonObject == null) {
            if (resultListener != null) {
                resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED, -1);
            }
            return;
        }
        final Handler handler = new Handler();

        new Thread() {
            @Override
            public void run() {
                final ReaderPostList serverPosts = ReaderPostList.fromJson(jsonObject);

                // remember when this topic was updated if newer posts were requested, regardless of
                // whether the response contained any posts
                if (updateAction == ReaderActions.RequestDataAction.LOAD_NEWER) {
                    ReaderTagTable.setTagLastUpdated(tag, DateTimeUtils.javaDateToIso8601(new Date()));
                }

                // go no further if the response didn't contain any posts
                if (serverPosts.size() == 0) {
                    AppLog.d(T.READER, "no new posts in tag " + tag.getTagNameForLog());
                    if (resultListener != null) {
                        handler.post(new Runnable() {
                            public void run() {
                                resultListener.onUpdateResult(ReaderActions.UpdateResult.UNCHANGED, 0);
                            }
                        });
                    }
                    return;
                }

                // json "date_range" tells the the range of dates in the response, which we want to
                // store for use the next time we request newer/older if this response contained any
                // posts - note that freshly-pressed uses "newest" and "oldest" but other endpoints
                // use "after" and "before"
                JSONObject jsonDateRange = jsonObject.optJSONObject("date_range");
                if (jsonDateRange != null) {
                    switch (updateAction) {
                        case LOAD_NEWER:
                            String newest = jsonDateRange.has("before") ? JSONUtil.getString(jsonDateRange, "before") : JSONUtil.getString(jsonDateRange, "newest");
                            if (!TextUtils.isEmpty(newest)) {
                                ReaderTagTable.setTagNewestDate(tag, newest);
                            }
                            break;
                        case LOAD_OLDER:
                            String oldest = jsonDateRange.has("after") ? JSONUtil.getString(jsonDateRange, "after") : JSONUtil.getString(jsonDateRange, "oldest");
                            if (!TextUtils.isEmpty(oldest)) {
                                ReaderTagTable.setTagOldestDate(tag, oldest);
                            }
                            break;
                    }
                }

                // remember whether there were existing posts with this tag before adding
                // the ones we just retrieved
                final boolean hasExistingPostsWithTag = ReaderPostTable.hasPostsWithTag(tag);

                // determine how many of the downloaded posts are new (response may contain both
                // new posts and posts updated since the last call), then save the posts even if
                // none are new in order to update comment counts, likes, etc., on existing posts
                final int numNewPosts;
                if (hasExistingPostsWithTag) {
                    numNewPosts = ReaderPostTable.getNumNewPostsWithTag(tag, serverPosts);
                } else {
                    numNewPosts = serverPosts.size();
                }
                ReaderPostTable.addOrUpdatePosts(tag, serverPosts);

                AppLog.d(T.READER, String.format("retrieved %d posts (%d new) in tag %s",
                        serverPosts.size(), numNewPosts, tag.getTagNameForLog()));

                handler.post(new Runnable() {
                    public void run() {
                        if (resultListener != null) {
                            // always pass CHANGED as the result even if there are no new posts (since if
                            // get this far, it means there are changed - updated - posts)
                            resultListener.onUpdateResult(ReaderActions.UpdateResult.CHANGED, numNewPosts);
                        }

                        // if a backfill listener was passed, there were existing posts with this tag,
                        // and all posts retrieved are new, then backfill the posts to fill in gaps
                        // between posts just retrieved and posts previously retrieved
                        if (backfillListener != null && hasExistingPostsWithTag) {
                            boolean areAllPostsNew = (numNewPosts == ReaderConstants.READER_MAX_POSTS_TO_REQUEST);
                            if (areAllPostsNew) {
                                Date dtOldestServerPost = serverPosts.getOldestPubDate();
                                backfillPostsWithTag(tag, dtOldestServerPost, 0, backfillListener);
                            }
                        }
                    }
                });
            }
        }.start();
    }

    /*
     * get the latest posts in the passed blog
     */
    public static void requestPostsForBlog(final long blogId,
                                           final String blogUrl,
                                           final ReaderActions.RequestDataAction updateAction,
                                           final ReaderActions.ActionListener actionListener) {
        String path;
        if (blogId == 0) {
            path = "sites/" + UrlUtils.getDomainFromUrl(blogUrl);
        } else {
            path = "sites/" + blogId;
        }
        path += "/posts/?meta=site,likes";

        // append the date of the oldest cached post in this blog when requesting older posts
        if (updateAction == ReaderActions.RequestDataAction.LOAD_OLDER) {
            String dateOldest = ReaderPostTable.getOldestPubDateInBlog(blogId);
            if (!TextUtils.isEmpty(dateOldest)) {
                path += "&before=" + UrlUtils.urlEncode(dateOldest);
            }
        }
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleGetPostsResponse(jsonObject, actionListener);
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
        AppLog.d(T.READER, "updating posts in blog " + blogId);
        WordPress.getRestClientUtils().get(path, null, null, listener, errorListener);
    }

    private static void handleGetPostsResponse(JSONObject jsonObject, final ReaderActions.ActionListener actionListener) {
        if (jsonObject==null) {
            if (actionListener != null) {
                actionListener.onActionResult(false);
            }
            return;
        }

        ReaderPostList posts = ReaderPostList.fromJson(jsonObject);
        ReaderPostTable.addOrUpdatePosts(null, posts);

        if (actionListener != null) {
            actionListener.onActionResult(posts.size() > 0);
        }
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
            return tag.getEndpoint();
        }

        // check the db for the endpoint
        String endpoint = ReaderTagTable.getEndpointForTag(tag);
        if (!TextUtils.isEmpty(endpoint)) {
            return endpoint;
        }

        // never hand craft the endpoint for default tags, since these MUST be updated
        // using their stored endpoints
        if (tag.tagType == ReaderTagType.DEFAULT) {
            return null;
        }

        return String.format("/read/tags/%s/posts", ReaderUtils.sanitizeTagName(tag.getTagName()));
    }

    /*
     * "backfill" posts with a specific tag - used to fill in gaps between syncs, ex: sync the
     * reader, come back the next day and sync again, with a popular tag there may be posts
     * missing between the posts retrieved the previous day and the posts just retrieved
     */
    private static final int BACKFILL_MAX_RECURSION = 3;
    private static void backfillPostsWithTag(final ReaderTag tag,
                                             final Date dateBefore,
                                             final int recursionCounter,
                                             final ReaderActions.PostBackfillListener backfillListener) {
        String endpoint = getEndpointForTag(tag);
        if (TextUtils.isEmpty(endpoint)) {
            return;
        }

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleBackfillResponse(jsonObject, tag, recursionCounter, backfillListener);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
            }
        };

        String strDateBefore = DateTimeUtils.javaDateToIso8601(dateBefore);
        String path = endpoint
                    + "?number=" + ReaderConstants.READER_MAX_POSTS_TO_REQUEST
                    + "&order=DESC"
                    + "&before=" + UrlUtils.urlEncode(strDateBefore);
        AppLog.i(T.READER, String.format("backfilling tag %s, recursion %d", tag.getTagNameForLog(), recursionCounter));
        WordPress.getRestClientUtils().get(path, null, null, listener, errorListener);
    }
    private static void handleBackfillResponse(final JSONObject jsonObject,
                                               final ReaderTag tag,
                                               final int recursionCounter,
                                               final ReaderActions.PostBackfillListener backfillListener) {
        if (jsonObject == null) {
            return;
        }

        final Handler handler = new Handler();

        new Thread() {
            @Override
            public void run() {
                final ReaderPostList serverPosts = ReaderPostList.fromJson(jsonObject);
                final int numNewPosts = ReaderPostTable.getNumNewPostsWithTag(tag, serverPosts);
                if (numNewPosts == 0) {
                    return;
                }

                AppLog.i(T.READER, String.format("backfilling tag %s found %d new posts", tag.getTagNameForLog(), numNewPosts));
                ReaderPostTable.addOrUpdatePosts(tag, serverPosts);

                handler.post(new Runnable() {
                    public void run() {
                        if (backfillListener != null) {
                            backfillListener.onPostsBackfilled();
                        }

                        // backfill again if all posts were new, but enforce a max on recursion
                        // so we don't backfill forever
                        boolean areAllPostsNew = (numNewPosts == ReaderConstants.READER_MAX_POSTS_TO_REQUEST);
                        if (areAllPostsNew && recursionCounter < BACKFILL_MAX_RECURSION) {
                            backfillPostsWithTag(tag,
                                    serverPosts.getOldestPubDate(),
                                    recursionCounter + 1,
                                    backfillListener);
                        }
                    }
                });
            }
        }.start();
    }

}
