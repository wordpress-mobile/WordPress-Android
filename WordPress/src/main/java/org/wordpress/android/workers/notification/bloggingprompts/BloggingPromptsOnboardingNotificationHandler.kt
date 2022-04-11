package org.wordpress.android.workers.notification.bloggingprompts

import android.content.Context
import android.content.Intent
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.push.NotificationType.BLOGGING_PROMPTS_ONBOARDING
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.notifications.SystemNotificationsTracker
import org.wordpress.android.workers.notification.local.LocalNotificationHandler
import javax.inject.Inject

class BloggingPromptsOnboardingNotificationHandler @Inject constructor(
    private val accountStore: AccountStore,
    private val notificationsTracker: SystemNotificationsTracker
) : LocalNotificationHandler {
    // TODO @RenanLukas update with show notification business rule
    override fun shouldShowNotification(): Boolean {
        return accountStore.hasAccessToken()
    }

    override fun buildIntent(context: Context): Intent {
        return ActivityLauncher.createMainActivityAndShowBloggingPromptsOnboardingActivityIntent(
                context, BLOGGING_PROMPTS_ONBOARDING
        )
    }

    override fun onNotificationShown() {
        notificationsTracker.trackShownNotification(BLOGGING_PROMPTS_ONBOARDING)
    }
}
