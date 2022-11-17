package org.wordpress.android.ui.deeplinks.handlers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenNotifications
import org.wordpress.android.ui.deeplinks.buildUri

@RunWith(MockitoJUnitRunner::class)
class NotificationsLinkHandlerTest {
    private val notificationsLinkHandler = NotificationsLinkHandler()

    @Test
    fun `handles WPCom notifications URL`() {
        val uri = buildUri(host = "wordpress.com", "notifications")

        val isHandled = notificationsLinkHandler.shouldHandleUrl(uri)

        assertThat(isHandled).isTrue()
    }

    @Test
    fun `handles notifications app link`() {
        val uri = buildUri(host = "notifications")

        val isHandled = notificationsLinkHandler.shouldHandleUrl(uri)

        assertThat(isHandled).isTrue()
    }

    @Test
    fun `does not handle WPCom non-notifications URL`() {
        val uri = buildUri(host = "wordpress.com", "stats")

        val isHandled = notificationsLinkHandler.shouldHandleUrl(uri)

        assertThat(isHandled).isFalse()
    }

    @Test
    fun `does not handle non-WPCom notifications URL`() {
        val uri = buildUri(host = "wordpress.org", "notifications")

        val isHandled = notificationsLinkHandler.shouldHandleUrl(uri)

        assertThat(isHandled).isFalse()
    }

    @Test
    fun `builds notifications navigate action`() {
        val navigateAction = notificationsLinkHandler.buildNavigateAction(mock())

        assertThat(navigateAction).isEqualTo(OpenNotifications)
    }

    @Test
    fun `deeplink - strip url returns notifications url`() {
        val uri = buildUri(host = "wordpress.com", "notifications")

        val strippedUrl = notificationsLinkHandler.stripUrl(uri)

        assertThat(strippedUrl).isEqualTo("wordpress.com/notifications")
    }

    @Test
    fun `applink - strip url returns notifications url`() {
        val uri = buildUri(host = "notifications")

        val strippedUrl = notificationsLinkHandler.stripUrl(uri)

        assertThat(strippedUrl).isEqualTo("wordpress://notifications")
    }
}
