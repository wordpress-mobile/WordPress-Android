package org.wordpress.android.ui.posts

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.WordPress
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.PrepublishingActionUiState
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.PrepublishingButtonUiState
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.PrepublishingHomeHeaderUiState
import org.wordpress.android.ui.posts.PrepublishingHomeViewHolder.PrepublishingHeaderListItemViewHolder
import org.wordpress.android.ui.posts.PrepublishingHomeViewHolder.PrepublishingHomeListItemViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

private const val headerViewType: Int = 1
private const val actionItemViewType: Int = 2
private const val publishButtonViewType: Int = 3

class PrepublishingActionsAdapter(context: Context) : RecyclerView.Adapter<PrepublishingHomeViewHolder>() {
    private var items: List<PrepublishingActionItemUiState> = listOf()
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var imageManager: ImageManager

    init {
        (context.applicationContext as WordPress).component().inject(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrepublishingHomeViewHolder {
        return when (viewType) {
            headerViewType -> PrepublishingHeaderListItemViewHolder(parent, uiHelpers, imageManager)
            actionItemViewType -> PrepublishingHomeListItemViewHolder(parent, uiHelpers)
            publishButtonViewType -> TODO()
            else -> throw NotImplementedError("Unknown ViewType")
        }
    }

    fun update(newItems: List<PrepublishingActionItemUiState>) {
        val diffResult = DiffUtil.calculateDiff(
                PrepublishingActionsDiffCallback(
                        this.items,
                        newItems
                )
        )
        this.items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is PrepublishingHomeHeaderUiState -> headerViewType
            is PrepublishingActionUiState -> actionItemViewType
            is PrepublishingButtonUiState -> publishButtonViewType
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: PrepublishingHomeViewHolder, position: Int) {
        val item = items[position]
        holder.onBind(item)
    }
}
