package org.wordpress.android.bloggingreminders.resolver

import android.content.ContentResolver
import android.content.Context
import android.database.MatrixCursor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.bloggingreminders.BloggingRemindersSyncAnalyticsTracker
import org.wordpress.android.bloggingreminders.BloggingRemindersSyncAnalyticsTracker.ErrorType.QueryBloggingRemindersError
import org.wordpress.android.bloggingreminders.JetpackBloggingRemindersSyncFlag
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.MONDAY
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.localcontentmigration.LocalMigrationContentResolver
import org.wordpress.android.resolver.ContentResolverWrapper
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersModelMapper
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersUiModel
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.publicdata.WordPressPublicData
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.workers.reminder.ReminderScheduler
import java.time.DayOfWeek

@Suppress("ForbiddenComment")
// TODO: adapt these tests to the unified provider / orchestrator approach
@Ignore("Disabled for now: will refactor in another PR after unification.")
@ExperimentalCoroutinesApi
class BloggingRemindersResolverTest : BaseUnitTest() {
    private val jetpackBloggingRemindersSyncFlag: JetpackBloggingRemindersSyncFlag = mock()
    private val contextProvider: ContextProvider = mock()
    private val wordPressPublicData: WordPressPublicData = mock()
    private val contentResolverWrapper: ContentResolverWrapper = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val bloggingRemindersSyncAnalyticsTracker: BloggingRemindersSyncAnalyticsTracker = mock()
    private val siteStore: SiteStore = mock()
    private val bloggingRemindersStore: BloggingRemindersStore = mock()
    private val reminderScheduler: ReminderScheduler = mock()
    private val bloggingRemindersModelMapper: BloggingRemindersModelMapper = mock()
    private val localMigrationContentResolver: LocalMigrationContentResolver = mock()
    private val classToTest = BloggingRemindersResolver(
            jetpackBloggingRemindersSyncFlag,
            appPrefsWrapper,
            bloggingRemindersSyncAnalyticsTracker,
            siteStore,
            bloggingRemindersStore,
            TestScope(coroutinesTestRule.testDispatcher),
            reminderScheduler,
            bloggingRemindersModelMapper,
            localMigrationContentResolver,
    )

    private val context: Context = mock()
    private val contentResolver: ContentResolver = mock()
    private val mockCursor: MatrixCursor = mock()
    private val wordPressCurrentPackageId = "packageId"
    private val validLocalId = 123
    private val userSetBloggingRemindersModel = BloggingRemindersModel(validLocalId, setOf(MONDAY), 5, 43, false)
    private val defaultBloggingRemindersModel = BloggingRemindersModel(validLocalId)
    private val bloggingRemindersUiModel = BloggingRemindersUiModel(
            validLocalId, setOf(DayOfWeek.MONDAY), 5, 43, false
    )

    @Before
    fun setup() {
        whenever(contextProvider.getContext()).thenReturn(context)
        whenever(context.contentResolver).thenReturn(contentResolver)
        whenever(wordPressPublicData.currentPackageId()).thenReturn(wordPressCurrentPackageId)
        whenever(mockCursor.getString(0)).thenReturn("{}")
//        whenever(contentResolverWrapper.queryUri(contentResolver, uriValue)).thenReturn(mockCursor)
        whenever(siteStore.getSiteByLocalId(validLocalId)).thenReturn(SiteModel())
    }

    @Test
    fun `Should track start if feature flag is ENABLED and IS first try`() {
        featureEnabled()
        classToTest.trySyncBloggingReminders({}, {})
        verify(bloggingRemindersSyncAnalyticsTracker).trackStart()
    }

    @Test
    fun `Should trigger failure callback if feature flag is DISABLED`() {
        whenever(jetpackBloggingRemindersSyncFlag.isEnabled()).thenReturn(false)
        val onFailure: () -> Unit = mock()
        classToTest.trySyncBloggingReminders({}, onFailure)
        verify(onFailure).invoke()
    }

    @Test
    fun `Should trigger failure callback if IS NOT first try`() {
        whenever(appPrefsWrapper.getIsFirstTryBloggingRemindersSyncJetpack()).thenReturn(false)
        whenever(jetpackBloggingRemindersSyncFlag.isEnabled()).thenReturn(true)
        val onFailure: () -> Unit = mock()
        classToTest.trySyncBloggingReminders({}, onFailure)
        verify(onFailure).invoke()
    }

