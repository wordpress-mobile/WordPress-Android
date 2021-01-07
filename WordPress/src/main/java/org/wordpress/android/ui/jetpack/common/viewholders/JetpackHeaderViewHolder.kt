package org.wordpress.android.ui.jetpack.common.viewholders

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.jetpack_list_header_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.HeaderState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.getColorResIdFromAttribute

class JetpackHeaderViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder(R.layout.jetpack_list_header_item, parent) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val headerState = itemUiState as HeaderState
        val context = itemView.context

        header.text = uiHelpers.getTextOfUiString(context, headerState.text)
        val textColorRes = context.getColorResIdFromAttribute(headerState.textColorRes)
        header.setTextColor(ContextCompat.getColor(context, textColorRes))
    }
}
