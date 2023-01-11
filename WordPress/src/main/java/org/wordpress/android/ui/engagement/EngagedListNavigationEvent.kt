package org.wordpress.android.ui.engagement

import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource

sealed class EngagedListNavigationEvent(val closeUserProfileIfOpened: Boolean = true) {
    data class PreviewSiteByUrl(val siteUrl: String, val source: String) : EngagedListNavigationEvent()
    data class PreviewSiteById(val siteId: Long, val source: String) : EngagedListNavigationEvent()
    data class PreviewCommentInReader(
        val siteId: Long,
        val commentPostId: Long,
        val postOrCommentId: Long,
        val source: ThreadedCommentsActionSource
    ) : EngagedListNavigationEvent()

    data class PreviewPostInReader(val siteId: Long, val postId: Long) : EngagedListNavigationEvent()
    data class OpenUserProfileBottomSheet(
        val userProfile: UserProfile,
        val onClick: ((siteId: Long, siteUrl: String, source: String) -> Unit)? = null,
        val source: EngagementNavigationSource?
    ) : EngagedListNavigationEvent(false) {
        data class UserProfile(
            val userAvatarUrl: String,
            val blavatarUrl: String,
            val userName: String,
            val userLogin: String,
            val userBio: String,
            val siteTitle: String,
            val siteUrl: String,
            val siteId: Long
        )
    }
}
