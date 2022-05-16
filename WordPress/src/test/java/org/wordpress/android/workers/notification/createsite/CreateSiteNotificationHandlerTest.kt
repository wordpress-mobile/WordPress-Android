package org.wordpress.android.workers.notification.createsite

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.push.NotificationType.CREATE_SITE
import org.wordpress.android.ui.notifications.SystemNotificationsTracker

@RunWith(MockitoJUnitRunner::class)
class CreateSiteNotificationHandlerTest {
    private lateinit var createSiteNotificationHandler: CreateSiteNotificationHandler

    private val accountStore: AccountStore = mock()
    private val siteStore: SiteStore = mock()
    private val notificationsTracker: SystemNotificationsTracker = mock()

    @Before
    fun setUp() {
        createSiteNotificationHandler = CreateSiteNotificationHandler(
                accountStore,
                siteStore,
                notificationsTracker
        )
    }

    @Test
    fun `should not show notification when the user is logged out`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        assertThat(createSiteNotificationHandler.shouldShowNotification()).isFalse
    }

    @Test
    fun `should not show notification when the user has sites`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(siteStore.hasSite()).thenReturn(true)

        assertThat(createSiteNotificationHandler.shouldShowNotification()).isFalse
    }

    @Test
    fun `should show notification when the user is logged in and has no sites`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(siteStore.hasSite()).thenReturn(false)

        assertThat(createSiteNotificationHandler.shouldShowNotification()).isTrue
    }

    @Test
    fun `should track notification shown`() {
        createSiteNotificationHandler.onNotificationShown()

        verify(notificationsTracker).trackShownNotification(CREATE_SITE)
    }
}
