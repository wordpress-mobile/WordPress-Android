package org.wordpress.android.localcontentmigration

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.localcontentmigration.LocalContentEntityData.Companion.IneligibleReason

sealed class LocalMigrationError {
    sealed class ProviderError: LocalMigrationError() {
        data class NullValueFromQuery(val forEntity: LocalContentEntity): ProviderError()
        data class NullCursor(val forEntity: LocalContentEntity): ProviderError()
        data class ParsingException(
            val forEntity: LocalContentEntity,
            val throwable: Throwable,
        ): ProviderError()
    }
    data class Ineligibility(val reason: IneligibleReason): LocalMigrationError()
    sealed class FeatureDisabled: LocalMigrationError() {
        object SharedLoginDisabled: FeatureDisabled()
        object UserFlagsDisabled: FeatureDisabled()
        object ReaderSavedPostsDisabled: FeatureDisabled()
    }
    sealed class MigrationAlreadyAttempted: LocalMigrationError() {
        object SharedLoginAlreadyAttempted: MigrationAlreadyAttempted()
        object UserFlagsAlreadyAttempted: MigrationAlreadyAttempted()
        object ReaderSavedPostsAlreadyAttempted: MigrationAlreadyAttempted()
    }
    sealed class PersistenceError: LocalMigrationError() {
        data class FailedToSaveSites(val throwable: Throwable): PersistenceError()
        object FailedToSaveUserFlags: PersistenceError()
        data class FailedToSaveUserFlagsWithException(val throwable: Throwable): PersistenceError()
        object FailedToSaveReaderSavedPosts: PersistenceError()
        sealed class LocalPostsPersistenceError: PersistenceError() {
            data class FailedToResetSequenceForPosts(val throwable: Throwable): LocalPostsPersistenceError()
            data class FailedToInsertLocalPost(val post: PostModel): LocalPostsPersistenceError()
            data class FailedToInsertLocalPostWithException(
                val post: PostModel,
                val throwable: Throwable,
            ): LocalPostsPersistenceError()
        }
    }
    object NoUserFlagsFoundError: LocalMigrationError()
}
