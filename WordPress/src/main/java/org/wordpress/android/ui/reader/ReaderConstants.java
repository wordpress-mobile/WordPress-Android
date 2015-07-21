package org.wordpress.android.ui.reader;

public class ReaderConstants {
    public static final int  READER_MAX_POSTS_TO_REQUEST       = 20;     // max # posts to request when updating posts
    public static final int  READER_MAX_POSTS_TO_DISPLAY       = 200;    // max # posts to display
    public static final int  READER_MAX_COMMENTS_TO_REQUEST    = 20;     // max # top-level comments to request when updating comments
    public static final int  READER_MAX_USERS_TO_DISPLAY       = 500;    // max # users to show in ReaderUserListActivity
    public static final long READER_AUTO_UPDATE_DELAY_MINUTES  = 10;     // 10 minute delay between automatic updates
    public static final int  READER_MAX_RECOMMENDED_TO_REQUEST = 20;     // max # of recommended blogs to request

    public static final int MIN_FEATURED_IMAGE_WIDTH = 640;              // min width for an image to be suitable featured image

    // intent arguments / keys
    static final String ARG_TAG               = "tag";
    static final String ARG_BLOG_ID           = "blog_id";
    static final String ARG_FEED_ID           = "feed_id";
    static final String ARG_POST_ID           = "post_id";
    static final String ARG_COMMENT_ID        = "comment_id";
    static final String ARG_IMAGE_URL         = "image_url";
    static final String ARG_IS_PRIVATE        = "is_private";
    static final String ARG_POST_LIST_TYPE    = "post_list_type";
    static final String ARG_TITLE             = "title";
    static final String ARG_CONTENT           = "content";
    static final String ARG_IS_SINGLE_POST    = "is_single_post";

    static final String KEY_ALREADY_UPDATED   = "already_updated";
    static final String KEY_ALREADY_REQUESTED = "already_requested";
    static final String KEY_RESTORE_POSITION  = "restore_position";
    static final String KEY_WAS_PAUSED        = "was_paused";
}
