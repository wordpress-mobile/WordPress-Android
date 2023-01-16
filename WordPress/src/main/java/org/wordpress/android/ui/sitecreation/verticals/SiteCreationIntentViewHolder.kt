package org.wordpress.android.ui.sitecreation.verticals

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationIntentsViewModel.IntentListItemUiState

class SiteCreationIntentViewHolder(internal val parent: ViewGroup) :
    RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.site_creation_intents_item,
            parent,
            false
        )
    ) {
    private val container = itemView.findViewById<ViewGroup>(R.id.container)
    private val verticalText = itemView.findViewById<TextView>(R.id.vertical_text)
    private val emoji = itemView.findViewById<TextView>(R.id.vertical_emoji)
    private var onIntentSelected: (() -> Unit)? = null

    init {
        container.setOnClickListener {
            onIntentSelected?.invoke()
        }
    }

    fun onBind(uiState: IntentListItemUiState) {
        onIntentSelected = requireNotNull(uiState.onItemTapped) { "OnItemTapped is required." }
        verticalText.text = uiState.verticalText
        emoji.text = uiState.emoji
    }
}
