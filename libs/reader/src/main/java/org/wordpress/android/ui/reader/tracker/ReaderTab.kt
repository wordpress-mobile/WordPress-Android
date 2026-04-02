package org.wordpress.android.ui.reader.tracker

import org.wordpress.android.models.ReaderTag

enum class ReaderTab(
    val id: Int,
    val source: String
) {
    FOLLOWING(1, ReaderTracker.SOURCE_FOLLOWING),
    DISCOVER(2, ReaderTracker.SOURCE_DISCOVER),
    LIKED(3, ReaderTracker.SOURCE_LIKED),
    SAVED(4, ReaderTracker.SOURCE_SAVED),
    CUSTOM(5, ReaderTracker.SOURCE_CUSTOM),
    A8C(6, ReaderTracker.SOURCE_A8C),
    P2(7, ReaderTracker.SOURCE_P2),
    TAGS_FEED(8, ReaderTracker.SOURCE_TAGS_FEED);

    companion object {
        fun fromId(id: Int): ReaderTab {
            return when (id) {
                FOLLOWING.id -> FOLLOWING
                DISCOVER.id -> DISCOVER
                LIKED.id -> LIKED
                SAVED.id -> SAVED
                A8C.id -> A8C
                P2.id -> P2
                CUSTOM.id -> CUSTOM
                TAGS_FEED.id -> TAGS_FEED
                else -> throw RuntimeException(
                    "Unexpected ReaderTab id"
                )
            }
        }

        @JvmStatic
        fun transformTagToTab(readerTag: ReaderTag): ReaderTab {
            return when {
                readerTag.isFollowedSites -> FOLLOWING
                readerTag.isPostsILike -> LIKED
                readerTag.isBookmarked -> SAVED
                readerTag.isDiscover -> DISCOVER
                readerTag.isA8C -> A8C
                readerTag.isP2 -> P2
                readerTag.isTags -> TAGS_FEED
                else -> CUSTOM
            }
        }
    }
}
