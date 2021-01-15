package org.wordpress.android.ui.jetpack.backup.download.details.adapters

import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackBackupRestoreSubHeaderViewHolder
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.ViewType
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackButtonViewHolder
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackCheckboxViewHolder
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackDescriptionViewHolder
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackHeaderViewHolder
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackIconViewHolder
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager

class BackupDownloadDetailsAdapter(
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers
) : RecyclerView.Adapter<JetpackViewHolder>() {
    private val items = mutableListOf<JetpackListItemState>()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): JetpackViewHolder {
        return when (viewType) {
            ViewType.ICON.id -> JetpackIconViewHolder(imageManager, parent)
            ViewType.HEADER.id -> JetpackHeaderViewHolder(uiHelpers, parent)
            ViewType.DESCRIPTION.id -> JetpackDescriptionViewHolder(uiHelpers, parent)
            ViewType.ACTION_BUTTON.id -> JetpackButtonViewHolder(uiHelpers, parent)
            ViewType.CHECKBOX.id -> JetpackCheckboxViewHolder(uiHelpers, parent)
            ViewType.BACKUP_RESTORE_SUB_HEADER.id -> JetpackBackupRestoreSubHeaderViewHolder(
                    uiHelpers,
                    parent
            )
            else -> throw IllegalArgumentException("Unexpected view type in ${this::class.java.simpleName}")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(holder: JetpackViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    override fun getItemViewType(position: Int) = items[position].type.id

    @MainThread
    fun update(newItems: List<JetpackListItemState>) {
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
        val oldItems: List<JetpackListItemState>,
        val newItems: List<JetpackListItemState>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]
            if (oldItem::class != newItem::class) {
                return false
            }

            // todo: annmarie - adjust this
            return oldItem.longId() == newItem.longId()
        }

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }
}
