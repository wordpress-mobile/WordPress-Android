package org.wordpress.android.ui.mysite.dynamiccards.quickstart

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.databinding.QuickStartTaskCardBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.DynamicCard.QuickStartDynamicCard.QuickStartTaskCard
import org.wordpress.android.ui.mysite.dynamiccards.quickstart.QuickStartTaskCardAdapter.QuickStartTaskCardViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding

class QuickStartTaskCardAdapter(private val uiHelpers: UiHelpers) : Adapter<QuickStartTaskCardViewHolder>() {
    private var items = listOf<QuickStartTaskCard>()

    fun loadData(newItems: List<QuickStartTaskCard>) {
        val diffResult = DiffUtil.calculateDiff(QuickStartTaskCardAdapterDiffCallback(items, newItems))
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = QuickStartTaskCardViewHolder(
            parent.viewBinding(QuickStartTaskCardBinding::inflate)
    )

    override fun onBindViewHolder(holder: QuickStartTaskCardViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class QuickStartTaskCardViewHolder(val binding: QuickStartTaskCardBinding) : ViewHolder(binding.root) {
        fun bind(taskCard: QuickStartTaskCard) = with(binding) {
            taskCardTitle.text = uiHelpers.getTextOfUiString(root.context, taskCard.title)
            taskCardDescription.text = uiHelpers.getTextOfUiString(root.context, taskCard.description)
            taskCardIllustration.setImageResource(taskCard.illustration)

            taskCardView.apply {
                checkedIconTint = ContextCompat.getColorStateList(root.context, taskCard.accentColor)
                isChecked = taskCard.done
                setOnClickListener(if (taskCard.done) null else ({ taskCard.onClick.click() }))
            }
        }
    }

    inner class QuickStartTaskCardAdapterDiffCallback(
        private val oldItems: List<QuickStartTaskCard>,
        private val newItems: List<QuickStartTaskCard>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldItems.size

        override fun getNewListSize() = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldItems[oldItemPosition].quickStartTask == newItems[newItemPosition].quickStartTask

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldItems[oldItemPosition] == newItems[newItemPosition]
    }
}
