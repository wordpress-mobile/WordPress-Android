package org.wordpress.android.fluxc.network.rest.wpcom.stockmedia;

import android.content.Context;
import androidx.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.Response;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.StockMediaActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComErrorListener;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.store.StockMediaStore.FetchedStockMediaListPayload;
import org.wordpress.android.fluxc.store.StockMediaStore.StockMediaError;
import org.wordpress.android.fluxc.store.StockMediaStore.StockMediaErrorType;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.UrlUtils;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

@Singleton
public class StockMediaRestClient extends BaseWPComRestClient {
    // this should be a multiple of both 3 and 4 since WPAndroid shows either 3 or 4 pics per row
    public static final int DEFAULT_NUM_STOCK_MEDIA_PER_FETCH = 36;

    public StockMediaRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue,
                                AccessToken accessToken, UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
    }

    /**
     * Gets a list of stock media items matching a query string
     */
    public void searchStockMedia(@NonNull final String searchTerm, final int page) {
        String url = WPCOMREST.meta.external_media.pexels.getUrlV1_1();

        Map<String, String> params = new HashMap<>();
        params.put("number", Integer.toString(DEFAULT_NUM_STOCK_MEDIA_PER_FETCH));
        params.put("page_handle", Integer.toString(page));
        params.put("source", "pexels");
        params.put("search", UrlUtils.urlEncode(searchTerm));

        WPComGsonRequest request = WPComGsonRequest.buildGetRequest(url, params, SearchStockMediaResponse.class,
                new Response.Listener<SearchStockMediaResponse>() {
                    @Override
                    public void onResponse(SearchStockMediaResponse response) {
                        FetchedStockMediaListPayload payload =
                                new FetchedStockMediaListPayload(
                                        response.getMedia(),
                                        searchTerm,
                                        response.getNextPage(),
                                        response.getCanLoadMore());
                        mDispatcher.dispatch(StockMediaActionBuilder.newFetchedStockMediaAction(payload));
                    }
                }, new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        AppLog.e(AppLog.T.MEDIA, "VolleyError Fetching stock media: " + error);
                        StockMediaError mediaError = new StockMediaError(
                                StockMediaErrorType.fromBaseNetworkError(error), error.message);
                        FetchedStockMediaListPayload payload = new FetchedStockMediaListPayload(mediaError, searchTerm);
                        mDispatcher.dispatch(StockMediaActionBuilder.newFetchedStockMediaAction(payload));
                    }
                });
        add(request);
    }
}
