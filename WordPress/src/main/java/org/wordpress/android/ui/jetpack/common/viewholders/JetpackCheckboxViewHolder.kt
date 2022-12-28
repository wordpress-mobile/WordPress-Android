package org.wordpress.android.ui.jetpack.common.viewholders

import android.view.ViewGroup
import org.wordpress.android.databinding.JetpackListCheckboxItemBinding
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.CheckboxState
import org.wordpress.android.ui.utils.UiHelpers

class JetpackCheckboxViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder<JetpackListCheckboxItemBinding>(
    parent,
    JetpackListCheckboxItemBinding::inflate
) {
    override fun onBind(itemUiState: JetpackListItemState) = with(binding) {
        val checkboxState = itemUiState as CheckboxState
        if (checkboxState.labelSpannable == null) {
            uiHelpers.setTextOrHide(checkboxLabel, checkboxState.label)
        } else {
            checkboxLabel.text = checkboxState.labelSpannable
        }
        checkbox.isChecked = checkboxState.checked
        checkbox.isEnabled = checkboxState.isEnabled
        itemContainer.isEnabled = checkboxState.isEnabled
        itemContainer.setOnClickListener { checkboxState.onClick.invoke() }
    }
}
