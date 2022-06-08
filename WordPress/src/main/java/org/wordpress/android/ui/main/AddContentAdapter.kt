package org.wordpress.android.ui.main

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.WordPress
import org.wordpress.android.ui.main.MainActionListItem.ActionType
import org.wordpress.android.ui.main.MainActionListItem.AnswerBloggingPromptAction
import org.wordpress.android.ui.main.MainActionListItem.CreateAction
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.HtmlCompatWrapper
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

class AddContentAdapter(context: Context) : Adapter<AddContentViewHolder<*>>() {
    private var items: List<MainActionListItem> = listOf()
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var htmlCompatWrapper: HtmlCompatWrapper

    init {
        (context.applicationContext as WordPress).component().inject(this)
    }

    fun update(newItems: List<MainActionListItem>) {
        val diffResult = DiffUtil.calculateDiff(
                MainActionDiffCallback(
                        items,
                        newItems
                )
        )
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: AddContentViewHolder<*>, position: Int) {
        val item = items[position]
        when (holder) {
            is ActionListItemViewHolder -> holder.bind(item as CreateAction)
            is CompactBloggingPromptCardViewHolder -> holder.bind(item as AnswerBloggingPromptAction)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddContentViewHolder<*> {
        return when (viewType) {
            ActionType.ANSWER_BLOGGING_PROMPT.ordinal -> CompactBloggingPromptCardViewHolder(
                    parent, uiHelpers, htmlCompatWrapper
            )
            else -> ActionListItemViewHolder(parent, imageManager)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].actionType.ordinal
    }
}
