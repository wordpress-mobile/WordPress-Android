package org.wordpress.android.ui.comments.unified

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import org.wordpress.android.WordPress
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.Comment
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.CommentListItemType.COMMENT
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.CommentListItemType.NEXT_PAGE_LOADER
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.CommentListItemType.SUB_HEADER
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.NextPageLoader
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.SubHeader
import org.wordpress.android.ui.utils.AnimationUtilsWrapper
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.GravatarUtilsWrapper
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class UnifiedCommentListAdapter(context: Context) : ListAdapter<UnifiedCommentListItem,
        UnifiedCommentListViewHolder<*>>(
    diffCallback
) {
    @Inject
    lateinit var imageManager: ImageManager
    @Inject
    lateinit var uiHelpers: UiHelpers
    @Inject
    lateinit var commentListUiUtils: CommentListUiUtils
    @Inject
    lateinit var resourceProvider: ResourceProvider
    @Inject
    lateinit var gravatarUtilsWrapper: GravatarUtilsWrapper
    @Inject
    lateinit var animationUtilsWrapper: AnimationUtilsWrapper

    init {
        (context.applicationContext as WordPress).component().inject(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnifiedCommentListViewHolder<*> {
        return when (viewType) {
            SUB_HEADER.ordinal -> UnifiedCommentSubHeaderViewHolder(parent)
            COMMENT.ordinal -> UnifiedCommentViewHolder(
                parent,
                imageManager,
                uiHelpers,
                commentListUiUtils,
                resourceProvider,
                gravatarUtilsWrapper,
                animationUtilsWrapper
            )
            NEXT_PAGE_LOADER.ordinal -> LoadStateViewHolder(parent)
            else -> throw IllegalArgumentException("Unexpected view holder in UnifiedCommentListAdapter")
        }
    }

    override fun onBindViewHolder(holder: UnifiedCommentListViewHolder<*>, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && (payloads.first() as? Bundle)?.size() ?: 0 > 0) {
            val bundle = payloads.first() as Bundle
            if (bundle.containsKey(UnifiedCommentsListDiffCallback.COMMENT_SELECTION_TOGGLED)) {
                val isSelected = bundle.getBoolean(UnifiedCommentsListDiffCallback.COMMENT_SELECTION_TOGGLED)
                (holder as UnifiedCommentViewHolder).toggleSelected(isSelected)
            }

            if (bundle.containsKey(UnifiedCommentsListDiffCallback.COMMENT_PENDING_STATE_CHANGED)) {
                (holder as UnifiedCommentViewHolder).updateStateAndListeners(getItem(position) as Comment)
            }
        } else {
            onBindViewHolder(holder, position)
        }
    }

    override fun onBindViewHolder(holder: UnifiedCommentListViewHolder<*>, position: Int) {
        if (holder is UnifiedCommentSubHeaderViewHolder) {
            holder.bind((getItem(position) as SubHeader))
        } else if (holder is UnifiedCommentViewHolder) {
            holder.bind(getItem(position) as Comment)
        } else if (holder is LoadStateViewHolder) {
            holder.bind(getItem(position) as NextPageLoader)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type.ordinal
    }

    companion object {
        private val diffCallback = UnifiedCommentsListDiffCallback()
    }
}
