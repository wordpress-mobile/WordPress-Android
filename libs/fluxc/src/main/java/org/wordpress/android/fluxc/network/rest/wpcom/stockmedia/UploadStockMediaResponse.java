package org.wordpress.android.fluxc.network.rest.wpcom.stockmedia;

import android.support.annotation.NonNull;

import org.wordpress.android.fluxc.model.StockMediaUploadModel;
import org.wordpress.android.fluxc.network.Response;

import java.util.List;

/*
 * Response to POST request to upload stock media
 */
@SuppressWarnings("WeakerAccess")
public class UploadStockMediaResponse implements Response {
    public @NonNull List<StockMediaUploadModel> uploadedMedia;
}
