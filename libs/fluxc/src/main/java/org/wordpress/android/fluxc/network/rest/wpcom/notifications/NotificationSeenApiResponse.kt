package org.wordpress.android.fluxc.network.rest.wpcom.notifications

import org.wordpress.android.fluxc.network.Response

@Suppress("VariableNaming")
class NotificationSeenApiResponse : Response {
    val last_seen_time: Long? = null
    val success: Boolean = false
}
