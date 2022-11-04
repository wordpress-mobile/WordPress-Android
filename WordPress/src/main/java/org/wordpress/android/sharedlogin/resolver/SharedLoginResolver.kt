package org.wordpress.android.sharedlogin.resolver

import android.content.Intent
import android.database.Cursor
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.localcontentmigration.LocalMigrationContentResolver
import org.wordpress.android.provider.query.QueryResult
import org.wordpress.android.reader.savedposts.resolver.ReaderSavedPostsResolver
import org.wordpress.android.resolver.ContentResolverWrapper
import org.wordpress.android.resolver.ResolverUtility
import org.wordpress.android.sharedlogin.JetpackSharedLoginFlag
import org.wordpress.android.sharedlogin.SharedLoginAnalyticsTracker
import org.wordpress.android.sharedlogin.SharedLoginAnalyticsTracker.ErrorType
import org.wordpress.android.sharedlogin.SharedLoginData
import org.wordpress.android.sharedlogin.provider.SharedLoginProvider
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.userflags.resolver.UserFlagsResolver
import org.wordpress.android.util.AccountActionBuilderWrapper
import org.wordpress.android.util.publicdata.WordPressPublicData
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class SharedLoginResolver @Inject constructor(
    private val jetpackSharedLoginFlag: JetpackSharedLoginFlag,
    private val contextProvider: ContextProvider,
    private val wordPressPublicData: WordPressPublicData,
    private val dispatcher: Dispatcher,
    private val queryResult: QueryResult,
    private val accountStore: AccountStore,
    private val contentResolverWrapper: ContentResolverWrapper,
    private val accountActionBuilderWrapper: AccountActionBuilderWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val sharedLoginAnalyticsTracker: SharedLoginAnalyticsTracker,
    private val userFlagsResolver: UserFlagsResolver,
    private val readerSavedPostsResolver: ReaderSavedPostsResolver,
    private val localMigrationContentResolver: LocalMigrationContentResolver,
    private val resolverUtility: ResolverUtility
) {
    fun tryJetpackLogin() {
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
        val loginDataCursor = getLoginDataCursor()
        if (loginDataCursor != null) {
            val loginData = queryResult.getValue<SharedLoginData>(loginDataCursor)

            if (loginData != null) {
                val accessToken = loginData.token ?: ""
                val sites = loginData.sites ?: listOf()
                val selfHostedSites = loginData.sites?.filter { site -> !site.isUsingWpComRestApi } ?: listOf()

                if (accessToken.isNotEmpty() || selfHostedSites.isNotEmpty()) {
                    runFlow(accessToken, sites)
                } else {
                    sharedLoginAnalyticsTracker.trackLoginFailed(ErrorType.WPNotLoggedInError)
                }
            } else {
                sharedLoginAnalyticsTracker.trackLoginFailed(ErrorType.NullLoginDataError)
            }
        } else {
            sharedLoginAnalyticsTracker.trackLoginFailed(ErrorType.QueryLoginDataError)
        }
    }

    private fun getLoginDataCursor(): Cursor? {
        val wordpressAccessTokenUriValue =
                "content://${wordPressPublicData.currentPackageId()}.${SharedLoginProvider::class.simpleName}"
        return contentResolverWrapper.queryUri(
                contextProvider.getContext().contentResolver,
                wordpressAccessTokenUriValue
        )
    }

    @Suppress("SwallowedException")
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
