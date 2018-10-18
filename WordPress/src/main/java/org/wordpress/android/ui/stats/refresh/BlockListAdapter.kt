package org.wordpress.android.ui.stats.refresh

import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView.Adapter
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.BlockListAdapter.BlockItemViewHolder
import org.wordpress.android.ui.stats.refresh.BlockListAdapter.BlockItemViewHolder.EmptyViewHolder
import org.wordpress.android.ui.stats.refresh.BlockListAdapter.BlockItemViewHolder.ItemViewHolder
import org.wordpress.android.ui.stats.refresh.BlockListAdapter.BlockItemViewHolder.TitleViewHolder
import org.wordpress.android.ui.stats.refresh.BlockListItem.Item
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title

class BlockListAdapter : Adapter<BlockItemViewHolder>() {
    private var items: List<BlockListItem> = listOf()
    fun update(newItems: List<BlockListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockItemViewHolder {
        return when (BlockListItem.Type.values()[viewType]) {
            BlockListItem.Type.TITLE -> TitleViewHolder(parent)
            BlockListItem.Type.ITEM -> ItemViewHolder(parent)
            BlockListItem.Type.EMPTY -> EmptyViewHolder(parent)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: BlockItemViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is TitleViewHolder -> holder.bind(item as Title)
            is ItemViewHolder -> holder.bind(item as Item)
        }
    }

    sealed class BlockItemViewHolder(
        parent: ViewGroup,
        @LayoutRes layout: Int
    ) : ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
        class TitleViewHolder(parent: ViewGroup) : BlockItemViewHolder(parent, R.layout.stats_block_title) {
            private val text = itemView.findViewById<TextView>(R.id.text)
            fun bind(item: Title) {
                text.setText(item.text)
            }
        }

        class ItemViewHolder(parent: ViewGroup) : BlockItemViewHolder(parent, R.layout.stats_block_item) {
            private val icon = itemView.findViewById<ImageView>(R.id.icon)
            private val text = itemView.findViewById<TextView>(R.id.text)
            private val value = itemView.findViewById<TextView>(R.id.value)
            private val divider = itemView.findViewById<View>(R.id.divider)

            fun bind(item: Item) {
                icon.setImageResource(item.icon)
                text.setText(item.text)
                value.text = item.value
                divider.visibility = if (item.showDivider) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        }

        class EmptyViewHolder(parent: ViewGroup) : BlockItemViewHolder(parent, R.layout.stats_block_empty_item)
    }
}
