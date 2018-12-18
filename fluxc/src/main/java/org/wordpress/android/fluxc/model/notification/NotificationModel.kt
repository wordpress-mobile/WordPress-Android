package org.wordpress.android.fluxc.model.notification

import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.tools.FormattableMeta
import java.util.Locale

data class NotificationModel(
    val noteId: Int = 0,
    val remoteNoteId: Long = 0L,
    var localSiteId: Int = 0,
    val noteHash: Long = 0L,
    val type: Kind = NotificationModel.Kind.UNKNOWN,
    val subtype: Subkind? = null,
    var read: Boolean = false,
    val icon: String? = null,
    val noticon: String? = null,
    val timestamp: String? = null,
    val url: String? = null,
    val title: String? = null,
    val body: List<FormattableContent>? = null,
    val subject: List<FormattableContent>? = null,
    val meta: FormattableMeta? = null
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
