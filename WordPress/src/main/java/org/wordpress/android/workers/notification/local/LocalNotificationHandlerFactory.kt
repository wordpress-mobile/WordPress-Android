package org.wordpress.android.workers.notification.local

import android.app.PendingIntent
import android.content.Context
import org.wordpress.android.workers.notification.bloggingprompts.BloggingPromptsOnboardingNotificationHandler
import org.wordpress.android.workers.notification.createsite.CreateSiteNotificationHandler
import org.wordpress.android.workers.notification.local.LocalNotification.Type
import org.wordpress.android.workers.notification.local.LocalNotification.Type.BLOGGING_PROMPTS_ONBOARDING
import org.wordpress.android.workers.notification.local.LocalNotification.Type.CREATE_SITE
import javax.inject.Inject

class LocalNotificationHandlerFactory @Inject constructor(
    private val createSiteNotificationHandler: CreateSiteNotificationHandler,
    private val bloggingPromptsOnboardingNotificationHandler: BloggingPromptsOnboardingNotificationHandler
) {
    fun buildLocalNotificationHandler(type: Type): LocalNotificationHandler {
        return when (type) {
            CREATE_SITE -> createSiteNotificationHandler
            BLOGGING_PROMPTS_ONBOARDING -> bloggingPromptsOnboardingNotificationHandler
        }
    }
}

interface LocalNotificationHandler {
    fun shouldShowNotification(): Boolean
    fun buildFirstActionPendingIntent(context: Context, notificationId: Int): PendingIntent
    fun buildSecondActionPendingIntent(context: Context, notificationId: Int): PendingIntent? {
        return null
    }

    fun onNotificationShown()
}
