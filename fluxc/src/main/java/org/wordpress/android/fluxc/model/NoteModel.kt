package org.wordpress.android.fluxc.model

import org.wordpress.android.fluxc.tools.FormattableContent

data class NoteModel(
    val noteId: Int,
    val remoteNoteId: Long,
    val localSiteId: Int,
    val noteHash: Long,
    val type: Kind,
    val subtype: Subkind?,
    val read: Boolean,
    val icon: String?,
    val noticon: String?,
    val timestamp: String?,
    val url: String?,
    val title: String?,
    val body: FormattableContent?,
    val subject: FormattableContent?,
    val meta: FormattableContent?
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
        UNKNOWN
    }

    enum class Subkind {
        STORE_REVIEW
    }
}
