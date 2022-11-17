package org.wordpress.android.sharedlogin.resolver

import android.content.Intent
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.localcontentmigration.LocalContentEntity.AccessToken
import org.wordpress.android.localcontentmigration.LocalContentEntityData.AccessTokenData
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.localcontentmigration.LocalContentEntity.EligibilityStatus
import org.wordpress.android.localcontentmigration.LocalContentEntity.Post
import org.wordpress.android.localcontentmigration.LocalContentEntity.Sites
import org.wordpress.android.localcontentmigration.LocalContentEntityData.EligibilityStatusData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.PostData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.PostsData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.SitesData
import org.wordpress.android.localcontentmigration.LocalMigrationContentResolver
import org.wordpress.android.localcontentmigration.LocalMigrationError
import org.wordpress.android.localcontentmigration.LocalMigrationError.Ineligibility
import org.wordpress.android.localcontentmigration.LocalMigrationError.ProviderError
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Success
import org.wordpress.android.localcontentmigration.otherwise
import org.wordpress.android.localcontentmigration.then
import org.wordpress.android.localcontentmigration.validate
import org.wordpress.android.reader.savedposts.resolver.ReaderSavedPostsResolver
import org.wordpress.android.resolver.ResolverUtility
import org.wordpress.android.sharedlogin.JetpackSharedLoginFlag
import org.wordpress.android.sharedlogin.SharedLoginAnalyticsTracker
import org.wordpress.android.sharedlogin.SharedLoginAnalyticsTracker.ErrorType
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.userflags.resolver.UserFlagsResolver
import org.wordpress.android.util.AccountActionBuilderWrapper
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class LocalMigrationOrchestrator @Inject constructor(
    private val jetpackSharedLoginFlag: JetpackSharedLoginFlag,
    private val contextProvider: ContextProvider,
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
    private val accountActionBuilderWrapper: AccountActionBuilderWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val sharedLoginAnalyticsTracker: SharedLoginAnalyticsTracker,
    private val userFlagsResolver: UserFlagsResolver,
    private val readerSavedPostsResolver: ReaderSavedPostsResolver,
    private val localMigrationContentResolver: LocalMigrationContentResolver,
    private val resolverUtility: ResolverUtility,
    private val siteStore: SiteStore,
) {
    fun tryLocalMigration() {
        localMigrationContentResolver.getResultForEntityType<EligibilityStatusData>(EligibilityStatus).validate()
                .then {
                    originalTryLocalMigration()
                    Success(it)
                }.otherwise(::handleErrors)
    }

    @Suppress("ForbiddenComment")
    // TODO: Handle the errors appropriately
    private fun handleErrors(error: LocalMigrationError) {
        when(error) {
            is ProviderError -> Unit
            is Ineligibility -> Unit
        }
    }
    private fun originalTryLocalMigration() {
        val isFeatureFlagEnabled = jetpackSharedLoginFlag.isEnabled()

        if (!isFeatureFlagEnabled) {
            return
        }

        val hasSelfHostedSites = siteStore.hasSite() && siteStore.sites.any { !it.isUsingWpComRestApi }
        val isAlreadyLoggedIn = accountStore.hasAccessToken() || hasSelfHostedSites
        val isFirstTry = appPrefsWrapper.getIsFirstTrySharedLoginJetpack()

        if (isAlreadyLoggedIn || !isFirstTry) {
            return
        }

        sharedLoginAnalyticsTracker.trackLoginStart()
        appPrefsWrapper.saveIsFirstTrySharedLoginJetpack(false)
        val (accessToken) = localMigrationContentResolver.getDataForEntityType<AccessTokenData>(AccessToken)
        val (sites) = localMigrationContentResolver.getDataForEntityType<SitesData >(Sites)
        val hasLocalSelfHostedSites = sites.any { !it.isUsingWpComRestApi }
        @Suppress("ForbiddenComment")
        // TODO: Unify error tracking for resolver / provider errors too
        if (accessToken.isNotEmpty() || hasLocalSelfHostedSites) {
            runFlow(accessToken, sites)
        } else {
            sharedLoginAnalyticsTracker.trackLoginFailed(ErrorType.WPNotLoggedInError)
        }
    }

    private fun runFlow(accessToken: String, sites: List<SiteModel>) {
        val hasSites = sites.isNotEmpty()

        if (hasSites) {
            resolverUtility.copySitesWithIndexes(sites)
        }

        sharedLoginAnalyticsTracker.trackLoginSuccess()
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
