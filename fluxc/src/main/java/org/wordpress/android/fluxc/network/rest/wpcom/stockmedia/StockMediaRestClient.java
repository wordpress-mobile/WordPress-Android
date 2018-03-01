package org.wordpress.android.fluxc.network.rest.wpcom.stockmedia;

import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.Response;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.StockMediaActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.model.StockMediaModel;
import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.store.StockMediaStore;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.UrlUtils;

import java.util.ArrayList;
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
                + "&page="
                + page + UrlUtils.urlEncode(searchTerm);
        add(WPComGsonRequest.buildGetRequest(url, null, SearchStockMediaResponse.class,
                new Response.Listener<SearchStockMediaResponse>() {
                    @Override
                    public void onResponse(SearchStockMediaResponse response) {
                        List<StockMediaModel> mediaList = getStockMediaListFromRestResponse(response);
                        AppLog.v(AppLog.T.MEDIA, "Fetched stock media list with size: " + mediaList.size());
                        boolean canLoadMore = mediaList.size() == number;
                        notifyStockMediaListFetched(mediaList, response.nextPage, canLoadMore);
                    }
                }, new BaseRequest.BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                        AppLog.e(AppLog.T.MEDIA, "VolleyError Fetching stock media: " + error);
                        notifyStockMediaListFetched(error);
                    }
                }
        ));
    }

    private void notifyStockMediaListFetched(@NonNull List<StockMediaModel> mediaList,
                                             int nextPage,
                                             boolean canLoadMore) {
        StockMediaStore.FetchStockMediaListPayload payload = new StockMediaStore.FetchStockMediaListPayload(mediaList,
                nextPage, canLoadMore);
        mDispatcher.dispatch(StockMediaActionBuilder.newFetchStockMediaAction(payload));
    }

    private void notifyStockMediaListFetched(BaseRequest.BaseNetworkError error) {
        StockMediaStore.FetchStockMediaListPayload payload = new StockMediaStore.FetchStockMediaListPayload(error);
        mDispatcher.dispatch(StockMediaActionBuilder.newFetchStockMediaAction(payload));
    }

    /**
     * Creates a {@link StockMediaModel} list from a WP.com REST response to a request for all media.
     */
    private List<StockMediaModel> getStockMediaListFromRestResponse(final SearchStockMediaResponse from) {
        if (from == null || from.stockMedia == null) return null;

        final List<StockMediaModel> mediaList = new ArrayList<>();
        for (StockMediaResponse mediaItem : from.stockMedia) {
            StockMediaModel mediaModel = getStockMediaFromRestResponse(mediaItem);
            mediaList.add(mediaModel);
        }
        return mediaList;
    }

    private StockMediaModel getStockMediaFromRestResponse(final StockMediaResponse from) {
        if (from == null) return null;

        final StockMediaModel media = new StockMediaModel();

        media.setId(from.ID);
        media.setDate(from.date);
        media.setExtension(from.extension);
        media.setUrl(from.URL);
        media.setGuid(from.guid);
        media.setFile(from.file);
        media.setTitle(StringEscapeUtils.unescapeHtml4(from.title));
        media.setName(StringEscapeUtils.unescapeHtml4(from.name));
        media.setType(from.type);

        media.setHeight(from.height);
        media.setWidth(from.width);

        if (from.thumbnails != null) {
            media.setThumbnail(from.thumbnails.thumbnail);
            media.setLargeThumbnail(from.thumbnails.large);
            media.setMediumThumbnail(from.thumbnails.medium);
            media.setPostThumbnail(from.thumbnails.post_thumbnail);
        }

        return media;
    }
}
