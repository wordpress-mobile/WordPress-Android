package org.wordpress.android.ui.reader.actions;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderLikeTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderUserTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostList;
import org.wordpress.android.models.ReaderUserIdList;
import org.wordpress.android.models.ReaderUserList;
import org.wordpress.android.ui.reader.ReaderEvents;
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult;
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResultListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.VolleyUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import de.greenrobot.event.EventBus;

public class ReaderPostActions {

    private static final String TRACKING_REFERRER = "https://wordpress.com/";
    private static final Random mRandom = new Random();

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
        int numCurrentLikes = ReaderPostTable.getNumLikesForPost(post.blogId, post.postId);
        int newNumLikes = (isAskingToLike ? numCurrentLikes + 1 : numCurrentLikes - 1);
        if (newNumLikes < 0) {
            newNumLikes = 0;
        }
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

        WordPress.getRestClientUtilsV1_1().post(path, listener, errorListener);
        return true;
    }

    /*
     * get the latest version of this post - note that the post is only considered changed if the
     * like/comment count has changed, or if the current user's like/follow status has changed
     */
    public static void updatePost(final ReaderPost localPost,
                                  final UpdateResultListener resultListener) {
        String path = "read/sites/" + localPost.blogId + "/posts/" + localPost.postId + "/?meta=site,likes";

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleUpdatePostResponse(localPost, jsonObject, resultListener);
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
        WordPress.getRestClientUtilsV1_2().get(path, null, null, listener, errorListener);
    }

    private static void handleUpdatePostResponse(final ReaderPost localPost,
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
                ReaderPost serverPost = ReaderPost.fromJson(jsonObject);

                // TODO: this temporary fix was added 25-Apr-2016 as a workaround for the fact that
                // the read/sites/{blogId}/posts/{postId} endpoint doesn't contain the feedId or
                // feedItemId of the post. because of this, we need to copy them from the local post
                // before calling isSamePost (since the difference in those IDs causes it to return false)
                if (serverPost.feedId == 0 && localPost.feedId != 0) {
                    serverPost.feedId = localPost.feedId;
                }

                if (serverPost.feedItemId == 0 && localPost.feedItemId != 0) {
                    serverPost.feedItemId = localPost.feedItemId;
                }

                boolean hasChanges = !serverPost.isSamePost(localPost);

                if (hasChanges) {
                    AppLog.d(T.READER, "post updated");
                    // copy changes over to the local post - this is done instead of simply overwriting
                    // the local post with the server post because the server post was retrieved using
                    // the read/sites/$siteId/posts/$postId endpoint which is missing some information
                    // https://github.com/wordpress-mobile/WordPress-Android/issues/3164
                    localPost.numReplies = serverPost.numReplies;
                    localPost.numLikes = serverPost.numLikes;
                    localPost.isFollowedByCurrentUser = serverPost.isFollowedByCurrentUser;
                    localPost.isLikedByCurrentUser = serverPost.isLikedByCurrentUser;
                    localPost.isCommentsOpen = serverPost.isCommentsOpen;
                    localPost.setTitle(serverPost.getTitle());
                    localPost.setText(serverPost.getText());
                    ReaderPostTable.addOrUpdatePost(localPost);
                }

                // always update liking users regardless of whether changes were detected - this
                // ensures that the liking avatars are immediately available to post detail
                if (handlePostLikes(serverPost, jsonObject)) {
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

        JSONObject jsonLikes = JSONUtils.getJSONChild(jsonPost, "meta/data/likes");
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
    public static void requestPost(final long blogId,
                                   final long postId,
                                   final ReaderActions.OnRequestListener requestListener) {
        String path = "read/sites/" + blogId + "/posts/" + postId + "/?meta=site,likes";

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                ReaderPost post = ReaderPost.fromJson(jsonObject);
                ReaderPostTable.addOrUpdatePost(post);
                handlePostLikes(post, jsonObject);
                if (requestListener != null) {
                    requestListener.onSuccess();
                }
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                if (requestListener != null) {
                    int statusCode = 0;
                    // first try to get the error code from the JSON response, example:
                    //   {"code":403,"headers":[{"name":"Content-Type","value":"application\/json"}],
                    //    "body":{"error":"unauthorized","message":"User cannot access this private blog."}}
                    JSONObject jsonObject = VolleyUtils.volleyErrorToJSON(volleyError);
                    if (jsonObject != null && jsonObject.has("code")) {
                        statusCode = jsonObject.optInt("code");
                    }
                    if (statusCode == 0) {
                        statusCode = VolleyUtils.statusCodeFromVolleyError(volleyError);
                    }
                    requestListener.onFailure(statusCode);
                }
            }
        };
        AppLog.d(T.READER, "requesting post");
        WordPress.getRestClientUtilsV1_2().get(path, null, null, listener, errorListener);
    }

    private static String getTrackingPixelForPost(@NonNull ReaderPost post) {
        return "https://pixel.wp.com/g.gif?v=wpcom&reader=1"
                + "&blog=" + post.blogId
                + "&post=" + post.postId
                + "&host=" + UrlUtils.urlEncode(UrlUtils.getHost(post.getBlogUrl()))
                + "&ref="  + UrlUtils.urlEncode(TRACKING_REFERRER)
                + "&t="    + mRandom.nextInt();
    }

    public static void bumpPageViewForPost(long blogId, long postId) {
        bumpPageViewForPost(ReaderPostTable.getPost(blogId, postId, true));
    }
    public static void bumpPageViewForPost(ReaderPost post) {
        if (post == null) {
            return;
        }

        // don't bump stats for posts in blogs the current user is an admin of, unless
        // this is a private post since we count views for private posts from admins
        if (!post.isPrivate && WordPress.wpDB.isCurrentUserAdminOfRemoteBlogId(post.blogId)) {
            AppLog.d(T.READER, "skipped bump page view - user is admin");
            return;
        }

        Response.Listener<String> listener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                AppLog.d(T.READER, "bump page view succeeded");
            }
        };
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                AppLog.w(T.READER, "bump page view failed");
            }
        };

        Request request = new StringRequest(
                Request.Method.GET,
                getTrackingPixelForPost(post),
                listener,
                errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                // call will fail without correct refer(r)er
                Map<String, String> headers = new HashMap<>();
                headers.put("Referer", TRACKING_REFERRER);
                return headers;
            }
        };

        WordPress.requestQueue.add(request);
    }

    /*
     * request posts related to the passed one
     */
    public static void requestRelatedPosts(final ReaderPost sourcePost) {
        if (sourcePost == null) return;

        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
               handleRelatedPostsResponse(sourcePost, jsonObject);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.w(T.READER, "updateRelatedPosts failed");
                AppLog.e(T.READER, volleyError);

            }
        };

        String path = "/read/site/" + sourcePost.blogId + "/post/" + sourcePost.postId + "/related";
        WordPress.getRestClientUtilsV1_2().get(path, null, null, listener, errorListener);
    }

    private static void handleRelatedPostsResponse(final ReaderPost sourcePost, final JSONObject jsonObject) {
        if (jsonObject == null) return;

        new Thread() {
            @Override
            public void run() {
                ReaderPostList relatedPosts = ReaderPostList.fromJson(jsonObject);
                if (relatedPosts != null && relatedPosts.size() > 0) {
                    ReaderPostTable.addOrUpdatePosts(null, relatedPosts);
                    EventBus.getDefault().post(new ReaderEvents.RelatedPostsUpdated(sourcePost, relatedPosts));
                }
            }
        }.start();

    }
}
