package org.wordpress.android.ui.reader.adapters

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.utils.ThreadedCommentsUtils
import org.wordpress.android.ui.reader.viewholders.CommentButtonViewHolder
import org.wordpress.android.ui.reader.viewholders.CommentMessageViewHolder
import org.wordpress.android.ui.reader.viewholders.CommentViewHolder
import org.wordpress.android.ui.reader.viewholders.CommentsLoadingViewHolder
import org.wordpress.android.ui.reader.viewholders.CommentsSnippetViewHolder
import org.wordpress.android.ui.reader.views.uistates.CommentItemType.BUTTON
import org.wordpress.android.ui.reader.views.uistates.CommentItemType.COMMENT
import org.wordpress.android.ui.reader.views.uistates.CommentItemType.LOADING
import org.wordpress.android.ui.reader.views.uistates.CommentItemType.TEXT_MESSAGE
import org.wordpress.android.ui.reader.views.uistates.CommentSnippetItemState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

class CommentSnippetAdapter constructor(
    context: Context,
    val post: ReaderPost?
) : Adapter<CommentsSnippetViewHolder<*>>() {
    @Inject
    lateinit var threadedCommentsUtils: ThreadedCommentsUtils

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var accountStore: AccountStore

    @Inject
    lateinit var readerTracker: ReaderTracker

    @Inject
    lateinit var uiHelpers: UiHelpers

    private var itemsList = listOf<CommentSnippetItemState>()
    private var contentWidth: Int

    init {
        (context.applicationContext as WordPress).component().inject(this)
        // calculate the max width of comment content
        contentWidth = threadedCommentsUtils.getMaxWidthForContent()
    }

    fun loadData(items: List<CommentSnippetItemState>) {
        val diffResult = DiffUtil.calculateDiff(
            CommentSnippetAdatperDiffCallback(itemsList, items)
        )
        itemsList = items
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentsSnippetViewHolder<*> {
        return when (viewType) {
            LOADING.ordinal -> CommentsLoadingViewHolder(parent)
            COMMENT.ordinal -> CommentViewHolder(parent, imageManager, threadedCommentsUtils)
            BUTTON.ordinal -> CommentButtonViewHolder(parent, uiHelpers)
            TEXT_MESSAGE.ordinal -> CommentMessageViewHolder(parent, uiHelpers)
            else -> throw IllegalArgumentException("Unexpected ViewHolder in CommentSnippetAdapter: $viewType")
        }
    }

    override fun getItemViewType(position: Int) = itemsList[position].type.ordinal

    override fun getItemCount(): Int {
        return itemsList.size
    }

    override fun onBindViewHolder(holder: CommentsSnippetViewHolder<*>, position: Int) {
        holder.onBind(itemsList[position])
    }
}
