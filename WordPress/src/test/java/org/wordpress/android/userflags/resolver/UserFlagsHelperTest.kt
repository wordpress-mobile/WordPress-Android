package org.wordpress.android.userflags.resolver

import android.content.ContentResolver
import android.content.Context
import org.junit.Assert
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.localcontentmigration.LocalMigrationContentResolver
import org.wordpress.android.localcontentmigration.LocalMigrationError.FeatureDisabled.UserFlagsDisabled
import org.wordpress.android.localcontentmigration.LocalMigrationError.MigrationAlreadyAttempted.UserFlagsAlreadyAttempted
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Failure
import org.wordpress.android.provider.query.QueryResult
import org.wordpress.android.resolver.ResolverUtility
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.userflags.JetpackLocalUserFlagsFlag
import org.wordpress.android.userflags.UserFlagsAnalyticsTracker
import org.wordpress.android.util.publicdata.WordPressPublicData
import org.wordpress.android.viewmodel.ContextProvider

class UserFlagsHelperTest {
    private val jetpackLocalUserFlagsFlag: JetpackLocalUserFlagsFlag = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val userFlagsAnalyticsTracker: UserFlagsAnalyticsTracker = mock()
    private val contextProvider: ContextProvider = mock()
    private val wordPressPublicData: WordPressPublicData = mock()
    private val queryResult: QueryResult = mock()
    private val localMigrationContentResolver = LocalMigrationContentResolver(
            contextProvider,
            wordPressPublicData,
            queryResult,
    )
    private val resolverUtility: ResolverUtility = mock()
    private val classToTest = UserFlagsHelper(
            jetpackLocalUserFlagsFlag,
            appPrefsWrapper,
            userFlagsAnalyticsTracker,
            localMigrationContentResolver,
            resolverUtility,
    )

    @Test
    fun `Should return Failure(UserFlagsDisabled) if feature flag returns FALSE`() {
        featureFlagEnabled(false)
        val expected = Failure(UserFlagsDisabled)
        val actual = classToTest.migrateUserFlags()
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `Should return Failure(UserFlagsAlreadyAttempted) if is first try returns FALSE`() {
        featureFlagEnabled(true)
        firstTry(false)
        val expected = Failure(UserFlagsAlreadyAttempted)
        val actual = classToTest.migrateUserFlags()
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `Should call userFlagsAnalyticsTracker trackStart if first try AND feature flag returns TRUE`() {
        mockLocalMigrationContentResolver()
        featureFlagEnabled(true)
        firstTry(true)
        classToTest.migrateUserFlags()
        verify(userFlagsAnalyticsTracker).trackStart()
    }

    private fun mockLocalMigrationContentResolver() {
        val context: Context = mock()
        whenever(contextProvider.getContext()).thenReturn(context)
        val contentResolver: ContentResolver = mock()
        whenever(context.contentResolver).thenReturn(contentResolver)
    }

    private fun featureFlagEnabled(isEnabled: Boolean) {
        whenever(jetpackLocalUserFlagsFlag.isEnabled()).thenReturn(isEnabled)
    }

    private fun firstTry(isFirstTry: Boolean) {
        whenever(appPrefsWrapper.getIsFirstTryUserFlagsJetpack()).thenReturn(isFirstTry)
    }
}
