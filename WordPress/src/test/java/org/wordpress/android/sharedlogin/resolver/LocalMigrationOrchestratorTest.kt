package org.wordpress.android.sharedlogin.resolver

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.localcontentmigration.ContentMigrationAnalyticsTracker
import org.wordpress.android.localcontentmigration.ContentMigrationAnalyticsTracker.ErrorType.LocalDraftContent
import org.wordpress.android.localcontentmigration.EligibilityHelper
import org.wordpress.android.localcontentmigration.LocalContentEntity.AccessToken
import org.wordpress.android.localcontentmigration.LocalContentEntity.Post
import org.wordpress.android.localcontentmigration.LocalContentEntity.ReaderPosts
import org.wordpress.android.localcontentmigration.LocalContentEntity.Sites
import org.wordpress.android.localcontentmigration.LocalContentEntity.UserFlags
import org.wordpress.android.localcontentmigration.LocalContentEntityData.AccessTokenData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.Companion.IneligibleReason.LocalDraftContentIsPresent
import org.wordpress.android.localcontentmigration.LocalContentEntityData.Companion.IneligibleReason.WPNotLoggedIn
import org.wordpress.android.localcontentmigration.LocalContentEntityData.EligibilityStatusData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.PostData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.ReaderPostsData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.SitesData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.UserFlagsData
import org.wordpress.android.localcontentmigration.LocalMigrationError
import org.wordpress.android.localcontentmigration.LocalMigrationError.ProviderError
import org.wordpress.android.localcontentmigration.LocalMigrationError.ProviderError.NullCursor
import org.wordpress.android.localcontentmigration.LocalMigrationError.ProviderError.NullValueFromQuery
import org.wordpress.android.localcontentmigration.LocalMigrationError.ProviderError.ParsingException
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Failure
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Success
import org.wordpress.android.localcontentmigration.LocalMigrationState
import org.wordpress.android.localcontentmigration.LocalMigrationState.Finished
import org.wordpress.android.localcontentmigration.LocalMigrationState.Finished.Ineligible
import org.wordpress.android.localcontentmigration.LocalMigrationState.Finished.Successful
import org.wordpress.android.localcontentmigration.LocalMigrationState.Initial
import org.wordpress.android.localcontentmigration.LocalPostsHelper
import org.wordpress.android.localcontentmigration.SharedLoginHelper
import org.wordpress.android.localcontentmigration.SitesMigrationHelper
import org.wordpress.android.localcontentmigration.WelcomeScreenData
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.reader.savedposts.resolver.ReaderSavedPostsHelper
import org.wordpress.android.sharedlogin.SharedLoginAnalyticsTracker
import org.wordpress.android.sharedlogin.SharedLoginAnalyticsTracker.ErrorType.WPNotLoggedInError
import org.wordpress.android.userflags.resolver.UserFlagsHelper
import org.wordpress.android.util.AppLog
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class LocalMigrationOrchestratorTest : BaseUnitTest() {
    private val sharedLoginAnalyticsTracker: SharedLoginAnalyticsTracker = mock()
    private val migrationAnalyticsTracker: ContentMigrationAnalyticsTracker = mock()
    private val userFlagsHelper: UserFlagsHelper = mock()
    private val readerSavedPostsHelper: ReaderSavedPostsHelper = mock()
    private val sharedLoginHelper: SharedLoginHelper = mock()
    private val sitesMigrationHelper: SitesMigrationHelper = mock()
    private val localPostsHelper: LocalPostsHelper = mock()
    private val eligibilityHelper: EligibilityHelper = mock()
    private val appLogWrapper: AppLogWrapper = mock()
    private val classToTest = LocalMigrationOrchestrator(
        sharedLoginAnalyticsTracker,
        migrationAnalyticsTracker,
        userFlagsHelper,
        readerSavedPostsHelper,
        sharedLoginHelper,
        sitesMigrationHelper,
        localPostsHelper,
        eligibilityHelper,
        appLogWrapper,
    )
    private val avatarUrl = "avatarUrl"
    private val sites = listOf(SiteModel(), SiteModel())

    @Test
    fun `Should call validate on EligibilityHelper when tryLocalMigration is called (happy path)`() {
        mockHappyPath()
        classToTest.tryLocalMigration(MutableStateFlow(Initial))
        verify(eligibilityHelper).validate()
    }

    @Test
    fun `Should emit Ineligible state when eligibilityHelper validate returns WPNotLoggedIn`() {
        whenever(eligibilityHelper.validate()).thenReturn(Failure(LocalMigrationError.Ineligibility(WPNotLoggedIn)))
        val mutableStateFlow: MutableStateFlow<LocalMigrationState> = MutableStateFlow(Initial)
        classToTest.tryLocalMigration(mutableStateFlow)
        val expected = Ineligible
        val actual = mutableStateFlow.value
        assertEquals(expected, actual)
    }

    @Test
    fun `Should trackLoginFailed when eligibilityHelper validate returns WPNotLoggedIn`() {
        whenever(eligibilityHelper.validate()).thenReturn(Failure(LocalMigrationError.Ineligibility(WPNotLoggedIn)))
        val mutableStateFlow: MutableStateFlow<LocalMigrationState> = MutableStateFlow(Initial)
        classToTest.tryLocalMigration(mutableStateFlow)
        verify(sharedLoginAnalyticsTracker).trackLoginFailed(WPNotLoggedInError)
    }

    @Test
    fun `Should emit Ineligible state when eligibilityHelper validate returns LocalDraftContentIsPresent`() {
        whenever(eligibilityHelper.validate())
            .thenReturn(Failure(LocalMigrationError.Ineligibility(LocalDraftContentIsPresent)))
        val mutableStateFlow: MutableStateFlow<LocalMigrationState> = MutableStateFlow(Initial)
        classToTest.tryLocalMigration(mutableStateFlow)
        val expected = Ineligible
        val actual = mutableStateFlow.value
        assertEquals(expected, actual)
    }

    @Test
    fun `Should trackContentMigrationFailed when eligibilityHelper validate returns LocalDraftContentIsPresent`() {
        whenever(eligibilityHelper.validate())
            .thenReturn(Failure(LocalMigrationError.Ineligibility(LocalDraftContentIsPresent)))
        val mutableStateFlow: MutableStateFlow<LocalMigrationState> = MutableStateFlow(Initial)
        classToTest.tryLocalMigration(mutableStateFlow)
        migrationAnalyticsTracker.trackContentMigrationFailed(LocalDraftContent)
    }

    @Test
    fun `Should call sharedLoginHelper login when tryLocalMigration is called (happy path)`() {
        mockHappyPath()
        classToTest.tryLocalMigration(MutableStateFlow(Initial))
        verify(sharedLoginHelper).login()
    }

    @Test
    fun `Should emit Failure if sharedLoginHelper login fails`() {
        val error = NullValueFromQuery(AccessToken)
        mockHappyPath()
        whenever(sharedLoginHelper.login()).thenReturn(Failure(error))
        assertFailure(error)
    }

    @Test
    fun `Should call sitesMigrationHelper migrateSites when tryLocalMigration is called (happy path)`() {
        mockHappyPath()
        classToTest.tryLocalMigration(MutableStateFlow(Initial))
        verify(sitesMigrationHelper).migrateSites()
    }

    @Test
    fun `Should emit Failure if sitesMigrationHelper migrateSites fails`() {
        val error = NullCursor(Sites)
        mockHappyPath()
        whenever(sitesMigrationHelper.migrateSites()).thenReturn(Failure(error))
        assertFailure(error)
    }

    @Test
    fun `Should log Failure if sitesMigrationHelper migrateSites fails`() {
        val error = NullCursor(Sites)
        mockHappyPath()
        whenever(sitesMigrationHelper.migrateSites()).thenReturn(Failure(error))
        val mutableStateFlow: MutableStateFlow<LocalMigrationState> = MutableStateFlow(Initial)
        classToTest.tryLocalMigration(mutableStateFlow)
        verify(appLogWrapper).e(AppLog.T.JETPACK_MIGRATION, "$error")
    }

    @Test
    fun `Should call userFlagsHelper migrateUserFlags when tryLocalMigration is called (happy path)`() {
        mockHappyPath()
        classToTest.tryLocalMigration(MutableStateFlow(Initial))
        verify(userFlagsHelper).migrateUserFlags()
    }

    @Test
    fun `Should emit Failure if userFlagsHelper migrateUserFlags fails`() {
        val error = ParsingException(UserFlags, IllegalArgumentException())
        mockHappyPath()
        whenever(userFlagsHelper.migrateUserFlags()).thenReturn(Failure(error))
        assertFailure(error)
    }

    @Test
    fun `Should call readerSavedPostsHelper migrateReaderSavedPosts when tryLocalMigration is called (happy path)`() {
        mockHappyPath()
        classToTest.tryLocalMigration(MutableStateFlow(Initial))
        verify(readerSavedPostsHelper).migrateReaderSavedPosts()
    }

    @Test
    fun `Should emit Failure if readerSavedPostsHelper migrateReaderSavedPosts fails`() {
        val error = NullValueFromQuery(ReaderPosts)
        mockHappyPath()
        whenever(readerSavedPostsHelper.migrateReaderSavedPosts()).thenReturn(Failure(error))
        assertFailure(error)
    }

    @Test
    fun `Should call localPostsHelper migratePosts when tryLocalMigration is called (happy path)`() {
        mockHappyPath()
        classToTest.tryLocalMigration(MutableStateFlow(Initial))
        verify(localPostsHelper).migratePosts()
    }

    @Test
    fun `Should emit Failure if localPostsHelper migratePosts fails`() {
        val error = NullCursor(Post)
        mockHappyPath()
        whenever(localPostsHelper.migratePosts()).thenReturn(Failure(error))
        assertFailure(error)
    }

    @Test
    fun `Should emit Successful state on happy path`() {
        mockHappyPath()
        val mutableStateFlow: MutableStateFlow<LocalMigrationState> = MutableStateFlow(Initial)
        classToTest.tryLocalMigration(mutableStateFlow)
        val expected = Successful(WelcomeScreenData(avatarUrl, sites))
        val actual = mutableStateFlow.value
        assertTrue(actual is Successful)
        assertEquals(expected.data, actual.data)
    }

    private fun mockHappyPath() {
        whenever(eligibilityHelper.validate()).thenReturn(Success(EligibilityStatusData(true)))
        whenever(sharedLoginHelper.login()).thenReturn(Success(AccessTokenData("", avatarUrl)))
        whenever(sitesMigrationHelper.migrateSites()).thenReturn(Success(SitesData(sites)))
        whenever(userFlagsHelper.migrateUserFlags()).thenReturn(Success(UserFlagsData(mapOf(), listOf(), listOf())))
        whenever(readerSavedPostsHelper.migrateReaderSavedPosts())
            .thenReturn(Success(ReaderPostsData(ReaderPostList())))
        whenever(localPostsHelper.migratePosts()).thenReturn(Success(PostData(PostModel())))
    }

    private fun assertFailure(error: ProviderError) {
        val mutableStateFlow: MutableStateFlow<LocalMigrationState> = MutableStateFlow(Initial)
        classToTest.tryLocalMigration(mutableStateFlow)
        val expected = Finished.Failure(error)
        val actual = mutableStateFlow.value
        assertEquals(expected, actual)
    }
}
