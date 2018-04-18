package org.wordpress.android.fluxc.model.activity

import java.util.Date

data class ActivityLogModel(
    val activityID: String,
    val summary: String,
    val text: String,
    val name: String?,
    val type: String?,
    val gridicon: String?,
    val status: String?,
    val rewindable: Boolean?,
    val rewindID: String?,
    val published: Date,
    val discarded: Boolean?,
    val actor: ActivityActor? = null
) {
    enum class Status(value: String) {
        ERROR("error"), SUCCESS("success"), WARNING("warning");
    }

    data class ActivityActor(
        val displayName: String?,
        val type: String?,
        val wpcomUserID: Long?,
        val avatarURL: String?,
        val role: String?
    ) {
        val isJetpack = { type == "Application" && displayName == "Jetpack" }
    }
}
