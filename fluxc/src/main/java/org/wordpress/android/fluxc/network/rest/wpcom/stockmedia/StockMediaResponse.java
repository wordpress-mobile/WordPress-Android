package org.wordpress.android.fluxc.network.rest.wpcom.stockmedia;

import org.wordpress.android.fluxc.network.Response;

/**
 * Response to GET request for stock media item
 */
public class StockMediaResponse implements Response {
    public class Thumbnails {
        public String thumbnail;
        public String medium;
        public String large;
        public String post_thumbnail;
    }

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

    public Thumbnails thumbnails;
}
