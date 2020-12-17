package org.wordpress.android.ui.jetpack.scan.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.jetpack.scan.ScanListItemState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ScanState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ViewType
import org.wordpress.android.ui.jetpack.scan.adapters.viewholders.ScanStateViewHolder
import org.wordpress.android.ui.jetpack.scan.adapters.viewholders.ScanViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager

class ScanAdapter(
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers
) : Adapter<ScanViewHolder>() {
    private val items = mutableListOf<ScanListItemState>()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanViewHolder {
        return when (viewType) {
            ViewType.SCAN_STATE.id -> ScanStateViewHolder(imageManager, uiHelpers, parent)
            else -> throw IllegalArgumentException("Unexpected view type in ${this::class.java.simpleName}")
        }
    }

    override fun onBindViewHolder(holder: ScanViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    override fun getItemViewType(position: Int) = items[position].type.id

    override fun getItemId(position: Int): Long = items[position].longId()

    override fun getItemCount() = items.size

    fun update(newItems: List<ScanListItemState>) {
        val diffResult = DiffUtil.calculateDiff(ScanDiffCallback(this.items.toList(), items))
        items.clear()
        items.addAll(newItems)

        diffResult.dispatchUpdatesTo(this)
    }

    class ScanDiffCallback(
        private val oldList: List<ScanListItemState>,
        private val newList: List<ScanListItemState>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return when {
                oldItem is ScanState && newItem is ScanState -> // TODO: ashiagr revisit
                    oldItem.scanTitle == newItem.scanTitle &&
                        oldItem.scanDescription == newItem.scanDescription
                else -> false
            }
        }

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
