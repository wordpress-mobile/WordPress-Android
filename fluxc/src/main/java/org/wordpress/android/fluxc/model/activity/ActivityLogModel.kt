package org.wordpress.android.fluxc.model.activity

import org.wordpress.android.fluxc.tools.FormattableContent
import java.util.Date

data class ActivityLogModel(
    val activityID: String,
    val summary: String,
    val content: FormattableContent?,
    val name: String?,
    val type: String?,
    val gridicon: String?,
    val status: String?,
    val rewindable: Boolean?,
    val rewindID: String?,
    val published: Date,
    val actor: ActivityActor? = null
) {
    data class ActivityActor(
        val displayName: String?,
        val type: String?,
        val wpcomUserID: Long?,
        val avatarURL: String?,
        val role: String?
    )
}
