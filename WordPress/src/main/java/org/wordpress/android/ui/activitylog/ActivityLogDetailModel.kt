package org.wordpress.android.ui.activitylog

data class ActivityLogDetailModel(
    val activityID: String,
    val actorIconUrl: String? = null,
    val actorName: String? = null,
    val actorRole: String? = null,
    val text: String? = null,
    val summary: String? = null,
    val createdDate: String = "",
    val createdTime: String = "",
    val rewindAction: (() -> Unit)? = null
)
