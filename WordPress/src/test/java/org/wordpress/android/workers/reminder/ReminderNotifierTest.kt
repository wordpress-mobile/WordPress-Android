package org.wordpress.android.workers.reminder

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersAnalyticsTracker
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider

@RunWith(MockitoJUnitRunner::class)
class ReminderNotifierTest {
    lateinit var reminderNotifier: ReminderNotifier

    private val contextProvider: ContextProvider = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doReturn ""
        on { getString(any(), any()) } doReturn ""
    }
    private val siteStore: SiteStore = mock {
        on { getSiteByLocalId(SITE_ID) } doReturn TEST_SITE
    }
    private val accountStore: AccountStore = mock()
    private val notificationManager: ReminderNotificationManager = mock()
    private val analyticsTracker: BloggingRemindersAnalyticsTracker = mock()

    @Before
    fun setUp() {
        reminderNotifier = ReminderNotifier(
            contextProvider,
            resourceProvider,
            siteStore,
            accountStore,
            notificationManager,
            analyticsTracker
        )
    }

    @Test
    fun `notify correctly tracks notification received event`() {
        reminderNotifier.notify(SITE_ID)
        verify(analyticsTracker).setSite(SITE_ID)
        verify(analyticsTracker).trackNotificationReceived(false)
    }

    private companion object {
        private val TEST_SITE = SiteModel().apply {
            id = SITE_ID
        }

        private const val SITE_ID = 1
    }
}
