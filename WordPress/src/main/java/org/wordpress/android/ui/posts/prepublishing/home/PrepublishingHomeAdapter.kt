package org.wordpress.android.ui.posts.prepublishing.home

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.WordPress
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ButtonUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.FeaturedImageUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.HeaderUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.HomeUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.SocialUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeViewHolder.PrepublishingFeaturedImageViewHolder
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeViewHolder.PrepublishingHeaderListItemViewHolder
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeViewHolder.PrepublishingHomeListItemViewHolder
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeViewHolder.PrepublishingSocialItemViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

private const val VIEW_TYPE_HEADER_VIEW: Int = 1
private const val VIEW_TYPE_HOME_ITEM: Int = 2
private const val VIEW_TYPE_SOCIAL_ITEM: Int = 4
private const val VIEW_TYPE_FEATURED_IMAGE_ITEM: Int = 5

class PrepublishingHomeAdapter(context: Context) : RecyclerView.Adapter<PrepublishingHomeViewHolder>() {
    private var items: List<PrepublishingHomeItemUiState> = listOf()

    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var imageManager: ImageManager

    init {
        (context.applicationContext as WordPress).component().inject(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrepublishingHomeViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER_VIEW -> PrepublishingHeaderListItemViewHolder(
                parent,
                uiHelpers,
                imageManager
            )
            VIEW_TYPE_HOME_ITEM -> PrepublishingHomeListItemViewHolder(parent, uiHelpers)
            VIEW_TYPE_SOCIAL_ITEM -> PrepublishingSocialItemViewHolder(parent, uiHelpers)
            VIEW_TYPE_FEATURED_IMAGE_ITEM -> PrepublishingFeaturedImageViewHolder(parent, uiHelpers, imageManager)
            else -> throw NotImplementedError("Unknown ViewType")
        }
    }

    fun update(newItems: List<PrepublishingHomeItemUiState>) {
        // don't even account for items that are not visible, they are completely ignored
        val visibleNewItems = newItems.filter { it.isVisible }

        val diffResult = DiffUtil.calculateDiff(
            PrepublishingHomeDiffCallback(
                this.items,
                visibleNewItems
            )
        )
        this.items = visibleNewItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is HeaderUiState -> VIEW_TYPE_HEADER_VIEW
            is HomeUiState -> VIEW_TYPE_HOME_ITEM
            // The publish/submit button is rendered as a pinned footer, not a list item.
            is ButtonUiState -> error("ButtonUiState is not rendered in the list")
            is SocialUiState -> VIEW_TYPE_SOCIAL_ITEM
            is FeaturedImageUiState -> VIEW_TYPE_FEATURED_IMAGE_ITEM
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: PrepublishingHomeViewHolder, position: Int) {
        val item = items[position]
        holder.onBind(item)
    }
}
