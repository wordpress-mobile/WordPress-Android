package org.wordpress.android.ui.reader.discover

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestsCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderRecommendedBlogsCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderWelcomeBannerCardUiState
import org.wordpress.android.ui.reader.discover.viewholders.ReaderInterestsCardViewHolder
import org.wordpress.android.ui.reader.discover.viewholders.ReaderPostViewHolder
import org.wordpress.android.ui.reader.discover.viewholders.ReaderRecommendedBlogsCardViewHolder
import org.wordpress.android.ui.reader.discover.viewholders.ReaderViewHolder
import org.wordpress.android.ui.reader.discover.viewholders.WelcomeBannerViewHolder
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager

private const val welcomeBannerViewType: Int = 1
private const val postViewType: Int = 2
private const val interestViewType: Int = 3
private const val recommendedBlogsViewType: Int = 4

class ReaderDiscoverAdapter(
    private val uiHelpers: UiHelpers,
    private val imageManager: ImageManager,
    private val readerTracker: ReaderTracker
) : Adapter<ReaderViewHolder<*>>() {
    private val items = mutableListOf<ReaderCardUiState>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReaderViewHolder<*> {
        return when (viewType) {
            welcomeBannerViewType -> WelcomeBannerViewHolder(parent)
            postViewType -> ReaderPostViewHolder(uiHelpers, imageManager, readerTracker, parent)
            interestViewType -> ReaderInterestsCardViewHolder(uiHelpers, parent)
            recommendedBlogsViewType -> ReaderRecommendedBlogsCardViewHolder(parent, imageManager, uiHelpers)
            else -> throw NotImplementedError("Unknown ViewType")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ReaderViewHolder<*>, position: Int) {
        holder.onBind(items[position])
    }

    fun update(newItems: List<ReaderCardUiState>) {
        val diffResult = DiffUtil.calculateDiff(DiscoverDiffUtil(items, newItems))
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ReaderWelcomeBannerCardUiState -> welcomeBannerViewType
            is ReaderPostUiState -> postViewType
            is ReaderInterestsCardUiState -> interestViewType
            is ReaderRecommendedBlogsCardUiState -> recommendedBlogsViewType
        }
    }

    private class DiscoverDiffUtil(
        val oldItems: List<ReaderCardUiState>,
        val newItems: List<ReaderCardUiState>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]
            if (oldItem::class != newItem::class) {
                return false
            }
            return when (oldItem) {
                is ReaderPostUiState -> {
                    oldItem.postId == (newItem as ReaderPostUiState).postId && oldItem.blogId == newItem.blogId
                }
                is ReaderRecommendedBlogsCardUiState -> {
                    val newItemState = newItem as? ReaderRecommendedBlogsCardUiState
                    oldItem.blogs.map { it.blogId to it.feedId } == newItemState?.blogs?.map { it.blogId to it.feedId }
                }
                is ReaderWelcomeBannerCardUiState,
                is ReaderInterestsCardUiState -> {
                    oldItem == newItem
                }
            }
        }

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            /* Returning true suppresses the default item animation. That's all we need - posts in Reader are static
            except of the like/follow/bookmark/... action states. We don't want to play the default animation when
            one of these states changes. */
            return true
        }
    }
}
