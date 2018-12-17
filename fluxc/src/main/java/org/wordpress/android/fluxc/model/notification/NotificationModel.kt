package org.wordpress.android.fluxc.model.notification

import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.tools.FormattableMeta
import java.util.Locale

data class NotificationModel(
    val noteId: Int,
    val remoteNoteId: Long,
    var localSiteId: Int,
    val noteHash: Long,
    val type: Kind,
    val subtype: Subkind?,
    val read: Boolean,
    val icon: String?,
    val noticon: String?,
    val timestamp: String?,
    val url: String?,
    val title: String?,
    val body: List<FormattableContent>?,
    val subject: List<FormattableContent>?,
    val meta: FormattableMeta?
) {
    enum class Kind {
        AUTOMATTCHER,
        COMMENT,
        COMMENT_LIKE,
        FOLLOW,
        LIKE,
        NEW_POST,
        POST,
        STORE_ORDER,
        USER,
        REWIND_BACKUP_INITIAL,
        PLAN_SETUP_NUDGE,
        UNKNOWN;

        companion object {
            private val reverseMap = Kind.values().associateBy(
                    Kind::name)
            fun fromString(type: String) = reverseMap[type.toUpperCase(Locale.US)] ?: UNKNOWN
        }
    }

    enum class Subkind {
        STORE_REVIEW,
        REWIND_BACKUP_INITIAL,
        UNKNOWN;

        companion object {
            private val reverseMap = Subkind.values().associateBy(
                    Subkind::name)
            fun fromString(type: String) = reverseMap[type.toUpperCase(Locale.US)] ?: UNKNOWN
        }
    }

    fun getRemoteSiteId(): Long? = meta?.ids?.site
}
