package org.wordpress.android.workers.reminder

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore
import org.wordpress.android.util.config.BloggingPromptsFeatureConfig
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider

class PromptReminderNotifierTest {
    private val contextProvider: ContextProvider = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val siteStore: SiteStore = mock()
    private val bloggingPromptsStore: BloggingPromptsStore = mock()
    private val accountStore: AccountStore = mock()
    private val reminderNotificationManager: ReminderNotificationManager = mock()
    private val bloggingPromptsFeatureConfig: BloggingPromptsFeatureConfig = mock()
    private val bloggingRemindersStore: BloggingRemindersStore = mock()
    private val bloggingReminder: BloggingRemindersModel = mock()

    private val classToTest = PromptReminderNotifier(
            contextProvider = contextProvider,
            resourceProvider = resourceProvider,
            siteStore = siteStore,
            accountStore = accountStore,
            reminderNotificationManager = reminderNotificationManager,
            bloggingPromptsFeatureConfig = bloggingPromptsFeatureConfig,
            bloggingPromptsStore = bloggingPromptsStore,
            bloggingRemindersStore
    )

    @Before
    fun setUp() {
        whenever(bloggingRemindersStore.bloggingRemindersModel(any())).thenReturn(flowOf(bloggingReminder))
    }

    @Test
    fun `Should NOT notify if hasAccessToken returns FALSE`() = runBlocking {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        assertFalse(classToTest.shouldNotify(123))
    }

    @Test
    fun `Should NOT notify if blogging prompts notification flag isEnabled returns FALSE`() = runBlocking {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(false)
        assertFalse(classToTest.shouldNotify(123))
    }

    @Test
    fun `Should NOT notify if getSiteByLocalId returns NULL`() = runBlocking {
        val siteId = 123
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(null)
        assertFalse(classToTest.shouldNotify(siteId))
    }

    @Test
    fun `Should NOT notify if SiteModel hasOptedInBloggingPromptsReminders returns FALSE`() = runBlocking {
        val siteId = 123
        val siteModel: SiteModel = mock()

        whenever(bloggingReminder.isPromptIncluded).thenReturn(false)
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(siteModel)
        assertFalse(classToTest.shouldNotify(123))
    }

    @Test
    fun `Should notify if has access token, flag is enabled and has opted in for blogging prompts reminders`() =
            runBlocking {
                val siteId = 123
                val siteModel: SiteModel = mock()

                whenever(bloggingReminder.isPromptIncluded).thenReturn(true)
                whenever(accountStore.hasAccessToken()).thenReturn(true)
                whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)
                whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(siteModel)
                assertTrue(classToTest.shouldNotify(siteId))
            }
}
