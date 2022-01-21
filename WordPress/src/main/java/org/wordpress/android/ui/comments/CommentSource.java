package org.wordpress.android.ui.comments;

import org.wordpress.android.util.analytics.AnalyticsUtils.AnalyticsCommentActionSource;

enum CommentSource {
    NOTIFICATION,
    SITE_COMMENTS;

    AnalyticsCommentActionSource toAnalyticsCommentActionSource() {
        switch (this) {
            case NOTIFICATION:
                return AnalyticsCommentActionSource.NOTIFICATIONS;
            case SITE_COMMENTS:
                return AnalyticsCommentActionSource.SITE_COMMENTS;
        }
        throw new IllegalArgumentException(
                this + " CommentSource is not mapped to corresponding AnalyticsCommentActionSource");
    }
}
