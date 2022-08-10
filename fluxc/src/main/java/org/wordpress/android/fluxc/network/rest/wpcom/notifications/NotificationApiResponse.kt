package org.wordpress.android.fluxc.network.rest.wpcom.notifications

import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.tools.FormattableMeta

@Suppress("VariableNaming")
class NotificationApiResponse : Response {
    val id: Long? = null
    val type: String? = null
    val subtype: String? = null
    val read: Int? = null
    val note_hash: Long? = null
    val noticon: String? = null
    val timestamp: String? = null
    val icon: String? = null
    val url: String? = null
    val title: String? = null
    val subject: List<FormattableContent>? = null
    val body: List<FormattableContent>? = null
    val meta: FormattableMeta? = null

    companion object {
        fun notificationResponseToNotificationModel(
            response: NotificationApiResponse
        ): NotificationModel {
            val noteType = response.type?.let {
                NotificationModel.Kind.fromString(response.type)
            } ?: NotificationModel.Kind.UNKNOWN
            val noteSubType = response.subtype?.let {
                NotificationModel.Subkind.fromString(response.subtype)
            }
            val isRead = response.read?.let { it == 1 } ?: false

            return NotificationModel(
                    noteId = 0,
                    remoteNoteId = response.id ?: 0,
                    remoteSiteId = response.meta?.ids?.site ?: 0L,
                    noteHash = response.note_hash ?: 0L,
                    type = noteType,
                    subtype = noteSubType,
                    read = isRead,
                    icon = response.icon,
                    noticon = response.noticon,
                    timestamp = response.timestamp,
                    url = response.url,
                    title = response.title,
                    body = response.body,
                    subject = response.subject,
                    meta = response.meta
            )
        }

        fun getRemoteSiteId(response: NotificationApiResponse): Long? = response.meta?.ids?.site
    }
}
