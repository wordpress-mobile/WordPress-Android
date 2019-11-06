package org.wordpress.android.ui

enum class PagePostCreationSources(val label: String) {
    POST_FROM_SHORTCUT("post-from-short-cut"),
    POST_FROM_LIST("post-from-list"),
    PAGE_FROM_LIST("page-from-list"),
    POST_FROM_NAV_BAR("post-from-nav-bar"),
    POST_FROM_MY_SITE("post-from-my-site"),
    PAGE_FROM_MY_SITE("page-from-my-site"),
    POST_FROM_STATS("post-from-stats"),
    POST_FROM_NOTIFS_EMPTY_VIEW("post-from-notif-empty-view"),
    OTHER("other");

    companion object {
        const val CREATED_POST_SOURCE_DETAIL_KEY = "created_post_source_detail"
    }
}
