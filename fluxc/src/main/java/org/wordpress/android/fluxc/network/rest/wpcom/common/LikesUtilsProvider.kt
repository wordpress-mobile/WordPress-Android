package org.wordpress.android.fluxc.network.rest.wpcom.common

import com.wellsql.generated.LikeModelTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.LikeModel
import org.wordpress.android.fluxc.model.LikeModel.LikeType
import org.wordpress.android.fluxc.network.rest.wpcom.common.LikeWPComRestResponse.LikesWPComRestResponse
import java.util.ArrayList
import java.util.HashMap
import javax.inject.Inject

class LikesUtilsProvider @Inject constructor() {
    fun getPageOffsetParams(type: LikeType, siteId: Long, remoteItemId: Long): Map<String, String?>? {
        val oldestDateLiked = WellSql.select(LikeModel::class.java)
                .columns(LikeModelTable.DATE_LIKED)
                .limit(1)
                .where().beginGroup()
                .equals(LikeModelTable.TYPE, type.typeName)
                .equals(LikeModelTable.REMOTE_SITE_ID, siteId)
                .equals(LikeModelTable.REMOTE_ITEM_ID, remoteItemId)
                .endGroup().endWhere()
                .orderBy(LikeModelTable.DATE_LIKED, SelectQuery.ORDER_ASCENDING)
                .asModel

        val params: MutableMap<String, String?> = HashMap()

        if (oldestDateLiked.size == 1) {
            params["before"] = oldestDateLiked[0].dateLiked

            val oldestLikers = WellSql.select(LikeModel::class.java)
                    .where()
                    .beginGroup()
                    .equals(LikeModelTable.TYPE, type.typeName)
                    .equals(LikeModelTable.REMOTE_SITE_ID, siteId)
                    .equals(LikeModelTable.REMOTE_ITEM_ID, remoteItemId)
                    .equals(LikeModelTable.DATE_LIKED, oldestDateLiked[0].dateLiked)
                    .endGroup()
                    .endWhere()
                    .asModel

            oldestLikers?.let {
                val excludeParams = oldestLikers.joinToString(separator = "&exclude[]=") { liker ->
                    "${liker.likerId}"
                }

                if (excludeParams.isNotBlank()) {
                    params["exclude[]"] = excludeParams
                }
            }
        }
        return params
    }

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
            likerId = response.ID
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
            dateLiked = response.date_liked
        }
    }
}
