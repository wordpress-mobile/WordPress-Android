package org.wordpress.android.ui.engagement

import org.wordpress.android.ui.engagement.EngageItem.EngageItemType.LIKED_ITEM
import org.wordpress.android.ui.engagement.EngageItem.EngageItemType.LIKER
import org.wordpress.android.ui.engagement.EngageItem.EngageItemType.NEXT_LIKES_PAGE_LOADER
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.OpenUserProfileBottomSheet.UserProfile

sealed class EngageItem(val type: EngageItemType) {
    data class LikedItem(
        val author: AuthorName, // author of liked post or comment
        val postOrCommentText: CharSequence, // liked post title or comment excerpt
        val authorAvatarUrl: String, // author avatar url

        val likedItemId: Long, // liked post or comment remote id
        val likedItemSiteId: Long, // site id to which the liked post or comment belongs
        val likedItemSiteUrl: String, // used for comments only; site id to which the liked comment belongs
        val likedItemPostId: Long, // used for comments only; the post to which the comment belongs

        val authorUserId: Long, // author user id

        val authorPreferredSiteId: Long, // author preferred site id
        val authorPreferredSiteUrl: String, // author preferred site url

        val onGravatarClick: (siteId: Long, siteUrl: String, source: String) -> Unit,
        val blogPreviewSource: String,

        val onHeaderClicked: (siteId: Long, siteUrl: String, postOrCommentId: Long, commentPostId: Long) -> Unit
    ) : EngageItem(LIKED_ITEM)

    data class Liker(
        val name: String,
        val login: String,
        val userSiteId: Long,
        val userSiteUrl: String,
        val userAvatarUrl: String,
        val userBio: String,
        val userId: Long,
        val preferredBlogId: Long,
        val preferredBlogName: String,
        val preferredBlogUrl: String,
        val preferredBlogBlavatar: String,
        val onClick: ((userProfile: UserProfile, source: EngagementNavigationSource?) -> Unit)? = null,
        val source: EngagementNavigationSource?
    ) : EngageItem(LIKER)

    data class NextLikesPageLoader(val isLoading: Boolean, val action: () -> Unit) : EngageItem(NEXT_LIKES_PAGE_LOADER)

    enum class EngageItemType {
        LIKED_ITEM,
        LIKER,
        NEXT_LIKES_PAGE_LOADER
    }
}
