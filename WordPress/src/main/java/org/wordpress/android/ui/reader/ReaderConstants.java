package org.wordpress.android.ui.reader;

public class ReaderConstants {
    public static final int  READER_MAX_POSTS_TO_REQUEST      = 20;     // max # posts to request when updating posts
    public static final int  READER_MAX_POSTS_TO_DISPLAY      = 200;    // max # posts to display
    public static final int  READER_MAX_COMMENTS_TO_REQUEST   = 20;     // max # comments to request when updating comments
    public static final int  READER_MAX_USERS_TO_DISPLAY      = 500;    // max # users to show in ReaderUserListActivity
    public static final long READER_AUTO_UPDATE_DELAY_MINUTES = 10;     // 10 minute delay between automatic updates

    public static final int  READER_MAX_RECOMMENDED_TO_REQUEST = 40;     // max # of recommended blogs to request
    public static final int  READER_MAX_RECOMMENDED_TO_DISPLAY = 4;      // max # of recommended blogs to display

    // intent IDs
    static final int INTENT_READER_SUBS        = 1000;
    static final int INTENT_READER_REBLOG      = 1001;

    // intent arguments / keys
    static final String ARG_TAG               = "tag";
    static final String ARG_BLOG_ID           = "blog_id";
    static final String ARG_BLOG_URL          = "blog_url";
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
    static final String KEY_LIST_STATE        = "list_state";
    static final String KEY_WAS_PAUSED        = "was_paused";
}
