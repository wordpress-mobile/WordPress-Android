package org.wordpress.android.ui.reader.actions;

import android.os.Handler;
import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.ReaderBlogInfo;
import org.wordpress.android.models.ReaderFollowedBlogList;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderRecommendBlogList;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateBlogInfoListener;
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResultListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
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
            ReaderBlogTable.setIsFollowedBlogUrl(blogId, blogUrl, isAskingToFollow);
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
                    ReaderBlogTable.setIsFollowedBlogUrl(blogId, blogUrl, !isAskingToFollow);
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
    public static void updateFollowedBlogs(final UpdateResultListener resultListener) {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleFollowedBlogsResponse(jsonObject, resultListener);
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
        WordPress.getRestClientUtils().get("/read/following/mine", listener, errorListener);
    }
    private static void handleFollowedBlogsResponse(final JSONObject jsonObject, final UpdateResultListener resultListener) {
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
                ReaderFollowedBlogList serverBlogs = ReaderFollowedBlogList.fromJson(jsonObject);
                ReaderFollowedBlogList localBlogs = ReaderBlogTable.getFollowedBlogs();

                final boolean hasChanges = !localBlogs.isSameList(serverBlogs);
                if (hasChanges) {
                    ReaderBlogTable.setFollowedBlogs(serverBlogs);
                }

                if (resultListener != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            ReaderActions.UpdateResult result = (hasChanges ? ReaderActions.UpdateResult.CHANGED : ReaderActions.UpdateResult.UNCHANGED);
                            resultListener.onUpdateResult(result);
                        }
                    });
                }
            }
        }.start();
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
                // if we failed to get the blog info using the id, trying again using just the domain
                if (hasBlogId && hasBlogUrl) {
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
            WordPress.getRestClientUtils().get("/sites/" + UrlUtils.getDomainFromUrl(blogUrl), listener, errorListener);
        }

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

    /*
     * request the latest recommended blogs, replaces all local ones
     */
    public static void updateRecommendedBlogs(final UpdateResultListener resultListener) {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleRecommendedBlogsResponse(jsonObject, resultListener);
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

        String path = "/read/recommendations/mine/?source=mobile&number=" + Integer.toString(ReaderConstants.READER_MAX_RECOMMENDED_BLOGS);
        WordPress.getRestClientUtils().get(path, listener, errorListener);
    }
    private static void handleRecommendedBlogsResponse(final JSONObject jsonObject,
                                                       final UpdateResultListener resultListener) {
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
                ReaderRecommendBlogList serverBlogs = ReaderRecommendBlogList.fromJson(jsonObject);
                ReaderRecommendBlogList localBlogs = ReaderBlogTable.getRecommendedBlogs();

                final boolean hasChanges = !localBlogs.isSameList(serverBlogs);
                if (hasChanges) {
                    ReaderBlogTable.setRecommendedBlogs(serverBlogs);
                }

                if (resultListener != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            ReaderActions.UpdateResult result = (hasChanges ? ReaderActions.UpdateResult.CHANGED : ReaderActions.UpdateResult.UNCHANGED);
                            resultListener.onUpdateResult(result);
                        }
                    });
                }
            }
        }.start();
    }


}
