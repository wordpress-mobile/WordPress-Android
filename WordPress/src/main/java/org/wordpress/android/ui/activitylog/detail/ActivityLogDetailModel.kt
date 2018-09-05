package org.wordpress.android.ui.activitylog.detail

import android.text.Spannable

data class ActivityLogDetailModel(
    val activityID: String,
    val rewindId: String?,
    val actorIconUrl: String? = null,
    val showJetpackIcon: Boolean? = null,
    val isRewindButtonVisible: Boolean,
    val actorName: String? = null,
    val actorRole: String? = null,
    val content: Spannable? = null,
    val summary: String? = null,
    val createdDate: String = "",
    val createdTime: String = "",
    private val rewindAction: ((ActivityLogDetailModel) -> Unit)
) {
    fun onClick() {
        rewindAction(this)
    }
}
