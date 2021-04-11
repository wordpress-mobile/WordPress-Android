package org.wordpress.android.fluxc.network.rest.wpcom.common

import org.wordpress.android.fluxc.model.LikeModel
import org.wordpress.android.fluxc.model.LikeModel.LikeType
import org.wordpress.android.fluxc.network.rest.wpcom.common.LikeWPComRestResponse.LikesWPComRestResponse
import java.util.ArrayList
import javax.inject.Inject

class LikesResponseUtilsProvider @Inject constructor() {
    fun likesResponseToLikeList(
        response: LikesWPComRestResponse,
        siteId: Long,
        commentId: Long,
        likeType: LikeType
    ): List<LikeModel> {
        val likes: MutableList<LikeModel> = ArrayList()
        response.likes?.let { likesItems ->
            for (restLike in likesItems) {
                likes.add(likeResponseToLike(restLike, siteId, commentId, likeType))
            }
        }
        return likes
    }

    private fun likeResponseToLike(
        response: LikeWPComRestResponse,
        siteId: Long,
        commentId: Long,
        likeType: LikeType
    ): LikeModel {
        return LikeModel().apply {
            remoteSiteId = siteId
            type = likeType.typeName
            remoteItemId = commentId
            remoteLikeId = response.ID
            likerLogin = response.login
            likerName = response.name
            likerAvatarUrl = response.avatar_URL
            likerBio = response.bio
            likerSiteId = response.site_ID
            likerSiteUrl = response.primary_blog
            preferredBlogId = response.preferred_blog?.id ?: 0
            preferredBlogName = response.preferred_blog?.name
            preferredBlogUrl = response.preferred_blog?.url
            preferredBlogBlavatarUrl = response.preferred_blog?.icon?.img
        }
    }
}
