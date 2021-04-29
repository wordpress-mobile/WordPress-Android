package org.wordpress.android.ui.engagement.utils

import org.wordpress.android.fluxc.model.LikeModel
import org.wordpress.android.fluxc.model.LikeModel.LikeType
import org.wordpress.android.ui.engagement.EngageItem
import org.wordpress.android.ui.engagement.EngageItem.Liker

fun getDefaultLikers(numLikers: Int, likedItemType: LikeType, siteId: Long, itemId: Long): List<LikeModel> {
    val likersList = mutableListOf<LikeModel>()

    for (i in 0 until numLikers) {
        likersList.add(LikeModel().apply {
            type = likedItemType.typeName
            remoteSiteId = siteId
            remoteItemId = itemId
            likerName = "liker name $i"
            likerLogin = "liker${i}login"
            likerSiteId = 10L + i * 100L
            likerSiteUrl = "https://liker$i-site.org"
            likerAvatarUrl = "https://liker$i-site.org/avatar.jpg"
            likerBio = "This is the bio for liker$i"
            likerId = 100L + i * 1000L
            preferredBlogId = 10L + i * 100L
            preferredBlogName = "Liker $i preferred blog name"
            preferredBlogUrl = "https://liker$i-preferred-blog.wordpress.com"
            preferredBlogBlavatarUrl = "https://liker$i-preferred-blog.wordpress.com/avatar.jpg"
        })
    }

    return likersList
}

fun List<LikeModel>.isEqualTo(engageItemList: List<EngageItem>): Boolean {
    val sameSize = this.size == engageItemList.size
    val likersList = engageItemList as? List<Liker>

    val allEquals = this.all { likeModel ->
        val liker = likersList!!.firstOrNull {
            it.userId == likeModel.likerId
        }

        liker != null && likeModel.isEqualTo(liker)
    }

    return sameSize && allEquals
}

private fun LikeModel.isEqualTo(liker: Liker): Boolean {
    return liker.name == this.likerName &&
            liker.login == this.likerLogin &&
            liker.userSiteId == this.likerSiteId &&
            liker.userSiteUrl == this.likerSiteUrl &&
            liker.userAvatarUrl == this.likerAvatarUrl &&
            liker.userBio == this.likerBio &&
            liker.userId == this.likerId &&
            liker.preferredBlogId == this.preferredBlogId &&
            liker.preferredBlogName == this.preferredBlogName &&
            liker.preferredBlogUrl == this.preferredBlogUrl &&
            liker.preferredBlogBlavatar == this.preferredBlogBlavatarUrl
}
