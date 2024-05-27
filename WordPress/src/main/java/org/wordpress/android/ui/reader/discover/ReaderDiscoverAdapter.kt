package org.wordpress.android.ui.reader.discover

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderAnnouncementCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestsCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostNewUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderRecommendedBlogsCardUiState
import org.wordpress.android.ui.reader.discover.viewholders.ReaderAnnouncementCardViewHolder
import org.wordpress.android.ui.reader.discover.viewholders.ReaderInterestsCardNewViewHolder
import org.wordpress.android.ui.reader.discover.viewholders.ReaderInterestsCardViewHolder
import org.wordpress.android.ui.reader.discover.viewholders.ReaderPostNewViewHolder
import org.wordpress.android.ui.reader.discover.viewholders.ReaderPostViewHolder
import org.wordpress.android.ui.reader.discover.viewholders.ReaderRecommendedBlogsCardNewViewHolder
import org.wordpress.android.ui.reader.discover.viewholders.ReaderRecommendedBlogsCardViewHolder
import org.wordpress.android.ui.reader.discover.viewholders.ReaderViewHolder
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.utils.HideItemDivider
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.image.ImageManager

private const val POST_VIEW_TYPE: Int = 1
private const val INTEREST_VIEW_TYPE: Int = 2
private const val RECOMMENDED_BLOGS_VIEW_TYPE: Int = 3
private const val POST_NEW_VIEW_TYPE: Int = 4
private const val READER_ANNOUNCEMENT_TYPE: Int = 5

class ReaderDiscoverAdapter(
    private val uiHelpers: UiHelpers,
    private val imageManager: ImageManager,
    private val readerTracker: ReaderTracker,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val isReaderImprovementsEnabled: Boolean,
) : Adapter<ReaderViewHolder<*>>() {
    private val items = mutableListOf<ReaderCardUiState>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReaderViewHolder<*> {
        return when (viewType) {
            POST_VIEW_TYPE -> ReaderPostViewHolder(uiHelpers, imageManager, readerTracker, parent)
            POST_NEW_VIEW_TYPE -> ReaderPostNewViewHolder(
                uiHelpers,
                imageManager,
                readerTracker,
                networkUtilsWrapper,
                parent
            )

            INTEREST_VIEW_TYPE -> {
                if (isReaderImprovementsEnabled) {
                    ReaderInterestsCardNewViewHolder(uiHelpers, parent)
                } else {
                    ReaderInterestsCardViewHolder(uiHelpers, parent)
                }
            }

            RECOMMENDED_BLOGS_VIEW_TYPE ->
                if (isReaderImprovementsEnabled) {
                    ReaderRecommendedBlogsCardNewViewHolder(
                        parent, imageManager
                    )
                } else {
                    ReaderRecommendedBlogsCardViewHolder(
                        parent, imageManager, uiHelpers
                    )
                }

            READER_ANNOUNCEMENT_TYPE -> ReaderAnnouncementCardViewHolder(parent)

            else -> throw NotImplementedError("Unknown ViewType")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ReaderViewHolder<*>, position: Int) {
        if (isReaderImprovementsEnabled) {
            // hide the item divider by setting the HideItemDivider object as view tag, which is used by the
            // DividerItemDecorator to skip drawing the bottom divider for the item. It should be hidden for any
            // recommendation cards and cards above them.
            val nextPosition = position + 1
            val shouldHideDivider = isRecommendationCard(position) ||
                    (nextPosition < itemCount && isRecommendationCard(nextPosition))
            holder.itemView.tag = HideItemDivider.takeIf { shouldHideDivider }
        }

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
            is ReaderPostUiState -> POST_VIEW_TYPE
            is ReaderPostNewUiState -> POST_NEW_VIEW_TYPE
            is ReaderInterestsCardUiState -> INTEREST_VIEW_TYPE
            is ReaderRecommendedBlogsCardUiState -> RECOMMENDED_BLOGS_VIEW_TYPE
            is ReaderAnnouncementCardUiState -> READER_ANNOUNCEMENT_TYPE
        }
    }

    private fun isRecommendationCard(position: Int): Boolean {
        val type = getItemViewType(position)
        return type in listOf(INTEREST_VIEW_TYPE, RECOMMENDED_BLOGS_VIEW_TYPE)
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

                is ReaderPostNewUiState -> {
                    oldItem.postId == (newItem as ReaderPostNewUiState).postId && oldItem.blogId == newItem.blogId
                }

                is ReaderRecommendedBlogsCardUiState -> {
                    val newItemState = newItem as? ReaderRecommendedBlogsCardUiState
                    oldItem.blogs.map { it.blogId to it.feedId } == newItemState?.blogs?.map { it.blogId to it.feedId }
                }

                is ReaderInterestsCardUiState, is ReaderAnnouncementCardUiState -> {
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
