package org.wordpress.android.localcontentmigration

import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.QuickStartStatusModel
import org.wordpress.android.fluxc.model.QuickStartTaskModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.localcontentmigration.EligibilityState.Ineligible
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Failure
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Success
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

sealed class EligibilityState {
    object Eligible: EligibilityState()
    sealed class Ineligible: EligibilityState() {
        object WPNotLoggedIn: Ineligible()
    }
}

sealed class LocalContentEntityData {
    data class EligibilityStatusData(val eligibilityState: EligibilityState): LocalContentEntityData()
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
}

sealed class LocalMigrationError {
    sealed class ProviderError: LocalMigrationError() {
        data class NullValueFromQuery(val forEntity: LocalContentEntity): ProviderError()
        data class NullCursor(val forEntity: LocalContentEntity): ProviderError()
        data class ParsingException(val forEntity: LocalContentEntity): ProviderError()
    }
    data class Ineligibility(val reason: Ineligible): LocalMigrationError()
    sealed class FeatureDisabled: LocalMigrationError() {
        object SharedLoginDisabled: FeatureDisabled()
    }
    sealed class MigrationAlreadyAttempted: LocalMigrationError() {
        object SharedLoginAlreadyAttempted: MigrationAlreadyAttempted()
    }
}

sealed class LocalMigrationResult<out T: LocalContentEntityData, out E: LocalMigrationError> {
    data class Success<T: LocalContentEntityData>(val value: T): LocalMigrationResult<T, Nothing>()
    data class Failure<E: LocalMigrationError>(val error: E): LocalMigrationResult<Nothing, E>()
}

fun <T: LocalContentEntityData, U: LocalContentEntityData, E: LocalMigrationError> LocalMigrationResult<T, E>
        .thenWith(next: (T) -> LocalMigrationResult<U, E>) = when (this) {
    is Success -> next(this.value)
    is Failure -> this
}

fun <T: LocalContentEntityData> LocalMigrationResult<LocalContentEntityData, LocalMigrationError>
        .then(next: () -> LocalMigrationResult<T, LocalMigrationError>) = when (this) {
    is Success -> next()
    is Failure -> this
}

fun <T: LocalContentEntityData, E: LocalMigrationError> LocalMigrationResult<T, E>
        .otherwise(handleError: (E) -> Unit) = when (this) {
    is Success -> Unit
    is Failure -> handleError(this.error)
}
