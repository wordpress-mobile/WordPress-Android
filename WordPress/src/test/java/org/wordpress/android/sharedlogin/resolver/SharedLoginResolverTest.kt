package org.wordpress.android.sharedlogin.resolver

import android.content.ContentResolver
import android.content.Context
import android.database.MatrixCursor
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.UpdateTokenPayload
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.localcontentmigration.ContentMigrationAnalyticsTracker
import org.wordpress.android.localcontentmigration.EligibilityHelper
import org.wordpress.android.localcontentmigration.LocalMigrationContentProvider
import org.wordpress.android.localcontentmigration.LocalMigrationState
import org.wordpress.android.localcontentmigration.LocalPostsHelper
import org.wordpress.android.reader.savedposts.resolver.ReaderSavedPostsHelper
import org.wordpress.android.localcontentmigration.SharedLoginHelper
import org.wordpress.android.localcontentmigration.SitesMigrationHelper
import org.wordpress.android.resolver.ContentResolverWrapper
import org.wordpress.android.sharedlogin.JetpackSharedLoginFlag
import org.wordpress.android.sharedlogin.SharedLoginAnalyticsTracker
import org.wordpress.android.sharedlogin.SharedLoginAnalyticsTracker.ErrorType
import org.wordpress.android.sharedlogin.SharedLoginData
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.userflags.resolver.UserFlagsHelper
import org.wordpress.android.util.AccountActionBuilderWrapper
import org.wordpress.android.util.publicdata.WordPressPublicData
import org.wordpress.android.viewmodel.ContextProvider

@Suppress("ForbiddenComment")
// TODO: adapt these tests to the unified provider / orchestrator approach
@Ignore("Disabled for now: will refactor in another PR after unification.")
class SharedLoginResolverTest : BaseUnitTest() {
    private lateinit var onSuccessFlagsCaptor: KArgumentCaptor<() -> Unit>
    private lateinit var onSuccessReaderPostsCaptor: KArgumentCaptor<() -> Unit>

    private val jetpackSharedLoginFlag: JetpackSharedLoginFlag = mock()
    private val contextProvider: ContextProvider = mock()
    private val wordPressPublicData: WordPressPublicData = mock()
    private val dispatcher: Dispatcher = mock()
    private val accountStore: AccountStore = mock()
    private val contentResolverWrapper: ContentResolverWrapper = mock()
    private val accountActionBuilderWrapper: AccountActionBuilderWrapper = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val sharedLoginAnalyticsTracker: SharedLoginAnalyticsTracker = mock()
    private val migrationAnalyticsTracker: ContentMigrationAnalyticsTracker = mock()
    private val userFlagsResolver: UserFlagsHelper = mock()
    private val readerSavedPostsResolver: ReaderSavedPostsHelper = mock()
    private val siteStore: SiteStore = mock()
    private val sharedLoginHelper: SharedLoginHelper = mock()
    private val sitesMigrationHelper: SitesMigrationHelper = mock()
    private val postsHelper: LocalPostsHelper = mock()
    private val eligibilityHelper: EligibilityHelper = mock()
    private val migrationStateFlow: MutableStateFlow<LocalMigrationState> = mock()

    private val classToTest = LocalMigrationOrchestrator(
            sharedLoginAnalyticsTracker,
            migrationAnalyticsTracker,
            userFlagsResolver,
            readerSavedPostsResolver,
            sharedLoginHelper,
            sitesMigrationHelper,
            postsHelper,
            eligibilityHelper
    )

    private val sharedDataLoggedInNoSites = SharedLoginData(
            token = "valid",
            sites = listOf()
    )
    private val notLoggedInToken = ""
    private val wordPressCurrentPackageId = "packageId"
    private val uriValue = "content://$wordPressCurrentPackageId.${LocalMigrationContentProvider::class.simpleName}"
    private val context: Context = mock()
    private val contentResolver: ContentResolver = mock()
    private val updateTokenAction: Action<UpdateTokenPayload> = mock()
    private val mockCursor: MatrixCursor = mock()

    @Before
    fun setup() {
        whenever(contextProvider.getContext()).thenReturn(context)
        whenever(context.contentResolver).thenReturn(contentResolver)
        whenever(wordPressPublicData.currentPackageId()).thenReturn(wordPressCurrentPackageId)
        val notLoggedInData = SharedLoginData(
            token = notLoggedInToken,
            sites = listOf()
        )
        whenever(mockCursor.getString(0)).thenReturn(Gson().toJson(notLoggedInData))

        whenever(accountActionBuilderWrapper.newUpdateAccessTokenAction(
                sharedDataLoggedInNoSites.token!!
        )).thenReturn(updateTokenAction)
        whenever(contentResolverWrapper.queryUri(contentResolver, uriValue)).thenReturn(mockCursor)
    }

    @Test
    fun `Should NOT query ContentResolver if feature flag is DISABLED`() {
        whenever(jetpackSharedLoginFlag.isEnabled()).thenReturn(false)
        classToTest.tryLocalMigration(migrationStateFlow)
        verify(contentResolverWrapper, never()).queryUri(contentResolver, uriValue)
    }

