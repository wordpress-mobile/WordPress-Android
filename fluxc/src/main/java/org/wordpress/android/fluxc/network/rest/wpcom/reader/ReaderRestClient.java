package org.wordpress.android.fluxc.network.rest.wpcom.reader;

import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.Response;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.ReaderActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComErrorListener;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.store.ReaderStore.ReaderError;
import org.wordpress.android.fluxc.store.ReaderStore.ReaderErrorType;
import org.wordpress.android.fluxc.store.ReaderStore.ReaderSearchSitesResponsePayload;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.UrlUtils;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

@Singleton
public class ReaderRestClient extends BaseWPComRestClient {
    public ReaderRestClient(Context appContext, Dispatcher dispatcher,
                            RequestQueue requestQueue,
                            AccessToken accessToken,
                            UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
    }

    /*
    {
    "algorithm": "reader/manage/search:0",
    "feeds": [
        {
            "feed_ID": "46238560",
            "meta": {
                "links": {
                    "feed": "https://public-api.wordpress.com/rest/v1.1/read/feed/46238560"
                }
            },
            "railcar": {
                "fetch_algo": "reader/manage/search:0",
                "fetch_lang": "en",
                "fetch_position": 0,
                "fetch_query": "yoga",
                "railcar": "8&6VDLsINQLj",
                "rec_feed_id": 46238560
            },
            "subscribe_URL": "http://theyogalunchbox.co.nz/feed",
            "subscribers_count": 167,
            "title": "The Yoga Lunchbox",
            "URL": "https://theyogalunchbox.co.nz/"
        },
     */

    public void searchReaderSites(@NonNull final String searchTerm, final int offset) {
        String url = WPCOMREST.read.feed.getUrlV1_1();

        Map<String, String> params = new HashMap<>();
        params.put("offset", Integer.toString(offset));
        params.put("exclude_followed", Boolean.toString(true));
        params.put("sort", "relevance");
        params.put("q", UrlUtils.urlEncode(searchTerm));

        WPComGsonRequest request = WPComGsonRequest.buildGetRequest(url, params, ReaderSearchSitesResponse.class,
                new Response.Listener<ReaderSearchSitesResponse>() {
                    @Override
                    public void onResponse(ReaderSearchSitesResponse response) {
                        ReaderSearchSitesResponsePayload payload =
                                new ReaderSearchSitesResponsePayload(
                                        response.sites,
                                        searchTerm,
                                        offset,
                                        response.canLoadMore);
                        mDispatcher.dispatch(ReaderActionBuilder.newReaderSearchedSitesAction(payload));
                    }
                }, new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        AppLog.e(AppLog.T.MEDIA, "VolleyError searching reader sites: " + error);
                        ReaderError readerError = new ReaderError(
                                ReaderErrorType.fromBaseNetworkError(error), error.message);
                        ReaderSearchSitesResponsePayload payload =
                                new ReaderSearchSitesResponsePayload(readerError, searchTerm);
                        mDispatcher.dispatch(ReaderActionBuilder.newReaderSearchedSitesAction(payload));
                    }
                }
                                                                   );
        add(request);
    }
}
