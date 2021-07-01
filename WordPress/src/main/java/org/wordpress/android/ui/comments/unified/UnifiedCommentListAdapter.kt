package org.wordpress.android.ui.comments.unified

import android.content.Context

/**
 * Temporarily commented out untill migration between Paging 2 and 3 is sorted out.
 */
class UnifiedCommentListAdapter(context: Context) {
// : PagingDataAdapter<UnifiedCommentListItem,
// UnifiedCommentListViewHolder<*>>(
//                diffCallback
//        ) {
//    init {
//        (context.applicationContext as WordPress).component().inject(this)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnifiedCommentListViewHolder<*> {
//        return when (viewType) {
//            SUB_HEADER.ordinal -> UnifiedCommentSubHeaderViewHolder(parent)
//            COMMENT.ordinal -> UnifiedCommentViewHolder(
//                    parent,
//                    imageManager,
//                    uiHelpers,
//                    commentListUiUtils,
//                    resourceProvider,
//                    gravatarUtilsWrapper
//            )
//            else -> throw IllegalArgumentException("Unexpected view holder in UnifiedCommentListAdapter")
//        }
//    }
//
//    override fun onBindViewHolder(holder: UnifiedCommentListViewHolder<*>, position: Int) {
//        if (holder is UnifiedCommentSubHeaderViewHolder) {
//            holder.bind((getItem(position) as SubHeader))
//        } else if (holder is UnifiedCommentViewHolder) {
//            holder.bind(getItem(position) as Comment)
//        }
//    }
//
//    override fun getItemViewType(position: Int): Int {
//        return getItem(position)!!.type.ordinal
//    }
//
//    companion object {
//        private val diffCallback = UnifiedCommentsListDiffCallback()
//    }
}
