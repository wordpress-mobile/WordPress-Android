package org.wordpress.android.ui.activitylog.detail

data class ActivityLogDetailModel(
    val activityID: String,
    val rewindId: String?,
    val actorIconUrl: String? = null,
    val showJetpackIcon: Boolean? = null,
    val isRewindButtonVisible: Boolean,
    val actorName: String? = null,
    val actorRole: String? = null,
    val text: String? = null,
    val summary: String? = null,
    val createdDate: String = "",
    val createdTime: String = "",
    val rewindAction: (() -> Unit)
)
