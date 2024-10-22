package org.wordpress.android.fluxc.notifications

import com.google.gson.Gson
import org.wordpress.android.fluxc.network.rest.wpcom.notifications.NotificationApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.notifications.NotificationsApiResponse

object NotificationTestUtils {
    fun parseNotificationsApiResponseFromJsonString(json: String) =
            Gson().fromJson(json, NotificationsApiResponse::class.java)

    fun parseNotificationApiResponseFromJsonString(json: String) =
            Gson().fromJson(json, NotificationApiResponse::class.java)
}
