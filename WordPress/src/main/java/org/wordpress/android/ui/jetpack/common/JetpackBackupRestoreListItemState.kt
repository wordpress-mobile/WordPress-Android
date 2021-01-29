package org.wordpress.android.ui.jetpack.common

import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.annotation.StringRes
import org.wordpress.android.R.color
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.viewmodel.ResourceProvider

sealed class JetpackBackupRestoreListItemState(override val type: ViewType) : JetpackListItemState(type) {
    data class SubHeaderState(val text: UiString) :
            JetpackBackupRestoreListItemState(ViewType.BACKUP_RESTORE_SUB_HEADER)

    data class FootnoteState(val text: UiString) :
            JetpackListItemState(ViewType.BACKUP_RESTORE_FOOTNOTE)
}

fun buildSpannableLabel(
    resourceProvider: ResourceProvider,
    @StringRes labelRes: Int,
    @StringRes labelHintRes: Int?
): SpannableString? {
    val labelText = resourceProvider.getString(labelRes)
    if (labelHintRes == null) {
        return null
    }
    val labelHintText = resourceProvider.getString(labelHintRes)
    val spannable = SpannableString(labelHintText)
    spannable.setSpan(
            ForegroundColorSpan(resourceProvider.getColor(color.neutral_40)),
            0,
            labelHintText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    return SpannableString(SpannableStringBuilder().append(labelText).append(" ").append(spannable))
}
