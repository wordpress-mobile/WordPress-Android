package org.wordpress.android.fluxc.network.rest.wpcom.stockmedia;

import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.Response;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.StockMediaActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.model.StockMediaModel;
import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.StockMediaStore;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.UrlUtils;

import java.util.List;

import javax.inject.Singleton;

@Singleton
public class StockMediaRestClient extends BaseWPComRestClient {
    public StockMediaRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue,
                                AccessToken accessToken, UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
    }

    /**
     * Gets a list of stock media items matching a query string
     */
    public void searchStockMedia(final String searchTerm, final int number, final int page) {
        String url = WPCOMREST.meta.external_media.pexels.getUrlV1_1()
                + "?number=" + number
                + "&page_handle=" + page
                + "&source=pexels"
                + "&path=recent"
                + "&search=" + UrlUtils.urlEncode(searchTerm);
        add(WPComGsonRequest.buildGetRequest(url, null, SearchStockMediaResponse.class,
                new Response.Listener<SearchStockMediaResponse>() {
                    @Override
                    public void onResponse(SearchStockMediaResponse response) {
                        boolean canLoadMore = response.nextPage > page;
                        notifyStockMediaListFetched(response.media, searchTerm, response.nextPage, canLoadMore);
                    }
                }, new BaseRequest.BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                        AppLog.e(AppLog.T.MEDIA, "VolleyError Fetching stock media: " + error);
                        MediaStore.MediaError mediaError =
                                new MediaStore.MediaError(MediaStore.MediaErrorType.fromBaseNetworkError(error));
                        notifyStockMediaListFetched(mediaError);
                    }
                }
        ));
    }

    private void notifyStockMediaListFetched(@NonNull List<StockMediaModel> mediaList,
                                             @NonNull String searchTerm,
                                             int nextPage,
                                             boolean canLoadMore) {
        StockMediaStore.FetchedStockMediaListPayload payload = new StockMediaStore.FetchedStockMediaListPayload(
                mediaList,
                searchTerm,
                nextPage,
                canLoadMore);
        mDispatcher.dispatch(StockMediaActionBuilder.newFetchedStockMediaAction(payload));
    }

    private void notifyStockMediaListFetched(MediaStore.MediaError error) {
        StockMediaStore.FetchedStockMediaListPayload payload = new StockMediaStore.FetchedStockMediaListPayload(error);
        mDispatcher.dispatch(StockMediaActionBuilder.newFetchedStockMediaAction(payload));
    }
}
