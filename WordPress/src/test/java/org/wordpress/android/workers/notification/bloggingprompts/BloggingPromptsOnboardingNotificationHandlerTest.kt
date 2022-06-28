package org.wordpress.android.workers.notification.bloggingprompts

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.push.NotificationType.BLOGGING_PROMPTS_ONBOARDING
import org.wordpress.android.ui.notifications.SystemNotificationsTracker

class BloggingPromptsOnboardingNotificationHandlerTest {
    private val accountStore: AccountStore = mock()
    private val systemNotificationsTracker: SystemNotificationsTracker = mock()
    private val classToTest = BloggingPromptsOnboardingNotificationHandler(accountStore, systemNotificationsTracker)

    @Test
    fun `Should show notification if user has access token`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        val actual = classToTest.shouldShowNotification()
        val expected = true
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should NOT show notification if user has access token`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        val actual = classToTest.shouldShowNotification()
        val expected = false
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should track notification shown when onNotificationShown is called`() {
        classToTest.onNotificationShown()
        verify(systemNotificationsTracker).trackShownNotification(BLOGGING_PROMPTS_ONBOARDING)
    }
}
