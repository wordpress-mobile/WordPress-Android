package org.wordpress.android.ui.deeplinks

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenNotifications

@RunWith(MockitoJUnitRunner::class)
class NotificationsLinkHandlerTest {
    private val notificationsLinkHandler = NotificationsLinkHandler()

    @Test
    fun `handles WPCom notifications URL`() {
        val uri = buildUri(host = "wordpress.com", path1 = "notifications")

        val isHandled = notificationsLinkHandler.isNotificationsUrl(uri)

        assertThat(isHandled).isTrue()
    }

    @Test
    fun `handles notifications app link`() {
        val uri = buildUri(host = "notifications")

        val isHandled = notificationsLinkHandler.isNotificationsUrl(uri)

        assertThat(isHandled).isTrue()
    }

    @Test
    fun `does not handle WPCom non-notifications URL`() {
        val uri = buildUri(host = "wordpress.com", path1 = "stats")

        val isHandled = notificationsLinkHandler.isNotificationsUrl(uri)

        assertThat(isHandled).isFalse()
    }

    @Test
    fun `does not handle non-WPCom notifications URL`() {
        val uri = buildUri(host = "wordpress.org", path1 = "notifications")

        val isHandled = notificationsLinkHandler.isNotificationsUrl(uri)

        assertThat(isHandled).isFalse()
    }

    @Test
    fun `builds notifications navigate action`() {
        val navigateAction = notificationsLinkHandler.buildNavigateAction()

        assertThat(navigateAction).isEqualTo(OpenNotifications)
    }
}
