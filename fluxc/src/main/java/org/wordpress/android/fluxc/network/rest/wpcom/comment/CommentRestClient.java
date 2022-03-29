package org.wordpress.android.fluxc.network.rest.wpcom.comment;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.CommentActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.LikeModel;
import org.wordpress.android.fluxc.model.LikeModel.LikeType;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComErrorListener;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentWPComRestResponse.CommentsWPComRestResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.common.LikeWPComRestResponse.LikesWPComRestResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.common.LikesUtilsProvider;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsResponsePayload;
import org.wordpress.android.fluxc.store.CommentStore.FetchedCommentLikesResponsePayload;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentResponsePayload;
import org.wordpress.android.fluxc.utils.CommentErrorUtils;
import org.wordpress.android.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class CommentRestClient extends BaseWPComRestClient {
    LikesUtilsProvider mLikesUtilsProvider;

    @Inject public CommentRestClient(
            Context appContext,
            Dispatcher dispatcher,
            @Named("regular") RequestQueue requestQueue,
            AccessToken accessToken,
            UserAgent userAgent,
            LikesUtilsProvider likesUtilsProvider
    ) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
        mLikesUtilsProvider = likesUtilsProvider;
    }

    public void fetchComments(final SiteModel site, final int number, final int offset, final CommentStatus status) {
        String url = WPCOMREST.sites.site(site.getSiteId()).comments.getUrlV1_1();
        Map<String, String> params = new HashMap<>();
        params.put("status", status.toString());
        params.put("offset", String.valueOf(offset));
        params.put("number", String.valueOf(number));
        params.put("force", "wpcom");
        final WPComGsonRequest<CommentsWPComRestResponse> request = WPComGsonRequest.buildGetRequest(
                url, params, CommentsWPComRestResponse.class,
                new Listener<CommentsWPComRestResponse>() {
                    @Override
                    public void onResponse(CommentsWPComRestResponse response) {
                        List<CommentModel> comments = commentsResponseToCommentList(response, site);
                        FetchCommentsResponsePayload payload = new FetchCommentsResponsePayload(comments, site, number,
                                offset, status);
                        mDispatcher.dispatch(CommentActionBuilder.newFetchedCommentsAction(payload));
                    }
                },

                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        mDispatcher.dispatch(CommentActionBuilder.newFetchedCommentsAction(
                                CommentErrorUtils.commentErrorToFetchCommentsPayload(error, site)));
                    }
                }
        );
        add(request);
    }

    public void pushComment(final SiteModel site, @NonNull final CommentModel comment) {
        String url = WPCOMREST.sites.site(site.getSiteId()).comments.comment(comment.getRemoteCommentId()).getUrlV1_1();
        Map<String, Object> params = new HashMap<>();
        params.put("content", comment.getContent());
        params.put("date", comment.getDatePublished());
        params.put("status", comment.getStatus());
        final WPComGsonRequest<CommentWPComRestResponse> request = WPComGsonRequest.buildPostRequest(
                url, params, CommentWPComRestResponse.class,
                new Listener<CommentWPComRestResponse>() {
                    @Override
                    public void onResponse(CommentWPComRestResponse response) {
                        CommentModel newComment = commentResponseToComment(response, site);
                        newComment.setId(comment.getId()); // reconciliate local instance and newly created object
                        RemoteCommentResponsePayload payload = new RemoteCommentResponsePayload(newComment);
                        mDispatcher.dispatch(CommentActionBuilder.newPushedCommentAction(payload));
                    }
                },

                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        mDispatcher.dispatch(CommentActionBuilder.newPushedCommentAction(
                                CommentErrorUtils.commentErrorToPushCommentPayload(error, comment)));
                    }
                }
        );
        add(request);
    }

    public void fetchComment(final SiteModel site, long remoteCommentId, @Nullable final CommentModel comment) {
        // Prioritize CommentModel over comment id.
        if (comment != null) {
            remoteCommentId = comment.getRemoteCommentId();
        }

        String url = WPCOMREST.sites.site(site.getSiteId()).comments.comment(remoteCommentId).getUrlV1_1();
        final WPComGsonRequest<CommentWPComRestResponse> request = WPComGsonRequest.buildGetRequest(
                url, null, CommentWPComRestResponse.class,
                new Listener<CommentWPComRestResponse>() {
                    @Override
                    public void onResponse(CommentWPComRestResponse response) {
                        CommentModel comment = commentResponseToComment(response, site);
                        RemoteCommentResponsePayload payload = new RemoteCommentResponsePayload(comment);
                        mDispatcher.dispatch(CommentActionBuilder.newFetchedCommentAction(payload));
                    }
                },

                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        mDispatcher.dispatch(CommentActionBuilder.newFetchedCommentAction(
                                CommentErrorUtils.commentErrorToFetchCommentPayload(error, comment)));
                    }
                }
        );
        add(request);
    }

    public void fetchCommentLikes(
            final long siteId,
            final long commentId,
            final boolean requestNextPage,
            final int pageLength
    ) {
        String url = WPCOMREST.sites.site(siteId).comments.comment(commentId).likes.getUrlV1_2();

        Map<String, String> params = new HashMap<>();
        params.put("number", String.valueOf(pageLength));

        if (requestNextPage) {
            Map<String, String> pageOffsetParams = mLikesUtilsProvider.getPageOffsetParams(
                    LikeType.COMMENT_LIKE,
                    siteId,
                    commentId
            );
            if (pageOffsetParams != null) {
                params.putAll(pageOffsetParams);
            }
        }

        final WPComGsonRequest<LikesWPComRestResponse> request = WPComGsonRequest.buildGetRequest(
                url, params, LikesWPComRestResponse.class,
                new Listener<LikesWPComRestResponse>() {
                    @Override
                    public void onResponse(LikesWPComRestResponse response) {
                        List<LikeModel> likes = mLikesUtilsProvider.likesResponseToLikeList(
                                response,
                                siteId,
                                commentId,
                                LikeType.COMMENT_LIKE
                        );

                        FetchedCommentLikesResponsePayload payload = new FetchedCommentLikesResponsePayload(
                                likes,
                                siteId,
                                commentId,
                                requestNextPage,
                                likes.size() >= pageLength
                        );
                        mDispatcher.dispatch(CommentActionBuilder.newFetchedCommentLikesAction(payload));
                    }
                },

                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        mDispatcher.dispatch(CommentActionBuilder.newFetchedCommentLikesAction(
                                CommentErrorUtils.commentErrorToFetchedCommentLikesPayload(
                                        error,
                                        siteId,
                                        commentId,
                                        requestNextPage,
                                        true
                                )
                        ));
                    }
                }
        );
        add(request);
    }

    public void deleteComment(final SiteModel site, long remoteCommentId, @Nullable final CommentModel comment) {
        // Prioritize CommentModel over comment id.
        if (comment != null) {
            remoteCommentId = comment.getRemoteCommentId();
        }

        String url = WPCOMREST.sites.site(site.getSiteId()).comments.comment(remoteCommentId).delete.getUrlV1_1();
        final WPComGsonRequest<CommentWPComRestResponse> request = WPComGsonRequest.buildPostRequest(
                url, null, CommentWPComRestResponse.class,
                new Listener<CommentWPComRestResponse>() {
                    @Override
                    public void onResponse(CommentWPComRestResponse response) {
                        CommentModel modifiedComment = commentResponseToComment(response, site);
                        if (comment != null) {
                            // reconciliate local instance and newly created object if it exists locally
                            modifiedComment.setId(comment.getId());
                        }
                        RemoteCommentResponsePayload payload = new RemoteCommentResponsePayload(modifiedComment);
                        mDispatcher.dispatch(CommentActionBuilder.newDeletedCommentAction(payload));
                    }
                },

                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        mDispatcher.dispatch(CommentActionBuilder.newDeletedCommentAction(
                                CommentErrorUtils.commentErrorToFetchCommentPayload(error, comment)));
                    }
                }
        );
        add(request);
    }

    public void createNewReply(final SiteModel site, final CommentModel comment, final CommentModel reply) {
        String url = WPCOMREST.sites.site(site.getSiteId()).comments.comment(comment.getRemoteCommentId())
                .replies.new_.getUrlV1_1();
        Map<String, Object> params = new HashMap<>();
        params.put("content", reply.getContent());
        final WPComGsonRequest<CommentWPComRestResponse> request = WPComGsonRequest.buildPostRequest(
                url, params, CommentWPComRestResponse.class,
                new Listener<CommentWPComRestResponse>() {
                    @Override
                    public void onResponse(CommentWPComRestResponse response) {
                        CommentModel newComment = commentResponseToComment(response, site);
                        newComment.setId(reply.getId()); // reconciliate local instance and newly created object
                        RemoteCommentResponsePayload payload = new RemoteCommentResponsePayload(newComment);
                        mDispatcher.dispatch(CommentActionBuilder.newCreatedNewCommentAction(payload));
                    }
                },

                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        mDispatcher.dispatch(CommentActionBuilder.newCreatedNewCommentAction(
                                CommentErrorUtils.commentErrorToFetchCommentPayload(error, reply)));
                    }
                }
        );
        add(request);
    }

    public void createNewComment(final SiteModel site, final PostModel post, final CommentModel comment) {
        String url = WPCOMREST.sites.site(site.getSiteId()).posts.post(post.getRemotePostId())
                .replies.new_.getUrlV1_1();
        Map<String, Object> params = new HashMap<>();
        params.put("content", comment.getContent());
        final WPComGsonRequest<CommentWPComRestResponse> request = WPComGsonRequest.buildPostRequest(
                url, params, CommentWPComRestResponse.class,
                new Listener<CommentWPComRestResponse>() {
                    @Override
                    public void onResponse(CommentWPComRestResponse response) {
                        CommentModel newComment = commentResponseToComment(response, site);
                        newComment.setId(comment.getId()); // reconciliate local instance and newly created object
                        RemoteCommentResponsePayload payload = new RemoteCommentResponsePayload(newComment);
                        mDispatcher.dispatch(CommentActionBuilder.newCreatedNewCommentAction(payload));
                    }
                },

                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        mDispatcher.dispatch(CommentActionBuilder.newCreatedNewCommentAction(
                                CommentErrorUtils.commentErrorToFetchCommentPayload(error, comment)));
                    }
                }
        );
        add(request);
    }

    public void likeComment(final SiteModel site, long remoteCommentId, @Nullable final CommentModel comment,
                            boolean like) {
        // Prioritize CommentModel over comment id.
        if (comment != null) {
            remoteCommentId = comment.getRemoteCommentId();
        }

        String url;
        if (like) {
            url = WPCOMREST.sites.site(site.getSiteId()).comments.comment(remoteCommentId).likes.new_.getUrlV1_1();
        } else {
            url = WPCOMREST.sites.site(site.getSiteId()).comments.comment(remoteCommentId).likes.mine.delete
                    .getUrlV1_1();
        }
        final WPComGsonRequest<CommentLikeWPComRestResponse> request = WPComGsonRequest.buildPostRequest(
                url, null, CommentLikeWPComRestResponse.class,
                new Listener<CommentLikeWPComRestResponse>() {
                    @Override
                    public void onResponse(CommentLikeWPComRestResponse response) {
                        RemoteCommentResponsePayload payload = new RemoteCommentResponsePayload(comment);

                        if (comment != null) {
                            comment.setILike(response.i_like);
                        }
                        mDispatcher.dispatch(CommentActionBuilder.newLikedCommentAction(payload));
                    }
                },

                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        mDispatcher.dispatch(CommentActionBuilder.newLikedCommentAction(
                                CommentErrorUtils.commentErrorToFetchCommentPayload(error, comment)));
                    }
                }
        );
        add(request);
    }

    // Private methods

    private List<CommentModel> commentsResponseToCommentList(CommentsWPComRestResponse response, SiteModel site) {
        List<CommentModel> comments = new ArrayList<>();
        if (response.comments != null) {
            for (CommentWPComRestResponse restComment : response.comments) {
                comments.add(commentResponseToComment(restComment, site));
            }
        }
        return comments;
    }

    private CommentModel commentResponseToComment(CommentWPComRestResponse response, SiteModel site) {
        CommentModel comment = new CommentModel();

        comment.setRemoteCommentId(response.ID);
        comment.setLocalSiteId(site.getId());
        comment.setRemoteSiteId(site.getSiteId());

        comment.setStatus(response.status);
        comment.setDatePublished(response.date);
        comment.setContent(response.content);
        comment.setILike(response.i_like);
        comment.setUrl(response.URL);
        comment.setPublishedTimestamp(DateTimeUtils.timestampFromIso8601(response.date));

        if (response.author != null) {
            comment.setAuthorUrl(response.author.URL);
            comment.setAuthorName(StringEscapeUtils.unescapeHtml4(response.author.name));
            if ("false".equals(response.author.email)) {
                comment.setAuthorEmail("");
            } else {
                comment.setAuthorEmail(response.author.email);
            }
            comment.setAuthorProfileImageUrl(response.author.avatar_URL);
        }

        if (response.post != null) {
            comment.setRemotePostId(response.post.ID);
            comment.setPostTitle(StringEscapeUtils.unescapeHtml4(response.post.title));
        }

        if (response.author != null) {
            comment.setAuthorId(response.author.ID);
        }

        if (response.parent != null) {
            comment.setHasParent(true);
            comment.setParentId(response.parent.ID);
        } else {
            comment.setHasParent(false);
        }

        return comment;
    }
}
