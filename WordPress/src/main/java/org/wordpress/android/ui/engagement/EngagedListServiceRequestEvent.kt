package org.wordpress.android.ui.engagement

sealed class EngagedListServiceRequestEvent {
    data class RequestBlogPost(val siteId: Long, val postId: Long) : EngagedListServiceRequestEvent()
    data class RequestComment(
        val siteId: Long,
        val postId: Long,
        val commentId: Long
    ) : EngagedListServiceRequestEvent()
}
