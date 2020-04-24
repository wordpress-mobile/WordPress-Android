package org.wordpress.android.ui.posts

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.WordPress
import org.wordpress.android.ui.utils.UiHelpers
import javax.inject.Inject

class PrepublishingHomeAdapter(context: Context) : RecyclerView.Adapter<PrepublishingHomeListItemViewHolder>() {
    private var items: List<PrepublishingHomeItemUiState> = listOf()
    @Inject lateinit var uiHelpers: UiHelpers

    init {
        (context.applicationContext as WordPress).component().inject(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrepublishingHomeListItemViewHolder {
        return PrepublishingHomeListItemViewHolder(parent, uiHelpers)
    }

    fun update(newItems: List<PrepublishingHomeItemUiState>) {
        val diffResult = DiffUtil.calculateDiff(
                PrepublishingHomeDiffCallback(
                        this.items,
                        newItems
                )
        )
        this.items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: PrepublishingHomeListItemViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }
}