    @Test
    fun `Should save IS NOT first try sync blogging reminders as FALSE if feature flag is ENABLED and IS first try`() {
        featureEnabled()
        classToTest.trySyncBloggingReminders({}, {})
        verify(appPrefsWrapper).saveIsFirstTryBloggingRemindersSyncJetpack(false)
    }

    @Test
    fun `Should NOT query ContentResolver if feature flag is DISABLED`() {
        whenever(jetpackBloggingRemindersSyncFlag.isEnabled()).thenReturn(false)
        classToTest.trySyncBloggingReminders({}, {})
        verify(contentResolverWrapper, never()).queryUri(any(), any())
    }

    @Test
    fun `Should NOT query ContentResolver if IS NOT the first try`() {
        whenever(appPrefsWrapper.getIsFirstTryBloggingRemindersSyncJetpack()).thenReturn(false)
        whenever(jetpackBloggingRemindersSyncFlag.isEnabled()).thenReturn(true)
        classToTest.trySyncBloggingReminders({}, {})
        verify(contentResolverWrapper, never()).queryUri(any(), any())
    }

    @Test
    fun `Should query ContentResolver if feature flag is ENABLED and IS first try`() {
        featureEnabled()
        classToTest.trySyncBloggingReminders({}, {})
//        verify(contentResolverWrapper).queryUri(contentResolver, uriValue)
    }

    @Test
    fun `Should track failed with error QueryBloggingRemindersError if cursor is null`() {
        featureEnabled()
//        whenever(contentResolverWrapper.queryUri(contentResolver, uriValue)).thenReturn(null)
        classToTest.trySyncBloggingReminders({}, {})
        verify(bloggingRemindersSyncAnalyticsTracker).trackFailed(QueryBloggingRemindersError)
    }

    @Test
    fun `Should trigger failure callback if cursor is null`() {
        featureEnabled()
//        whenever(contentResolverWrapper.queryUri(contentResolver, uriValue)).thenReturn(null)
        val onFailure: () -> Unit = mock()
        classToTest.trySyncBloggingReminders({}, onFailure)
        verify(onFailure).invoke()
    }

    @Test
    fun `Should track success with reminders synced count 0 if result map is empty`() {
        featureEnabled()
        classToTest.trySyncBloggingReminders({}, {})
        verify(bloggingRemindersSyncAnalyticsTracker).trackSuccess(0)
    }

    @Test
    fun `Should trigger failure callback if result map is empty`() {
        featureEnabled()
        val onSuccess: () -> Unit = mock()
        classToTest.trySyncBloggingReminders(onSuccess) {}
        verify(onSuccess).invoke()
    }

    @Test
    fun `Should track success if result map has entries`() = test {
        whenever(bloggingRemindersModelMapper.toUiModel(any())).thenReturn(bloggingRemindersUiModel)
        whenever(bloggingRemindersStore.bloggingRemindersModel(validLocalId))
                .thenReturn(flowOf(defaultBloggingRemindersModel))
        featureEnabled()
        whenever(mockCursor.getString(0)).thenReturn(
                "[{\"enabledDays\":[\"MONDAY\"],\"hour\":5" +
                        ",\"isPromptIncluded\":false,\"minute\":43,\"siteId\":123}]"
        )
        classToTest.trySyncBloggingReminders({}, {})
        verify(bloggingRemindersSyncAnalyticsTracker).trackSuccess(1)
    }

    @Test
    fun `Should trigger success callback if result map has entries`() = test {
        whenever(bloggingRemindersStore.bloggingRemindersModel(validLocalId))
                .thenReturn(flowOf(userSetBloggingRemindersModel))
        featureEnabled()
        whenever(mockCursor.getString(0)).thenReturn(
                "[{\"enabledDays\":[\"MONDAY\"],\"hour\":5" +
                        ",\"isPromptIncluded\":false,\"minute\":43,\"siteId\":123}]"
        )
        val onSuccess: () -> Unit = mock()
        classToTest.trySyncBloggingReminders(onSuccess) {}
        verify(onSuccess).invoke()
    }