    @Test
    fun `Should NOT query ContentResolver if IS already logged in`() {
        whenever(appPrefsWrapper.getIsFirstTrySharedLoginJetpack()).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(jetpackSharedLoginFlag.isEnabled()).thenReturn(true)
        classToTest.tryLocalMigration(migrationStateFlow)
        verify(contentResolverWrapper, never()).queryUri(contentResolver, uriValue)
    }

    @Test
    fun `Should NOT query ContentResolver if a selfhosted site is already configured`() {
        whenever(appPrefsWrapper.getIsFirstTrySharedLoginJetpack()).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(jetpackSharedLoginFlag.isEnabled()).thenReturn(true)
        whenever(siteStore.hasSite()).thenReturn(true)
        whenever(siteStore.sites).thenReturn(listOf(SiteModel()))
        classToTest.tryLocalMigration(migrationStateFlow)
        verify(contentResolverWrapper, never()).queryUri(contentResolver, uriValue)
    }

    @Test
    fun `Should NOT query ContentResolver if IS NOT the first try`() {
        whenever(appPrefsWrapper.getIsFirstTrySharedLoginJetpack()).thenReturn(false)
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(jetpackSharedLoginFlag.isEnabled()).thenReturn(true)
        classToTest.tryLocalMigration(migrationStateFlow)
        verify(contentResolverWrapper, never()).queryUri(contentResolver, uriValue)
    }

    @Test
    fun `Should query ContentResolver if NOT already logged in, feature flag is ENABLED and IS first try`() {
        featureEnabled()
        classToTest.tryLocalMigration(migrationStateFlow)
        verify(contentResolverWrapper).queryUri(contentResolver, uriValue)
    }

    @Test
    fun `Should dispatch UpdateTokenPayload if access token is NOT empty, flags and saved posts are migrated`() {
        featureEnabled()
        onSuccessFlagsCaptor = argumentCaptor()
        onSuccessReaderPostsCaptor = argumentCaptor()

        whenever(mockCursor.getString(0)).thenReturn(Gson().toJson(sharedDataLoggedInNoSites))
//        whenever(userFlagsResolver.tryGetUserFlags(
//                onSuccessFlagsCaptor.capture(),
//                any()
//        )).doAnswer { onSuccessFlagsCaptor.firstValue.invoke() }
//        whenever(readerSavedPostsResolver.tryGetReaderSavedPosts(
//                onSuccessReaderPostsCaptor.capture(),
//                any()
//        )).doAnswer { onSuccessReaderPostsCaptor.firstValue.invoke() }

        classToTest.tryLocalMigration(migrationStateFlow)

        verify(dispatcher).dispatch(updateTokenAction)
    }

    @Test
    fun `Should try to get user flags if access token is NOT empty`() {
        featureEnabled()
        whenever(mockCursor.getString(0)).thenReturn(Gson().toJson(sharedDataLoggedInNoSites))
        classToTest.tryLocalMigration(migrationStateFlow)
//        verify(userFlagsResolver).tryGetUserFlags(any(), any())
    }

    @Test
    fun `Should NOT dispatch UpdateTokenPayload if access token IS empty and no self-hosted sites`() {
        featureEnabled()
        onSuccessFlagsCaptor = argumentCaptor()
        onSuccessReaderPostsCaptor = argumentCaptor()

        val loginData = SharedLoginData(
                token = "",
                sites = listOf()
        )
        whenever(mockCursor.getString(0)).thenReturn(Gson().toJson(loginData))

        classToTest.tryLocalMigration(migrationStateFlow)
        verify(dispatcher, never()).dispatch(updateTokenAction)
    }

    @Test
    fun `Should dispatch UpdateTokenPayload if access token IS empty and there are self-hosted sites`() {
        featureEnabled()
        onSuccessFlagsCaptor = argumentCaptor()
        onSuccessReaderPostsCaptor = argumentCaptor()

        val loginData = SharedLoginData(
                token = "",
                sites = listOf(SiteModel())
        )
        whenever(mockCursor.getString(0)).thenReturn(Gson().toJson(loginData))
//        whenever(userFlagsResolver.tryGetUserFlags(
//                onSuccessFlagsCaptor.capture(),
//                any()
//        )).doAnswer { onSuccessFlagsCaptor.firstValue.invoke() }
//        whenever(readerSavedPostsResolver.tryGetReaderSavedPosts(
//                onSuccessReaderPostsCaptor.capture(),
//                any()
//        )).doAnswer { onSuccessReaderPostsCaptor.firstValue.invoke() }
        whenever(accountActionBuilderWrapper.newUpdateAccessTokenAction(
                loginData.token!!
        )).thenReturn(updateTokenAction)

        classToTest.tryLocalMigration(migrationStateFlow)
        verify(dispatcher, times(1)).dispatch(updateTokenAction)
    }

