package org.wordpress.android.ui.activitylog.detail

import android.text.Spannable
import android.widget.TextView
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
    val spannableBuilder: (FormattableContent, TextView) -> Spannable,
    val summary: String? = null,
    val createdDate: String = "",
    val createdTime: String = "",
    private val rewindAction: ((ActivityLogDetailModel) -> Unit)
) {
    fun onClick() {
        rewindAction(this)
    }
}
