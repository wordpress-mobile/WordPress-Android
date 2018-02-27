package org.wordpress.android.fluxc.network.rest.wpcom.media;

import org.wordpress.android.fluxc.network.Response;

/**
 * Response to GET request for stock media items
 */
public class StockMediaWPComRestResponse implements Response {
    public String ID;
    public String date;
    public String extension;
    public String URL;
    public String guid;
    public String file;
    public String title;
    public String name;
    public String type;

    public int height;
    public int width;

    public MediaWPComRestResponse.Thumbnails thumbnails;
}
