package org.wordpress.android.ui.engagement

sealed class EngagedListNavigationEvent(val closeUserProfileIfOpened: Boolean = true) {
    data class PreviewSiteByUrl(val siteUrl: String) : EngagedListNavigationEvent()
    data class PreviewSiteById(val siteId: Long) : EngagedListNavigationEvent()
    data class PreviewCommentInReader(
        val siteId: Long,
        val commentPostId: Long,
        val postOrCommentId: Long
    ) : EngagedListNavigationEvent()
    data class PreviewPostInReader(val siteId: Long, val postId: Long) : EngagedListNavigationEvent()
    data class OpenUserProfileBottomSheet(
        val userProfile: UserProfile,
        val onClick: ((siteId: Long, siteUrl: String) -> Unit)? = null
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