    @Test
    fun `Should track login start if NOT already logged in, feature flag is ENABLED and IS first try`() {
        featureEnabled()
        classToTest.tryLocalMigration(migrationStateFlow)
        verify(sharedLoginAnalyticsTracker).trackLoginStart()
    }

    @Test
    fun `Should NOT track login start if IS already logged in`() {
        whenever(appPrefsWrapper.getIsFirstTrySharedLoginJetpack()).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(jetpackSharedLoginFlag.isEnabled()).thenReturn(true)
        classToTest.tryLocalMigration(migrationStateFlow)
        verify(sharedLoginAnalyticsTracker, never()).trackLoginStart()
    }

    @Test
    fun `Should NOT track login start if feature flag is DISABLED`() {
        whenever(jetpackSharedLoginFlag.isEnabled()).thenReturn(false)
        classToTest.tryLocalMigration(migrationStateFlow)
        verify(sharedLoginAnalyticsTracker, never()).trackLoginStart()
    }

    @Test
    fun `Should NOT track login start if IS NOT the first try`() {
        whenever(appPrefsWrapper.getIsFirstTrySharedLoginJetpack()).thenReturn(false)
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(jetpackSharedLoginFlag.isEnabled()).thenReturn(true)
        classToTest.tryLocalMigration(migrationStateFlow)
        verify(sharedLoginAnalyticsTracker, never()).trackLoginStart()
    }

    @Test
    fun `Should track login failed if access token result cursor IS null`() {
        whenever(contentResolverWrapper.queryUri(contentResolver, uriValue)).thenReturn(null)
        featureEnabled()
        classToTest.tryLocalMigration(migrationStateFlow)
        verify(sharedLoginAnalyticsTracker, never()).trackLoginSuccess()
        verify(sharedLoginAnalyticsTracker, times(1)).trackLoginFailed(ErrorType.QueryLoginDataError)
    }

    @Test
    fun `Should track login failed if loginData IS null`() {
        whenever(mockCursor.getString(0)).thenReturn("{}?trigger an error")
        featureEnabled()
        classToTest.tryLocalMigration(migrationStateFlow)
        verify(sharedLoginAnalyticsTracker, never()).trackLoginSuccess()
        verify(sharedLoginAnalyticsTracker, times(1)).trackLoginFailed(ErrorType.NullLoginDataError)
    }

    @Test
    fun `Should track login failed if access token IS empty and no self-hosted sites`() {
        featureEnabled()
        val notLoggedInData = SharedLoginData(
                token = notLoggedInToken,
                sites = listOf()
        )
        whenever(mockCursor.getString(0)).thenReturn(Gson().toJson(notLoggedInData))
        classToTest.tryLocalMigration(migrationStateFlow)
        verify(sharedLoginAnalyticsTracker, never()).trackLoginSuccess()
        verify(sharedLoginAnalyticsTracker, times(1)).trackLoginFailed(ErrorType.WPNotLoggedInError)
    }

    @Test
    fun `Should track login success if access token IS empty and we have self-hosted sites`() {
        featureEnabled()
        val selfHosted = SiteModel()
        val notLoggedInData = SharedLoginData(
                token = notLoggedInToken,
                sites = listOf(selfHosted)
        )
        whenever(mockCursor.getString(0)).thenReturn(Gson().toJson(notLoggedInData))
        classToTest.tryLocalMigration(migrationStateFlow)
        verify(sharedLoginAnalyticsTracker, times(1)).trackLoginSuccess()
        verify(sharedLoginAnalyticsTracker, never()).trackLoginFailed(any())
    }

    @Test
    fun `Should track login success if access token IS NOT empty and no self-hosted sites`() {
        featureEnabled()
        val notSelfHosted = SiteModel().apply { setIsWPCom(true) }
        val loginData = sharedDataLoggedInNoSites.copy(
                sites = listOf(notSelfHosted)
        )
        whenever(mockCursor.getString(0)).thenReturn(Gson().toJson(loginData))
        classToTest.tryLocalMigration(migrationStateFlow)
        verify(sharedLoginAnalyticsTracker, never()).trackLoginFailed(any())
        verify(sharedLoginAnalyticsTracker, times(1)).trackLoginSuccess()
    }

    @Test
    fun `Should track login success if access token IS NOT empty and we have self-hosted sites`() {
        featureEnabled()
        val selfHosted = SiteModel()
        val loginData = sharedDataLoggedInNoSites.copy(
                sites = listOf(selfHosted)
        )
        whenever(mockCursor.getString(0)).thenReturn(Gson().toJson(loginData))
        classToTest.tryLocalMigration(migrationStateFlow)
        verify(sharedLoginAnalyticsTracker, never()).trackLoginFailed(any())
        verify(sharedLoginAnalyticsTracker, times(1)).trackLoginSuccess()
    }

    private fun featureEnabled() {
        whenever(appPrefsWrapper.getIsFirstTrySharedLoginJetpack()).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(jetpackSharedLoginFlag.isEnabled()).thenReturn(true)
    }
}
