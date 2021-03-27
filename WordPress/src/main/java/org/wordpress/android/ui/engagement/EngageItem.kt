package org.wordpress.android.ui.engagement

import org.wordpress.android.ui.engagement.EngageItem.EngageItemType.LIKED_ITEM
import org.wordpress.android.ui.engagement.EngageItem.EngageItemType.LIKER

sealed class EngageItem(val type: EngageItemType) {
    data class LikedItem(
        val author: UserName,
        val itemText: CharSequence,
        val userAvatarUrl: String,
        val likedItemSiteId: Long,
        val userSiteId: Long,
        val likedItemId: Long,
        val userId: Long,
        val siteUrl: String,
        val onGravatarClick: (siteId: Long, siteUrl: String) -> Unit,
        val onHeaderClicked: (siteId: Long, postId: Long) -> Unit
    ) : EngageItem(LIKED_ITEM)

    data class Liker(
        val name: String,
        val login: String,
        val userSiteId: Long,
        val userSiteUrl: String,
        val userAvatarUrl: String,
        val remoteId: Long,
        val onClick: (siteId: Long, siteUrl: String) -> Unit,
    ) : EngageItem(LIKER)

    enum class EngageItemType {
        LIKED_ITEM,
        LIKER
    }
}
