package org.wordpress.android.workers.notification.bloggingprompts

import org.wordpress.android.R
import org.wordpress.android.workers.notification.local.LocalNotification
import org.wordpress.android.workers.notification.local.LocalNotification.Type.BLOGGING_PROMPTS_ONBOARDING
import org.wordpress.android.workers.notification.local.LocalNotificationScheduler
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

class BloggingPromptsOnboardingNotificationScheduler
@Inject constructor(
    private val localNotificationScheduler: LocalNotificationScheduler,
    private val bloggingPromptsOnboardingNotificationHandler: BloggingPromptsOnboardingNotificationHandler
) {
    // TODO @RenanLukas: if we have a local notification for the blogging prompts onboarding, we should track it
    fun scheduleBloggingPromptsOnboardingNotificationIfNeeded() {
        // TODO @RenanLukas: verify if we should also check AppPrefs
        if (bloggingPromptsOnboardingNotificationHandler.shouldShowNotification()) {
            val firstNotification = LocalNotification(
                    type = BLOGGING_PROMPTS_ONBOARDING,
                    delay = 3000, // TODO @RenanLukas replace with real delay
                    delayUnits = MILLISECONDS,
                    title = R.string.blogging_prompts_onboarding_notification_title,
                    text = R.string.blogging_prompts_onboarding_notification_text,
                    icon = R.drawable.ic_wordpress_white_24dp,
                    actionIcon = -1,
                    actionTitle = R.string.blogging_prompts_onboarding_notification_action
            )
            localNotificationScheduler.scheduleOneTimeNotification(firstNotification)
        }
    }

    fun cancelBloggingPromptsOnboardingNotification() {
        localNotificationScheduler.cancelScheduledNotification(BLOGGING_PROMPTS_ONBOARDING)
    }
}
