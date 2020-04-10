package org.wordpress.android.ui.posts

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.PrepublishingActionUiState
import org.wordpress.android.ui.utils.UiHelpers

class PrepublishingActionsListItemViewHolder(internal val parent: ViewGroup, val uiHelpers: UiHelpers) : ViewHolder(
        LayoutInflater.from(parent.context).inflate(
                R.layout.prepublishing_action_list_item, parent, false
        )
) {
    private val actionType: TextView = itemView.findViewById(R.id.action_type)
    private val actionResult: TextView = itemView.findViewById(R.id.action_result)

    fun bind(uiState: PrepublishingActionItemUiState) {
        uiState as PrepublishingActionUiState

        actionType.text = uiHelpers.getTextOfUiString(itemView.context, uiState.actionType.textRes)
        uiState.actionResult?.let { resultText ->
            actionResult.text = uiHelpers.getTextOfUiString(itemView.context, resultText)
        }
    }
}
