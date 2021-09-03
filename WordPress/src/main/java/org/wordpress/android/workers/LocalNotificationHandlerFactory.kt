package org.wordpress.android.workers

import android.content.Context
import android.content.Intent
import org.wordpress.android.workers.LocalNotification.Type
import org.wordpress.android.workers.LocalNotification.Type.CREATE_SITE
import javax.inject.Inject

class LocalNotificationHandlerFactory @Inject constructor(
    private val createSiteNotificationHandler: CreateSiteNotificationHandler
) {
    fun buildLocalNotificationHandler(type: Type): LocalNotificationHandler {
        return when (type) {
            CREATE_SITE -> createSiteNotificationHandler
        }
    }
}

interface LocalNotificationHandler {
    fun shouldShowNotification(): Boolean
    fun buildIntent(context: Context): Intent
    fun onNotificationShown()
}
