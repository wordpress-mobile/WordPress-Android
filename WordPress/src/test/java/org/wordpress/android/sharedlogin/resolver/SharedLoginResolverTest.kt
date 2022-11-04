package org.wordpress.android.sharedlogin.resolver

import android.content.ContentResolver
import android.content.Context
import android.database.MatrixCursor
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.UpdateTokenPayload
import org.wordpress.android.localcontentmigration.LocalMigrationContentProvider
import org.wordpress.android.localcontentmigration.LocalMigrationContentResolver
import org.wordpress.android.provider.query.QueryResult
import org.wordpress.android.reader.savedposts.resolver.ReaderSavedPostsResolver
import org.wordpress.android.resolver.ContentResolverWrapper
import org.wordpress.android.sharedlogin.JetpackSharedLoginFlag
import org.wordpress.android.sharedlogin.SharedLoginAnalyticsTracker
import org.wordpress.android.sharedlogin.SharedLoginAnalyticsTracker.ErrorType
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.userflags.resolver.UserFlagsResolver
import org.wordpress.android.util.AccountActionBuilderWrapper
import org.wordpress.android.util.publicdata.WordPressPublicData
import org.wordpress.android.viewmodel.ContextProvider

@Suppress("ForbiddenComment")
// TODO: adapt these tests to the unified provider / orchestrator approach
class SharedLoginResolverTest {
    private lateinit var onSuccessFlagsCaptor: KArgumentCaptor<() -> Unit>
    private lateinit var onSuccessReaderPostsCaptor: KArgumentCaptor<() -> Unit>

    private val jetpackSharedLoginFlag: JetpackSharedLoginFlag = mock()
    private val contextProvider: ContextProvider = mock()
    private val wordPressPublicData: WordPressPublicData = mock()
    private val dispatcher: Dispatcher = mock()
    private val queryResult: QueryResult = mock()
    private val accountStore: AccountStore = mock()
    private val contentResolverWrapper: ContentResolverWrapper = mock()
    private val accountActionBuilderWrapper: AccountActionBuilderWrapper = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val sharedLoginAnalyticsTracker: SharedLoginAnalyticsTracker = mock()
    private val userFlagsResolver: UserFlagsResolver = mock()
    private val readerSavedPostsResolver: ReaderSavedPostsResolver = mock()
    private val localMigrationContentResolver: LocalMigrationContentResolver = mock()

