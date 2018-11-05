package org.wordpress.android.ui.posts

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.arch.paging.PagedList
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActionableEmptyView
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.adapters.PostListAdapter
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.ui.uploads.UploadUtils
import org.wordpress.android.util.AccessibilityUtils
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper
import org.wordpress.android.util.helpers.RecyclerViewScrollPositionManager
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout
import org.wordpress.android.viewmodel.posts.PostListEmptyViewState
import org.wordpress.android.viewmodel.posts.PostListEmptyViewState.EMPTY_LIST
import org.wordpress.android.viewmodel.posts.PostListEmptyViewState.HIDDEN_LIST
import org.wordpress.android.viewmodel.posts.PostListEmptyViewState.LOADING
import org.wordpress.android.viewmodel.posts.PostListEmptyViewState.PERMISSION_ERROR
import org.wordpress.android.viewmodel.posts.PostListEmptyViewState.REFRESH_ERROR
import org.wordpress.android.viewmodel.posts.PostListViewModel
import org.wordpress.android.widgets.RecyclerItemDecoration
import javax.inject.Inject

class PostListFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PostListViewModel

    private val rvScrollPositionSaver = RecyclerViewScrollPositionManager()
    private var swipeToRefreshHelper: SwipeToRefreshHelper? = null
    private var fabView: View? = null

    private var swipeRefreshLayout: CustomSwipeRefreshLayout? = null
    private var recyclerView: RecyclerView? = null
    private var actionableEmptyView: ActionableEmptyView? = null
    private var progressLoadMore: ProgressBar? = null

    private lateinit var nonNullActivity: Activity
    private lateinit var site: SiteModel

    private val postListAdapter: PostListAdapter by lazy {
        PostListAdapter(
                context = nonNullActivity,
                isAztecEditorEnabled = AppPrefs.isAztecEditorEnabled(),
                hasCapabilityPublishPosts = site.hasCapabilityPublishPosts,
                isPhotonCapable = SiteUtils.isPhotonCapable(site)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nonNullActivity = checkNotNull(activity)
        (nonNullActivity.application as WordPress).component().inject(this)

        val site: SiteModel? = if (savedInstanceState == null) {
            val nonNullIntent = checkNotNull(nonNullActivity.intent)
            nonNullIntent.getSerializableExtra(WordPress.SITE) as SiteModel?
        } else {
            rvScrollPositionSaver.onRestoreInstanceState(savedInstanceState)
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel?
        }

        if (site == null) {
            ToastUtils.showToast(nonNullActivity, R.string.blog_not_found, ToastUtils.Duration.SHORT)
            nonNullActivity.finish()
        } else {
            this.site = site
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.let { postListActivity ->
            viewModel = ViewModelProviders.of(postListActivity, viewModelFactory)
                    .get<PostListViewModel>(PostListViewModel::class.java)
            viewModel.start(site)
            viewModel.pagedListData.observe(this, Observer {
                it?.let { pagedListData -> updatePagedListData(pagedListData) }
            })
            viewModel.emptyViewState.observe(this, Observer {
                it?.let { emptyViewState -> updateEmptyViewForState(emptyViewState) }
            })
            viewModel.userAction.observe(this, Observer {
                it?.let { userAction -> handleUserAction(userAction) }
            })
            viewModel.postUploadAction.observe(this, Observer {
                it?.let { uploadAction -> handleUploadAction(uploadAction) }
            })
            viewModel.toastMessage.observe(this, Observer {
                it?.show(nonNullActivity)
            })
            viewModel.snackbarAction.observe(this, Observer {
                it?.let { snackbarHolder -> showSnackbar(snackbarHolder) }
            })
            viewModel.dialogAction.observe(this, Observer {
                it?.show(nonNullActivity)
            })
        }
        postListAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (positionStart == 0) {
                    // TODO: Do we always want to show when new items are inserted?
//                    recyclerView?.smoothScrollToPosition(0)
                }
                super.onItemRangeInserted(positionStart, itemCount)
            }
        })
    }

    private fun handleUserAction(action: PostListUserAction) {
        when (action) {
            is PostListUserAction.EditPost -> {
                ActivityLauncher.editPostOrPageForResult(nonNullActivity, action.site, action.post)
            }
            is PostListUserAction.NewPost -> {
                ActivityLauncher.addNewPostForResult(nonNullActivity, action.site, action.isPromo)
            }
            is PostListUserAction.PreviewPost -> {
                ActivityLauncher.viewPostPreviewForResult(nonNullActivity, action.site, action.post)
            }
            is PostListUserAction.RetryUpload -> {
                // restart the UploadService with retry parameters
                val intent = UploadService.getUploadPostServiceIntent(
                        nonNullActivity,
                        action.post,
                        action.trackAnalytics,
                        action.publish,
                        action.retry
                )
                nonNullActivity.startService(intent)
            }
            is PostListUserAction.ViewStats -> {
                ActivityLauncher.viewStatsSinglePostDetails(nonNullActivity, action.site, action.post)
            }
            is PostListUserAction.ViewPost -> {
                ActivityLauncher.browsePostOrPage(nonNullActivity, action.site, action.post)
            }
        }
    }

    private fun showSnackbar(holder: SnackbarMessageHolder) {
        nonNullActivity.findViewById<View>(R.id.root_view)?.let { parent ->
            val message = getString(holder.messageRes)
            val duration = AccessibilityUtils.getSnackbarDuration(nonNullActivity)
            val snackbar = Snackbar.make(parent, message, duration)
            if (holder.buttonTitleRes != null) {
                snackbar.setAction(getString(holder.buttonTitleRes)) {
                    holder.buttonAction()
                }
            }
            snackbar.show()
        }
    }

    private fun handleUploadAction(action: PostUploadAction) {
        when (action) {
            is PostUploadAction.EditPostResult -> {
                UploadUtils.handleEditPostResultSnackbars(
                        nonNullActivity,
                        nonNullActivity.findViewById(R.id.coordinator),
                        action.data,
                        action.post,
                        action.site
                ) {
                    action.publishAction()
                }
            }
            is PostUploadAction.PublishPost -> {
                UploadUtils.publishPost(
                        nonNullActivity,
                        action.post,
                        action.site,
                        action.dispatcher
                )
            }
            is PostUploadAction.PostUploadedSnackbar -> {
                UploadUtils.onPostUploadedSnackbarHandler(
                        nonNullActivity,
                        nonNullActivity.findViewById(R.id.coordinator),
                        action.isError,
                        action.post,
                        action.errorMessage,
                        action.site,
                        action.dispatcher
                )
            }
            is PostUploadAction.MediaUploadedSnackbar -> {
                UploadUtils.onMediaUploadedSnackbarHandler(
                        nonNullActivity,
                        nonNullActivity.findViewById(R.id.coordinator),
                        action.isError,
                        action.mediaList,
                        action.site,
                        action.message
                )
            }
            is PostUploadAction.CancelPostAndMediaUpload -> {
                UploadService.cancelQueuedPostUploadAndRelatedMedia(nonNullActivity, action.post)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(WordPress.SITE, site)
        rvScrollPositionSaver.onSaveInstanceState(outState, recyclerView)
    }

    override fun onResume() {
        super.onResume()

        // scale in the fab after a brief delay if it's not already showing
        if (fabView?.visibility != View.VISIBLE) {
            val delayMs = resources.getInteger(R.integer.fab_animation_delay).toLong()
            Handler().postDelayed({
                if (isAdded) {
                    AniUtils.scaleIn(fabView, AniUtils.Duration.MEDIUM)
                }
            }, delayMs)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.post_list_fragment, container, false)

        swipeRefreshLayout = view.findViewById(R.id.ptr_layout)
        recyclerView = view.findViewById(R.id.recycler_view)
        progressLoadMore = view.findViewById(R.id.progress)
        fabView = view.findViewById(R.id.fab_button)

        actionableEmptyView = view.findViewById(R.id.actionable_empty_view)

        val context = nonNullActivity
        val spacingVertical = context.resources.getDimensionPixelSize(R.dimen.card_gutters)
        val spacingHorizontal = context.resources.getDimensionPixelSize(R.dimen.content_margin)
        recyclerView?.layoutManager = LinearLayoutManager(context)
        recyclerView?.addItemDecoration(RecyclerItemDecoration(spacingHorizontal, spacingVertical))
        recyclerView?.adapter = postListAdapter

        // hide the fab so we can animate it
        fabView?.visibility = View.GONE
        fabView?.setOnClickListener { viewModel.newPost() }

        swipeToRefreshHelper = buildSwipeToRefreshHelper(swipeRefreshLayout) {
            if (!NetworkUtils.isNetworkAvailable(nonNullActivity)) {
                swipeRefreshLayout?.isRefreshing = false
            } else {
                swipeRefreshLayout?.isRefreshing = false
                viewModel.refreshList()
            }
        }
        return view
    }

    fun handleEditPostResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            viewModel.handleEditPostResult(data)
        }
    }

    private fun updateEmptyViewForState(emptyViewState: PostListEmptyViewState) {
        if (!isAdded) {
            return
        }
        if (emptyViewState == HIDDEN_LIST) {
            actionableEmptyView?.visibility = View.GONE
            return
        }
        var isHidden = false
        val stringId = when (emptyViewState) {
            EMPTY_LIST -> if (NetworkUtils.isNetworkAvailable(nonNullActivity)) {
                isHidden = true
                R.string.posts_empty_list
            } else {
                R.string.no_network_message
            }
            LOADING -> R.string.posts_fetching
            REFRESH_ERROR -> R.string.error_refresh_posts
            PERMISSION_ERROR -> R.string.error_refresh_unauthorized_posts
            HIDDEN_LIST -> throw IllegalArgumentException("Hidden state should already be handled")
        }
        actionableEmptyView?.let {
            it.image.setImageResource(R.drawable.img_illustration_posts_75dp)
            it.image.visibility = if (isHidden) View.VISIBLE else View.GONE
            it.title.setText(stringId)
            it.button.setText(R.string.posts_empty_list_button)
            it.button.visibility = if (isHidden) View.VISIBLE else View.GONE
            it.button.setOnClickListener { _ ->
                viewModel.newPost()
            }
            it.visibility = View.VISIBLE
        }
    }

    private fun updatePagedListData(pagedListData: PagedList<PostAdapterItemType>) {
        if (!isAdded) {
            return
        }
        actionableEmptyView?.visibility = if (pagedListData.size == 0) View.VISIBLE else View.GONE
        postListAdapter.submitList(pagedListData)
    }

//    private fun update(postListData: PostListData) {
//        if (!isAdded) {
//            return
//        }
//        swipeRefreshLayout?.isRefreshing = postListData.isLoadingFirstPage
//        progressLoadMore?.visibility = if (postListData.isLoadingMore) View.VISIBLE else View.GONE
//        postListAdapter.setPostListData(postListData)
//
//        // TODO: This might be an issue now that we moved the diff calculation to adapter - rotation doesn't work
//        // If offset is saved, restore it here. This is for when we save the scroll position in the bundle.
//        recyclerView?.let {
//            rvScrollPositionSaver.restoreScrollOffset(it)
//        }
//    }

    companion object {
        const val TAG = "post_list_fragment_tag"

        @JvmStatic
        fun newInstance(site: SiteModel, targetPost: PostModel?): PostListFragment {
            val fragment = PostListFragment()
            val bundle = Bundle()
            bundle.putSerializable(WordPress.SITE, site)
            targetPost?.let {
                bundle.putInt(PostsListActivity.EXTRA_TARGET_POST_LOCAL_ID, it.id)
            }
            fragment.arguments = bundle
            return fragment
        }
    }
}
