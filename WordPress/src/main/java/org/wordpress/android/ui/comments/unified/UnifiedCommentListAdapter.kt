package org.wordpress.android.ui.comments.unified

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import org.wordpress.android.WordPress
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.Comment
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.CommentListItemType.COMMENT
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.CommentListItemType.SUB_HEADER
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.SubHeader
import org.wordpress.android.ui.utils.AnimationUtilsWrapper
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.GravatarUtilsWrapper
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class UnifiedCommentListAdapter(context: Context) :
        PagingDataAdapter<UnifiedCommentListItem, UnifiedCommentListViewHolder<*>>(
                diffCallback
        ) {
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var commentListUiUtils: CommentListUiUtils
    @Inject lateinit var resourceProvider: ResourceProvider
    @Inject lateinit var gravatarUtilsWrapper: GravatarUtilsWrapper
    @Inject lateinit var animationUtilsWrapper: AnimationUtilsWrapper

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
            else -> throw IllegalArgumentException("Unexpected view holder in UnifiedCommentListAdapter")
        }
    }

    override fun onBindViewHolder(holder: UnifiedCommentListViewHolder<*>, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && (payloads.first() as? Bundle)?.size() ?: 0 > 0) {
            val bundle = payloads.first() as Bundle
            val isSelected = bundle.getBoolean(UnifiedCommentsListDiffCallback.COMMENT_SELECTION_TOGGLED)
            (holder as UnifiedCommentViewHolder).toggleSelected(isSelected)
        } else {
            onBindViewHolder(holder, position)
        }
    }

    override fun onBindViewHolder(holder: UnifiedCommentListViewHolder<*>, position: Int) {
        if (holder is UnifiedCommentSubHeaderViewHolder) {
            holder.bind((getItem(position) as SubHeader))
        } else if (holder is UnifiedCommentViewHolder) {
            holder.bind(getItem(position) as Comment)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position)!!.type.ordinal
    }

    companion object {
        private val diffCallback = UnifiedCommentsListDiffCallback()
    }
}
