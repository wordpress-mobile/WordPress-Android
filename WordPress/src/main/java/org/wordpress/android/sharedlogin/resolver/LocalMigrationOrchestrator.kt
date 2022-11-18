package org.wordpress.android.sharedlogin.resolver

import android.content.Intent
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.localcontentmigration.EligibilityHelper
import org.wordpress.android.localcontentmigration.LocalContentEntityData.Companion.IneligibleReason.WPNotLoggedIn
import org.wordpress.android.localcontentmigration.LocalMigrationError
import org.wordpress.android.localcontentmigration.LocalMigrationError.FeatureDisabled
import org.wordpress.android.localcontentmigration.LocalMigrationError.Ineligibility
import org.wordpress.android.localcontentmigration.LocalMigrationError.MigrationAlreadyAttempted
import org.wordpress.android.localcontentmigration.LocalMigrationError.NoUserFlagsFoundError
import org.wordpress.android.localcontentmigration.LocalMigrationError.PersistenceError
import org.wordpress.android.localcontentmigration.LocalMigrationError.ProviderError
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Success
import org.wordpress.android.localcontentmigration.LocalPostsHelper
import org.wordpress.android.localcontentmigration.SharedLoginHelper
import org.wordpress.android.localcontentmigration.SitesMigrationHelper
import org.wordpress.android.localcontentmigration.otherwise
import org.wordpress.android.localcontentmigration.then
import org.wordpress.android.localcontentmigration.thenWith
import org.wordpress.android.reader.savedposts.resolver.ReaderSavedPostsHelper
import org.wordpress.android.sharedlogin.SharedLoginAnalyticsTracker
import org.wordpress.android.sharedlogin.SharedLoginAnalyticsTracker.ErrorType
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.userflags.resolver.UserFlagsHelper
import org.wordpress.android.util.AccountActionBuilderWrapper
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class LocalMigrationOrchestrator @Inject constructor(
    private val contextProvider: ContextProvider,
    private val dispatcher: Dispatcher,
    private val accountActionBuilderWrapper: AccountActionBuilderWrapper,
    private val sharedLoginAnalyticsTracker: SharedLoginAnalyticsTracker,
    private val userFlagsHelper: UserFlagsHelper,
    private val readerSavedPostsHelper: ReaderSavedPostsHelper,
    private val sharedLoginHelper: SharedLoginHelper,
    private val sitesMigrationHelper: SitesMigrationHelper,
    private val localPostsHelper: LocalPostsHelper,
        private val eligibilityHelper: EligibilityHelper,
) {
    fun tryLocalMigration() {
        eligibilityHelper.validate()
                .then(sitesMigrationHelper::migrateSites)
                .then(localPostsHelper::migratePosts)
                .then(userFlagsHelper::migrateUserFlags)
                .then(readerSavedPostsHelper::migrateReaderSavedPosts)
                .then(sharedLoginHelper::login)
                .thenWith {
                    originalTryLocalMigration(it.token)
                    Success(it)
                }
                .otherwise(::handleErrors)
    }

    @Suppress("ForbiddenComment")
    // TODO: Handle the errors appropriately
    private fun handleErrors(error: LocalMigrationError) {
        when(error) {
            is ProviderError -> Unit
            is Ineligibility -> when (error.reason) {
                WPNotLoggedIn -> sharedLoginAnalyticsTracker.trackLoginFailed(ErrorType.WPNotLoggedInError)
            }
            is FeatureDisabled -> Unit
            is MigrationAlreadyAttempted -> Unit
            is PersistenceError -> Unit
            is NoUserFlagsFoundError -> Unit
        }
    }
    private fun originalTryLocalMigration(accessToken: String) {
        dispatchUpdateAccessToken(accessToken)
        reloadMainScreen()
    }

    private fun dispatchUpdateAccessToken(accessToken: String) {
        dispatcher.dispatch(
                accountActionBuilderWrapper.newUpdateAccessTokenAction(accessToken)
        )
    }

    private fun reloadMainScreen() {
        contextProvider.getContext().run {
            val mainActivityIntent = Intent(this, WPMainActivity::class.java)
            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(mainActivityIntent)
        }
    }
}
