package org.wordpress.android.ui.reader;

public class ReaderConstants {
    // max # posts to request when updating posts
    public static final int READER_MAX_POSTS_TO_REQUEST = 20;

    // max # results to request when searching posts & sites
    public static final int READER_MAX_SEARCH_RESULTS_TO_REQUEST = 20;

    // max # posts to display
    public static final int READER_MAX_POSTS_TO_DISPLAY = 200;

    // max # top-level comments to request when updating comments
    public static final int READER_MAX_COMMENTS_TO_REQUEST = 20;

    public static final int READER_MAX_USERS_TO_DISPLAY = 500; // max # users to show in ReaderUserListActivity
    public static final long READER_AUTO_UPDATE_DELAY_MINUTES = 10; // 10 minute delay between automatic updates
    public static final int READER_MAX_RECOMMENDED_TO_REQUEST = 20; // max # of recommended blogs to request

    // min width for an image to be suitable featured image
    public static final int MIN_FEATURED_IMAGE_WIDTH = 640;

    public static final long DISCOVER_SITE_ID = 53424024; // site id for discover.wordpress.com

    // min size for images in post content to be shown in a gallery (thumbnail strip) - matches
    // the Calypso web reader
    public static final int MIN_GALLERY_IMAGE_WIDTH = 144;

    public static final int THUMBNAIL_STRIP_IMG_COUNT = 4;

    // referrer url for reader posts opened in a browser
    public static final String HTTP_REFERER_URL = "https://wordpress.com";

    // intent arguments / keys
    static final String ARG_TAG = "tag";
    static final String ARG_ORIGINAL_TAG = "original_tag";
    static final String ARG_IS_FEED = "is_feed";
    static final String ARG_BLOG_ID = "blog_id";
    static final String ARG_FEED_ID = "feed_id";
    static final String ARG_POST_ID = "post_id";
    static final String ARG_INTERCEPTED_URI = "intercepted_uri";
    static final String ARG_COMMENT_ID = "comment_id";
    static final String ARG_DIRECT_OPERATION = "direct_operation";
    static final String ARG_IMAGE_URL = "image_url";
    static final String ARG_IS_PRIVATE = "is_private";
    static final String ARG_IS_GALLERY = "is_gallery";
    static final String ARG_POST_LIST_TYPE = "post_list_type";
    static final String ARG_CONTENT = "content";
    static final String ARG_IS_SINGLE_POST = "is_single_post";
    static final String ARG_IS_RELATED_POST = "is_related_post";
    static final String ARG_SEARCH_QUERY = "search_query";
    static final String ARG_VIDEO_URL = "video_url";
    static final String ARG_IS_TOP_LEVEL = "is_top_level";
    static final String ARG_SUBS_TAB_POSITION = "subs_tab_position";

    static final String KEY_POST_SLUGS_RESOLUTION_UNDERWAY = "post_slugs_resolution_underway";
    static final String KEY_ALREADY_UPDATED = "already_updated";
    static final String KEY_ALREADY_REQUESTED = "already_requested";
    static final String KEY_RESTORE_POSITION = "restore_position";
    static final String KEY_SITE_SEARCH_RESTORE_POSITION = "site_search_restore_position";
    static final String KEY_WAS_PAUSED = "was_paused";
    static final String KEY_ERROR_MESSAGE = "error_message";
    static final String KEY_FIRST_LOAD = "first_load";
    static final String KEY_ACTIVITY_TITLE = "activity_title";
    static final String KEY_TRACKED_POSITIONS = "tracked_positions";
    static final String KEY_IS_REFRESHING = "is_refreshing";
    static final String KEY_ACTIVE_SEARCH_TAB = "active_search_tab";
    static final String KEY_SITE_ID = "site_id";

    static final String KEY_ALREADY_TRACKED_GLOBAL_RELATED_POSTS = "already_tracked_global_related_posts";
    static final String KEY_ALREADY_TRACKED_LOCAL_RELATED_POSTS = "already_tracked_local_related_posts";

    // JSON key names
    public static final String JSON_TAG_TAGS_ARRAY = "tags";
    public static final String JSON_TAG_TITLE = "title";
    public static final String JSON_TAG_DISPLAY_NAME = "tag_display_name";
    public static final String JSON_TAG_SLUG = "slug";
    public static final String JSON_TAG_URL = "URL";
    public static final String JSON_CARDS = "cards";
    public static final String JSON_CARD_TYPE = "type";
    public static final String JSON_CARD_INTERESTS_YOU_MAY_LIKE = "interests_you_may_like";
    public static final String JSON_CARD_POST = "post";
    public static final String JSON_CARD_DATA = "data";
    public static final String JSON_NEXT_PAGE_HANDLE = "next_page_handle";

    // JSON Post key names
    public static final String POST_ID = "ID";
    public static final String POST_SITE_ID = "site_ID";
    public static final String POST_PSEUDO_ID = "pseudo_ID";

    public static final String KEY_FOLLOWING = "following";
    public static final String KEY_DISCOVER = "discover";
    public static final String KEY_LIKES = "likes";
    public static final String KEY_SAVED = "saved";
}
