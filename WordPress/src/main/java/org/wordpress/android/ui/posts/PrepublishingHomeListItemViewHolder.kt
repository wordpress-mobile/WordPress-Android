package org.wordpress.android.ui.posts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.PrepublishingHomeUiState
import org.wordpress.android.ui.utils.UiHelpers

class PrepublishingHomeListItemViewHolder(internal val parent: ViewGroup, val uiHelpers: UiHelpers) : ViewHolder(
        LayoutInflater.from(parent.context).inflate(
                R.layout.prepublishing_action_list_item, parent, false
        )
) {
    private val actionType: TextView = itemView.findViewById(R.id.action_type)
    private val actionResult: TextView = itemView.findViewById(R.id.action_result)
    private val actionLayout: View = itemView.findViewById(R.id.action_layout)

    fun bind(uiState: PrepublishingHomeItemUiState) {
        uiState as PrepublishingHomeUiState

        actionType.text = uiHelpers.getTextOfUiString(itemView.context, uiState.actionType.textRes)
        uiState.actionResult?.let { resultText ->
            actionResult.text = uiHelpers.getTextOfUiString(itemView.context, resultText)
        }
        actionLayout.setOnClickListener {
            uiState.onActionClicked.invoke(uiState.actionType)
        }
    }
}
