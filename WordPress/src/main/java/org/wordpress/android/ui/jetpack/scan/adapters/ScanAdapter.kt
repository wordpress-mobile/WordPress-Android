package org.wordpress.android.ui.jetpack.scan.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.ViewType
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackButtonViewHolder
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackDescriptionViewHolder
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackHeaderViewHolder
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackIconViewHolder
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackProgressViewHolder
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackViewHolder
import org.wordpress.android.ui.jetpack.scan.adapters.viewholders.ScanFootnoteViewHolder
import org.wordpress.android.ui.jetpack.scan.adapters.viewholders.ThreatLoadingSkeletonViewHolder
import org.wordpress.android.ui.jetpack.scan.adapters.viewholders.ThreatViewHolder
import org.wordpress.android.ui.jetpack.scan.adapters.viewholders.ThreatsDateHeaderViewHolder
import org.wordpress.android.ui.jetpack.scan.adapters.viewholders.ThreatsHeaderViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager

class ScanAdapter(
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers
) : Adapter<JetpackViewHolder<*>>() {
    private val items = mutableListOf<JetpackListItemState>()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JetpackViewHolder<*> {
        return when (viewType) {
            ViewType.ICON.id -> JetpackIconViewHolder(imageManager, parent)
            ViewType.HEADER.id -> JetpackHeaderViewHolder(uiHelpers, parent)
            ViewType.DESCRIPTION.id -> JetpackDescriptionViewHolder(uiHelpers, parent)
            ViewType.PROGRESS.id -> JetpackProgressViewHolder(uiHelpers, parent)
            ViewType.PRIMARY_ACTION_BUTTON.id -> JetpackButtonViewHolder.Primary(uiHelpers, parent)
            ViewType.SECONDARY_ACTION_BUTTON.id -> JetpackButtonViewHolder.Secondary(uiHelpers, parent)
            ViewType.THREATS_HEADER.id -> ThreatsHeaderViewHolder(uiHelpers, parent)
            ViewType.THREAT_ITEM.id -> ThreatViewHolder(uiHelpers, parent)
            ViewType.THREAT_ITEM_LOADING_SKELETON.id -> ThreatLoadingSkeletonViewHolder(parent)
            ViewType.THREAT_DETECTED_DATE.id -> ThreatsDateHeaderViewHolder(uiHelpers, parent)
            ViewType.SCAN_FOOTNOTE.id -> ScanFootnoteViewHolder(imageManager, uiHelpers, parent)
            else -> throw IllegalArgumentException("Unexpected view type in ${this::class.java.simpleName}")
        }
    }

    override fun onBindViewHolder(holder: JetpackViewHolder<*>, position: Int) {
        holder.onBind(items[position])
    }

    override fun getItemViewType(position: Int) = items[position].type.id

    override fun getItemId(position: Int): Long = items[position].longId()

    override fun getItemCount() = items.size

    fun update(newItems: List<JetpackListItemState>) {
        val diffResult = DiffUtil.calculateDiff(ScanDiffCallback(items.toList(), newItems))
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
            if (oldItem::class != newItem::class) {
                return false
            }
            return oldItem.longId() == newItem.longId()
        }

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
