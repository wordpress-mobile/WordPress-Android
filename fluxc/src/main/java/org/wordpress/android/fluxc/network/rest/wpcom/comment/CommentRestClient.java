package org.wordpress.android.fluxc.network.rest.wpcom.comment;

import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.CommentActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentWPComRestResponse.CommentsWPComRestResponse;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsResponsePayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class CommentRestClient extends BaseWPComRestClient {
    private static final String DEFAULT_NUMBER_COMMENTS = "20";

    @Inject
    public CommentRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue,
                             AccessToken accessToken, UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
    }

    public void fetchComments(final SiteModel site, int offset, CommentStatus status) {
        String url = WPCOMREST.sites.site(site.getSiteId()).comments.getUrlV1_1();
        Map<String, String> params = new HashMap<>();
        params.put("status", status.toString());
        params.put("offset", String.valueOf(offset));
        params.put("number", DEFAULT_NUMBER_COMMENTS);
        final WPComGsonRequest<CommentsWPComRestResponse> request = new WPComGsonRequest<>(Method.GET,
                url, params, CommentsWPComRestResponse.class,
                new Listener<CommentsWPComRestResponse>() {
                    @Override
                    public void onResponse(CommentsWPComRestResponse response) {
                        List<CommentModel> comments = commentsResponseToCommentList(response, site);
                        FetchCommentsResponsePayload payload = new FetchCommentsResponsePayload(comments);
                        mDispatcher.dispatch(CommentActionBuilder.newFetchedCommentsAction(payload));
                    }
                },

                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        // FIXME:
                    }
                }
        );
        add(request);
    }

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

        if (response.author != null) {
            comment.setAuthorUrl(response.author.URL);
            comment.setAuthorName(response.author.name);
            comment.setAuthorEmail(response.author.email);
            comment.setAuthorProfileImageUrl(response.author.avatar_URL);
        }

        if (response.post != null) {
            comment.setRemotePostId(response.post.ID);
            comment.setPostTitle(response.post.title);
        }

        return comment;
    }
}
