package org.wordpress.android.bloggingreminders.resolver

import android.content.ContentResolver
import android.content.Context
import android.database.MatrixCursor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.wordpress.android.MainCoroutineScopeRule
import org.wordpress.android.bloggingreminders.BloggingRemindersSyncAnalyticsTracker
import org.wordpress.android.bloggingreminders.BloggingRemindersSyncAnalyticsTracker.ErrorType.NoBloggingRemindersFoundError
import org.wordpress.android.bloggingreminders.BloggingRemindersSyncAnalyticsTracker.ErrorType.QueryBloggingRemindersError
import org.wordpress.android.bloggingreminders.JetpackBloggingRemindersSyncFlag
import org.wordpress.android.bloggingreminders.provider.BloggingRemindersProvider
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.MONDAY
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.provider.query.QueryResult
import org.wordpress.android.resolver.ContentResolverWrapper
import org.wordpress.android.test
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.publicdata.WordPressPublicData
import org.wordpress.android.viewmodel.ContextProvider

@ExperimentalCoroutinesApi
class BloggingRemindersResolverTest {
    @Rule
    @JvmField val coroutineScope = MainCoroutineScopeRule()

    private val jetpackBloggingRemindersSyncFlag: JetpackBloggingRemindersSyncFlag = mock()
    private val contextProvider: ContextProvider = mock()
    private val wordPressPublicData: WordPressPublicData = mock()
    private val queryResult: QueryResult = mock()
    private val contentResolverWrapper: ContentResolverWrapper = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val bloggingRemindersSyncAnalyticsTracker: BloggingRemindersSyncAnalyticsTracker = mock()
    private val siteStore: SiteStore = mock()
    private val bloggingRemindersStore: BloggingRemindersStore = mock()
    private val classToTest = BloggingRemindersResolver(
            jetpackBloggingRemindersSyncFlag,
            contextProvider,
            wordPressPublicData,
            queryResult,
            contentResolverWrapper,
            appPrefsWrapper,
            bloggingRemindersSyncAnalyticsTracker,
            siteStore,
            bloggingRemindersStore,
            coroutineScope
    )

    private val context: Context = mock()
    private val contentResolver: ContentResolver = mock()
    private val mockCursor: MatrixCursor = mock()
    private val wordPressCurrentPackageId = "packageId"
    private val uriValue = "content://$wordPressCurrentPackageId.${BloggingRemindersProvider::class.simpleName}"
    private val validLocalId = 123
    private val userSetBloggingRemindersModel = BloggingRemindersModel(validLocalId, setOf(MONDAY))
    private val defaultBloggingRemindersModel = BloggingRemindersModel(validLocalId)

    @Before
    fun setup() {
        whenever(contextProvider.getContext()).thenReturn(context)
        whenever(context.contentResolver).thenReturn(contentResolver)
        whenever(wordPressPublicData.currentPackageId()).thenReturn(wordPressCurrentPackageId)
        whenever(mockCursor.getString(0)).thenReturn("{}")
        whenever(contentResolverWrapper.queryUri(contentResolver, uriValue)).thenReturn(mockCursor)
    }

    @Test
    fun `Should track start if feature flag is ENABLED and IS first try`() {
        featureEnabled()
        classToTest.trySyncBloggingReminders({}, {})
        verify(bloggingRemindersSyncAnalyticsTracker).trackStart()
    }

    @Test
    fun `Should trigger failure callback if feature flag is DISABLED`() {
        whenever(appPrefsWrapper.getIsFirstTryBloggingRemindersSyncJetpack()).thenReturn(true)
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
        whenever(appPrefsWrapper.getIsFirstTryBloggingRemindersSyncJetpack()).thenReturn(true)
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
        verify(contentResolverWrapper).queryUri(contentResolver, uriValue)
    }

    @Test
    fun `Should track failed with error QueryBloggingRemindersError if cursor is null`() {
        featureEnabled()
        whenever(contentResolverWrapper.queryUri(contentResolver, uriValue)).thenReturn(null)
        classToTest.trySyncBloggingReminders({}, {})
        verify(bloggingRemindersSyncAnalyticsTracker).trackFailed(QueryBloggingRemindersError)
    }

    @Test
    fun `Should trigger failure callback if cursor is null`() {
        featureEnabled()
        whenever(contentResolverWrapper.queryUri(contentResolver, uriValue)).thenReturn(null)
        val onFailure: () -> Unit = mock()
        classToTest.trySyncBloggingReminders({}, onFailure)
        verify(onFailure).invoke()
    }

