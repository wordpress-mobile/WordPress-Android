package org.wordpress.android.ui.postsrs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.PostStore
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

    @Inject
    lateinit var postStore: PostStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppThemeM3 {
                PostRsListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        onBackPressedDispatcher.onBackPressed()
                    },
                    onPostClick = ::openPost,
                    onCreatePost = ::createNewPost
                )
            }
        }
    }

    /**
     * Opens a post in the editor if it exists in the local FluxC
     * database. This FluxC dependency will be removed once the
     * editor supports loading posts via wordpress-rs.
     */
    private fun openPost(remotePostId: Long) {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            ToastUtils.showToast(this, R.string.blog_not_found)
            return
        }
        val post = postStore.getPostByRemotePostId(
            remotePostId, site
        )
        if (post == null) {
            ToastUtils.showToast(this, R.string.post_not_found)
            return
        }
        ActivityLauncher.editPostOrPageForResult(
            this, site, post
        )
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
