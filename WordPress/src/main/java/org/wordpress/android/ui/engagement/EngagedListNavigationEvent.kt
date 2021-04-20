package org.wordpress.android.ui.engagement

sealed class EngagedListNavigationEvent {
    data class PreviewSiteByUrl(val siteUrl: String) : EngagedListNavigationEvent()
    data class PreviewSiteById(val siteId: Long) : EngagedListNavigationEvent()
    data class PreviewCommentInReader(
        val siteId: Long,
        val commentPostId: Long,
        val postOrCommentId: Long
    ) : EngagedListNavigationEvent()
    data class PreviewPostInReader(val siteId: Long, val postId: Long) : EngagedListNavigationEvent()
}
