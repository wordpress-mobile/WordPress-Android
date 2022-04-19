package org.wordpress.android.workers.notification.bloggingprompts

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Test
import org.wordpress.android.R
import org.wordpress.android.workers.notification.local.LocalNotification
import org.wordpress.android.workers.notification.local.LocalNotification.Type.BLOGGING_PROMPTS_ONBOARDING
import org.wordpress.android.workers.notification.local.LocalNotificationScheduler
import java.util.concurrent.TimeUnit.MILLISECONDS

class BloggingPromptsOnboardingNotificationSchedulerTest {
    private val localnotificationScheduler: LocalNotificationScheduler = mock()
    private val bloggingPromptsOnboardingNotificationHandler: BloggingPromptsOnboardingNotificationHandler = mock()
    private val classToTest = BloggingPromptsOnboardingNotificationScheduler(
            localnotificationScheduler, bloggingPromptsOnboardingNotificationHandler
    )

    @Test
    fun `Should schedule the correct notification if should show notification`() {
        whenever(bloggingPromptsOnboardingNotificationHandler.shouldShowNotification()).thenReturn(true)
        val expectedLocalNotification = LocalNotification(
                type = BLOGGING_PROMPTS_ONBOARDING,
                delay = 3000,
                delayUnits = MILLISECONDS,
                title = R.string.blogging_prompts_onboarding_notification_title,
                text = R.string.blogging_prompts_onboarding_notification_text,
                icon = R.drawable.ic_wordpress_white_24dp,
                actionIcon = -1,
                actionTitle = R.string.blogging_prompts_onboarding_notification_action
        )
        classToTest.scheduleBloggingPromptsOnboardingNotificationIfNeeded()
        verify(localnotificationScheduler).scheduleOneTimeNotification(expectedLocalNotification)
    }

    @Test
    fun `Should NOT schedule the notification if should NOT show notification`() {
        whenever(bloggingPromptsOnboardingNotificationHandler.shouldShowNotification()).thenReturn(false)
        classToTest.scheduleBloggingPromptsOnboardingNotificationIfNeeded()
        verifyZeroInteractions(localnotificationScheduler)
    }

    @Test
    fun `Should cancel scheduled notification when cancelBloggingPromptsOnboardingNotification is called`() {
        classToTest.cancelBloggingPromptsOnboardingNotification()
        verify(localnotificationScheduler).cancelScheduledNotification(BLOGGING_PROMPTS_ONBOARDING)
    }
}
