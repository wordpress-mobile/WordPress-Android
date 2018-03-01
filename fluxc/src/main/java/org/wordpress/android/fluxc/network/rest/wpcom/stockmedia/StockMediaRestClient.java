package org.wordpress.android.fluxc.network.rest.wpcom.stockmedia;

import android.content.Context;

import com.android.volley.RequestQueue;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.StockMediaModel;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;

import javax.inject.Singleton;

import okhttp3.OkHttpClient;

@Singleton
public class StockMediaRestClient extends BaseWPComRestClient {
    private OkHttpClient mOkHttpClient;

    public StockMediaRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue,
                                OkHttpClient okHttpClient, AccessToken accessToken, UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
        mOkHttpClient = okHttpClient;
    }

    /**
     * Creates a {@link StockMediaModel} from a WP.com REST response to a fetch request.
     */
    private StockMediaModel getStockMediaFromRestResponse(final StockMediaWPComRestResponse from) {
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
