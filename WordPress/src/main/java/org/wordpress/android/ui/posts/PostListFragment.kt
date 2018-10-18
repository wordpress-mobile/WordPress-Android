package org.wordpress.android.ui.posts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.util.DiffUtil
import android.support.v7.util.DiffUtil.DiffResult
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearSmoothScroller
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ProgressBar
import de.greenrobot.event.EventBus
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.Main
import kotlinx.coroutines.experimental.isActive
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListItemDataSource
import org.wordpress.android.fluxc.model.list.ListManager
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForRestSite
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForXmlRpcSite
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.ListStore.OnListChanged
import org.wordpress.android.fluxc.store.ListStore.OnListItemsChanged
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchPostListPayload
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.push.NativeNotificationsUtils
import org.wordpress.android.ui.ActionableEmptyView
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.EmptyViewMessageType
import org.wordpress.android.ui.notifications.utils.PendingDraftsNotificationsUtils
import org.wordpress.android.ui.posts.adapters.PostListAdapter
import org.wordpress.android.ui.uploads.PostEvents
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.ui.uploads.UploadUtils
import org.wordpress.android.ui.uploads.VideoOptimizer
import org.wordpress.android.util.AccessibilityUtils
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.util.helpers.RecyclerViewScrollPositionManager
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper.RefreshListener
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout
import org.wordpress.android.widgets.PostListButton
import org.wordpress.android.widgets.RecyclerItemDecoration
import java.util.ArrayList
import java.util.HashMap
import javax.inject.Inject

