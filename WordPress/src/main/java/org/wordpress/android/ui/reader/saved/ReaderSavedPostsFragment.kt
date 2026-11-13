package org.wordpress.android.ui.reader.saved

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.ui.reader.ReaderActivityLauncher
import org.wordpress.android.ui.reader.repository.ReaderPostListRepository
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyButton
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
import org.wordpress.android.ui.reader.views.compose.MeasureOnLayoutComposeView
import org.wordpress.android.widgets.WPSnackbar
import uniffi.wp_api.ReaderSavedKey
import uniffi.wp_mobile.ReaderSavedListItem
import javax.inject.Inject

/**
 * The Saved tab once saves are synced with the account. See [ReaderSavedPostsViewModel].
 */
@AndroidEntryPoint
class ReaderSavedPostsFragment : Fragment(), WPMainActivity.OnScrollToTopListener {
    @Inject
    lateinit var readerPostListRepository: ReaderPostListRepository

    private val viewModel: ReaderSavedPostsViewModel by viewModels()
    private val listState = LazyListState()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val actions = ReaderSavedPostsActions(
            onRefresh = viewModel::refresh,
            onLoadMore = viewModel::loadMore,
            onItemClicked = viewModel::onItemClicked,
            onUnsaveClicked = viewModel::onUnsaveClicked,
            onEmptyStateButtonClicked = ::onEmptyStateButtonClicked
        )
        return MeasureOnLayoutComposeView(requireContext(), {
            AppThemeM3 {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                ReaderSavedPostsScreen(uiState = uiState, listState = listState, actions = actions)
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect(::handleEvent)
            }
        }
        viewModel.start()
    }

    override fun onScrollToTop() {
        listState.requestScrollToItem(0)
    }

    private fun handleEvent(event: ReaderSavedPostsViewModel.Event) {
        when (event) {
            is ReaderSavedPostsViewModel.Event.OpenItem -> openItem(event.item)
            is ReaderSavedPostsViewModel.Event.Unsaved -> {
                val snackbar = WPSnackbar.make(
                    snackbarParent(),
                    getString(R.string.reader_saved_post_removed),
                    Snackbar.LENGTH_LONG
                )
                if (event.item.post != null) {
                    snackbar.setAction(R.string.undo) { viewModel.onUndoUnsave(event.item) }
                }
                snackbar.show()
            }
            ReaderSavedPostsViewModel.Event.RequestFailed -> showMessage(R.string.reader_error_request_failed_title)
            ReaderSavedPostsViewModel.Event.NoNetwork -> showMessage(R.string.error_network_connection)
        }
    }

    private fun openItem(item: ReaderSavedListItem) {
        val context = requireContext()
        when (val key = item.key) {
            is ReaderSavedKey.Post -> ReaderActivityLauncher.showReaderPostDetail(
                context,
                key.blogId.toLong(),
                key.postId
            )
            is ReaderSavedKey.FeedItem -> {
                val identity = item.post?.identity
                val feedId = identity?.feedId?.toLong()
                if (feedId != null) {
                    ReaderActivityLauncher.showReaderPostDetail(
                        context = context,
                        isFeed = true,
                        blogId = feedId,
                        postId = key.feedItemId.toLong(),
                        directOperation = null,
                        isRelatedPost = false
                    )
                } else {
                    ReaderActivityLauncher.openUrl(context, identity?.url)
                }
            }
            is ReaderSavedKey.Url -> ReaderActivityLauncher.openUrl(context, key.url)
        }
    }

    private fun onEmptyStateButtonClicked(button: ReaderPostListEmptyButton) {
        val path = when (button) {
            ReaderPostListEmptyButton.DISCOVER -> ReaderTag.DISCOVER_PATH
            ReaderPostListEmptyButton.FOLLOWED -> ReaderTag.FOLLOWING_PATH
        }
        val readerViewModel = ViewModelProvider(requireParentFragment())[ReaderViewModel::class.java]
        viewLifecycleOwner.lifecycleScope.launch {
            readerViewModel.updateSelectedContent(readerPostListRepository.getTagFromEndpointOrDefault(path))
        }
    }

    private fun showMessage(stringRes: Int) {
        WPSnackbar.make(snackbarParent(), getString(stringRes), Snackbar.LENGTH_LONG).show()
    }

    // the main activity's coordinator, which sits above the bottom navigation
    private fun snackbarParent(): View = requireActivity().findViewById(R.id.coordinator) ?: requireView()
}
