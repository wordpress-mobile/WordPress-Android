package org.wordpress.android.ui.posts

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.WordPress
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.SubmitButtonUiState
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.HeaderUiState
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.HomeUiState
import org.wordpress.android.ui.posts.PrepublishingHomeViewHolder.PrepublishingHeaderListItemViewHolder
import org.wordpress.android.ui.posts.PrepublishingHomeViewHolder.PrepublishingHomeListItemViewHolder
import org.wordpress.android.ui.posts.PrepublishingHomeViewHolder.PrepublishingSubmitButtonViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

private const val headerViewType: Int = 1
private const val homeItemViewType: Int = 2
private const val submitButtonViewType: Int = 3

class PrepublishingHomeAdapter(context: Context) : RecyclerView.Adapter<PrepublishingHomeViewHolder>() {
    private var items: List<PrepublishingHomeItemUiState> = listOf()
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var imageManager: ImageManager

    init {
        (context.applicationContext as WordPress).component().inject(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrepublishingHomeViewHolder {
        return when (viewType) {
            headerViewType -> PrepublishingHeaderListItemViewHolder(parent, uiHelpers, imageManager)
            homeItemViewType -> PrepublishingHomeListItemViewHolder(parent, uiHelpers)
            submitButtonViewType -> PrepublishingSubmitButtonViewHolder(parent, uiHelpers)
            else -> throw NotImplementedError("Unknown ViewType")
        }
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

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is HeaderUiState -> headerViewType
            is HomeUiState -> homeItemViewType
            is SubmitButtonUiState -> submitButtonViewType
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: PrepublishingHomeViewHolder, position: Int) {
        val item = items[position]
        holder.onBind(item)
    }
}
