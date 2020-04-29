package org.wordpress.android.ui.posts.prepublishing.visibility

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.WordPress
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.VisibilityUiState
import org.wordpress.android.ui.utils.UiHelpers
import javax.inject.Inject

class PrepublishingVisibilityAdapter(context: Context) : RecyclerView.Adapter<PrepublishingVisibilityListItemViewHolder>() {
    private var items: List<VisibilityUiState> = listOf()
    @Inject lateinit var uiHelpers: UiHelpers

    init {
        (context.applicationContext as WordPress).component().inject(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrepublishingVisibilityListItemViewHolder {
        return PrepublishingVisibilityListItemViewHolder(parent, uiHelpers)
    }

    fun update(newItems: List<VisibilityUiState>) {
        val diffResult = DiffUtil.calculateDiff(
                PrepublishingVisibilityDiffCallback(
                        this.items,
                        newItems
                )
        )
        this.items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: PrepublishingVisibilityListItemViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }
}
