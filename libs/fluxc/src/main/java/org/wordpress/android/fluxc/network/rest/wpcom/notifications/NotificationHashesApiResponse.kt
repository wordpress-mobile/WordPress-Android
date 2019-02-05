package org.wordpress.android.fluxc.network.rest.wpcom.notifications

import org.wordpress.android.fluxc.network.Response

class NotificationHashesApiResponse : Response {
    val last_seen_time: Long? = null
    val number: Int? = null
    val notes: List<NotificationHashApiResponse>? = null
}
