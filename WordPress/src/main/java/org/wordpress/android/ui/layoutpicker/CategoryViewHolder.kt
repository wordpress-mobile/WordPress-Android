package org.wordpress.android.ui.layoutpicker

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.databinding.ModalLayoutPickerCategoryBinding
import org.wordpress.android.login.util.getColorStateListFromAttribute
import org.wordpress.android.util.getColorFromAttribute
import org.wordpress.android.util.setVisible
import org.wordpress.android.util.viewBinding

/**
 * Renders the Layout Category header buttons
 */
class CategoryViewHolder(
    internal val parent: ViewGroup,
    internal val binding: ModalLayoutPickerCategoryBinding =
            parent.viewBinding(ModalLayoutPickerCategoryBinding::inflate)
) : RecyclerView.ViewHolder(binding.root) {
    val container: View = binding.categoryContainer
    val category: TextView = binding.category
    val emoji: TextView = binding.emoji
    val check: ImageView = binding.check

    fun onBind(uiState: CategoryListItemUiState) {
        category.text = uiState.title
        emoji.text = uiState.emoji
        container.contentDescription = parent.context.getString(uiState.contentDescriptionResId, uiState.title)
        container.setOnClickListener {
            uiState.onItemTapped.invoke()
        }
        setSelectedStateUI(uiState)
    }

    private fun setSelectedStateUI(uiState: CategoryListItemUiState) {
        check.setVisible(uiState.checkIconVisible)
        emoji.setVisible(uiState.emojiIconVisible)
        container.backgroundTintList = parent.context.getColorStateListFromAttribute(uiState.background)
        category.setTextColor(parent.context.getColorFromAttribute(uiState.textColor))
    }
}
