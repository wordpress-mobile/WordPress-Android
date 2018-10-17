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
import org.wordpress.android.ui.stats.refresh.BlockListAdapter.BlockItemViewHolder.ColumnsViewHolder
import org.wordpress.android.ui.stats.refresh.BlockListAdapter.BlockItemViewHolder.EmptyViewHolder
import org.wordpress.android.ui.stats.refresh.BlockListAdapter.BlockItemViewHolder.ItemViewHolder
import org.wordpress.android.ui.stats.refresh.BlockListAdapter.BlockItemViewHolder.TextViewHolder
import org.wordpress.android.ui.stats.refresh.BlockListAdapter.BlockItemViewHolder.TitleViewHolder
import org.wordpress.android.ui.stats.refresh.BlockListItem.Columns
import org.wordpress.android.ui.stats.refresh.BlockListItem.Item
import org.wordpress.android.ui.stats.refresh.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.COLUMNS
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.ITEM
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.LINK
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.TEXT
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.values

class BlockListAdapter : Adapter<BlockItemViewHolder>() {
    private var items: List<BlockListItem> = listOf()
    fun update(newItems: List<BlockListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockItemViewHolder {
        return when (values()[viewType]) {
            TITLE -> TitleViewHolder(parent)
            ITEM -> ItemViewHolder(parent)
            EMPTY -> EmptyViewHolder(parent)
            TEXT -> TextViewHolder(parent)
            COLUMNS -> ColumnsViewHolder(parent)
            LINK -> TODO()
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
            is TextViewHolder -> holder.bind(item as Text)
            is ColumnsViewHolder -> holder.bind(item as Columns)
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

        class TextViewHolder(parent: ViewGroup) : BlockItemViewHolder(parent, R.layout.stats_block_text_item) {
            private val text = itemView.findViewById<TextView>(R.id.text)
            fun bind(textItem: Text) {
                text.text = textItem.text
            }
        }

        class ColumnsViewHolder(parent: ViewGroup) : BlockItemViewHolder(parent, R.layout.stats_block_column_item) {
            private val firstKey = itemView.findViewById<TextView>(R.id.first_key)
            private val secondKey = itemView.findViewById<TextView>(R.id.second_key)
            private val thirdKey = itemView.findViewById<TextView>(R.id.third_key)
            private val firstValue = itemView.findViewById<TextView>(R.id.first_value)
            private val secondValue = itemView.findViewById<TextView>(R.id.second_value)
            private val thirdValue = itemView.findViewById<TextView>(R.id.third_value)
            fun bind(columns: Columns) {
                firstKey.setText(columns.headers[0])
                secondKey.setText(columns.headers[1])
                thirdKey.setText(columns.headers[2])
                firstValue.text = columns.values[0]
                secondValue.text = columns.values[1]
                thirdValue.text = columns.values[2]
            }
        }
    }
}
