package org.wordpress.android.fluxc.network.rest.wpcom.stockmedia;

import org.wordpress.android.fluxc.network.Response;

import java.util.List;

/**
 * Response to GET request to search for stock media item
 */
public class SearchStockMediaResponse implements Response {
    public List<StockMediaResponse> stockMedia;
    public int found;
    public int nextPage;
}
