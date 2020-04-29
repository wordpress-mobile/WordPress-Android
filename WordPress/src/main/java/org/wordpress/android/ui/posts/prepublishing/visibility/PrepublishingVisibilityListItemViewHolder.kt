package org.wordpress.android.ui.posts.prepublishing.visibility

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.VisibilityUiState
import org.wordpress.android.ui.utils.UiHelpers

class PrepublishingVisibilityListItemViewHolder(internal val parent: ViewGroup, val uiHelpers: UiHelpers) : ViewHolder(
        LayoutInflater.from(parent.context).inflate(
                R.layout.prepublishing_visibility_list_item, parent, false
        )
) {
    private val layout: View = itemView.findViewById(R.id.visibility_layout)
    private val radioButton: RadioButton = itemView.findViewById(R.id.visibility_radio_button)
    private val visibilityText: TextView = itemView.findViewById(R.id.visibility_text)

    init {
        radioButton.buttonTintList = ContextCompat.getColorStateList(
                parent.context,
                R.color.neutral_10_primary_40_selector
        )
    }

    fun bind(uiState: VisibilityUiState) {
        radioButton.isChecked = uiState.checked
        uiHelpers.setTextOrHide(visibilityText, uiState.visibility.textRes)
        layout.setOnClickListener {
            uiState.onItemTapped.invoke(uiState.visibility)
        }
    }
}
