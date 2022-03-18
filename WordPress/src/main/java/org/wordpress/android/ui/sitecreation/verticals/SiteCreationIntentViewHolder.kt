package org.wordpress.android.ui.sitecreation.verticals

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationIntentsViewModel.IntentListItemUiState
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationIntentsViewModel.IntentListItemUiState.DefaultIntentItemUiState
import org.wordpress.android.ui.utils.UiHelpers

sealed class SiteCreationIntentViewHolder(internal val parent: ViewGroup, @LayoutRes layout: Int) :
        RecyclerView.ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                        layout,
                        parent,
                        false
                )
        ) {
    abstract fun onBind(uiState: IntentListItemUiState)

    class DefaultIntentItemViewHolder(
        parentView: ViewGroup,
        private val uiHelpers: UiHelpers
    ) : SiteCreationIntentViewHolder(parentView, R.layout.site_creation_intents_item) {
        private val container = itemView.findViewById<ViewGroup>(R.id.container)
        private val verticalText = itemView.findViewById<TextView>(R.id.vertical_text)
        private val verticalIcon = itemView.findViewById<ImageView>(R.id.icon)
        private var onIntentSelected: (() -> Unit)? = null

        init {
            container.setOnClickListener {
                onIntentSelected?.invoke()
            }
        }

        override fun onBind(uiState: IntentListItemUiState) {
            uiState as DefaultIntentItemUiState
//            onIntentSelected = requireNotNull(uiState.onItemTapped) { "OnItemTapped is required." }
            onIntentSelected = uiState.onItemTapped
            verticalText.text = uiState.verticalText
            verticalIcon.setImageResource(uiState.verticalIconResId)
        }
    }
}
