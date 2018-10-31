package org.wordpress.android.ui.posts

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearSmoothScroller
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.ListManager
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.push.NativeNotificationsUtils
import org.wordpress.android.ui.ActionableEmptyView
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.notifications.utils.PendingDraftsNotificationsUtils
import org.wordpress.android.ui.posts.adapters.PostListAdapter
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.ui.uploads.UploadUtils
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.NetworkUtils
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
import org.wordpress.android.widgets.PostListButton
import org.wordpress.android.widgets.RecyclerItemDecoration
import javax.inject.Inject

class PostListFragment : Fragment(),
        PostListAdapter.OnPostSelectedListener,
        PostListAdapter.OnPostButtonClickListener {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PostListViewModel

    private val rvScrollPositionSaver = RecyclerViewScrollPositionManager()
    private var swipeToRefreshHelper: SwipeToRefreshHelper? = null
    private var fabView: View? = null

    private var swipeRefreshLayout: CustomSwipeRefreshLayout? = null
    private var recyclerView: RecyclerView? = null
    private var actionableEmptyView: ActionableEmptyView? = null
    private var progressLoadMore: ProgressBar? = null

    private var targetPost: PostModel? = null
    private var shouldCancelPendingDraftNotification = false
    private var postIdForPostToBeDeleted = 0

    private lateinit var nonNullActivity: Activity
    private lateinit var site: SiteModel

    @Inject internal lateinit var siteStore: SiteStore
    @Inject internal lateinit var postStore: PostStore

    private val postListAdapter: PostListAdapter by lazy {
        val postListAdapter = PostListAdapter(nonNullActivity, site)
        postListAdapter.setOnPostSelectedListener(this)
        postListAdapter.setOnPostButtonClickListener(this)
        postListAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nonNullActivity = checkNotNull(activity)
        (nonNullActivity.application as WordPress).component().inject(this)

        val site: SiteModel?
        if (savedInstanceState == null) {
            val nonNullIntent = checkNotNull(nonNullActivity.intent)
            site = nonNullIntent.getSerializableExtra(WordPress.SITE) as SiteModel?
            targetPost = postStore.getPostByLocalPostId(
                    nonNullIntent.getIntExtra(PostsListActivity.EXTRA_TARGET_POST_LOCAL_ID, 0)
            )
        } else {
            rvScrollPositionSaver.onRestoreInstanceState(savedInstanceState)
            site = savedInstanceState.getSerializable(WordPress.SITE) as SiteModel?
            targetPost = postStore.getPostByLocalPostId(
                    savedInstanceState.getInt(PostsListActivity.EXTRA_TARGET_POST_LOCAL_ID)
            )
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
            viewModel.listManagerLiveData.observe(this, Observer {
                it?.let { listManager -> updateListManager(listManager) }
            })
            viewModel.emptyViewState.observe(this, Observer {
                it?.let { emptyViewState -> updateEmptyViewForState(emptyViewState) }
            })
            viewModel.editPost.observe(this, Observer {
                it?.let { (site, post) -> ActivityLauncher.editPostOrPageForResult(nonNullActivity, site, post) }
            })
            viewModel.retryPost.observe(this, Observer {
                it?.let { post ->
                    // restart the UploadService with retry parameters
                    val intent = UploadService.getUploadPostServiceIntent(
                            nonNullActivity,
                            post,
                            PostUtils.isFirstTimePublish(post),
                            false,
                            true
                    )
                    nonNullActivity.startService(intent)
                }
            })
            viewModel.displayTrashConfirmationDialog.observe(this, Observer {
                it?.let { post -> displayTrashPostDialog(post) }
            })
            viewModel.displayPublishConfirmationDialog.observe(this, Observer {
                it?.let { post ->
                    showPublishConfirmationDialog(post)
                }
            })
            viewModel.viewStats.observe(this, Observer {
                it?.let { (site, post) ->
                    ActivityLauncher.viewStatsSinglePostDetails(nonNullActivity, site, post)
                }
            })
            viewModel.previewPost.observe(this, Observer {
                it?.let { (site, post) ->
                    ActivityLauncher.viewPostPreviewForResult(nonNullActivity, site, post)
                }
            })
            viewModel.viewPost.observe(this, Observer {
                it?.let { (site, post) ->
                    ActivityLauncher.browsePostOrPage(nonNullActivity, site, post)
                }
            })
            viewModel.newPost.observe(this, Observer {
                it?.let { site -> ActivityLauncher.addNewPostForResult(nonNullActivity, site, false) }
            })
            viewModel.postUploadAction.observe(this, Observer {
                it?.let { uploadAction -> handleUploadAction(uploadAction) }
            })
            viewModel.toastMessage.observe(this, Observer {
                it?.let { toast ->
                    ToastUtils.showToast(nonNullActivity, toast.messageRes, toast.duration)
                }
            })
            viewModel.postDetailsUpdated.observe(this, Observer {
                it?.let { post -> postListAdapter.updateProgressForPost(post) }
            })
            viewModel.mediaChanged.observe(this, Observer {
                it?.let { mediaList -> postListAdapter.mediaChanged(mediaList) }
            })
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
        }
    }

    private fun displayTrashPostDialog(post: PostModel) {
        val messageRes = if (!UploadService.isPostUploadingOrQueued(post)) {
            if (post.isLocalDraft) {
                R.string.dialog_confirm_delete_permanently_post
            } else R.string.dialog_confirm_delete_post
        } else {
            R.string.dialog_confirm_cancel_post_media_uploading
        }
        val builder = AlertDialog.Builder(
                ContextThemeWrapper(nonNullActivity, R.style.Calypso_Dialog_Alert)
        )
        builder.setTitle(getString(R.string.delete_post))
                .setMessage(messageRes)
                .setPositiveButton(R.string.delete) { _, _ -> trashPost(post) }
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(true)
        builder.create().show()
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

    override fun onDetach() {
        if (shouldCancelPendingDraftNotification) {
            // delete the pending draft notification if available
            val pushId = PendingDraftsNotificationsUtils.makePendingDraftNotificationId(postIdForPostToBeDeleted)
            NativeNotificationsUtils.dismissNotification(pushId, nonNullActivity)
            shouldCancelPendingDraftNotification = false
        }
        super.onDetach()
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
            refreshPostList()
        }
        return view
    }

    fun handleEditPostResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            viewModel.handleEditPostResult(data)
        }
    }

    private fun refreshPostList() {
        if (!isAdded) {
            return
        }
        if (!NetworkUtils.isNetworkAvailable(nonNullActivity)) {
            swipeRefreshLayout?.isRefreshing = false
        } else {
            viewModel.refreshList()
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

    private fun showTargetPostIfNecessary() {
        if (!isAdded) {
            return
        }
        // If the activity was given a target post, and this is the first time posts are loaded, scroll to that post
        targetPost?.let { targetPost ->
            postListAdapter.getPositionForPost(targetPost)?.let { position ->
                val smoothScroller = object : LinearSmoothScroller(nonNullActivity) {
                    private val SCROLL_OFFSET_DP = 23

                    override fun getVerticalSnapPreference(): Int {
                        return LinearSmoothScroller.SNAP_TO_START
                    }

                    override fun calculateDtToFit(
                        viewStart: Int,
                        viewEnd: Int,
                        boxStart: Int,
                        boxEnd: Int,
                        snapPreference: Int
                    ): Int {
                        // Assume SNAP_TO_START, and offset the scroll, so the bottom of the above post shows
                        val offsetPx = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, SCROLL_OFFSET_DP.toFloat(), resources.displayMetrics
                        ).toInt()
                        return boxStart - viewStart + offsetPx
                    }
                }

                smoothScroller.targetPosition = position
                recyclerView?.layoutManager?.startSmoothScroll(smoothScroller)
            }
            this.targetPost = null
        }
    }

    private fun trashPost(post: PostModel) {}

    // TODO: Move trashing post to ViewModel
//    /*
//     * send the passed post to the trash with undo
//     */
//    private fun trashPost(post: PostModel) {
//        // only check if network is available in case this is not a local draft - local drafts have not yet
//        // been posted to the server so they can be trashed w/o further care
//        if (!isAdded || !post.isLocalDraft && !NetworkUtils.checkConnection(nonNullActivity)) {
//            return
//        }
//
//        // remove post from the list and add it to the list of trashed posts
//        val postIdPair = Pair(post.id, post.remotePostId)
//        trashedPostIds.add(postIdPair)
//        refreshListManagerFromStore(listDescriptor)
//
//        val undoListener = OnClickListener {
//            // user undid the trash, so un-hide the post and remove it from the list of trashed posts
//            trashedPostIds.remove(postIdPair)
//            refreshListManagerFromStore(listDescriptor, shouldRefreshFirstPageAfterLoading = false)
//        }
//
//        // different undo text if this is a local draft since it will be deleted rather than trashed
//        val text = if (post.isLocalDraft) getString(R.string.post_deleted) else getString(R.string.post_trashed)
//        val snackbar = Snackbar.make(
//                nonNullActivity.findViewById(R.id.root_view),
//                text,
//                AccessibilityUtils.getSnackbarDuration(nonNullActivity)
//        ).setAction(R.string.undo, undoListener)
//        // wait for the undo snackbar to disappear before actually deleting the post
//        snackbar.addCallback(object : Snackbar.Callback() {
//            override fun onDismissed(snackbar: Snackbar?, event: Int) {
//                super.onDismissed(snackbar, event)
//
//                // if the post no longer exists in the list of trashed posts it's because the
//                // user undid the trash, so don't perform the deletion
//                if (!trashedPostIds.contains(postIdPair)) {
//                    return
//                }
//
//                // remove from the list of trashed posts in case onDismissed is called multiple
//                // times - this way the above check prevents us making the call to delete it twice
//                // https://code.google.com/p/android/issues/detail?id=190529
//                trashedPostIds.remove(postIdPair)
//
//                // here cancel all media uploads related to this Post
//                UploadService.cancelQueuedPostUploadAndRelatedMedia(WordPress.getContext(), post)
//
//                if (post.isLocalDraft) {
//                    dispatcher.dispatch(PostActionBuilder.newRemovePostAction(post))
//
//                    // delete the pending draft notification if available
//                    shouldCancelPendingDraftNotification = false
//                    val pushId = PendingDraftsNotificationsUtils.makePendingDraftNotificationId(post.id)
//                    NativeNotificationsUtils.dismissNotification(pushId, WordPress.getContext())
//                } else {
//                    dispatcher.dispatch(PostActionBuilder.newDeletePostAction(RemotePostPayload(post, site)))
//                }
//            }
//        })
//
//        postIdForPostToBeDeleted = post.id
//        shouldCancelPendingDraftNotification = true
//        snackbar.show()
//    }

    // TODO: Move this
    private fun showPublishConfirmationDialog(post: PostModel) {
        if (!isAdded) {
            return
        }
        val builder = AlertDialog.Builder(
                ContextThemeWrapper(nonNullActivity, R.style.Calypso_Dialog_Alert)
        )
        builder.setTitle(resources.getText(R.string.dialog_confirm_publish_title))
                .setMessage(getString(R.string.dialog_confirm_publish_message_post))
                .setPositiveButton(R.string.dialog_confirm_publish_yes) { _, _ ->
                    viewModel.publishPost(post)
                }
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(true)
        builder.create().show()
    }

    // PostListAdapter listeners

    /**
     * called by the adapter when the user clicks the edit/view/stats/trash button for a post
     */
    override fun onPostButtonClicked(buttonType: Int, postClicked: PostModel) {
        viewModel.handlePostButton(buttonType, postClicked)
    }

    /**
     * called by the adapter when the user clicks a post
     */
    override fun onPostSelected(post: PostModel) {
        onPostButtonClicked(PostListButton.BUTTON_EDIT, post)
    }

    /**
     * A helper function to update the current [ListManager] with the given [listManager].
     *
     * @param listManager [ListManager] to be used to change with the current one
     *
     * This function deals with all the UI actions that needs to be taken after a [ListManager] change, including but
     * not limited to, updating the swipe to refresh layout, loading progress bar and updating the empty views.
     */
    private fun updateListManager(
        listManager: ListManager<PostModel>
    ) {
        if (!isAdded) {
            return
        }
        swipeRefreshLayout?.isRefreshing = listManager.isFetchingFirstPage
        progressLoadMore?.visibility = if (listManager.isLoadingMore) View.VISIBLE else View.GONE
        // TODO: We most likely need to move this to adapter since setListManager is now async
        // Save and restore the visible view. Without this, for example, if a new row is inserted, it does not show up.
        val recyclerViewState = recyclerView?.layoutManager?.onSaveInstanceState()
        postListAdapter.setListManager(listManager)
        recyclerViewState?.let {
            recyclerView?.layoutManager?.onRestoreInstanceState(it)
        }

        // TODO: This might be an issue now that we moved the diff calculation to adapter
        // If offset is saved, restore it here. This is for when we save the scroll position in the bundle.
        recyclerView?.let {
            rvScrollPositionSaver.restoreScrollOffset(it)
        }
        // TODO: This too
        showTargetPostIfNecessary()
    }

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
