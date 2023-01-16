package org.wordpress.android.ui.comments.unified

import org.wordpress.android.util.analytics.AnalyticsUtils.AnalyticsCommentActionSource

enum class CommentSource {
    NOTIFICATION, SITE_COMMENTS;

    fun toAnalyticsCommentActionSource(): AnalyticsCommentActionSource =
        when (this) {
            NOTIFICATION -> AnalyticsCommentActionSource.NOTIFICATIONS
            SITE_COMMENTS -> AnalyticsCommentActionSource.SITE_COMMENTS
        }
}
