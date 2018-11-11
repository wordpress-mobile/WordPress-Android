package org.wordpress.android.ui.posts.adapters

import android.arch.paging.PagedListAdapter
import android.content.Context
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.list.PagedListItemType
import org.wordpress.android.fluxc.model.list.PagedListItemType.EndListIndicatorItem
import org.wordpress.android.fluxc.model.list.PagedListItemType.LoadingItem
import org.wordpress.android.fluxc.model.list.PagedListItemType.ReadyItem
import org.wordpress.android.ui.posts.PostAdapterItem
import org.wordpress.android.ui.posts.PostViewHolder
import org.wordpress.android.ui.posts.PostViewHolderConfig
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

private const val VIEW_TYPE_POST = 0
private const val VIEW_TYPE_ENDLIST_INDICATOR = 1
private const val VIEW_TYPE_LOADING = 2

class PostListAdapter(
    context: Context,
    isAztecEditorEnabled: Boolean,
    hasCapabilityPublishPosts: Boolean,
    isPhotonCapable: Boolean
) : PagedListAdapter<PagedListItemType<PostAdapterItem>, ViewHolder>(DiffItemCallback) {
    private val endlistIndicatorHeight: Int
    private val layoutInflater: LayoutInflater
    private val postViewHolderConfig: PostViewHolderConfig

    @Inject internal lateinit var imageManager: ImageManager

    init {
        (context.applicationContext as WordPress).component().inject(this)

        layoutInflater = LayoutInflater.from(context)
        val displayWidth = DisplayUtils.getDisplayPixelWidth(context)
        val contentSpacing = context.resources.getDimensionPixelSize(R.dimen.content_margin)
        // endlist indicator height is hard-coded here so that its horizontal line is in the middle of the fab
        endlistIndicatorHeight = DisplayUtils.dpToPx(context, 74)

        postViewHolderConfig = PostViewHolderConfig(
                photonWidth = displayWidth - contentSpacing * 2,
                photonHeight = context.resources.getDimensionPixelSize(R.dimen.reader_featured_image_height),
                isPhotonCapable = isPhotonCapable,
                showAllButtons = displayWidth >= 1080, // on larger displays we can always show all buttons
                imageManager = imageManager,
                isAztecEditorEnabled = isAztecEditorEnabled,
                hasCapabilityPublishPosts = hasCapabilityPublishPosts
        )
    }

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
                view.layoutParams.height = endlistIndicatorHeight
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

private object DiffItemCallback : DiffUtil.ItemCallback<PagedListItemType<PostAdapterItem>>() {
    override fun areItemsTheSame(
        oldItem: PagedListItemType<PostAdapterItem>,
        newItem: PagedListItemType<PostAdapterItem>
    ): Boolean {
        if (oldItem is EndListIndicatorItem && newItem is EndListIndicatorItem) {
            return true
        }
        if (oldItem is LoadingItem && newItem is LoadingItem) {
            return oldItem.remoteItemId == newItem.remoteItemId
        }
        if (oldItem is ReadyItem && newItem is ReadyItem) {
            return oldItem.item.data.localPostId == newItem.item.data.localPostId
        }
        if (oldItem is LoadingItem && newItem is ReadyItem) {
            return oldItem.remoteItemId == newItem.item.data.remotePostId
        }
        return false
    }

    override fun areContentsTheSame(
        oldItem: PagedListItemType<PostAdapterItem>,
        newItem: PagedListItemType<PostAdapterItem>
    ): Boolean {
        if (oldItem is EndListIndicatorItem && newItem is EndListIndicatorItem) {
            return true
        }
        if (oldItem is LoadingItem && newItem is LoadingItem) {
            return true
        }
        if (oldItem is ReadyItem && newItem is ReadyItem) {
            return oldItem.item.data == newItem.item.data
        }
        return false
    }
}
