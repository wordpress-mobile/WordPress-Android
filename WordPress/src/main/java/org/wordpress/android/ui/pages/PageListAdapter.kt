package org.wordpress.android.ui.pages

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.PageItemViewHolder.EmptyViewHolder
import org.wordpress.android.ui.pages.PageItemViewHolder.PageDividerViewHolder
import org.wordpress.android.ui.pages.PageItemViewHolder.PageViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager

class PageListAdapter(
    private val onMenuAction: (PageItem.Action, Page) -> Boolean,
    private val onItemTapped: (Page) -> Unit,
    private val onEmptyActionButtonTapped: () -> Unit,
    private val isSitePhotonCapable: Boolean,
    private val isPrivateAtSite: Boolean,
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers
) : Adapter<PageItemViewHolder>() {
    private val items = mutableListOf<PageItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageItemViewHolder {
        return when (viewType) {
            PageItem.Type.PAGE.viewType -> PageViewHolder(
                    parent, onMenuAction, onItemTapped, imageManager,
                    isSitePhotonCapable, isPrivateAtSite, uiHelpers
            )
            PageItem.Type.DIVIDER.viewType -> PageDividerViewHolder(parent)
            PageItem.Type.EMPTY.viewType -> EmptyViewHolder(parent, onEmptyActionButtonTapped)
            else -> throw Throwable("Unexpected view type")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return items[position].type.viewType
    }

    override fun onBindViewHolder(holder: PageItemViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    fun update(result: List<PageItem>) {
        val diffResult = DiffUtil.calculateDiff(PageItemDiffUtil(items.toList(), result))
        items.clear()
        items.addAll(result)
        diffResult.dispatchUpdatesTo(this)
    }
}
