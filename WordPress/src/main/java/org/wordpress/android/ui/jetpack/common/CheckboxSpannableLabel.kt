package org.wordpress.android.ui.jetpack.common

import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.annotation.StringRes
import org.wordpress.android.R.color
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class CheckboxSpannableLabel @Inject constructor(
    private val resourceProvider: ResourceProvider
) {
    fun buildSpannableLabel(
        @StringRes labelRes: Int,
        @StringRes labelHintRes: Int?
    ): CharSequence? {
        val labelText = resourceProvider.getString(labelRes)
        if (labelHintRes == null) {
            return null
        }
        val labelHintText = resourceProvider.getString(labelHintRes)
        val spannable = SpannableString(labelHintText)
        spannable.setSpan(
            ForegroundColorSpan(resourceProvider.getColor(color.neutral)),
            0,
            labelHintText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return SpannableString(SpannableStringBuilder().append(labelText).append(" ").append(spannable))
    }
}
