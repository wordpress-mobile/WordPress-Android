package org.wordpress.android.ui

enum class PagePostCreationSourcesDetail(val label: String) {
    // post created from android shortcut long pressing on app icon
    POST_FROM_SHORTCUT("post-from-shortcut"),
    // post created from the posts list fab
    POST_FROM_POSTS_LIST("post-from-posts-list"),
    // story created from the posts list fab
    STORY_FROM_POSTS_LIST("story-from-posts-list"),
    // page created from the pages list fab
    PAGE_FROM_PAGES_LIST("page-from-pages-list"),
    // post created from the navigation bar (until we will remove it)
    POST_FROM_NAV_BAR("post-from-nav-bar"),
    // post created from bottom sheet in my site screen
    POST_FROM_MY_SITE("post-from-my-site"),
    // page created from bottom sheet in my site screen
    PAGE_FROM_MY_SITE("page-from-my-site"),
    // story created from bottom sheet in my site screen
    STORY_FROM_MY_SITE("story-from-my-site"),
    // post created from stats empty view when no stats/post available yet
    POST_FROM_STATS("post-from-stats"),
    // post created from notifications unread page when empty
    POST_FROM_NOTIFS_EMPTY_VIEW("post-from-notif-empty-view"),
    // post created from reader reblog action
    POST_FROM_REBLOG("post-from-reader-reblog"),
    // post created from reader reblog action
    POST_FROM_DETAIL_REBLOG("post-from-reader-detail-reblog"),
    // all other cases container
    NO_DETAIL("no-detail");

    companion object {
        const val CREATED_POST_SOURCE_DETAIL_KEY = "created_post_source_detail"
    }
}
