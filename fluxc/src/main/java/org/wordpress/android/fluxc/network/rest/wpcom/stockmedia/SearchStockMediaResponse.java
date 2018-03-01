package org.wordpress.android.fluxc.network.rest.wpcom.stockmedia;

import org.wordpress.android.fluxc.network.Response;

import java.util.List;

/**
 * Response to GET request to search for stock media item
 */
public class SearchStockMediaResponse implements Response {
    public List<StockMediaResponse> media;
    public int found;
    public Meta meta;

    public class Meta {
        public int next_page;
    }

    public int getNextPage() {
        return meta != null ? meta.next_page : 1;
    }
}
