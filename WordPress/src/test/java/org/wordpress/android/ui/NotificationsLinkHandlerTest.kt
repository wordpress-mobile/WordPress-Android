package org.wordpress.android.ui

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenNotifications

@RunWith(MockitoJUnitRunner::class)
class NotificationsLinkHandlerTest {
    private val notificationsLinkHandler = NotificationsLinkHandler()

    @Test
    fun `handles WPCom notifications URL`() {
        val uri = buildUri("wordpress.com", "notifications")

        val isHandled = notificationsLinkHandler.isNotificationsUrl(uri)

        assertThat(isHandled).isTrue()
    }

    @Test
    fun `does not handle WPCom non-notifications URL`() {
        val uri = buildUri("wordpress.com", "stats")

        val isHandled = notificationsLinkHandler.isNotificationsUrl(uri)

        assertThat(isHandled).isFalse()
    }

    @Test
    fun `does not handle non-WPCom notifications URL`() {
        val uri = buildUri("wordpress.org", "notifications")

        val isHandled = notificationsLinkHandler.isNotificationsUrl(uri)

        assertThat(isHandled).isFalse()
    }

    @Test
    fun `builds notifications navigate action`() {
        val navigateAction = notificationsLinkHandler.buildNavigateAction()

        assertThat(navigateAction).isEqualTo(OpenNotifications)
    }
}
