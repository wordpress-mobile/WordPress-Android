package org.wordpress.android.ui.posts.prepublishing.visibility

import android.content.Context
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.WordPress
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.VisibilityUiState
import org.wordpress.android.ui.utils.UiHelpers
import javax.inject.Inject

class PrepublishingVisibilityAdapter(context: Context) : Adapter<PrepublishingVisibilityListItemViewHolder>() {
    private var items = mutableListOf<VisibilityUiState>()
    @Inject lateinit var uiHelpers: UiHelpers

    init {
        (context.applicationContext as WordPress).component().inject(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrepublishingVisibilityListItemViewHolder {
        return PrepublishingVisibilityListItemViewHolder(parent, uiHelpers)
    }

    @MainThread
    fun update(newItems: List<VisibilityUiState>) {
        val diffResult = DiffUtil.calculateDiff(
                PrepublishingVisibilityDiffCallback(
                        items.toList(),
                        newItems
                )
        )
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: PrepublishingVisibilityListItemViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }
}
