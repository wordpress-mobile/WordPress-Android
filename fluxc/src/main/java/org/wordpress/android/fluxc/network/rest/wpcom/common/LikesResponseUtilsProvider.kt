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
        siteid: Long,
        commentId: Long,
        likeType: LikeType
    ): LikeModel {
        return LikeModel().apply {
            remoteSiteId = siteid
            type = likeType.typeName
            remoteItemId = commentId
            remoteLikeId = response.ID
            likerLogin = response.login
            likerName = response.name
            likerAvatarUrl = response.avatar_URL
            likerSiteUrl = response.primary_blog
        }
    }
}
