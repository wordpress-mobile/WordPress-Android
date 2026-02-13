package org.wordpress.android.ui.postsrs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged.UpdatePost
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.PagePostCreationSourcesDetail
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.postsrs.screens.PostRsListScreen
import org.wordpress.android.util.AppLog
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

    @Inject
    lateinit var dispatcher: Dispatcher

    private var pendingPostRemoteId: Long? = null
    private val isFetchingPost = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pendingPostRemoteId = savedInstanceState
            ?.getLong(KEY_PENDING_POST_ID, NO_PENDING_POST)
            ?.takeIf { it != NO_PENDING_POST }

        setContent {
            val isFetching by isFetchingPost.collectAsState()

            AppThemeM3 {
                PostRsListScreen(
                    viewModel = viewModel,
                    isFetchingPost = isFetching,
                    onNavigateBack = {
                        onBackPressedDispatcher.onBackPressed()
                    },
                    onPostClick = ::openPost,
                    onCreatePost = ::createNewPost
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(
            KEY_PENDING_POST_ID,
            pendingPostRemoteId ?: NO_PENDING_POST
        )
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
    }

    /**
     * Opens a post in the editor. If the post already exists in the local
     * FluxC database it is opened immediately; otherwise a fetch is
     * dispatched and the editor is launched once [onPostChanged] receives
     * the result. Note the FluxC part will eventually be dropped and
     * we'll load the post via wordpress-rs instead.
     */
    private fun openPost(remotePostId: Long) {
        val site = selectedSiteRepository.getSelectedSite() ?: return

        val post = postStore.getPostByRemotePostId(remotePostId, site)
        if (post != null) {
            ActivityLauncher.editPostOrPageForResult(this, site, post)
        } else {
            pendingPostRemoteId = remotePostId
            isFetchingPost.value = true
            val postToFetch = PostModel().apply {
                setRemotePostId(remotePostId)
            }
            dispatcher.dispatch(
                PostActionBuilder.newFetchPostAction(
                    RemotePostPayload(postToFetch, site)
                )
            )
        }
    }

    /**
     * Handles FluxC post-fetch completion triggered by [openPost].
     * Called by EventBus when the dispatcher emits [OnPostChanged].
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostChanged(event: OnPostChanged) {
        val pending = pendingPostRemoteId
        val cause = event.causeOfChange as? UpdatePost
        if (pending == null || cause == null ||
            cause.remotePostId != pending
        ) {
            return
        }

        pendingPostRemoteId = null
        isFetchingPost.value = false

        if (event.isError) {
            AppLog.e(
                AppLog.T.POSTS,
                "Failed to fetch post $pending: " +
                    "${event.error?.message}"
            )
            ToastUtils.showToast(
                this, R.string.post_not_found
            )
        } else {
            val site = selectedSiteRepository.getSelectedSite()
            if (site != null) {
                val post = postStore.getPostByRemotePostId(
                    pending, site
                )
                ActivityLauncher.editPostOrPageForResult(
                    this, site, post
                )
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
        private const val KEY_PENDING_POST_ID =
            "key_pending_post_remote_id"
        private const val NO_PENDING_POST = -1L

        fun createIntent(context: Context): Intent {
            return Intent(context, PostRsListActivity::class.java)
        }
    }
}
