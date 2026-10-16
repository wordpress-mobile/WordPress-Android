package org.wordpress.android.ui.reader.adapters

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.databinding.ReaderCardviewRemovedPostBinding
import org.wordpress.android.databinding.ReaderCardviewXpostBinding
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.ReaderInterfaces.OnFollowListener
import org.wordpress.android.ui.reader.discover.viewholders.ReaderPostNewViewHolder
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.utils.ReaderXPostUtils
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListItem
import org.wordpress.android.ui.reader.views.ReaderGapMarkerView
import org.wordpress.android.ui.reader.views.ReaderSiteHeaderView
import org.wordpress.android.ui.reader.views.ReaderTagHeaderView
import org.wordpress.android.ui.reader.views.ReaderTagHeaderViewUiState.ReaderTagHeaderUiState
import org.wordpress.android.ui.reader.views.uistates.FollowButtonUiState
import org.wordpress.android.util.ColorUtils
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.WPAvatarUtils
import org.wordpress.android.util.extensions.getColorResIdFromAttribute
import org.wordpress.android.util.image.BlavatarShape
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType

/**
 * Shows the rows built by ReaderPostListViewModel - it has no state of its own
 */
class ReaderPostListAdapter(
    private val imageManager: ImageManager,
    private val readerTracker: ReaderTracker,
    private val listener: Listener
) : ListAdapter<ReaderPostListItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
    interface Listener {
        val source: String
        val followListener: OnFollowListener
        val blogInfoFailedListener: ReaderSiteHeaderView.OnBlogInfoFailedListener?
        val subscriptionSettingsClickListener: ReaderSiteHeaderView.OnSubscriptionSettingsClickListener
        fun onLastItemBound()
        fun onXPostClicked(post: ReaderPost)
        fun onRemovedPostClicked(post: ReaderPost)
        fun onTagHeaderFollowClicked()
        fun onGapMarkerClicked()
    }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = getItem(position).stableId

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is ReaderPostListItem.Post -> VIEW_TYPE_POST
        is ReaderPostListItem.XPost -> VIEW_TYPE_XPOST
        is ReaderPostListItem.SiteHeader -> VIEW_TYPE_SITE_HEADER
        is ReaderPostListItem.TagHeader -> VIEW_TYPE_TAG_HEADER
        is ReaderPostListItem.GapMarker -> VIEW_TYPE_GAP_MARKER
        is ReaderPostListItem.RemovedPost -> VIEW_TYPE_REMOVED_POST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        return when (viewType) {
            VIEW_TYPE_SITE_HEADER -> SiteHeaderViewHolder(ReaderSiteHeaderView(context).apply {
                setOnFollowListener(listener.followListener)
            })
            VIEW_TYPE_TAG_HEADER -> TagHeaderViewHolder(ReaderTagHeaderView(context))
            VIEW_TYPE_GAP_MARKER -> GapMarkerViewHolder(ReaderGapMarkerView(context).apply {
                setOnFillClickListener { listener.onGapMarkerClicked() }
            })
            VIEW_TYPE_XPOST -> XPostViewHolder(ReaderCardviewXpostBinding.inflate(inflater, parent, false))
            VIEW_TYPE_REMOVED_POST -> RemovedPostViewHolder(
                ReaderCardviewRemovedPostBinding.inflate(inflater, parent, false)
            )
            else -> ReaderPostNewViewHolder(readerTracker, parent)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ReaderPostListItem.Post -> (holder as ReaderPostNewViewHolder).onBind(item.uiState)
            is ReaderPostListItem.XPost -> (holder as XPostViewHolder).bind(item.post)
            is ReaderPostListItem.RemovedPost -> (holder as RemovedPostViewHolder).bind(item.post)
            is ReaderPostListItem.SiteHeader -> (holder as SiteHeaderViewHolder).bind(item)
            is ReaderPostListItem.TagHeader -> (holder as TagHeaderViewHolder).bind(item)
            is ReaderPostListItem.GapMarker -> (holder as GapMarkerViewHolder).view.setFilling(item.isFilling)
        }
        // only post rows load more, as before
        val item = getItem(position)
        if (position >= itemCount - 1 && (item is ReaderPostListItem.Post || item is ReaderPostListItem.XPost)) {
            listener.onLastItemBound()
        }
    }

    private inner class XPostViewHolder(
        private val binding: ReaderCardviewXpostBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private val avatarSize = itemView.resources.getDimensionPixelSize(R.dimen.avatar_sz_small)
        private var post: ReaderPost? = null

        init {
            binding.postContainer.setOnClickListener { post?.let { listener.onXPostClicked(it) } }
        }

        fun bind(post: ReaderPost) = with(binding) {
            this@XPostViewHolder.post = post
            imageManager.loadIntoCircle(
                imageAvatar,
                ImageType.AVATAR_WITH_BACKGROUND,
                WPAvatarUtils.rewriteAvatarUrl(post.postAvatar, avatarSize)
            )
            imageManager.loadIntoCircle(
                imageBlavatar,
                SiteUtils.getSiteImageType(post.isP2orA8C, BlavatarShape.CIRCULAR),
                WPAvatarUtils.rewriteAvatarUrl(post.blogImageUrl, avatarSize)
            )
            textTitle.text = ReaderXPostUtils.getXPostTitle(post)
            textSubtitle.text = ReaderXPostUtils.getXPostSubtitleHtml(post)
        }
    }

    private inner class RemovedPostViewHolder(
        private val binding: ReaderCardviewRemovedPostBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private var post: ReaderPost? = null

        init {
            val context = itemView.context
            val drawable = ColorUtils.applyTintToDrawable(
                context,
                R.drawable.ic_undo_white_24dp,
                context.getColorResIdFromAttribute(androidx.appcompat.R.attr.colorPrimary)
            )
            binding.undoRemove.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
            binding.postContainer.setOnClickListener { post?.let { listener.onRemovedPostClicked(it) } }
        }

        fun bind(post: ReaderPost) {
            this.post = post
            // "Removed [post title]" with the title in bold
            val removed = itemView.context.getString(R.string.removed)
            val text = "$removed ${post.title}"
            binding.removedPostTitle.text = SpannableStringBuilder(text).apply {
                setSpan(StyleSpan(Typeface.BOLD), removed.length, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private inner class SiteHeaderViewHolder(val view: ReaderSiteHeaderView) : RecyclerView.ViewHolder(view) {
        fun bind(item: ReaderPostListItem.SiteHeader) {
            view.setOnBlogInfoFailedListener(listener.blogInfoFailedListener)
            view.setOnSubscriptionSettingsClickListener(listener.subscriptionSettingsClickListener)
            view.loadBlogInfo(item.blogId, item.feedId, listener.source)
        }
    }

    private inner class TagHeaderViewHolder(val view: ReaderTagHeaderView) : RecyclerView.ViewHolder(view) {
        fun bind(item: ReaderPostListItem.TagHeader) {
            view.updateUi(
                ReaderTagHeaderUiState(
                    title = item.label,
                    followButtonUiState = FollowButtonUiState(
                        onFollowButtonClicked = { listener.onTagHeaderFollowClicked() },
                        isFollowed = item.isFollowed,
                        isFollowActionRunning = item.isFollowActionRunning
                    )
                )
            )
        }
    }

    private class GapMarkerViewHolder(val view: ReaderGapMarkerView) : RecyclerView.ViewHolder(view)

    companion object {
        private const val VIEW_TYPE_POST = 0
        private const val VIEW_TYPE_XPOST = 1
        private const val VIEW_TYPE_SITE_HEADER = 2
        private const val VIEW_TYPE_TAG_HEADER = 3
        private const val VIEW_TYPE_GAP_MARKER = 4
        private const val VIEW_TYPE_REMOVED_POST = 5

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ReaderPostListItem>() {
            override fun areItemsTheSame(oldItem: ReaderPostListItem, newItem: ReaderPostListItem) =
                oldItem.stableId == newItem.stableId && oldItem::class == newItem::class

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: ReaderPostListItem, newItem: ReaderPostListItem) =
                oldItem == newItem
        }
    }
}
