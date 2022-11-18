package org.wordpress.android.sharedlogin.resolver

import android.content.Intent
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.localcontentmigration.EligibilityState.Ineligible.WPNotLoggedIn
import org.wordpress.android.localcontentmigration.LocalContentEntity.EligibilityStatus
import org.wordpress.android.localcontentmigration.LocalContentEntity.Post
import org.wordpress.android.localcontentmigration.LocalContentEntityData.EligibilityStatusData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.PostData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.PostsData
import org.wordpress.android.localcontentmigration.LocalMigrationContentResolver
import org.wordpress.android.localcontentmigration.LocalMigrationError
import org.wordpress.android.localcontentmigration.LocalMigrationError.FeatureDisabled
import org.wordpress.android.localcontentmigration.LocalMigrationError.Ineligibility
import org.wordpress.android.localcontentmigration.LocalMigrationError.MigrationAlreadyAttempted
import org.wordpress.android.localcontentmigration.LocalMigrationError.ProviderError
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Success
import org.wordpress.android.localcontentmigration.SharedLoginHelper
import org.wordpress.android.localcontentmigration.otherwise
import org.wordpress.android.localcontentmigration.then
import org.wordpress.android.localcontentmigration.thenWith
import org.wordpress.android.localcontentmigration.validate
import org.wordpress.android.reader.savedposts.resolver.ReaderSavedPostsResolver
import org.wordpress.android.sharedlogin.SharedLoginAnalyticsTracker
import org.wordpress.android.sharedlogin.SharedLoginAnalyticsTracker.ErrorType
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.userflags.resolver.UserFlagsResolver
import org.wordpress.android.util.AccountActionBuilderWrapper
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class LocalMigrationOrchestrator @Inject constructor(
    private val contextProvider: ContextProvider,
    private val dispatcher: Dispatcher,
    private val accountActionBuilderWrapper: AccountActionBuilderWrapper,
    private val sharedLoginAnalyticsTracker: SharedLoginAnalyticsTracker,
    private val userFlagsResolver: UserFlagsResolver,
    private val readerSavedPostsResolver: ReaderSavedPostsResolver,
    private val localMigrationContentResolver: LocalMigrationContentResolver,
    private val sharedLoginHelper: SharedLoginHelper,
) {
    fun tryLocalMigration() {
        localMigrationContentResolver.getResultForEntityType<EligibilityStatusData>(EligibilityStatus).validate()
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
                is WPNotLoggedIn -> sharedLoginAnalyticsTracker.trackLoginFailed(ErrorType.WPNotLoggedInError)
            }
            is FeatureDisabled -> Unit
            is MigrationAlreadyAttempted -> Unit
        }
    }
    private fun originalTryLocalMigration(accessToken: String) {
        @Suppress("ForbiddenComment")
        // TODO: Extract sites migration to helper
//        val hasSites = sites.isNotEmpty()
//
//        if (hasSites) {
//            resolverUtility.copySitesWithIndexes(sites)
//        }
        userFlagsResolver.tryGetUserFlags(
                {
                    readerSavedPostsResolver.tryGetReaderSavedPosts(
                            {
                                migrateLocalContent()
                                dispatchUpdateAccessToken(accessToken)
                                reloadMainScreen()
                            },
                            {
                                reloadMainScreen()
                            }
                    )
                },
                {
                    reloadMainScreen()
                }
        )
    }

    fun migrateLocalContent() {
        val posts: PostsData = localMigrationContentResolver.getDataForEntityType(Post)
        for (localPostId in posts.localIds) {
            val postData: PostData = localMigrationContentResolver.getDataForEntityType(Post, localPostId)
            dispatcher.dispatch(PostActionBuilder.newUpdatePostAction(postData.post))
        }
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
