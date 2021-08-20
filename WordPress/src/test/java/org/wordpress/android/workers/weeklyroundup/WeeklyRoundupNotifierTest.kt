package org.wordpress.android.workers.weeklyroundup

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.push.NotificationType.WEEKLY_ROUNDUP
import org.wordpress.android.ui.notifications.SystemNotificationsTracker
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider

@RunWith(MockitoJUnitRunner::class)
class WeeklyRoundupNotifierTest {
    private lateinit var weeklyRoundupNotifier: WeeklyRoundupNotifier

    private val accountStore: AccountStore = mock()
    private val siteStore: SiteStore = mock()
    private val contextProvider: ContextProvider = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val weeklyRoundupScheduler: WeeklyRoundupScheduler = mock()
    private val notificationsTracker: SystemNotificationsTracker = mock()
    private val siteUtils: SiteUtilsWrapper = mock()
    private val weeklyRoundupRepository: WeeklyRoundupRepository = mock()

    @Before
    fun setUp() {
        weeklyRoundupNotifier = WeeklyRoundupNotifier(
                accountStore,
                siteStore,
                contextProvider,
                resourceProvider,
                weeklyRoundupScheduler,
                notificationsTracker,
                siteUtils,
                weeklyRoundupRepository
        )
    }

    @Test
    fun `should not show notification when the user is logged out`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        assertThat(weeklyRoundupNotifier.shouldShowNotifications()).isFalse
    }

    @Test
    fun `should not show notification when the user has no sites`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(siteStore.hasSitesAccessedViaWPComRest()).thenReturn(false)

        assertThat(weeklyRoundupNotifier.shouldShowNotifications()).isFalse
    }

    @Test
    fun `should show notification when the user is logged in and has sites`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(siteStore.hasSitesAccessedViaWPComRest()).thenReturn(true)

        assertThat(weeklyRoundupNotifier.shouldShowNotifications()).isTrue
    }

    @Test
    fun `should track notification shown once for each notification`() {
        val numberOfNotifications = 5

        val notification: WeeklyRoundupNotification = mock()
        val notifications = (1..numberOfNotifications).map { notification }

        weeklyRoundupNotifier.onNotificationsShown(notifications)

        verify(notificationsTracker, times(numberOfNotifications)).trackShownNotification(WEEKLY_ROUNDUP)
    }
}
