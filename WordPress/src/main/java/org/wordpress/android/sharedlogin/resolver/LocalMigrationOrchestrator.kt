package org.wordpress.android.sharedlogin.resolver

import kotlinx.coroutines.flow.MutableStateFlow
import org.wordpress.android.bloggingreminders.resolver.BloggingRemindersHelper
import org.wordpress.android.localcontentmigration.ContentMigrationAnalyticsTracker
import org.wordpress.android.localcontentmigration.ContentMigrationAnalyticsTracker.ErrorType.LocalDraftContent
import org.wordpress.android.localcontentmigration.EligibilityHelper
import org.wordpress.android.localcontentmigration.LocalContentEntityData.Companion.IneligibleReason.LocalDraftContentIsPresent
import org.wordpress.android.localcontentmigration.LocalContentEntityData.Companion.IneligibleReason.WPNotLoggedIn
import org.wordpress.android.localcontentmigration.LocalMigrationError
import org.wordpress.android.localcontentmigration.LocalMigrationError.FeatureDisabled
import org.wordpress.android.localcontentmigration.LocalMigrationError.Ineligibility
import org.wordpress.android.localcontentmigration.LocalMigrationError.MigrationAlreadyAttempted
import org.wordpress.android.localcontentmigration.LocalMigrationError.NoUserFlagsFoundError
import org.wordpress.android.localcontentmigration.LocalMigrationError.PersistenceError
import org.wordpress.android.localcontentmigration.LocalMigrationError.ProviderError
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Companion.EmptyResult
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Failure
import org.wordpress.android.localcontentmigration.LocalMigrationState
import org.wordpress.android.localcontentmigration.LocalMigrationState.Finished
import org.wordpress.android.localcontentmigration.LocalMigrationState.Finished.Ineligible
import org.wordpress.android.localcontentmigration.LocalMigrationState.Migrating
import org.wordpress.android.localcontentmigration.LocalPostsHelper
import org.wordpress.android.localcontentmigration.SharedLoginHelper
import org.wordpress.android.localcontentmigration.SitesMigrationHelper
import org.wordpress.android.localcontentmigration.WelcomeScreenData
import org.wordpress.android.localcontentmigration.emitTo
import org.wordpress.android.localcontentmigration.orElse
import org.wordpress.android.localcontentmigration.otherwise
import org.wordpress.android.localcontentmigration.then
import org.wordpress.android.reader.savedposts.resolver.ReaderSavedPostsHelper
import org.wordpress.android.sharedlogin.SharedLoginAnalyticsTracker
import org.wordpress.android.sharedlogin.SharedLoginAnalyticsTracker.ErrorType.WPNotLoggedInError
import org.wordpress.android.userflags.resolver.UserFlagsHelper
import javax.inject.Inject

class LocalMigrationOrchestrator @Inject constructor(
    private val sharedLoginAnalyticsTracker: SharedLoginAnalyticsTracker,
    private val migrationAnalyticsTracker: ContentMigrationAnalyticsTracker,
    private val userFlagsHelper: UserFlagsHelper,
    private val readerSavedPostsHelper: ReaderSavedPostsHelper,
    private val sharedLoginHelper: SharedLoginHelper,
    private val sitesMigrationHelper: SitesMigrationHelper,
    private val localPostsHelper: LocalPostsHelper,
    private val eligibilityHelper: EligibilityHelper,
    private val bloggingRemindersHelper: BloggingRemindersHelper,
) {
    fun tryLocalMigration(migrationStateFlow: MutableStateFlow<LocalMigrationState>) {
        eligibilityHelper.validate()
            .then(sharedLoginHelper::login).emitTo(migrationStateFlow)
            .then(sitesMigrationHelper::migrateSites).emitTo(migrationStateFlow)
            .then(userFlagsHelper::migrateUserFlags)
            .then(readerSavedPostsHelper::migrateReaderSavedPosts)
            .then(localPostsHelper::migratePosts)
            .then(bloggingRemindersHelper::migrateBloggingReminders)
            .orElse { error ->
                migrationStateFlow.value = when (error) {
                    is Ineligibility -> Ineligible
                    else -> Finished.Failure(error)
                }
                Failure(error)
            }
            .then {
                with(migrationStateFlow) {
                    value = Finished.Successful(
                        (value as? Migrating)?.data ?: WelcomeScreenData()
                    )
                }
                EmptyResult
            }
            .otherwise(::handleErrors)
    }

    @Suppress("ForbiddenComment")
    // TODO: Handle the errors appropriately
    private fun handleErrors(error: LocalMigrationError) {
        when (error) {
            is ProviderError -> Unit
            is Ineligibility -> when (error.reason) {
                WPNotLoggedIn -> sharedLoginAnalyticsTracker.trackLoginFailed(WPNotLoggedInError)
                LocalDraftContentIsPresent -> migrationAnalyticsTracker.trackContentMigrationFailed(LocalDraftContent)
            }
            is FeatureDisabled -> Unit
            is MigrationAlreadyAttempted -> Unit
            is PersistenceError -> Unit
            is NoUserFlagsFoundError -> Unit
        }
    }
}
