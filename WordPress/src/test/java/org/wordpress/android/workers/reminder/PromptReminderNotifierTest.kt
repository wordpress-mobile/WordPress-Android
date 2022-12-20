package org.wordpress.android.workers.reminder

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersAnalyticsTracker
import org.wordpress.android.util.HtmlCompatWrapper
import org.wordpress.android.util.config.BloggingPromptsFeatureConfig
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.workers.reminder.prompt.PromptReminderNotifier

@ExperimentalCoroutinesApi
class PromptReminderNotifierTest : BaseUnitTest() {
    private val contextProvider: ContextProvider = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val siteStore: SiteStore = mock()
    private val bloggingPromptsStore: BloggingPromptsStore = mock()
    private val accountStore: AccountStore = mock()
    private val reminderNotificationManager: ReminderNotificationManager = mock()
    private val bloggingPromptsFeatureConfig: BloggingPromptsFeatureConfig = mock()
    private val bloggingRemindersAnalyticsTracker: BloggingRemindersAnalyticsTracker = mock()
    private val bloggingRemindersStore: BloggingRemindersStore = mock()
    private val bloggingReminder: BloggingRemindersModel = mock()
    private val htmlCompatWrapper: HtmlCompatWrapper = mock()

    private val classToTest = PromptReminderNotifier(
            contextProvider = contextProvider,
            resourceProvider = resourceProvider,
            siteStore = siteStore,
            accountStore = accountStore,
            reminderNotificationManager = reminderNotificationManager,
            bloggingPromptsFeatureConfig = bloggingPromptsFeatureConfig,
            bloggingPromptsStore = bloggingPromptsStore,
            bloggingRemindersAnalyticsTracker = bloggingRemindersAnalyticsTracker,
            htmlCompatWrapper = htmlCompatWrapper,
            bloggingRemindersStore = bloggingRemindersStore
    )

    @Before
    fun setUp() {
        whenever(bloggingRemindersStore.bloggingRemindersModel(any())).thenReturn(flowOf(bloggingReminder))
    }

    @Test
    fun `Should NOT notify if hasAccessToken returns FALSE`() = test {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        assertFalse(classToTest.shouldNotify(123))
    }

    @Test
    fun `Should NOT notify if blogging prompts notification flag isEnabled returns FALSE`() = test {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(false)
        assertFalse(classToTest.shouldNotify(123))
    }

    @Test
    fun `Should NOT notify if getSiteByLocalId returns NULL`() = test {
        val siteId = 123
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(null)
        assertFalse(classToTest.shouldNotify(siteId))
    }

    @Test
    fun `Should NOT notify if user did NOT opt in to include prompts in the reminders`() = test {
        val siteId = 123
        val siteModel: SiteModel = mock()
        val disabledPromptBloggingReminderModel = BloggingRemindersModel(
                siteId = siteId,
                isPromptIncluded = false
        )
        whenever(bloggingRemindersStore.bloggingRemindersModel(any())).thenReturn(
                flowOf(disabledPromptBloggingReminderModel)
        )
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(false)
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(siteModel)
        assertFalse(classToTest.shouldNotify(123))
    }

    @Test
    fun `Should notify if has access token, flag enabled and user opted in to include prompts in reminders`() = test {
        val siteId = 123
        val siteModel: SiteModel = mock()
        val enabledPromptBloggingReminderModel = BloggingRemindersModel(
                siteId = siteId,
                isPromptIncluded = true
        )
        whenever(bloggingRemindersStore.bloggingRemindersModel(siteId)).thenReturn(
                flowOf(enabledPromptBloggingReminderModel)
        )
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(siteModel)
        assertTrue(classToTest.shouldNotify(siteId))
    }
}
