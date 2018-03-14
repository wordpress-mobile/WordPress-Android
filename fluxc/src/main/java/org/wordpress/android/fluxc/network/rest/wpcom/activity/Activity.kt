package org.wordpress.android.fluxc.network.rest.wpcom.activity

import org.wordpress.android.fluxc.store.Store
import java.util.Date

data class Activity(val activityID: String,
                    val summary: String,
                    val text: String,
                    val name: String?,
                    val type: String?,
                    val gridicon: String?,
                    val status: String?,
                    val rewindable: Boolean?,
                    val rewindID: Float?,
                    val published: Date,
                    val isDiscarded: Boolean?,
                    val actor: ActivityActor? = null) {

    enum class Status(value: String) {
        ERROR("error"), SUCCESS("success"), WARNING("warning");
    }

    data class ActivityActor(val displayName: String?,
                             val type: String?,
                             val wpcomUserID: Long?,
                             val avatarURL: String?,
                             val role: String?) {
        val isJetpack = { type == "Application" && displayName == "Jetpack" }
    }

    enum class ActivityErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE,
        MISSING_ACTIVITY_ID,
        MISSING_SUMMARY,
        MISSING_CONTENT_TEXT,
        MISSING_PUBLISHED_DATE
    }

    data class ActivityError(var type: ActivityErrorType, var message: String? = null) : Store.OnChangedError
}
