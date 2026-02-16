package org.wordpress.android.ui.postsrs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.PagePostCreationSourcesDetail
import org.wordpress.android.ui.blaze.BlazeFlowSource
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.postsrs.screens.PostRsListScreen
import org.wordpress.android.ui.reader.ReaderActivityLauncher
import org.wordpress.android.ui.reader.ReaderPostPagerActivity.DirectOperation
import org.wordpress.android.ui.stats.refresh.lists.detail.StatsDetailActivity
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.extensions.setContent
import javax.inject.Inject

@AndroidEntryPoint
class PostRsListActivity : BaseAppCompatActivity() {
    @Inject
    lateinit var dispatcher: Dispatcher

    private val viewModel: PostRsListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeEvents()

        setContent {
            val tabStates by viewModel.tabStates
                .collectAsState()
            val isSearchActive by viewModel.isSearchActive
                .collectAsState()
            val searchQuery by viewModel.searchQuery
                .collectAsState()
            val authorFilter by viewModel.authorFilter
                .collectAsState()
            AppThemeM3 {
                PostRsListScreen(
                    tabStates = tabStates,
                    isSearchActive = isSearchActive,
                    searchQuery = searchQuery,
                    isAuthorFilterVisible =
                        viewModel.isAuthorFilterVisible,
                    authorFilter = authorFilter,
                    onSearchOpen =
                        viewModel::onSearchOpen,
                    onSearchQueryChanged =
                        viewModel::onSearchQueryChanged,
                    onSearchClose =
                        viewModel::onSearchClose,
                    onAuthorFilterChanged =
                        viewModel::onAuthorFilterChanged,
                    onInitTab = viewModel::initTab,
                    onRefreshTab = { tab ->
                        viewModel.refreshTab(
                            tab, isUserRefresh = true
                        )
                    },
                    onLoadMore = viewModel::loadMorePosts,
                    onNavigateBack = {
                        onBackPressedDispatcher.onBackPressed()
                    },
                    onPostClick = viewModel::openPost,
                    onPostMenuAction =
                        viewModel::onPostMenuAction,
                    onCreatePost = viewModel::createNewPost
                )
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    handleEvent(event)
                }
            }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun handleEvent(event: PostRsListEvent) {
        when (event) {
            is PostRsListEvent.EditPost ->
                ActivityLauncher.editPostOrPageForResult(
                    this, event.site, event.post
                )
            is PostRsListEvent.CreatePost ->
                ActivityLauncher.addNewPostForResult(
                    this, event.site, false,
                    PagePostCreationSourcesDetail
                        .POST_FROM_POSTS_LIST,
                    -1, null
                )
            is PostRsListEvent.ShowError ->
                ToastUtils.showToast(this, event.messageResId)
            is PostRsListEvent.ViewPost ->
                ActivityLauncher.openUrlExternal(
                    this, event.url
                )
            is PostRsListEvent.ReadPost ->
                ReaderActivityLauncher.showReaderPostDetail(
                    this, event.blogId, event.postId
                )
            is PostRsListEvent.MoveToDraft -> {
                event.post.setStatus(
                    PostStatus.DRAFT.toString()
                )
                dispatcher.dispatch(
                    PostActionBuilder.newPushPostAction(
                        RemotePostPayload(
                            event.post, event.site
                        )
                    )
                )
            }
            is PostRsListEvent.SharePost ->
                ActivityLauncher.openShareIntent(
                    this, event.url, event.title
                )
            is PostRsListEvent.PromoteWithBlaze ->
                ActivityLauncher.openPromoteWithBlaze(
                    this, event.post,
                    BlazeFlowSource.POSTS_LIST
                )
            is PostRsListEvent.ViewStats ->
                StatsDetailActivity.start(
                    this, event.site,
                    event.remotePostId, "post",
                    event.postTitle, event.postUrl
                )
            is PostRsListEvent.ViewComments ->
                ReaderActivityLauncher.showReaderPostDetail(
                    this, false,
                    event.blogId, event.postId,
                    DirectOperation.COMMENT_JUMP,
                    false
                )
            is PostRsListEvent.TrashPost ->
                dispatcher.dispatch(
                    PostActionBuilder.newDeletePostAction(
                        RemotePostPayload(
                            event.post, event.site
                        )
                    )
                )
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, PostRsListActivity::class.java)
        }
    }
}