    @Test
    fun `Should track failed with error NoBloggingRemindersFoundError if result map is empty`() {
        featureEnabled()
        classToTest.trySyncBloggingReminders({}, {})
        verify(bloggingRemindersSyncAnalyticsTracker).trackFailed(NoBloggingRemindersFoundError)
    }

    @Test
    fun `Should trigger failure callback if result map is empty`() {
        featureEnabled()
        val onFailure: () -> Unit = mock()
        classToTest.trySyncBloggingReminders({}, onFailure)
        verify(onFailure).invoke()
    }

    @Test
    fun `Should track success if result map has entries`() = test {
        whenever(bloggingRemindersStore.bloggingRemindersModel(validLocalId))
                .thenReturn(flowOf(userSetBloggingRemindersModel))
        whenever(siteStore.getLocalIdForRemoteSiteId(123L)).thenReturn(validLocalId)
        featureEnabled()
        whenever(mockCursor.getString(0)).thenReturn("{\"123\":{\"enabledDays\":[\"MONDAY\"],\"hour\":5" +
                ",\"isPromptIncluded\":false,\"minute\":43,\"siteId\":123}}")
        classToTest.trySyncBloggingReminders({}, {})
        verify(bloggingRemindersSyncAnalyticsTracker).trackSuccess()
    }

    @Test
    fun `Should trigger success callback if result map has entries`() = test {
        whenever(bloggingRemindersStore.bloggingRemindersModel(validLocalId))
                .thenReturn(flowOf(userSetBloggingRemindersModel))
        whenever(siteStore.getLocalIdForRemoteSiteId(123L)).thenReturn(validLocalId)
        featureEnabled()
        whenever(mockCursor.getString(0)).thenReturn("{\"123\":{\"enabledDays\":[\"MONDAY\"],\"hour\":5" +
                ",\"isPromptIncluded\":false,\"minute\":43,\"siteId\":123}}")
        val onSuccess: () -> Unit = mock()
        classToTest.trySyncBloggingReminders(onSuccess) {}
        verify(onSuccess).invoke()
    }

    @Test
    fun `Should update blogging reminder if site local ID is valid AND store returns default reminder`() = test {
        whenever(siteStore.getLocalIdForRemoteSiteId(123)).thenReturn(validLocalId)
        whenever(bloggingRemindersStore.bloggingRemindersModel(validLocalId))
                .thenReturn(flowOf(defaultBloggingRemindersModel))
        featureEnabled()
        whenever(mockCursor.getString(0)).thenReturn("{\"123\":{\"enabledDays\":[\"MONDAY\"],\"hour\":5" +
                ",\"isPromptIncluded\":false,\"minute\":43,\"siteId\":123}}")
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
    fun `Should NOT update blogging reminder if site local ID is invalid`() = test {
        val invalidLocalId = 0
        whenever(bloggingRemindersStore.bloggingRemindersModel(invalidLocalId))
                .thenReturn(flowOf(defaultBloggingRemindersModel))
        whenever(siteStore.getLocalIdForRemoteSiteId(123)).thenReturn(invalidLocalId)
        featureEnabled()
        whenever(mockCursor.getString(0)).thenReturn("{\"123\":{\"enabledDays\":[\"MONDAY\"],\"hour\":5" +
                ",\"isPromptIncluded\":false,\"minute\":43,\"siteId\":123}}")
        classToTest.trySyncBloggingReminders({}, {})
        verify(bloggingRemindersStore, times(0)).updateBloggingReminders(any())
    }

    @Test
    fun `Should NOT update blogging reminder if reminder is already set`() = test {
        whenever(bloggingRemindersStore.bloggingRemindersModel(validLocalId))
                .thenReturn(flowOf(userSetBloggingRemindersModel))
        whenever(siteStore.getLocalIdForRemoteSiteId(123)).thenReturn(validLocalId)
        featureEnabled()
        whenever(mockCursor.getString(0)).thenReturn("{\"123\":{\"enabledDays\":[],\"hour\":5" +
                ",\"isPromptIncluded\":false,\"minute\":43,\"siteId\":123}}")
        classToTest.trySyncBloggingReminders({}, {})
        verify(bloggingRemindersStore, times(0)).updateBloggingReminders(any())
    }

    private fun featureEnabled() {
        whenever(appPrefsWrapper.getIsFirstTryBloggingRemindersSyncJetpack()).thenReturn(true)
        whenever(jetpackBloggingRemindersSyncFlag.isEnabled()).thenReturn(true)
    }
}
