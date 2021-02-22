package org.wordpress.android.ui.jetpack.common.viewholders

import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.jetpack_list_description_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.DescriptionState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.DescriptionState.ClickableTextInfo
import org.wordpress.android.ui.utils.UiHelpers

class JetpackDescriptionViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder(R.layout.jetpack_list_description_item, parent) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val descriptionState = itemUiState as DescriptionState
        uiHelpers.setTextOrHide(description, descriptionState.text)
        descriptionState.clickableTextsInfo?.let { setClickableSpan(it) }
    }

    private fun setClickableSpan(clickableTextsInfo: List<ClickableTextInfo>) {
        val spannableString = SpannableString(description.text)
        for (clickableTextInfo in clickableTextsInfo) {
            val (startIndex, endIndex, onClick) = clickableTextInfo
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    onClick.invoke()
                }
            }
            spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        with(description) {
            linksClickable = true
            isClickable = true
            movementMethod = LinkMovementMethod.getInstance()
            text = spannableString
        }
    }
}
