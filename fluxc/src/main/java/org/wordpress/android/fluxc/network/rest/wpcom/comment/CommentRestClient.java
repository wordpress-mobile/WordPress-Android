package org.wordpress.android.fluxc.network.rest.wpcom.comment;

import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentWPComRestResponse.CommentsWPComRestResponse;

import javax.inject.Inject;

public class CommentRestClient extends BaseWPComRestClient {
    @Inject
    public CommentRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue,
                             AccessToken accessToken, UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
    }

    public void fetchComments(final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).comments.getUrlV1_1();
        final WPComGsonRequest<CommentsWPComRestResponse> request = new WPComGsonRequest<>(Method.GET,
                url, null, CommentsWPComRestResponse.class,
                new Listener<CommentsWPComRestResponse>() {
                    @Override
                    public void onResponse(CommentsWPComRestResponse response) {
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                    }
                }
        );
        add(request);
    }
}
