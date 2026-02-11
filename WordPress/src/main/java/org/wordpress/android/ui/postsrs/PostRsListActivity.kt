package org.wordpress.android.ui.postsrs

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.PagePostCreationSourcesDetail
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.posts.PostUtils.EntryPoint
import org.wordpress.android.ui.postsrs.screens.PostRsTabListScreen
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
                    PostRsListContent()
                }
            }
        )

        observeUiEvents()
    }

    private fun observeUiEvents() {
        lifecycleScope.launch {
            viewModel.uiEvent.filterNotNull().collect { event ->
                handleUiEvent(event)
                viewModel.consumeEvent()
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PostRsListContent() {
        val selectedTab by viewModel.selectedTab.collectAsState()

        AppThemeM3 {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                stringResource(
                                    R.string.my_site_btn_blog_posts
                                )
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled
                                        .ArrowBack,
                                    contentDescription =
                                        stringResource(R.string.back)
                                )
                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { createNewPost() },
                        containerColor = MaterialTheme
                            .colorScheme.primaryContainer,
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription =
                                stringResource(
                                    R.string.new_post
                                )
                        )
                    }
                }
            ) { contentPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                ) {
                    PrimaryScrollableTabRow(
                        selectedTabIndex = selectedTab.ordinal
                    ) {
                        PostRsListTab.entries.forEach { tab ->
                            Tab(
                                selected = tab == selectedTab,
                                onClick = {
                                    viewModel.onTabSelected(tab)
                                },
                                text = {
                                    Text(
                                        stringResource(
                                            tab.titleResId
                                        )
                                    )
                                }
                            )
                        }
                    }

                    TabContent(selectedTab)
                }
            }
        }
    }

    @Composable
    private fun TabContent(tab: PostRsListTab) {
        val state by viewModel.tabState(tab)
            .collectAsState()
        val emptyMessage = stringResource(
            when (tab) {
                PostRsListTab.PUBLISHED ->
                    R.string.posts_empty_list
                PostRsListTab.DRAFTS ->
                    R.string.posts_draft_empty
                PostRsListTab.SCHEDULED ->
                    R.string.posts_scheduled_empty
                PostRsListTab.TRASHED ->
                    R.string.posts_trashed_empty
            }
        )

        LaunchedEffect(tab) {
            viewModel.onTabSelected(tab)
        }

        PostRsTabListScreen(
            state = state,
            emptyMessage = emptyMessage,
            onPostClick = { viewModel.onPostClicked(it) },
            onRefresh = { viewModel.refreshPosts(tab) },
            onLoadMore = { viewModel.loadMorePosts(tab) },
            onCreatePost = { createNewPost() }
        )
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
