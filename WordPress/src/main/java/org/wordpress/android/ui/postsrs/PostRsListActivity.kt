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
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.PagePostCreationSourcesDetail
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.postsrs.screens.PostRsListScreen
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.extensions.setContent

@AndroidEntryPoint
class PostRsListActivity : BaseAppCompatActivity() {
    private val viewModel: PostRsListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeEvents()

        setContent {
            val tabStates by viewModel.tabStates
                .collectAsState()
            AppThemeM3 {
                PostRsListScreen(
                    tabStates = tabStates,
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
                    onCreatePost = viewModel::createNewPost
                )
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is PostRsListEvent.EditPost ->
                            ActivityLauncher.editPostOrPageForResult(
                                this@PostRsListActivity,
                                event.site,
                                event.post
                            )
                        is PostRsListEvent.CreatePost ->
                            ActivityLauncher.addNewPostForResult(
                                this@PostRsListActivity,
                                event.site,
                                false,
                                PagePostCreationSourcesDetail
                                    .POST_FROM_POSTS_LIST,
                                -1,
                                null
                            )
                        is PostRsListEvent.ShowError ->
                            ToastUtils.showToast(
                                this@PostRsListActivity,
                                event.messageResId
                            )
                    }
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, PostRsListActivity::class.java)
        }
    }
}
