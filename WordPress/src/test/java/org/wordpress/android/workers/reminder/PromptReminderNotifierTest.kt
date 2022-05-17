package org.wordpress.android.workers.reminder

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.config.BloggingPromptsFeatureConfig
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.workers.reminder.prompt.PromptReminderNotifier

class PromptReminderNotifierTest {
    private val contextProvider: ContextProvider = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val siteStore: SiteStore = mock()
    private val accountStore: AccountStore = mock()
    private val reminderNotificationManager: ReminderNotificationManager = mock()
    private val bloggingPromptsFeatureConfig: BloggingPromptsFeatureConfig = mock()

    private val classToTest = PromptReminderNotifier(
            contextProvider = contextProvider,
            resourceProvider = resourceProvider,
            siteStore = siteStore,
            accountStore = accountStore,
            reminderNotificationManager = reminderNotificationManager,
            bloggingPromptsFeatureConfig = bloggingPromptsFeatureConfig
    )

    @Test
    fun `Should NOT notify if hasAccessToken returns FALSE`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        assertFalse(classToTest.shouldNotify(123))
    }

    @Test
    fun `Should NOT notify if blogging prompts notification flag isEnabled returns FALSE`() {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(false)
        assertFalse(classToTest.shouldNotify(123))
    }

    @Test
    fun `Should NOT notify if getSiteByLocalId returns NULL`() {
        val siteId = 123
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(null)
        assertFalse(classToTest.shouldNotify(siteId))
    }

    @Test
    fun `Should NOT notify if SiteModel hasOptedInBloggingPromptsReminders returns FALSE`() {
        val siteId = 123
        val siteModel: SiteModel = mock()
        classToTest.hasOptedInBloggingPromptsReminders = false
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(siteModel)
        assertFalse(classToTest.shouldNotify(123))
    }

    @Test
    fun `Should notify if has access token, flag is enabled and has opted in for blogging prompts reminders`() {
        val siteId = 123
        val siteModel: SiteModel = mock()
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)
        classToTest.hasOptedInBloggingPromptsReminders = true
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(siteModel)
        assertTrue(classToTest.shouldNotify(siteId))
    }
}
