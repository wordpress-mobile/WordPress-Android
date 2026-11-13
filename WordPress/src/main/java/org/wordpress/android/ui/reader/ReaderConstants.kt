package org.wordpress.android.ui.reader

object ReaderConstants {
    // max # posts to request when updating posts
    const val READER_MAX_POSTS_TO_REQUEST = 7

    // max # results to request when searching posts & sites
    const val READER_MAX_SEARCH_RESULTS_TO_REQUEST = 20

    // max # posts to display
    const val READER_MAX_POSTS_TO_DISPLAY = 200

    // max # top-level comments to request when updating comments
    const val READER_MAX_COMMENTS_TO_REQUEST = 20

    // # top-level comments to request when updating comments snippet in post details
    const val READER_COMMENTS_TO_REQUEST_FOR_POST_SNIPPET = 1

    const val READER_MAX_USERS_TO_DISPLAY = 500 // max # users to show in ReaderUserListActivity
    const val READER_AUTO_UPDATE_DELAY_MINUTES = 10L // 10 minute delay between automatic updates

    const val DISCOVER_SITE_ID = 53424024L // site id for discover.wordpress.com

    // min size for images in post content to be shown in a gallery (thumbnail strip) - matches
    // the Calypso web reader
    const val MIN_GALLERY_IMAGE_WIDTH = 144

    const val THUMBNAIL_STRIP_IMG_COUNT = 4

    // referrer url for reader posts opened in a browser
    const val HTTP_REFERER_URL = "https://wordpress.com"

    // intent arguments / keys
    internal const val ARG_SOURCE = "source"
    internal const val ARG_TAG = "tag"
    internal const val ARG_ORIGINAL_TAG = "original_tag"
    const val ARG_IS_FEED = "is_feed"
    const val ARG_BLOG_ID = "blog_id"
    internal const val ARG_FEED_ID = "feed_id"
    const val ARG_POST_ID = "post_id"
    const val ARG_INTERCEPTED_URI = "intercepted_uri"
    internal const val ARG_COMMENT_ID = "comment_id"
    internal const val ARG_DIRECT_OPERATION = "direct_operation"
    internal const val ARG_IMAGE_URL = "image_url"
    internal const val ARG_IS_PRIVATE = "is_private"
    internal const val ARG_IS_GALLERY = "is_gallery"
    internal const val ARG_POST_LIST_TYPE = "post_list_type"
    internal const val ARG_CONTENT = "content"
    internal const val ARG_GALLERY_IMAGE_URLS = "gallery_image_urls"
    internal const val ARG_IS_SINGLE_POST = "is_single_post"
    internal const val ARG_IS_RELATED_POST = "is_related_post"
    internal const val ARG_SEARCH_QUERY = "search_query"
    internal const val ARG_IS_TOP_LEVEL = "is_top_level"
    const val ARG_SUBS_TAB_POSITION = "subs_tab_position"
    internal const val ARG_IS_FILTERABLE = "is_filterable"

    internal const val KEY_POST_SLUGS_RESOLUTION_UNDERWAY = "post_slugs_resolution_underway"
    internal const val KEY_ALREADY_UPDATED = "already_updated"
    internal const val KEY_ALREADY_REQUESTED = "already_requested"
    internal const val KEY_RESTORE_POSITION = "restore_position"
    internal const val KEY_WAS_PAUSED = "was_paused"
    internal const val KEY_ERROR_MESSAGE = "error_message"
    internal const val KEY_TRACKED_POSITIONS = "tracked_positions"

    internal const val KEY_ALREADY_TRACKED_GLOBAL_RELATED_POSTS = "already_tracked_global_related_posts"
    internal const val KEY_ALREADY_TRACKED_LOCAL_RELATED_POSTS = "already_tracked_local_related_posts"

    // JSON key names
    const val JSON_TAG_TAGS_ARRAY = "tags"
    const val JSON_TAG_TITLE = "title"
    const val JSON_TAG_DISPLAY_NAME = "display_name"
    const val JSON_TAG_SLUG = "slug"
    const val JSON_TAG_URL = "URL"
    const val JSON_CARDS = "cards"
    const val JSON_CARD_TYPE = "type"
    const val JSON_CARD_POST = "post"
    const val JSON_CARD_DATA = "data"
    const val JSON_NEXT_PAGE_HANDLE = "next_page_handle"

    // JSON Post key names
    const val POST_ID = "ID"
    const val POST_SITE_ID = "site_ID"
    const val POST_PSEUDO_ID = "pseudo_ID"

    const val KEY_FOLLOWING = "following"
    const val KEY_DISCOVER = "discover"
    const val KEY_LIKES = "likes"
    const val KEY_SAVED = "saved"

    // Custom action to handle post deeplinks
    const val ACTION_VIEW_POST = "org.wordpress.android.action.VIEW_POST"
}
