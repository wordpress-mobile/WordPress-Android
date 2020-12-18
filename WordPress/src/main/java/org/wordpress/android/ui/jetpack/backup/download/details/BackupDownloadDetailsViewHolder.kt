package org.wordpress.android.ui.jetpack.backup.download.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.backup_details_list_item.view.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.backup.download.details.BackupDownloadDetailsViewModel.ListItemUiState
import org.wordpress.android.ui.utils.UiHelpers

sealed class BackupDownloadDetailsViewHolder(
    @LayoutRes layout: Int,
    parent: ViewGroup
) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    abstract fun onBind(uiState: ListItemUiState)

    class BackupDownloadDetailsListItemViewHolder(
        parentView: ViewGroup,
        private val uiHelpers: UiHelpers
    ) : BackupDownloadDetailsViewHolder(R.layout.backup_details_list_item, parentView) {
        private val container = itemView.item_container
        private val checkbox = itemView.checkbox
        private val label = itemView.checkbox_label

        override fun onBind(uiState: ListItemUiState) {
            uiHelpers.setTextOrHide(label, uiState.label)
            checkbox.isChecked = uiState.checked
            container.setOnClickListener { uiState.onClick.invoke() }
        }
    }
}
