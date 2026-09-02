package org.wordpress.android.ui.accounts.login

import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

private const val SITE_URL = "https://selfhosted.example.com"

class ApplicationPasswordReauthNotifierTest {
    private val notifier = ApplicationPasswordReauthNotifier()

    @Test
    fun `notifies a registered listener`() {
        val listener = mock<ApplicationPasswordReauthNotifier.Listener>()
        notifier.addListener(listener)

        notifier.notifyReauthRequired(SITE_URL)

        verify(listener).onReauthRequired(SITE_URL)
    }

    @Test
    fun `does not notify a removed listener`() {
        val listener = mock<ApplicationPasswordReauthNotifier.Listener>()
        notifier.addListener(listener)
        notifier.removeListener(listener)

        notifier.notifyReauthRequired(SITE_URL)

        verify(listener, never()).onReauthRequired(SITE_URL)
    }
}
