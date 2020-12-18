package org.wordpress.android.ui.jetpack.scan.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.ViewType
import org.wordpress.android.ui.jetpack.common.viewholders.ActionButtonViewHolder
import org.wordpress.android.ui.jetpack.common.viewholders.DescriptionViewHolder
import org.wordpress.android.ui.jetpack.common.viewholders.HeaderViewHolder
import org.wordpress.android.ui.jetpack.common.viewholders.IconViewHolder
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackViewHolder
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatItemState
import org.wordpress.android.ui.jetpack.scan.adapters.viewholders.ThreatViewHolder
import org.wordpress.android.ui.jetpack.scan.adapters.viewholders.ThreatsHeaderViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager

class ScanAdapter(
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers
) : Adapter<JetpackViewHolder>() {
    private val items = mutableListOf<JetpackListItemState>()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JetpackViewHolder {
        return when (viewType) {
            ViewType.IMAGE.id -> IconViewHolder(imageManager, parent)
            ViewType.HEADER.id -> HeaderViewHolder(uiHelpers, parent)
            ViewType.DESCRIPTION.id -> DescriptionViewHolder(uiHelpers, parent)
            ViewType.ACTION_BUTTON.id -> ActionButtonViewHolder(uiHelpers, parent)
            ViewType.THREATS_HEADER.id -> ThreatsHeaderViewHolder(uiHelpers, parent)
            ViewType.THREAT_ITEM.id -> ThreatViewHolder(parent)
            else -> throw IllegalArgumentException("Unexpected view type in ${this::class.java.simpleName}")
        }
    }

    override fun onBindViewHolder(holder: JetpackViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    override fun getItemViewType(position: Int) = items[position].type.id

    override fun getItemId(position: Int): Long = items[position].longId()

    override fun getItemCount() = items.size

    fun update(newItems: List<JetpackListItemState>) {
        val diffResult = DiffUtil.calculateDiff(ScanDiffCallback(this.items.toList(), items))
        items.clear()
        items.addAll(newItems)

        diffResult.dispatchUpdatesTo(this)
    }

    class ScanDiffCallback(
        private val oldList: List<JetpackListItemState>,
        private val newList: List<JetpackListItemState>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return when {
                oldItem is ThreatItemState && newItem is ThreatItemState -> oldItem.threatId == newItem.threatId
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
