package org.wordpress.android.ui.pages

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.R.layout
import org.wordpress.android.ui.pages.PageItem.ParentPage
import org.wordpress.android.ui.pages.PageItemViewHolder.EmptyViewHolder
import org.wordpress.android.ui.pages.PageItemViewHolder.PageDividerViewHolder
import org.wordpress.android.ui.pages.PageItemViewHolder.PageParentViewHolder

class PageParentSearchAdapter(
    private val onParentSelected: (ParentPage) -> Unit,
    private val uiScope: CoroutineScope
) : Adapter<PageItemViewHolder>() {
    private val items = mutableListOf<PageItem>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageItemViewHolder {
        return when (viewType) {
            PageItem.Type.PARENT.viewType -> PageParentViewHolder(parent,
                    this::selectParent,
                    layout.page_parent_list_item)
            PageItem.Type.TOP_LEVEL_PARENT.viewType -> PageParentViewHolder(parent,
                    this::selectParent,
                    layout.page_parent_top_level_item)
            PageItem.Type.DIVIDER.viewType -> PageDividerViewHolder(parent)
            PageItem.Type.EMPTY.viewType -> EmptyViewHolder(parent) { }
            else -> throw Throwable("Unexpected view type")
        }
    }

    private fun selectParent(parent: ParentPage) {
        onParentSelected(parent)
        uiScope.launch {
            delay(200) // let the selection animation play out before refreshing the list
            notifyDataSetChanged()
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
