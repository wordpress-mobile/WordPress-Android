package org.wordpress.android.ui.reader

import org.wordpress.android.ui.reader.tracker.ReaderTracker

object ReaderTypes {
    val DEFAULT_POST_LIST_TYPE = ReaderPostListType.TAG_FOLLOWED

    enum class ReaderPostListType(val source: String) {
        TAG_FOLLOWED(ReaderTracker.SOURCE_FOLLOWING), // list posts in a followed tag
        TAG_PREVIEW(ReaderTracker.SOURCE_TAG_PREVIEW), // list posts in a specific tag
        BLOG_PREVIEW(ReaderTracker.SOURCE_SITE_PREVIEW), // list posts in a specific blog/feed
        SEARCH_RESULTS(ReaderTracker.SOURCE_SEARCH), // list posts matching a specific search keyword or phrase
        TAGS_FEED(ReaderTracker.SOURCE_TAGS_FEED); // list posts in the tags feed

        val isTagType: Boolean
            get() = this == TAG_FOLLOWED || this == TAG_PREVIEW
    }
}
