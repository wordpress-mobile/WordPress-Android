package org.wordpress.android.reader.savedposts.resolver

import android.content.ContentResolver
import android.content.Context
import org.junit.Assert
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.datasets.wrappers.ReaderDatabaseWrapper
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.datasets.wrappers.ReaderTagTableWrapper
import org.wordpress.android.localcontentmigration.LocalMigrationContentResolver
import org.wordpress.android.localcontentmigration.LocalMigrationError.FeatureDisabled.ReaderSavedPostsDisabled
import org.wordpress.android.localcontentmigration.LocalMigrationError.MigrationAlreadyAttempted.ReaderSavedPostsAlreadyAttempted
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Failure
import org.wordpress.android.provider.query.QueryResult
import org.wordpress.android.reader.savedposts.JetpackReaderSavedPostsFlag
import org.wordpress.android.reader.savedposts.ReaderSavedPostsAnalyticsTracker
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.publicdata.WordPressPublicData
import org.wordpress.android.viewmodel.ContextProvider

class ReaderSavedPostsHelperTest {
    private val jetpackReaderSavedPostsFlag: JetpackReaderSavedPostsFlag = mock()
    private val contextProvider: ContextProvider = mock()
    private val readerSavedPostsAnalyticsTracker: ReaderSavedPostsAnalyticsTracker = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val readerPostTableWrapper: ReaderPostTableWrapper = mock()
    private val readerTagTableWrapper: ReaderTagTableWrapper = mock()
    private val readerDatabaseWrapper: ReaderDatabaseWrapper = mock()
    private val wordPressPublicData: WordPressPublicData = mock()
    private val queryResult: QueryResult = mock()
    private val localMigrationContentResolver = LocalMigrationContentResolver(
            contextProvider,
            wordPressPublicData,
            queryResult,
    )
    private val classToTest = ReaderSavedPostsHelper(
            jetpackReaderSavedPostsFlag,
            contextProvider,
            readerSavedPostsAnalyticsTracker,
            appPrefsWrapper,
            readerPostTableWrapper,
            readerTagTableWrapper,
            readerDatabaseWrapper,
            localMigrationContentResolver,
    )

    @Test
    fun `Should return Failure(ReaderSavedPostsDisabled) if feature flag returns FALSE`() {
        featureFlagEnabled(false)
        val expected = Failure(ReaderSavedPostsDisabled)
        val actual = classToTest.migrateReaderSavedPosts()
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `Should return Failure(ReaderSavedPostsAlreadyAttempted) if is first try returns FALSE`() {
        featureFlagEnabled(true)
        firstTry(false)
        val expected = Failure(ReaderSavedPostsAlreadyAttempted)
        val actual = classToTest.migrateReaderSavedPosts()
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `Should call readerSavedPostsAnalyticsTracker trackStart if first try AND feature flag returns TRUE`() {
        mockLocalMigrationContentResolver()
        featureFlagEnabled(true)
        firstTry(true)
        classToTest.migrateReaderSavedPosts()
        verify(readerSavedPostsAnalyticsTracker).trackStart()
    }

    private fun mockLocalMigrationContentResolver() {
        val context: Context = mock()
        whenever(contextProvider.getContext()).thenReturn(context)
        val contentResolver: ContentResolver = mock()
        whenever(context.contentResolver).thenReturn(contentResolver)
    }

    private fun featureFlagEnabled(isEnabled: Boolean) {
        whenever(jetpackReaderSavedPostsFlag.isEnabled()).thenReturn(isEnabled)
    }

    private fun firstTry(isFirstTry: Boolean) {
        whenever(appPrefsWrapper.getIsFirstTryReaderSavedPostsJetpack()).thenReturn(isFirstTry)
    }
}
