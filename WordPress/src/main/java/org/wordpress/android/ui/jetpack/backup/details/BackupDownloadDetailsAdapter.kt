package org.wordpress.android.ui.jetpack.backup.details

import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.ui.jetpack.backup.details.BackupDownloadDetailsViewModel.ListItemUiState
import org.wordpress.android.ui.utils.UiHelpers

class BackupDownloadDetailsAdapter(private val uiHelpers: UiHelpers) :
        RecyclerView.Adapter<BackupDownloadDetailsViewHolder>() {
    private val items = mutableListOf<ListItemUiState>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BackupDownloadDetailsViewHolder {
        return BackupDownloadDetailsViewHolder.BackupDownloadDetailsListItemViewHolder(parent, uiHelpers)
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(holder: BackupDownloadDetailsViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    @MainThread
    fun update(newItems: List<ListItemUiState>) {
        val diffResult = DiffUtil.calculateDiff(
                BackupDownloadDetailsListDiffUtils(
                        items.toList(),
                        newItems
                )
        )
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    private class BackupDownloadDetailsListDiffUtils(
        val oldItems: List<ListItemUiState>,
        val newItems: List<ListItemUiState>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]
            if (oldItem::class != newItem::class) {
                return false
            }

            return oldItem.label == newItem.label
        }

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }
}
