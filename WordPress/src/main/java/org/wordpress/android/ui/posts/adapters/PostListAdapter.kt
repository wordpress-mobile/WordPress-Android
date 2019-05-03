package org.wordpress.android.ui.posts.adapters

import android.arch.paging.PagedListAdapter
import android.content.Context
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.ui.posts.PostListItemViewHolder
import org.wordpress.android.ui.posts.PostListViewLayoutType
import org.wordpress.android.ui.posts.PostListViewLayoutType.COMPACT
import org.wordpress.android.ui.posts.PostListViewLayoutType.STANDARD
import org.wordpress.android.ui.posts.PostViewHolderConfig
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.viewmodel.posts.PostListItemProgressBar
import org.wordpress.android.viewmodel.posts.PostListItemType
import org.wordpress.android.viewmodel.posts.PostListItemType.EndListIndicatorItem
import org.wordpress.android.viewmodel.posts.PostListItemType.LoadingItem
import org.wordpress.android.viewmodel.posts.PostListItemType.PostListItemUiState

private const val VIEW_TYPE_POST = 0
private const val VIEW_TYPE_POST_COMPACT = 1
private const val VIEW_TYPE_ENDLIST_INDICATOR = 2
private const val VIEW_TYPE_LOADING = 3
private const val VIEW_TYPE_LOADING_COMPACT = 4

class PostListAdapter(
    context: Context,
    private val postViewHolderConfig: PostViewHolderConfig,
    private val uiHelpers: UiHelpers
) : PagedListAdapter<PostListItemType, ViewHolder>(PostListDiffItemCallback) {
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    private var itemLayoutType: PostListViewLayoutType = PostListViewLayoutType.defaultValue

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is EndListIndicatorItem -> VIEW_TYPE_ENDLIST_INDICATOR
            is PostListItemUiState -> {
                when (itemLayoutType) {
                    STANDARD -> VIEW_TYPE_POST
                    COMPACT -> VIEW_TYPE_POST_COMPACT
                }
            }
            is LoadingItem, null -> {
                when (itemLayoutType) {
                    STANDARD -> VIEW_TYPE_LOADING
                    COMPACT -> VIEW_TYPE_LOADING_COMPACT
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ENDLIST_INDICATOR -> {
                val view = layoutInflater.inflate(R.layout.list_with_fab_endlist_indicator, parent, false)
                EndListViewHolder(view)
            }
            VIEW_TYPE_LOADING -> {
                val view = layoutInflater.inflate(R.layout.post_list_item_skeleton, parent, false)
                LoadingViewHolder(view)
            }
            VIEW_TYPE_LOADING_COMPACT -> {
                val view = layoutInflater.inflate(R.layout.post_list_item_skeleton_compact, parent, false)
                LoadingViewHolder(view)
            }
            VIEW_TYPE_POST -> {
                PostListItemViewHolder.Standard(parent, postViewHolderConfig, uiHelpers)
            }
            VIEW_TYPE_POST_COMPACT -> {
                PostListItemViewHolder.Compact(parent, postViewHolderConfig, uiHelpers)
            }
            else -> {
                // Fail fast if a new view type is added so the we can handle it
                throw IllegalStateException("The view type '$viewType' needs to be handled")
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // The only holders that require special setup are PostListItemViewHolder sealed subclasses
        if (holder is PostListItemViewHolder) {
            val item = getItem(position)
            assert(item is PostListItemUiState) {
                "If we are presenting PostViewHolder, the item has to be of type PostListItemUiState " +
                        "for position: $position"
            }
            holder.onBind((item as PostListItemUiState))
        }
    }

    fun updateItemLayoutType(updatedItemLayoutType: PostListViewLayoutType) {
        if (updatedItemLayoutType == itemLayoutType) {
            return
        }
        itemLayoutType = updatedItemLayoutType
        notifyDataSetChanged()
    }

    private class LoadingViewHolder(view: View) : ViewHolder(view)
    private class EndListViewHolder(view: View) : ViewHolder(view)
}

private val PostListDiffItemCallback = object : DiffUtil.ItemCallback<PostListItemType>() {
    override fun areItemsTheSame(oldItem: PostListItemType, newItem: PostListItemType): Boolean {
        if (oldItem is EndListIndicatorItem && newItem is EndListIndicatorItem) {
            return true
        }
        if (oldItem is LoadingItem && newItem is LoadingItem) {
            return oldItem.localOrRemoteId == newItem.localOrRemoteId
        }
        if (oldItem is PostListItemUiState && newItem is PostListItemUiState) {
            return oldItem.data.localPostId == newItem.data.localPostId
        }
        if (oldItem is LoadingItem && newItem is PostListItemUiState) {
            return when (oldItem.localOrRemoteId) {
                is LocalId -> oldItem.localOrRemoteId == newItem.data.localPostId.id
                is RemoteId -> oldItem.localOrRemoteId == newItem.data.remotePostId.id
            }
        }
        return false
    }

    override fun areContentsTheSame(oldItem: PostListItemType, newItem: PostListItemType): Boolean {
        if (oldItem is EndListIndicatorItem && newItem is EndListIndicatorItem) {
            return true
        }
        if (oldItem is LoadingItem && newItem is LoadingItem) {
            return oldItem.localOrRemoteId == newItem.localOrRemoteId
        }
        if (oldItem is PostListItemUiState && newItem is PostListItemUiState) {
            return oldItem.data == newItem.data
        }
        return false
    }

    override fun getChangePayload(oldItem: PostListItemType, newItem: PostListItemType): Any? {
        if (oldItem is PostListItemUiState && newItem is PostListItemUiState) {
            /**
             * Suppresses the default animation if the progress has changed to prevent blinking as the upload progresses
             *
             * We don't need to use the payload in onBindViewHolder unless we want to. Passing a non-null value
             * suppresses the default ItemAnimator, which is all we need in this case.
             */
            if (oldItem.data.progressBarState is PostListItemProgressBar.Determinate &&
                    newItem.data.progressBarState is PostListItemProgressBar.Determinate &&
                    oldItem.data.progressBarState.progress != newItem.data.progressBarState.progress) {
                return true
            }
        }
        return super.getChangePayload(oldItem, newItem)
    }
}
