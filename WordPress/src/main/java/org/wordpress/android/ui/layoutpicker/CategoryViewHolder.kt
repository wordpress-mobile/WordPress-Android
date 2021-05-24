package org.wordpress.android.ui.layoutpicker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.modal_layout_picker_category.view.*
import org.wordpress.android.R
import org.wordpress.android.login.util.getColorStateListFromAttribute
import org.wordpress.android.util.getColorFromAttribute
import org.wordpress.android.util.setVisible

/**
 * Renders the Layout Category header buttons
 */
class CategoryViewHolder(internal val parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(
                R.layout.modal_layout_picker_category,
                parent,
                false
        )
) {
    val container: View = itemView.category_container
    val category: TextView = itemView.category
    val emoji: TextView = itemView.emoji
    val check: ImageView = itemView.check

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
