package org.wordpress.android.ui.mysite

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.android.synthetic.main.quick_start_task_card.view.*
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteItem.QuickStartCard.QuickStartTaskCard
import org.wordpress.android.ui.mysite.QuickStartTaskCardAdapter.QuickStartTaskCardViewHolder
import org.wordpress.android.ui.utils.UiHelpers

class QuickStartTaskCardAdapter(private val uiHelpers: UiHelpers) : Adapter<QuickStartTaskCardViewHolder>() {
    private var items = listOf<QuickStartTaskCard>()

    fun loadData(newItems: List<QuickStartTaskCard>) {
        val diffResult = DiffUtil.calculateDiff(QuickStartTaskCardAdapterDiffCallback(items, newItems))
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = QuickStartTaskCardViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.quick_start_task_card, parent, false)
    )

    override fun onBindViewHolder(holder: QuickStartTaskCardViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class QuickStartTaskCardViewHolder(itemView: View) : ViewHolder(itemView) {
        fun bind(taskCard: QuickStartTaskCard) = itemView.apply {
            task_card_title.text = uiHelpers.getTextOfUiString(context, taskCard.title)
            task_card_description.text = uiHelpers.getTextOfUiString(context, taskCard.description)
            task_card_illustration.setImageResource(taskCard.illustration)

            task_card_view.apply {
                checkedIconTint = ContextCompat.getColorStateList(context, taskCard.accentColor)
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
