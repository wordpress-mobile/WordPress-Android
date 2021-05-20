package org.wordpress.android.workers

import android.content.Context
import android.content.Intent
import org.wordpress.android.workers.LocalPush.Type
import org.wordpress.android.workers.LocalPush.Type.CREATE_SITE
import javax.inject.Inject

class LocalPushHandlerFactory @Inject constructor(
    private val createSitePushHandler: CreateSitePushHandler
) {
    fun buildLocalPushHandler(type: Type): LocalPushHandler {
        return when (type) {
            CREATE_SITE -> createSitePushHandler
        }
    }
}

interface LocalPushHandler {
    fun shouldShowNotification(): Boolean
    fun buildIntent(context: Context): Intent
}
