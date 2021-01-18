package org.wordpress.android.ui.jetpack.common.viewholders

import android.view.ViewGroup
import kotlinx.android.synthetic.main.jetpack_list_description_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.DescriptionState
import org.wordpress.android.ui.utils.UiHelpers

class JetpackDescriptionViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder(R.layout.jetpack_list_description_item, parent) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val descriptionState = itemUiState as DescriptionState
        with(uiHelpers) {
            description.text = getTextOfUiString(itemView.context, descriptionState.text)
        }
    }
}
