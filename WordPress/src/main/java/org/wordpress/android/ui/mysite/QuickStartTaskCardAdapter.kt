package org.wordpress.android.ui.mysite

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.android.synthetic.main.quick_start_task_card.view.*
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteItem.QuickStartCard.QuickStartTaskCard
import org.wordpress.android.ui.mysite.QuickStartTaskCardAdapter.QuickStartTaskCardViewHolder

class QuickStartTaskCardAdapter : Adapter<QuickStartTaskCardViewHolder>() {
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
            task_card_title.text = taskCard.title
            task_card_description.text = taskCard.description

            val alpha = if (taskCard.done) 0.2f else 1.0f
            task_card_title.alpha = alpha
            task_card_description.alpha = alpha
            task_card_illustration.alpha = alpha

            setOnClickListener { taskCard.onClick.click() }
            isClickable = !taskCard.done
        }
    }

    inner class QuickStartTaskCardAdapterDiffCallback(
        private val oldItems: List<QuickStartTaskCard>,
        private val newItems: List<QuickStartTaskCard>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldItems.size

        override fun getNewListSize() = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldItems[oldItemPosition].id == newItems[newItemPosition].id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldItems[oldItemPosition] == newItems[newItemPosition]
    }
}