class PostListFragment : Fragment(),
        PostListAdapter.OnPostSelectedListener,
        PostListAdapter.OnPostButtonClickListener {
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

    private val uploadedPostRemoteIds = ArrayList<Long>()
    private val trashedPosts = ArrayList<PostModel>()

    private lateinit var nonNullActivity: Activity
    private lateinit var site: SiteModel

    @Inject internal lateinit var siteStore: SiteStore
    @Inject internal lateinit var postStore: PostStore
    @Inject internal lateinit var listStore: ListStore
    @Inject internal lateinit var dispatcher: Dispatcher

    private var listManager: ListManager<PostModel>? = null
    private var refreshListDataJob: Job? = null
    private val listDescriptor: PostListDescriptor by lazy {
        if (site.isUsingWpComRestApi) {
            PostListDescriptorForRestSite(site)
        } else {
            PostListDescriptorForXmlRpcSite(site)
        }
    }
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

        EventBus.getDefault().register(this)
        dispatcher.register(this)
    }

    private fun refreshListManagerFromStore(listDescriptor: ListDescriptor, fetchAfter: Boolean) {
        refreshListDataJob?.cancel()
        refreshListDataJob = GlobalScope.launch(Dispatchers.Main) {
            val listManager = withContext(Dispatchers.Default) { getListDataFromStore(listDescriptor) }
            if (isActive && this@PostListFragment.listDescriptor == listDescriptor) {
                val diffResult = withContext(Dispatchers.Default) {
                    DiffUtil.calculateDiff(DiffCallback(this@PostListFragment.listManager, listManager))
                }
                if (isActive && this@PostListFragment.listDescriptor == listDescriptor) {
                    updateListManager(listManager, diffResult)
                    if (fetchAfter) {
                        listManager.refresh()
                    }
                }
            }
        }
    }

    private fun localItems(): List<PostModel>? {
        return postStore.getLocalPostsForDescriptor(listDescriptor)
    }

    private suspend fun getListDataFromStore(listDescriptor: ListDescriptor): ListManager<PostModel> =
            listStore.getListManager(listDescriptor, localItems(), object : ListItemDataSource<PostModel> {
                override fun fetchItem(listDescriptor: ListDescriptor, remoteItemId: Long) {
                    val postToFetch = PostModel()
                    postToFetch.remotePostId = remoteItemId
                    val payload = RemotePostPayload(postToFetch, site)
                    dispatcher.dispatch(PostActionBuilder.newFetchPostAction(payload))
                }

                override fun fetchList(listDescriptor: ListDescriptor, offset: Int) {
                    if (listDescriptor is PostListDescriptor) {
                        val fetchPostListPayload = FetchPostListPayload(listDescriptor, offset)
                        dispatcher.dispatch(PostActionBuilder.newFetchPostListAction(fetchPostListPayload))
                    }
                }

                override fun getItems(listDescriptor: ListDescriptor, remoteItemIds: List<Long>): Map<Long, PostModel> {
                    return postStore.getPostsByRemotePostIds(remoteItemIds, site)
                }
            }, remoteItemIdsToInclude = uploadedPostRemoteIds)

    private fun updateListManager(listManager: ListManager<PostModel>, diffResult: DiffResult) {
        this.listManager = listManager
        swipeRefreshLayout?.isRefreshing = listManager.isFetchingFirstPage
        progressLoadMore?.visibility = if (listManager.isLoadingMore) View.VISIBLE else View.GONE
        val recyclerViewState = recyclerView?.layoutManager?.onSaveInstanceState()
        postListAdapter.setListManager(listManager, diffResult)
        recyclerView?.layoutManager?.onRestoreInstanceState(recyclerViewState)
        onPostsLoaded(listManager.size)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        dispatcher.unregister(this)

        super.onDestroy()
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
        fabView?.setOnClickListener { newPost() }

        initSwipeToRefreshHelper()
        refreshListManagerFromStore(listDescriptor, fetchAfter = (savedInstanceState == null))

        return view
    }

    fun handleEditPostResult(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data == null || !isAdded) {
            return
        }

        val localId = data.getIntExtra(EditPostActivity.EXTRA_POST_LOCAL_ID, 0)
        val post = postStore.getPostByLocalPostId(localId)

        if (post == null) {
            if (!data.getBooleanExtra(EditPostActivity.EXTRA_IS_DISCARDABLE, false)) {
                ToastUtils.showToast(nonNullActivity, R.string.post_not_found, ToastUtils.Duration.LONG)
            }
            return
        }

        UploadUtils.handleEditPostResultSnackbars(
                nonNullActivity,
                nonNullActivity.findViewById(R.id.coordinator),
                data,
                post,
                site
        ) {
            UploadUtils.publishPost(nonNullActivity, post, site, dispatcher)
        }
    }

    private fun initSwipeToRefreshHelper() {
        swipeToRefreshHelper = buildSwipeToRefreshHelper(
                swipeRefreshLayout,
                RefreshListener {
                    if (!isAdded) {
                        return@RefreshListener
                    }
                    if (!NetworkUtils.checkConnection(nonNullActivity)) {
                        updateEmptyView(EmptyViewMessageType.NETWORK_ERROR)
                        return@RefreshListener
                    }
                    listManager?.refresh()
                }
        )
    }

    private fun newPost() {
        if (!isAdded) {
            return
        }
        ActivityLauncher.addNewPostOrPageForResult(nonNullActivity, site, false, false)
    }

    override fun onResume() {
        super.onResume()

        // TODO: refresh from store

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

// TODO: Do this with ListManager
//    private fun requestPosts(loadMore: Boolean) {
//        if (!NetworkUtils.isNetworkAvailable(nonNullActivity)) {
//            updateEmptyView(EmptyViewMessageType.NETWORK_ERROR)
//            return
//        }
//
//        if (postListAdapter.itemCount == 0) {
//            updateEmptyView(EmptyViewMessageType.LOADING)
//        }
//    }

    /*
     * Upload started, reload so correct status on uploading post appears
     */
    fun onEventMainThread(event: PostEvents.PostUploadStarted) {
        if (isAdded && site.id == event.post.localSiteId) {
            postListAdapter.updateRowForPost(event.post)
        }
    }

    /*
     * Upload cancelled (probably due to failed media), reload so correct status on uploading post appears
     */
    fun onEventMainThread(event: PostEvents.PostUploadCanceled) {
        if (isAdded && site.id == event.post.localSiteId) {
            postListAdapter.updateRowForPost(event.post)
        }
    }

    private fun updateEmptyView(emptyViewMessageType: EmptyViewMessageType) {
        val stringId: Int = when (emptyViewMessageType) {
            EmptyViewMessageType.LOADING -> R.string.posts_fetching
            EmptyViewMessageType.NO_CONTENT -> R.string.posts_empty_list
            EmptyViewMessageType.NETWORK_ERROR -> R.string.no_network_message
            EmptyViewMessageType.PERMISSION_ERROR -> R.string.error_refresh_unauthorized_posts
            EmptyViewMessageType.GENERIC_ERROR -> R.string.error_refresh_posts
        }

        val hasNoContent = emptyViewMessageType == EmptyViewMessageType.NO_CONTENT
        actionableEmptyView?.let {
            it.image.setImageResource(R.drawable.img_illustration_posts_75dp)
            it.image.visibility = if (hasNoContent) View.VISIBLE else View.GONE
            it.title.setText(stringId)
            it.button.setText(R.string.posts_empty_list_button)
            it.button.visibility = if (hasNoContent) View.VISIBLE else View.GONE
            it.button.setOnClickListener { _ ->
                ActivityLauncher.addNewPostOrPageForResult(nonNullActivity, site, false, false)
            }
            it.visibility = if (postListAdapter.itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    private fun hideEmptyView() {
        if (isAdded) {
            actionableEmptyView?.visibility = View.GONE
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

    /*
     * called by the adapter after posts have been loaded
     */
    private fun onPostsLoaded(postCount: Int) {
        if (!isAdded) {
            return
        }

        if (postCount == 0 && listManager?.isFetchingFirstPage != true) {
            if (NetworkUtils.isNetworkAvailable(nonNullActivity)) {
                updateEmptyView(EmptyViewMessageType.NO_CONTENT)
            } else {
                updateEmptyView(EmptyViewMessageType.NETWORK_ERROR)
            }
        } else if (postCount > 0) {
            hideEmptyView()
            recyclerView?.let {
                rvScrollPositionSaver.restoreScrollOffset(it)
            }
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

    /*
     * called by the adapter when the user clicks a post
     */
    override fun onPostSelected(post: PostModel) {
        onPostButtonClicked(PostListButton.BUTTON_EDIT, post)
    }

    /*
     * called by the adapter when the user clicks the edit/view/stats/trash button for a post
     */
    override fun onPostButtonClicked(buttonType: Int, postClicked: PostModel) {
        if (!isAdded) {
            return
        }

        // Get the latest version of the post, in case it's changed since the last time we refreshed the post list
        val post = postStore.getPostByLocalPostId(postClicked.id)
        // This is mostly a sanity check and the list should never go out of sync, but if there is an edge case, we
        // should refresh the list
        if (post == null) {
            // TODO: Can we safely remove this check?
            listManager?.refresh()
            return
        }

        when (buttonType) {
            PostListButton.BUTTON_EDIT -> {
                // track event
                val properties = HashMap<String, Any>()
                properties["button"] = "edit"
                if (!post.isLocalDraft) {
                    properties["post_id"] = post.remotePostId
                }
                properties[AnalyticsUtils.HAS_GUTENBERG_BLOCKS_KEY] =
                        PostUtils.contentContainsGutenbergBlocks(post.content)
                AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.POST_LIST_BUTTON_PRESSED, site, properties)

                if (UploadService.isPostUploadingOrQueued(post)) {
                    // If the post is uploading media, allow the media to continue uploading, but don't upload the
                    // post itself when they finish (since we're about to edit it again)
                    UploadService.cancelQueuedPostUpload(post)
                }
                ActivityLauncher.editPostOrPageForResult(nonNullActivity, site, post)
            }
            PostListButton.BUTTON_RETRY -> {
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
            PostListButton.BUTTON_SUBMIT, PostListButton.BUTTON_SYNC, PostListButton.BUTTON_PUBLISH -> {
                showPublishConfirmationDialog(post)
            }
            PostListButton.BUTTON_VIEW -> ActivityLauncher.browsePostOrPage(nonNullActivity, site, post)
            PostListButton.BUTTON_PREVIEW -> ActivityLauncher.viewPostPreviewForResult(nonNullActivity, site, post)
            PostListButton.BUTTON_STATS -> ActivityLauncher.viewStatsSinglePostDetails(
                    nonNullActivity,
                    site,
                    post,
                    false
            )
            PostListButton.BUTTON_TRASH, PostListButton.BUTTON_DELETE -> {
                if (!UploadService.isPostUploadingOrQueued(post)) {
                    var message = getString(R.string.dialog_confirm_delete_post)

                    if (post.isLocalDraft) {
                        message = getString(R.string.dialog_confirm_delete_permanently_post)
                    }

                    val builder = AlertDialog.Builder(
                            ContextThemeWrapper(nonNullActivity, R.style.Calypso_Dialog_Alert)
                    )
                    builder.setTitle(getString(R.string.delete_post))
                            .setMessage(message)
                            .setPositiveButton(R.string.delete) { _, _ -> trashPost(post) }
                            .setNegativeButton(R.string.cancel, null)
                            .setCancelable(true)
                    builder.create().show()
                } else {
                    val builder = AlertDialog.Builder(
                            ContextThemeWrapper(nonNullActivity, R.style.Calypso_Dialog_Alert)
                    )
                    builder.setTitle(getText(R.string.delete_post))
                            .setMessage(R.string.dialog_confirm_cancel_post_media_uploading)
                            .setPositiveButton(R.string.delete) { _, _ -> trashPost(post) }
                            .setNegativeButton(R.string.cancel, null)
                            .setCancelable(true)
                    builder.create().show()
                }
            }
        }
    }

    private fun showPublishConfirmationDialog(post: PostModel) {
        val builder = AlertDialog.Builder(
                ContextThemeWrapper(nonNullActivity, R.style.Calypso_Dialog_Alert)
        )
        builder.setTitle(resources.getText(R.string.dialog_confirm_publish_title))
                .setMessage(getString(R.string.dialog_confirm_publish_message_post))
                .setPositiveButton(R.string.dialog_confirm_publish_yes) { _, _ ->
                    UploadUtils.publishPost(
                            nonNullActivity,
                            post,
                            site,
                            dispatcher
                    )
                }
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(true)
        builder.create().show()
    }

    /*
     * send the passed post to the trash with undo
     */
    private fun trashPost(post: PostModel) {
        // only check if network is available in case this is not a local draft - local drafts have not yet
        // been posted to the server so they can be trashed w/o further care
        if (!isAdded || !post.isLocalDraft && !NetworkUtils.checkConnection(nonNullActivity)) {
            return
        }

        // remove post from the list and add it to the list of trashed posts
        postListAdapter.hidePost(post)
        trashedPosts.add(post)

        // make sure empty view shows if user deleted the only post
        if (postListAdapter.itemCount == 0) {
            updateEmptyView(EmptyViewMessageType.NO_CONTENT)
        }

        val undoListener = OnClickListener {
            // user undid the trash, so un-hide the post and remove it from the list of trashed posts
            trashedPosts.remove(post)
            postListAdapter.unhidePost(post)
            hideEmptyView()
        }

        // different undo text if this is a local draft since it will be deleted rather than trashed
        val text = if (post.isLocalDraft) getString(R.string.post_deleted) else getString(R.string.post_trashed)

        actionableEmptyView?.let { actionableEmptyView ->
            val snackbar = Snackbar.make(
                    actionableEmptyView,
                    text,
                    AccessibilityUtils.getSnackbarDuration(nonNullActivity)
            ).setAction(R.string.undo, undoListener)
            // wait for the undo snackbar to disappear before actually deleting the post
            snackbar.addCallback(object : Snackbar.Callback() {
                override fun onDismissed(snackbar: Snackbar?, event: Int) {
                    super.onDismissed(snackbar, event)

                    // if the post no longer exists in the list of trashed posts it's because the
                    // user undid the trash, so don't perform the deletion
                    if (!trashedPosts.contains(post)) {
                        return
                    }

                    // remove from the list of trashed posts in case onDismissed is called multiple
                    // times - this way the above check prevents us making the call to delete it twice
                    // https://code.google.com/p/android/issues/detail?id=190529
                    trashedPosts.remove(post)

                    // here cancel all media uploads related to this Post
                    UploadService.cancelQueuedPostUploadAndRelatedMedia(WordPress.getContext(), post)

                    if (post.isLocalDraft) {
                        dispatcher.dispatch(PostActionBuilder.newRemovePostAction(post))

                        // delete the pending draft notification if available
                        shouldCancelPendingDraftNotification = false
                        val pushId = PendingDraftsNotificationsUtils.makePendingDraftNotificationId(post.id)
                        NativeNotificationsUtils.dismissNotification(pushId, WordPress.getContext())
                    } else {
                        dispatcher.dispatch(PostActionBuilder.newDeletePostAction(RemotePostPayload(post, site)))
                    }
                }
            })

            postIdForPostToBeDeleted = post.id
            shouldCancelPendingDraftNotification = true
            snackbar.show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(WordPress.SITE, site)
        rvScrollPositionSaver.onSaveInstanceState(outState, recyclerView)
    }

    // FluxC events

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostChanged(event: OnPostChanged) {
        // TODO: do we need this at all?
        // TODO: We just need to handle the errors, I think...
//                val error = event.error
//                when (error.type) {
//                    PostStore.PostErrorType.UNAUTHORIZED -> updateEmptyView(EmptyViewMessageType.PERMISSION_ERROR)
//                    else -> updateEmptyView(EmptyViewMessageType.GENERIC_ERROR)
//                }
//            }
//        } else if (event.causeOfChange is CauseOfOnPostChanged.DeletePost) {
//            if (event.isError) {
//                val message = getString(R.string.error_deleting_post)
//                ToastUtils.showToast(nonNullActivity, message, ToastUtils.Duration.SHORT)
//                loadPosts(LoadMode.IF_CHANGED)
//            }
//        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onListChanged(event: OnListChanged) {
        if (!event.listDescriptors.contains(listDescriptor)) {
            return
        }
        if (event.loadedFirstPage) {
            uploadedPostRemoteIds.clear()
        }
        refreshListManagerFromStore(listDescriptor, false)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onListItemsChanged(event: OnListItemsChanged) {
        if (listDescriptor.typeIdentifier != event.type) {
            return
        }
        refreshListManagerFromStore(listDescriptor, false)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostUploaded(event: OnPostUploaded) {
        if (isAdded && event.post != null && event.post.localSiteId == site.id) {
            postListAdapter.getPositionForPost(event.post)?.let {
                postListAdapter.notifyItemChanged(it)
            }
            UploadUtils.onPostUploadedSnackbarHandler(
                    nonNullActivity,
                    nonNullActivity.findViewById(R.id.coordinator),
                    event.isError, event.post, null, site, dispatcher
            )
            // TODO: Only add if it's in the local items
            uploadedPostRemoteIds.add(event.post.remotePostId)
            listManager?.refresh()
        }
    }

    /*
     * Media info for a post's featured image has been downloaded, tell
     * the adapter so it can show the featured image now that we have its URL
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaChanged(event: OnMediaChanged) {
        if (isAdded && !event.isError) {
            if (event.mediaList != null && event.mediaList.size > 0) {
                val mediaModel = event.mediaList[0]
                postListAdapter.mediaChanged(mediaModel)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaUploaded(event: OnMediaUploaded) {
        if (!isAdded || event.isError || event.canceled) {
            return
        }

        if (event.media == null || event.media.localPostId == 0 || site.id != event.media.localSiteId) {
            // Not interested in media not attached to posts or not belonging to the current site
            return
        }

        postStore.getPostByLocalPostId(event.media.localPostId)?.let { post ->
            if (event.media.isError || event.canceled) {
                // if a media is cancelled or ends in error, and the post is not uploading nor queued,
                // (meaning there is no other pending media to be uploaded for this post)
                // then we should refresh it to show its new state
                if (!UploadService.isPostUploadingOrQueued(post)) {
                    postListAdapter.updateRowForPost(post)
                }
            } else {
                postListAdapter.updateProgressForPost(post)
            }
        }
    }

    fun onEventMainThread(event: VideoOptimizer.ProgressEvent) {
        if (isAdded) {
            postStore.getPostByLocalPostId(event.media.localPostId)?.let { post ->
                postListAdapter.updateProgressForPost(post)
            }
        }
    }

    fun onEventMainThread(event: UploadService.UploadErrorEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        if (event.post != null) {
            UploadUtils.onPostUploadedSnackbarHandler(
                    nonNullActivity,
                    nonNullActivity.findViewById(R.id.coordinator), true, event.post,
                    event.errorMessage, site, dispatcher
            )
        } else if (event.mediaModelList != null && !event.mediaModelList.isEmpty()) {
            UploadUtils.onMediaUploadedSnackbarHandler(
                    nonNullActivity,
                    nonNullActivity.findViewById(R.id.coordinator), true,
                    event.mediaModelList, site, event.errorMessage
            )
        }
    }

    fun onEventMainThread(event: UploadService.UploadMediaSuccessEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        if (event.mediaModelList != null && !event.mediaModelList.isEmpty()) {
            UploadUtils.onMediaUploadedSnackbarHandler(
                    nonNullActivity,
                    nonNullActivity.findViewById(R.id.coordinator), false,
                    event.mediaModelList, site, event.successMessage
            )
        }
    }

    fun onEventMainThread(event: UploadService.UploadMediaRetryEvent) {
        if (!isAdded) {
            return
        }

        if (event.mediaModelList != null && !event.mediaModelList.isEmpty()) {
            // if there' a Post to which the retried media belongs, clear their status
            val postsToRefresh = PostUtils.getPostsThatIncludeAnyOfTheseMedia(postStore, event.mediaModelList)
            // now that we know which Posts to refresh, let's do it
            for (post in postsToRefresh) {
                postListAdapter.getPositionForPost(post)?.let { position ->
                    postListAdapter.notifyItemChanged(position)
                }
            }
        }
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

class DiffCallback(
    private val old: ListManager<PostModel>?,
    private val new: ListManager<PostModel>
) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        if (old == null) {
            return false
        }
        return ListManager.areItemsTheSame(new, old, newItemPosition, oldItemPosition) { oldItem, newItem ->
            oldItem.id == newItem.id
        }
    }

    override fun getOldListSize(): Int {
        return old?.size ?: 0
    }

    override fun getNewListSize(): Int {
        return new.size
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = old?.getItem(oldItemPosition, false, false)
        val newItem = new.getItem(newItemPosition, false, false)
        if (oldItem == null && newItem == null) {
            return true
        }
        if (oldItem == null || newItem == null) {
            return false
        }
        if (oldItem.isLocallyChanged && newItem.isLocallyChanged) {
            return oldItem == newItem
        }
        if (oldItem.isLocallyChanged || newItem.isLocallyChanged) {
            return false
        }
        return oldItem.lastModified == newItem.lastModified
    }
}
