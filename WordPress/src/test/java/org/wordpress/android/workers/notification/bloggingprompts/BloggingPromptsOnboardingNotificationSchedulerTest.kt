package org.wordpress.android.workers.notification.bloggingprompts

import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import org.wordpress.android.util.config.BloggingPromptsFeatureConfig
import org.wordpress.android.workers.notification.local.LocalNotification
import org.wordpress.android.workers.notification.local.LocalNotification.Type.BLOGGING_PROMPTS_ONBOARDING
import org.wordpress.android.workers.notification.local.LocalNotificationScheduler
import java.util.concurrent.TimeUnit.MILLISECONDS

class BloggingPromptsOnboardingNotificationSchedulerTest {
    private val localnotificationScheduler: LocalNotificationScheduler = mock()
    private val bloggingPromptsOnboardingNotificationHandler: BloggingPromptsOnboardingNotificationHandler = mock()
    private val bloggingPromptsFeatureConfig: BloggingPromptsFeatureConfig = mock()
    private val classToTest = BloggingPromptsOnboardingNotificationScheduler(
        localnotificationScheduler, bloggingPromptsOnboardingNotificationHandler, bloggingPromptsFeatureConfig
    )

    @Test
    fun `Should schedule the correct notification if should show notification`() {
        whenever(bloggingPromptsOnboardingNotificationHandler.shouldShowNotification()).thenReturn(true)
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)
        val expectedLocalNotification = LocalNotification(
            type = BLOGGING_PROMPTS_ONBOARDING,
            delay = 3000,
            delayUnits = MILLISECONDS,
            title = R.string.blogging_prompts_onboarding_notification_title,
            text = R.string.blogging_prompts_onboarding_notification_text,
            icon = R.drawable.ic_wordpress_white_24dp,
            firstActionIcon = -1,
            firstActionTitle = R.string.blogging_prompts_onboarding_notification_action,
            secondActionIcon = -1,
            secondActionTitle = R.string.blogging_prompts_notification_dismiss
        )
        classToTest.scheduleBloggingPromptsOnboardingNotificationIfNeeded()
        verify(localnotificationScheduler).scheduleOneTimeNotification(expectedLocalNotification)
    }

    @Test
    fun `Should NOT schedule the notification if should NOT show notification`() {
        whenever(bloggingPromptsOnboardingNotificationHandler.shouldShowNotification()).thenReturn(false)
        classToTest.scheduleBloggingPromptsOnboardingNotificationIfNeeded()
        verifyNoInteractions(localnotificationScheduler)
    }

    @Test
    fun `Should NOT schedule the notification if feature flag is NOT enable`() {
        whenever(bloggingPromptsOnboardingNotificationHandler.shouldShowNotification()).thenReturn(true)
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(false)
        classToTest.scheduleBloggingPromptsOnboardingNotificationIfNeeded()
        verifyNoInteractions(localnotificationScheduler)
    }

    @Test
    fun `Should cancel scheduled notification when cancelBloggingPromptsOnboardingNotification is called`() {
        classToTest.cancelBloggingPromptsOnboardingNotification()
        verify(localnotificationScheduler).cancelScheduledNotification(BLOGGING_PROMPTS_ONBOARDING)
    }
}