    @Test
    fun `Should update blogging reminder if site local ID is valid AND store returns default reminder`() = test {
        whenever(bloggingRemindersModelMapper.toUiModel(any())).thenReturn(bloggingRemindersUiModel)
        whenever(bloggingRemindersStore.bloggingRemindersModel(validLocalId))
                .thenReturn(flowOf(defaultBloggingRemindersModel))
        featureEnabled()
        whenever(mockCursor.getString(0)).thenReturn(
                "[{\"enabledDays\":[\"MONDAY\"],\"hour\":5" +
                        ",\"isPromptIncluded\":false,\"minute\":43,\"siteId\":123}]"
        )
        classToTest.trySyncBloggingReminders({}, {})
        verify(bloggingRemindersStore, times(1)).updateBloggingReminders(
                BloggingRemindersModel(
                        siteId = validLocalId,
                        enabledDays = setOf(MONDAY),
                        hour = 5,
                        minute = 43,
                        isPromptIncluded = false
                )
        )
    }

    @Test
    fun `Should map blogging reminder when setting local notification`() = test {
        whenever(bloggingRemindersModelMapper.toUiModel(any())).thenReturn(bloggingRemindersUiModel)
        whenever(bloggingRemindersStore.bloggingRemindersModel(validLocalId))
                .thenReturn(flowOf(defaultBloggingRemindersModel))
        featureEnabled()
        whenever(mockCursor.getString(0)).thenReturn(
                "[{\"enabledDays\":[\"MONDAY\"],\"hour\":5" +
                        ",\"isPromptIncluded\":false,\"minute\":43,\"siteId\":123}]"
        )
        classToTest.trySyncBloggingReminders({}, {})
        verify(bloggingRemindersModelMapper).toUiModel(userSetBloggingRemindersModel)
    }

    @Test
    fun `Should schedule blogging reminder local notification`() = test {
        whenever(bloggingRemindersModelMapper.toUiModel(any())).thenReturn(bloggingRemindersUiModel)
        whenever(bloggingRemindersStore.bloggingRemindersModel(validLocalId))
                .thenReturn(flowOf(defaultBloggingRemindersModel))
        featureEnabled()
        whenever(mockCursor.getString(0)).thenReturn(
                "[{\"enabledDays\":[\"MONDAY\"],\"hour\":5" +
                        ",\"isPromptIncluded\":false,\"minute\":43,\"siteId\":123}]"
        )
        classToTest.trySyncBloggingReminders({}, {})
        verify(reminderScheduler).schedule(
                validLocalId,
                bloggingRemindersUiModel.hour,
                bloggingRemindersUiModel.minute,
                bloggingRemindersUiModel.toReminderConfig()
        )
    }

    @Test
    fun `Should NOT update blogging reminder if site local ID is invalid`() = test {
        val invalidLocalId = 0
        featureEnabled()
        whenever(mockCursor.getString(0)).thenReturn(
                "[{\"enabledDays\":[\"MONDAY\"],\"hour\":5" +
                        ",\"isPromptIncluded\":false,\"minute\":43,\"siteId\":$invalidLocalId}}]"
        )
        classToTest.trySyncBloggingReminders({}, {})
        verify(bloggingRemindersStore, times(0)).updateBloggingReminders(any())
    }

    @Test
    fun `Should NOT update blogging reminder if reminder is already set`() = test {
        whenever(bloggingRemindersStore.bloggingRemindersModel(validLocalId))
                .thenReturn(flowOf(userSetBloggingRemindersModel))
        featureEnabled()
        whenever(mockCursor.getString(0)).thenReturn(
                "[{\"enabledDays\":[],\"hour\":5" +
                        ",\"isPromptIncluded\":false,\"minute\":43,\"siteId\":123}]"
        )
        classToTest.trySyncBloggingReminders({}, {})
        verify(bloggingRemindersStore, times(0)).updateBloggingReminders(any())
    }

    private fun featureEnabled() {
        whenever(appPrefsWrapper.getIsFirstTryBloggingRemindersSyncJetpack()).thenReturn(true)
        whenever(jetpackBloggingRemindersSyncFlag.isEnabled()).thenReturn(true)
    }
}
