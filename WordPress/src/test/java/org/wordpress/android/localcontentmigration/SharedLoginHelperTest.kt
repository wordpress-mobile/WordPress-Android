package org.wordpress.android.localcontentmigration

import android.content.ContentResolver
import android.content.Context
import org.junit.Assert
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.localcontentmigration.LocalMigrationError.FeatureDisabled.SharedLoginDisabled
import org.wordpress.android.localcontentmigration.LocalMigrationError.MigrationAlreadyAttempted.SharedLoginAlreadyAttempted
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Failure
import org.wordpress.android.provider.query.QueryResult
import org.wordpress.android.sharedlogin.JetpackSharedLoginFlag
import org.wordpress.android.sharedlogin.SharedLoginAnalyticsTracker
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.AccountActionBuilderWrapper
import org.wordpress.android.util.publicdata.WordPressPublicData
import org.wordpress.android.viewmodel.ContextProvider

class SharedLoginHelperTest {
    private val jetpackSharedLoginFlag: JetpackSharedLoginFlag = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val sharedLoginAnalyticsTracker: SharedLoginAnalyticsTracker = mock()
    private val contextProvider: ContextProvider = mock()
    private val dispatcher: Dispatcher = mock()
    private val accountActionBuilderWrapper: AccountActionBuilderWrapper = mock()
    private val wordPressPublicData: WordPressPublicData = mock()
    private val queryResult: QueryResult = mock()
    private val localMigrationContentResolver = LocalMigrationContentResolver(
        contextProvider,
        wordPressPublicData,
        queryResult,
    )
    private val classToTest = SharedLoginHelper(
        jetpackSharedLoginFlag,
        appPrefsWrapper,
        sharedLoginAnalyticsTracker,
        localMigrationContentResolver,
        dispatcher,
        accountActionBuilderWrapper,
    )

    @Test
    fun `Should return Failure(SharedLoginDisabled) if feature flag returns FALSE`() {
        featureFlagEnabled(false)
        val expected = Failure(SharedLoginDisabled)
        val actual = classToTest.login()
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `Should return Failure(SharedLoginAlreadyAttempted) if is first try returns FALSE`() {
        featureFlagEnabled(true)
        firstTry(false)
        val expected = Failure(SharedLoginAlreadyAttempted)
        val actual = classToTest.login()
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `Should call sharedLoginAnalyticsTracker trackLoginStart if first try AND feature flag returns TRUE`() {
        mockLocalMigrationContentResolver()
        featureFlagEnabled(true)
        firstTry(true)
        classToTest.login()
        verify(sharedLoginAnalyticsTracker).trackLoginStart()
    }

    private fun mockLocalMigrationContentResolver() {
        val context: Context = mock()
        whenever(contextProvider.getContext()).thenReturn(context)
        val contentResolver: ContentResolver = mock()
        whenever(context.contentResolver).thenReturn(contentResolver)
    }

    private fun featureFlagEnabled(isEnabled: Boolean) {
        whenever(jetpackSharedLoginFlag.isEnabled()).thenReturn(isEnabled)
    }

    private fun firstTry(isFirstTry: Boolean) {
        whenever(appPrefsWrapper.getIsFirstTrySharedLoginJetpack()).thenReturn(isFirstTry)
    }
}