    private val classToTest = LocalMigrationOrchestrator(
            jetpackSharedLoginFlag,
            contextProvider,
            dispatcher,
            accountStore,
            accountActionBuilderWrapper,
            appPrefsWrapper,
            sharedLoginAnalyticsTracker,
            userFlagsResolver,
            readerSavedPostsResolver,
            localMigrationContentResolver,
    )
    private val loggedInToken = "valid"
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
        whenever(mockCursor.getString(0)).thenReturn(notLoggedInToken)
        whenever(accountActionBuilderWrapper.newUpdateAccessTokenAction(loggedInToken)).thenReturn(updateTokenAction)
        whenever(contentResolverWrapper.queryUri(contentResolver, uriValue)).thenReturn(mockCursor)
    }

    @Test
    fun `Should NOT query ContentResolver if feature flag is DISABLED`() {
        whenever(appPrefsWrapper.getIsFirstTrySharedLoginJetpack()).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(jetpackSharedLoginFlag.isEnabled()).thenReturn(false)
        classToTest.tryLocalMigration()
        verify(contentResolverWrapper, never()).queryUri(contentResolver, uriValue)
    }

    @Test
    fun `Should NOT query ContentResolver if IS already logged in`() {
        whenever(appPrefsWrapper.getIsFirstTrySharedLoginJetpack()).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(jetpackSharedLoginFlag.isEnabled()).thenReturn(true)
        classToTest.tryLocalMigration()
        verify(contentResolverWrapper, never()).queryUri(contentResolver, uriValue)
    }

    @Test
    fun `Should NOT query ContentResolver if IS NOT the first try`() {
        whenever(appPrefsWrapper.getIsFirstTrySharedLoginJetpack()).thenReturn(false)
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(jetpackSharedLoginFlag.isEnabled()).thenReturn(true)
        classToTest.tryLocalMigration()
        verify(contentResolverWrapper, never()).queryUri(contentResolver, uriValue)
    }

    @Test
    fun `Should query ContentResolver if NOT already logged in, feature flag is ENABLED and IS first try`() {
        featureEnabled()
        classToTest.tryLocalMigration()
        verify(contentResolverWrapper).queryUri(contentResolver, uriValue)
    }

    @Test
    fun `Should dispatch UpdateTokenPayload if access token is NOT empty, flags and saved posts are migrated`() {
        featureEnabled()
        onSuccessFlagsCaptor = argumentCaptor()
        onSuccessReaderPostsCaptor = argumentCaptor()

        whenever(queryResult.getValue<String>(mockCursor)).thenReturn(loggedInToken)
        whenever(userFlagsResolver.tryGetUserFlags(
                onSuccessFlagsCaptor.capture(),
                any()
        )).doAnswer { onSuccessFlagsCaptor.firstValue.invoke() }
        whenever(readerSavedPostsResolver.tryGetReaderSavedPosts(
                onSuccessReaderPostsCaptor.capture(),
                any()
        )).doAnswer { onSuccessReaderPostsCaptor.firstValue.invoke() }

        classToTest.tryLocalMigration()

        verify(dispatcher).dispatch(updateTokenAction)
    }

    @Test
    fun `Should try to get user flags if access token is NOT empty`() {
        featureEnabled()
        whenever(queryResult.getValue<String>(mockCursor)).thenReturn(loggedInToken)
        classToTest.tryLocalMigration()
        verify(userFlagsResolver).tryGetUserFlags(any(), any())
    }

    @Test
    fun `Should NOT dispatch UpdateTokenPayload if access token IS empty`() {
        featureEnabled()
        onSuccessFlagsCaptor = argumentCaptor()
        onSuccessReaderPostsCaptor = argumentCaptor()

        whenever(queryResult.getValue<String>(mockCursor)).thenReturn(notLoggedInToken)
        whenever(userFlagsResolver.tryGetUserFlags(
                onSuccessFlagsCaptor.capture(),
                any()
        )).doAnswer { onSuccessFlagsCaptor.firstValue.invoke() }
        whenever(readerSavedPostsResolver.tryGetReaderSavedPosts(
                onSuccessReaderPostsCaptor.capture(),
                any()
        )).doAnswer { onSuccessReaderPostsCaptor.firstValue.invoke() }

        classToTest.tryLocalMigration()
        verify(dispatcher, never()).dispatch(updateTokenAction)
    }

    @Test
    fun `Should track login start if NOT already logged in, feature flag is ENABLED and IS first try`() {
        featureEnabled()
        classToTest.tryLocalMigration()
        verify(sharedLoginAnalyticsTracker).trackLoginStart()
    }

    @Test
    fun `Should NOT track login start if IS already logged in`() {
        whenever(appPrefsWrapper.getIsFirstTrySharedLoginJetpack()).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(jetpackSharedLoginFlag.isEnabled()).thenReturn(true)
        classToTest.tryLocalMigration()
        verify(sharedLoginAnalyticsTracker, never()).trackLoginStart()
    }

    @Test
    fun `Should NOT track login start if feature flag is DISABLED`() {
        whenever(appPrefsWrapper.getIsFirstTrySharedLoginJetpack()).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(jetpackSharedLoginFlag.isEnabled()).thenReturn(false)
        classToTest.tryLocalMigration()
        verify(sharedLoginAnalyticsTracker, never()).trackLoginStart()
    }

    @Test
    fun `Should NOT track login start if IS NOT the first try`() {
        whenever(appPrefsWrapper.getIsFirstTrySharedLoginJetpack()).thenReturn(false)
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(jetpackSharedLoginFlag.isEnabled()).thenReturn(true)
        classToTest.tryLocalMigration()
        verify(sharedLoginAnalyticsTracker, never()).trackLoginStart()
    }

    @Test
    fun `Should track login failed if access token result cursor IS null`() {
        whenever(contentResolverWrapper.queryUri(contentResolver, uriValue)).thenReturn(null)
        featureEnabled()
        classToTest.tryLocalMigration()
        verify(sharedLoginAnalyticsTracker, never()).trackLoginSuccess()
        verify(sharedLoginAnalyticsTracker, times(1)).trackLoginFailed(ErrorType.QueryTokenError)
    }

    @Test
    fun `Should track login failed if access token IS empty`() {
        featureEnabled()
        whenever(queryResult.getValue<String>(mockCursor)).thenReturn(notLoggedInToken)
        classToTest.tryLocalMigration()
        verify(sharedLoginAnalyticsTracker, never()).trackLoginSuccess()
        verify(sharedLoginAnalyticsTracker, times(1)).trackLoginFailed(ErrorType.WPNotLoggedInError)
    }

    @Test
    fun `Should track login success if access token result cursor IS NOT null AND access token IS NOT empty`() {
        featureEnabled()
        whenever(queryResult.getValue<String>(mockCursor)).thenReturn(loggedInToken)
        classToTest.tryLocalMigration()
        verify(sharedLoginAnalyticsTracker, never()).trackLoginFailed(any())
        verify(sharedLoginAnalyticsTracker, times(1)).trackLoginSuccess()
    }

    private fun featureEnabled() {
        whenever(appPrefsWrapper.getIsFirstTrySharedLoginJetpack()).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(jetpackSharedLoginFlag.isEnabled()).thenReturn(true)
    }
}
