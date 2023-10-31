package org.wordpress.android.fluxc.network.rest.wpcom.media;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.fluxc.network.Response;

import java.util.List;

/**
 * Response to GET request for media items
 * <p>
 * @see <a href="https://developer.wordpress.com/docs/api/1.1/get/sites/%24site/media/%24media_ID/">doc</a>
 */
@SuppressWarnings("NotNullFieldNotInitialized")
public class MediaWPComRestResponse implements Response {
    public static final String DELETED_STATUS = "deleted";

    public static class MultipleMediaResponse {
        @NonNull public List<MediaWPComRestResponse> media;
    }

    public static class Thumbnails {
        @Nullable public String thumbnail;
        @Nullable public String medium;
        @Nullable public String large;
        @Nullable public String fmt_std;
    }

    public long ID;
    @NonNull public String date;
    public long post_ID;
    public long author_ID;
    @NonNull public String URL;
    @NonNull public String guid;
    @NonNull public String file;
    @NonNull public String extension;
    @NonNull public String mime_type;
    @NonNull public String title;
    @NonNull public String caption;
    @NonNull public String description;
    @NonNull public String alt;
    @Nullable public Thumbnails thumbnails;
    public int height;
    public int width;
    public int length;
    @Nullable public String videopress_guid;
    public boolean videopress_processing_done;
    @Nullable public String status;
}
