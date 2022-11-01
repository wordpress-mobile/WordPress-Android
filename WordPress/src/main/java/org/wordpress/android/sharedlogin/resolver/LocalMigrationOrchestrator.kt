package org.wordpress.android.sharedlogin.resolver

import android.content.Intent
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.localcontentmigration.LocalContentEntity.AccessToken
import org.wordpress.android.localcontentmigration.LocalContentEntityData.AccessTokenData
import org.wordpress.android.localcontentmigration.LocalMigrationContentResolver
import org.wordpress.android.reader.savedposts.resolver.ReaderSavedPostsResolver
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
) {
    fun tryLocalMigration() {
        val isFeatureFlagEnabled = jetpackSharedLoginFlag.isEnabled()
        if (!isFeatureFlagEnabled) {
            return
        }
        val isAlreadyLoggedIn = accountStore.hasAccessToken()
        val isFirstTry = appPrefsWrapper.getIsFirstTrySharedLoginJetpack()
        if (isAlreadyLoggedIn || !isFirstTry) {
            return
        }
        sharedLoginAnalyticsTracker.trackLoginStart()
        appPrefsWrapper.saveIsFirstTrySharedLoginJetpack(false)
        val (accessToken) = localMigrationContentResolver.getDataForEntityType<AccessTokenData>(AccessToken)
//        val accessTokenCursor = getAccessTokenCursor()
//        if (accessTokenCursor != null) {
//            val accessToken = queryResult.getValue<String>(accessTokenCursor) ?: ""
            if (accessToken.isNotEmpty()) {
                sharedLoginAnalyticsTracker.trackLoginSuccess()
                userFlagsResolver.tryGetUserFlags(
                        {
                            readerSavedPostsResolver.tryGetReaderSavedPosts(
                                    {
                                        localMigrationContentResolver.migrateLocalContent()
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
            } else {
                sharedLoginAnalyticsTracker.trackLoginFailed(ErrorType.WPNotLoggedInError)
            }
        // TODO: Unify error tracking for resolver / provider errors too
//        } else {
//            sharedLoginAnalyticsTracker.trackLoginFailed(ErrorType.QueryTokenError)
//        }
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
