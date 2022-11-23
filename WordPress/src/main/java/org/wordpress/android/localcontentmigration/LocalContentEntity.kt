package org.wordpress.android.localcontentmigration

import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.QuickStartStatusModel
import org.wordpress.android.fluxc.model.QuickStartTaskModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.ReaderPostList

enum class LocalContentEntity(private val isIdentifiable: Boolean = false) {
    EligibilityStatus,
    AccessToken,
    UserFlags,
    ReaderPosts,
    BloggingReminders,
    Sites,
    Post(isIdentifiable = true),
    ;

    open val contentIdCapturePattern = when (isIdentifiable) {
        true -> Regex("${name}(?:/(\\d+))?")
        false -> Regex(name)
    }

    open fun getPathForContent(localEntityId: Int?) = when (this.isIdentifiable) {
        true -> "${name}${ localEntityId?.let { "/${it}" } ?: "" }"
        false -> name
    }
}

sealed class LocalContentEntityData {
    data class EligibilityStatusData(val isEligible: Boolean, val reason: IneligibleReason?): LocalContentEntityData()
    data class AccessTokenData(val token: String): LocalContentEntityData()
    data class UserFlagsData(
        val flags: Map<String, Any?>,
        val quickStartTaskList: List<QuickStartTaskModel>,
        val quickStartStatusList: List<QuickStartStatusModel>,
    ): LocalContentEntityData()
    data class ReaderPostsData(val posts: ReaderPostList): LocalContentEntityData()
    data class BloggingRemindersData(val reminders: List<BloggingRemindersModel>): LocalContentEntityData()
    data class SitesData(val sites: List<SiteModel>): LocalContentEntityData()
    data class PostsData(val localIds: List<Int>): LocalContentEntityData()
    data class PostData(val post: PostModel) : LocalContentEntityData()
    object EmptyData: LocalContentEntityData()
    companion object {
        enum class IneligibleReason {
            WPNotLoggedIn,
            ;
        }
    }
}
