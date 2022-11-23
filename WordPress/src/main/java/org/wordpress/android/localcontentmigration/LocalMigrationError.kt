package org.wordpress.android.localcontentmigration

import org.wordpress.android.localcontentmigration.LocalContentEntityData.Companion.IneligibleReason

sealed class LocalMigrationError {
    sealed class ProviderError: LocalMigrationError() {
        data class NullValueFromQuery(val forEntity: LocalContentEntity): ProviderError()
        data class NullCursor(val forEntity: LocalContentEntity): ProviderError()
        data class ParsingException(val forEntity: LocalContentEntity): ProviderError()
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
        object FailedToSaveSites: PersistenceError()
        object FailedToSaveUserFlags: PersistenceError()
        object FailedToSaveReaderSavedPosts: PersistenceError()
    }
    object NoUserFlagsFoundError: LocalMigrationError()
}
