package org.wordpress.android.ui.posts.adapters

import android.arch.paging.PagedListAdapter
import android.content.Context
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.list.PagedListItemType
import org.wordpress.android.fluxc.model.list.PagedListItemType.EndListIndicatorItem
import org.wordpress.android.fluxc.model.list.PagedListItemType.LoadingItem
import org.wordpress.android.fluxc.model.list.PagedListItemType.ReadyItem
import org.wordpress.android.ui.PagedListDiffItemCallback
import org.wordpress.android.ui.posts.PostAdapterItem
import org.wordpress.android.ui.posts.PostViewHolder
import org.wordpress.android.ui.posts.PostViewHolderConfig

private const val VIEW_TYPE_POST = 0
private const val VIEW_TYPE_ENDLIST_INDICATOR = 1
private const val VIEW_TYPE_LOADING = 2

class PostListAdapter(
    context: Context,
    private val postViewHolderConfig: PostViewHolderConfig
) : PagedListAdapter<PagedListItemType<PostAdapterItem>, ViewHolder>(PostListDiffItemCallback) {
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is EndListIndicatorItem -> VIEW_TYPE_ENDLIST_INDICATOR
            is LoadingItem -> VIEW_TYPE_LOADING
            is ReadyItem<PostAdapterItem> -> VIEW_TYPE_POST
            null -> VIEW_TYPE_LOADING // Placeholder by paged list
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ENDLIST_INDICATOR -> {
                val view = layoutInflater.inflate(R.layout.endlist_indicator, parent, false)
                view.layoutParams.height = postViewHolderConfig.endlistIndicatorHeight
                EndListViewHolder(view)
            }
            VIEW_TYPE_LOADING -> {
                val view = layoutInflater.inflate(R.layout.post_cardview_skeleton, parent, false)
                LoadingViewHolder(view)
            }
            VIEW_TYPE_POST -> {
                val view = layoutInflater.inflate(R.layout.post_cardview, parent, false)
                PostViewHolder(view, postViewHolderConfig)
            }
            else -> {
                // Fail fast if a new view type is added so the we can handle it
                throw IllegalStateException("The view type '$viewType' needs to be handled")
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // The only holder type that requires a special setup is the PostViewHolder
        if (holder is PostViewHolder) {
            val item = getItem(position)
            assert(item is ReadyItem) {
                "If we are presenting PostViewHolder, the item has to be of type ReadyItem for position: $position"
            }
            holder.onBind((item as ReadyItem).item)
        }
    }

    private class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view)
    private class EndListViewHolder(view: View) : RecyclerView.ViewHolder(view)
}

private val PostListDiffItemCallback = PagedListDiffItemCallback<PostAdapterItem>(
        getRemoteItemId = { item -> item.data.remotePostId },
        areItemsTheSame = { oldItem, newItem -> oldItem.data.localPostId == newItem.data.localPostId },
        areContentsTheSame = { oldItem, newItem -> oldItem.data == newItem.data }
)
