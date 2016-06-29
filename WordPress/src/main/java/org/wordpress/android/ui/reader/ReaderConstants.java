package org.wordpress.android.ui.reader;

public class ReaderConstants {
    public static final int  READER_MAX_POSTS_TO_REQUEST        = 20;       // max # posts to request when updating posts
    public static final int  READER_MAX_SEARCH_POSTS_TO_REQUEST = 10;       // max # posts to request when searching posts
    public static final int  READER_MAX_POSTS_TO_DISPLAY        = 200;      // max # posts to display
    public static final int  READER_MAX_COMMENTS_TO_REQUEST     = 20;       // max # top-level comments to request when updating comments
    public static final int  READER_MAX_USERS_TO_DISPLAY        = 500;      // max # users to show in ReaderUserListActivity
    public static final long READER_AUTO_UPDATE_DELAY_MINUTES   = 10;       // 10 minute delay between automatic updates
    public static final int  READER_MAX_RECOMMENDED_TO_REQUEST  = 20;       // max # of recommended blogs to request

    public static final int MIN_FEATURED_IMAGE_WIDTH = 640;                 // min width for an image to be suitable featured image

    // min size for images in post content to be shown in a gallery (thumbnail strip) - matches
    // the Calypso web reader
    public static int MIN_GALLERY_IMAGE_WIDTH  = 144;

    public static final String HTTP_REFERER_URL = "https://wordpress.com";  // referrer url for reader posts opened in a browser

    // intent arguments / keys
    static final String ARG_TAG               = "tag";
    static final String ARG_BLOG_ID           = "blog_id";
    static final String ARG_FEED_ID           = "feed_id";
    static final String ARG_POST_ID           = "post_id";
    static final String ARG_COMMENT_ID        = "comment_id";
    static final String ARG_IMAGE_URL         = "image_url";
    static final String ARG_IS_PRIVATE        = "is_private";
    static final String ARG_IS_GALLERY        = "is_gallery";
    static final String ARG_POST_LIST_TYPE    = "post_list_type";
    static final String ARG_CONTENT           = "content";
    static final String ARG_IS_SINGLE_POST    = "is_single_post";
    static final String ARG_IS_RELATED_POST   = "is_related_post";
    static final String ARG_SEARCH_QUERY      = "search_query";

    static final String KEY_ALREADY_UPDATED   = "already_updated";
    static final String KEY_ALREADY_REQUESTED = "already_requested";
    static final String KEY_RESTORE_POSITION  = "restore_position";
    static final String KEY_WAS_PAUSED        = "was_paused";
    static final String KEY_ERROR_MESSAGE     = "error_message";
    static final String KEY_FIRST_LOAD        = "first_load";

    // JSON key names
    //  tag endpoints
    public static final String JSON_TAG_TAGS_ARRAY      = "tags";
    public static final String JSON_TAG_TITLE           = "title";
    public static final String JSON_TAG_DISPLAY_NAME    = "tag_display_name";
    public static final String JSON_TAG_SLUG            = "slug";
    public static final String JSON_TAG_URL             = "URL";
}
