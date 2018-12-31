package org.wordpress.android.fluxc.network.rest.wpcom.notifications

import org.wordpress.android.fluxc.network.Response

/**
 * The response to fetching only the `id` and `note_hash` from the remote api endpoint
 */
class NotificationHashApiResponse : Response {
    var id: Long = 0
    var note_hash: Long = 0
}
