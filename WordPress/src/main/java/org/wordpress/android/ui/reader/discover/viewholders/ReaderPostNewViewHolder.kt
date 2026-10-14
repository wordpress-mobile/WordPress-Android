package org.wordpress.android.ui.reader.discover.viewholders

import android.view.ViewGroup
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.compose.theme.AppThemeM3WithoutBackground
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.views.compose.postcard.ReaderPostCard
import org.wordpress.android.util.NetworkUtilsWrapper

class ReaderPostNewViewHolder(
    private val readerTracker: ReaderTracker,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    parentView: ViewGroup
) : RecyclerView.ViewHolder(ComposeView(parentView.context)) {
    private val uiState = mutableStateOf<ReaderPostUiState?>(null)

    init {
        itemView.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        (itemView as ComposeView).setContent {
            uiState.value?.let { state ->
                AppThemeM3WithoutBackground {
                    ReaderPostCard(
                        uiState = state,
                        onCardClick = { onCardClicked(state) },
                        isNetworkAvailable = networkUtilsWrapper::isNetworkAvailable,
                    )
                }
            }
        }
    }

    fun onBind(state: ReaderPostUiState) {
        uiState.value = state
        // The adapter re-binds with the menu items when the more button is tapped
        if (state.moreMenuItems != null) {
            readerTracker.track(AnalyticsTracker.Stat.POST_CARD_MORE_TAPPED)
        }
        state.onItemRendered(state)
    }

    private fun onCardClicked(state: ReaderPostUiState) {
        readerTracker.trackBlog(
            AnalyticsTracker.Stat.READER_POST_CARD_TAPPED,
            state.blogId,
            state.feedId,
            state.isFollowed,
            state.source
        )
        state.onItemClicked(state.postId, state.blogId)
    }
}
