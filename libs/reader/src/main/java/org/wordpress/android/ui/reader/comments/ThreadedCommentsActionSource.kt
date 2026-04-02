package org.wordpress.android.ui.reader.comments

import org.wordpress.android.ui.reader.tracker.ReaderTracker.Companion.SOURCE_DISCOVER
import org.wordpress.android.ui.reader.tracker.ReaderTracker.Companion.SOURCE_POST_DETAIL
import org.wordpress.android.ui.reader.tracker.ReaderTracker.Companion.SOURCE_POST_DETAIL_COMMENT_SNIPPET

@Suppress("ForbiddenComment")
enum class ThreadedCommentsActionSource(val sourceDescription: String) {
    READER_POST_CARD("reader_post_card"),
    READER_POST_DETAILS("reader_post_details"),
    READER_POST_DETAILS_COMMENTS("reader_post_details_comments"),
    READER_THREADED_COMMENTS("reader_threaded_comments"),
    COMMENT_NOTIFICATION("comment_notification"),
    MY_SITE_COMMENT("my_site_comment"), // TODO: currently not used but we could want to mimic iOS here
    COMMENT_LIKE_NOTIFICATION("comment_like_notification"),
    ACTIVITY_LOG_DETAIL("activity_log_detail"),
    DIRECT_OPERATION("direct_operation"),
    UNKNOWN("unknown");

    companion object {
        fun mapReaderPostSource(source: String): ThreadedCommentsActionSource {
            return if (source.equals(SOURCE_POST_DETAIL)) {
                READER_POST_DETAILS
            } else if (source.equals(SOURCE_POST_DETAIL_COMMENT_SNIPPET)) {
                READER_POST_DETAILS_COMMENTS
            } else if (source.equals(SOURCE_DISCOVER)) {
                READER_POST_CARD
            } else {
                UNKNOWN
            }
        }
    }
}
