package org.wordpress.android.ui.postsrs

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.PagePostCreationSourcesDetail
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.posts.PostUtils.EntryPoint
import org.wordpress.android.ui.postsrs.screens.PostRsListScreen
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

@AndroidEntryPoint
class PostRsListActivity : BaseAppCompatActivity() {
    private val viewModel by viewModels<PostRsListViewModel>()

    @Inject lateinit var postStore: PostStore
    @Inject lateinit var selectedSiteRepository: SelectedSiteRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val composeView = ComposeView(this)
        setContentView(
            composeView.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    this.isForceDarkAllowed = false
                }
                setViewCompositionStrategy(
                    ViewCompositionStrategy
                        .DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
                    val selectedTab by viewModel
                        .selectedTab.collectAsState()
                    PostRsListScreen(
                        selectedTab = selectedTab,
                        tabStateFlow = viewModel::tabState,
                        onTabSelected = viewModel::onTabSelected,
                        onPostClick = viewModel::onPostClicked,
                        onRefresh = viewModel::refreshPosts,
                        onLoadMore = viewModel::loadMorePosts,
                        onCreatePost = ::createNewPost,
                        onBack = ::finish
                    )
                }
            }
        )

        observeUiEvents()
    }

    private fun observeUiEvents() {
        lifecycleScope.launch {
            viewModel.uiEvent.collect { event ->
                handleUiEvent(event)
            }
        }
    }

    private fun handleUiEvent(event: PostRsListUiEvent) {
        when (event) {
            is PostRsListUiEvent.ShowError -> {
                ToastUtils.showToast(
                    this, event.message, ToastUtils.Duration.LONG
                )
            }
            is PostRsListUiEvent.OpenPost -> {
                openPostEditor(event.remotePostId)
            }
        }
    }

    private fun openPostEditor(remotePostId: Long) {
        val site =
            selectedSiteRepository.getSelectedSite() ?: return
        val post = postStore.getPostByRemotePostId(
            remotePostId, site
        )
        if (post != null) {
            ActivityLauncher.editPostOrPageForResult(
                this, site, post
            )
        } else {
            ToastUtils.showToast(
                this,
                R.string.post_not_found,
                ToastUtils.Duration.SHORT
            )
        }
    }

    private fun createNewPost() {
        val site =
            selectedSiteRepository.getSelectedSite()
                ?: return
        ActivityLauncher.addNewPostForResult(
            this,
            site,
            false,
            PagePostCreationSourcesDetail
                .POST_FROM_POSTS_LIST,
            -1,
            EntryPoint.MY_SITE_CARD_ANSWER_PROMPT
        )
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent {
            return Intent(
                context, PostRsListActivity::class.java
            )
        }
    }
}
