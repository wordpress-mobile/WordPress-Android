package org.wordpress.android.fluxc.network.rest.wpcom.media;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.Response;

import java.util.List;

/**
 * Response to GET request for media items
 *
 * https://developer.wordpress.com/docs/api/1.1/get/sites/%24site/media/%24media_ID/
 */
public class MediaWPComRestResponse extends Payload implements Response {
    public static final String DELETED_STATUS = "deleted";

    public class MultipleMediaResponse {
        public List<MediaWPComRestResponse> media;
        public List<String> errors;
        public int found;
    }

    public class Thumbnails {
        public String thumbnail;
        public String medium;
        public String large;
        public String post_thumbnail;
    }

    public long ID;
    public String date;
    public long post_ID;
    public long author_ID;
    public String URL;
    public String guid;
    public String file;
    public String extension;
    public String mime_type;
    public String title;
    public String caption;
    public String description;
    public String alt;
    public Thumbnails thumbnails;
    public int height;
    public int width;
    public int length;
    public String videopress_guid;
    public boolean videopress_processing_done;
    public String status;
}
