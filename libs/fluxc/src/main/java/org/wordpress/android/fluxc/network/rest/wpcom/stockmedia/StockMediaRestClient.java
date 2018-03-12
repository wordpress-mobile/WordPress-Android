package org.wordpress.android.fluxc.network.rest.wpcom.stockmedia;

import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.StockMediaActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.StockMediaModel;
import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaWPComRestResponse.MultipleMediaResponse;
import org.wordpress.android.fluxc.store.StockMediaStore;
import org.wordpress.android.fluxc.store.StockMediaStore.StockMediaError;
import org.wordpress.android.fluxc.store.StockMediaStore.StockMediaErrorType;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import static org.wordpress.android.fluxc.network.rest.wpcom.media.MediaRestClient.getMediaListFromRestResponse;

@Singleton
public class StockMediaRestClient extends BaseWPComRestClient {
    public static final int DEFAULT_NUM_STOCK_MEDIA_PER_FETCH = 20;

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
                        StockMediaStore.FetchedStockMediaListPayload payload =
                                new StockMediaStore.FetchedStockMediaListPayload(
                                        response.media,
                                        searchTerm,
                                        response.nextPage,
                                        response.canLoadMore);
                        mDispatcher.dispatch(StockMediaActionBuilder.newFetchedStockMediaAction(payload));
                    }
                }, new BaseRequest.BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                        AppLog.e(AppLog.T.MEDIA, "VolleyError Fetching stock media: " + error);
                        StockMediaError mediaError = new StockMediaError(
                                StockMediaErrorType.fromBaseNetworkError(error), error.message);
                        StockMediaStore.FetchedStockMediaListPayload payload =
                                new StockMediaStore.FetchedStockMediaListPayload(mediaError, searchTerm);
                        mDispatcher.dispatch(StockMediaActionBuilder.newFetchedStockMediaAction(payload));
                    }
                }
                                                                   );

        add(request);
    }

    public void uploadStockMedia(@NonNull final SiteModel site,
                                 @NonNull List<StockMediaModel> stockMediaList) {
        String url = WPCOMREST.sites.site(site.getSiteId()).external_media_upload.getUrlV1_1();

        JsonArray jsonBody = new JsonArray();
        for (StockMediaModel stockMedia : stockMediaList) {
            JsonObject json = new JsonObject();
            json.addProperty("url", StringUtils.notNullStr(stockMedia.getUrl()));
            json.addProperty("name", StringUtils.notNullStr(stockMedia.getName()));
            json.addProperty("title", StringUtils.notNullStr(stockMedia.getTitle()));
            jsonBody.add(json.toString());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("service", "pexels");
        body.put("external_ids", jsonBody);

        WPComGsonRequest request = WPComGsonRequest.buildPostRequest(url, body, MultipleMediaResponse.class,
                new Response.Listener<MultipleMediaResponse>() {
                    @Override
                    public void onResponse(MultipleMediaResponse response) {
                        List<MediaModel> mediaList = getMediaListFromRestResponse(response, site.getId());
                        StockMediaStore.UploadedStockMediaPayload payload =
                                new StockMediaStore.UploadedStockMediaPayload(site, mediaList);
                        mDispatcher.dispatch(StockMediaActionBuilder.newUploadedStockMediaAction(payload));
                    }
                }, new BaseRequest.BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                        AppLog.e(AppLog.T.MEDIA, "VolleyError uploading stock media: " + error);
                        StockMediaError mediaError = new StockMediaError(
                                StockMediaErrorType.fromBaseNetworkError(error), error.message);
                        StockMediaStore.UploadedStockMediaPayload payload =
                                new StockMediaStore.UploadedStockMediaPayload(site, mediaError);
                        mDispatcher.dispatch(StockMediaActionBuilder.newUploadedStockMediaAction(payload));
                    }
                }
                                                                    );

        try {
            String strBody = new String(request.getBody());
            AppLog.w(AppLog.T.MEDIA, strBody);
        } catch (AuthFailureError authFailureError) {
            authFailureError.printStackTrace();
        }

        add(request);
    }
}
