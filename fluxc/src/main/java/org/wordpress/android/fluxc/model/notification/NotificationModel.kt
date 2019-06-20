package org.wordpress.android.fluxc.model.notification

import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.tools.FormattableMeta
import java.util.Locale

data class NotificationModel(
    val noteId: Int = 0,
    val remoteNoteId: Long = 0L,
    var localSiteId: Int = 0,
    var noteHash: Long = 0L,
    val type: Kind = Kind.UNKNOWN,
    val subtype: Subkind? = Subkind.NONE,
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
            private val reverseMap = values().associateBy(
                    Kind::name)
            fun fromString(type: String) = reverseMap[type.toUpperCase(Locale.US)] ?: UNKNOWN
        }
    }

    enum class Subkind {
        STORE_REVIEW,
        REWIND_BACKUP_INITIAL,
        UNKNOWN,
        NONE;

        companion object {
            private val reverseMap = values().associateBy(
                    Subkind::name)
            fun fromString(type: String): Subkind {
                return if (type.isEmpty()) {
                    NONE
                } else {
                    reverseMap[type.toUpperCase(Locale.US)] ?: UNKNOWN
                }
            }
        }
    }

    fun getRemoteSiteId(): Long? = meta?.ids?.site

    fun toLogString(): String {
        return "[id=$noteId, remoteNoteId=$remoteNoteId, read=$read, " +
                "localSiteId=$localSiteId, type=${type.name}, subtype=${subtype?.name}, title=$title]"
    }
}
