package org.wordpress.android.ui.engagement

enum class EngagementNavigationSource(private val sourceDescription: String) {
    LIKE_NOTIFICATION_LIST("like_notification_list"),
    LIKE_READER_LIST("like_reader_list");

    companion object {
        fun getSourceDescription(source: EngagementNavigationSource?): String {
            return source?.sourceDescription ?: "unknown"
        }
    }
}

enum class PreviewBlogByUrlSource(val sourceDescription: String) {
    LIKED_COMMENT_USER_HEADER("liked_comment_user_avatar"),
}
