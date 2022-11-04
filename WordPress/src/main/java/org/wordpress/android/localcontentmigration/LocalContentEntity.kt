package org.wordpress.android.localcontentmigration

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.models.ReaderPostList

enum class LocalContentEntity(private val isSiteContent: Boolean = false) {
    EligibilityStatus,
    AccessToken,
    UserFlags,
    ReaderPosts,
    Site,
    Post(isSiteContent = true),
    ;

    open val contentIdCapturePattern = when (isSiteContent) {
        true -> Regex("site/(\\d+)/${name}(?:/(\\d+))?")
        false -> Regex(name)
    }

    open fun getPathForContent(localSiteId: Int?, localEntityId: Int?) = when (this.isSiteContent) {
        true -> "site/${localSiteId}/${name}${ localEntityId?.let { "/${it}" } ?: "" }"
        false -> name
    }
}

sealed class LocalContentEntityData {
    data class EligibilityStatusData(
        val isEligible: Boolean,
        val siteCount: Int,
    ): LocalContentEntityData()

    data class AccessTokenData(val token: String): LocalContentEntityData()
    data class UserFlagsData(val flags: Map<String, Any?>): LocalContentEntityData()
    data class ReaderPostsData(val posts: ReaderPostList): LocalContentEntityData()
    data class SitesData(val localIds: List<Int>): LocalContentEntityData()
    data class PostsData(val localIds: List<Int>): LocalContentEntityData()
    data class PostData(val post: PostModel) : LocalContentEntityData()
}
