package org.wordpress.android.ui.postsrs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.PagePostCreationSourcesDetail
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.postsrs.screens.PostRsListScreen
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.extensions.setContent
import javax.inject.Inject

@AndroidEntryPoint
class PostRsListActivity : BaseAppCompatActivity() {
    private val viewModel: PostRsListViewModel by viewModels()

    @Inject
    lateinit var selectedSiteRepository: SelectedSiteRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeEvents()

        setContent {
            AppThemeM3 {
                PostRsListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        onBackPressedDispatcher.onBackPressed()
                    },
                    onPostClick = viewModel::openPost,
                    onCreatePost = ::createNewPost
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

    private fun createNewPost() {
        val site = selectedSiteRepository.getSelectedSite()
            ?: return
        ActivityLauncher.addNewPostForResult(
            this,
            site,
            false,
            PagePostCreationSourcesDetail.POST_FROM_POSTS_LIST,
            -1,
            null
        )
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, PostRsListActivity::class.java)
        }
    }
}
