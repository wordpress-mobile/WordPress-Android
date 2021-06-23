package org.wordpress.android.workers.reminder

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.AccountModel
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
        on { getSiteBySiteId(SITE_ID) } doReturn TEST_SITE
    }
    private val accountStore: AccountStore = mock {
        on { account } doReturn TEST_ACCOUNT
    }
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
        verify(analyticsTracker).setSite(LOCAL_ID)
        verify(analyticsTracker).trackNotificationReceived()
    }

    private companion object {
        private val TEST_SITE = SiteModel().apply {
            id = LOCAL_ID
            siteId = SITE_ID
        }

        private val TEST_ACCOUNT = AccountModel().apply {
            userName = "username"
        }

        private const val LOCAL_ID = 1
        private const val SITE_ID = 1001L
    }
}
