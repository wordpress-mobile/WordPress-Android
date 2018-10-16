package org.wordpress.android.ui.activitylog.detail

import org.wordpress.android.fluxc.tools.FormattableContent

data class ActivityLogDetailModel(
    val activityID: String,
    val rewindId: String?,
    val actorIconUrl: String? = null,
    val showJetpackIcon: Boolean? = null,
    val isRewindButtonVisible: Boolean,
    val actorName: String? = null,
    val actorRole: String? = null,
    val content: FormattableContent? = null,
    val summary: String? = null,
    val createdDate: String = "",
    val createdTime: String = ""
)
