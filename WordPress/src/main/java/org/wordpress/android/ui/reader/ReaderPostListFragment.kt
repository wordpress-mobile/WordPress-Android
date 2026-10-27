package org.wordpress.android.ui.reader

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.core.os.BundleCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.datasets.ReaderDatabase
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.store.AccountStore.OnSubscriptionUpdated
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostDiscoverData
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.ViewPagerFragment
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.ChooseSiteActivity
import org.wordpress.android.ui.main.WPMainActivity.OnActivityBackPressedListener
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.ReaderActivityLauncher.OpenUrlType
import org.wordpress.android.ui.reader.ReaderEvents.TagAdded
import org.wordpress.android.ui.reader.ReaderInterfaces.OnBlogInfoFailedListener
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.OpenEditorForReblog
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBookmarkedSavedOnlyLocallyDialog
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBookmarkedTab
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowNoSitesToReblog
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowLoginRequiredBottomSheet
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowReportPost
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowReportUser
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowSitePickerForResult
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType
import org.wordpress.android.ui.reader.subfilter.SubFilterViewModel
import org.wordpress.android.ui.reader.subfilter.SubFilterViewModelProvider.Companion.getSubFilterViewModelForTag
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem
import org.wordpress.android.ui.reader.subscription.ReaderSubscriptionSettingsBottomSheetFragment
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.usecases.BookmarkPostState.PreLoadPostContent
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListArgs
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListUiEvent
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListViewModel
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
import org.wordpress.android.ui.reader.views.compose.postlist.ReaderPostListActions
import org.wordpress.android.ui.reader.views.compose.postlist.ReaderPostListComposeView
import org.wordpress.android.ui.reader.views.compose.postlist.ReaderPostListScreen
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.config.SeenUnseenWithCounterFeatureConfig
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.widgets.AppReviewManager.incrementInteractions
import javax.inject.Inject

@Suppress("LargeClass")
class ReaderPostListFragment : ViewPagerFragment(), OnActivityBackPressedListener, OnScrollToTopListener {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var dispatcher: Dispatcher

    @Inject
    lateinit var seenUnseenWithCounterFeatureConfig: SeenUnseenWithCounterFeatureConfig

    @Inject
    lateinit var readerTracker: ReaderTracker

    @Inject
    lateinit var snackbarSequencer: SnackbarSequencer

    private lateinit var postListViewModel: ReaderPostListViewModel

    // created with each view, since it holds that view's layout. The position is kept here in between, so
    // the list keeps it when the view is recreated from the back stack.
    private var listState: LazyListState? = null
    private var savedFirstVisibleIndex = 0
    private var savedFirstVisibleOffset = 0

    private var isTopLevel = false
    private var wasPaused = false

    private var tagFragmentStartedWith: ReaderTag? = null

    private var isFilterableScreen = false

    private var bookmarksSavedLocallyDialog: AlertDialog? = null

    // This VM is initialized only on the Following tab
    private var subFilterViewModel: SubFilterViewModel? = null
    private var readerViewModel: ReaderViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)

        val arguments = requireArguments()
        isTopLevel = arguments.getBoolean(ReaderConstants.ARG_IS_TOP_LEVEL)
        isFilterableScreen = arguments.getBoolean(ReaderConstants.ARG_IS_FILTERABLE)
        tagFragmentStartedWith =
            BundleCompat.getSerializable(arguments, ReaderConstants.ARG_ORIGINAL_TAG, ReaderTag::class.java)

        savedInstanceState?.let {
            AppLog.d(AppLog.T.READER, "reader post list fragment > restoring instance state")
            wasPaused = it.getBoolean(ReaderConstants.KEY_WAS_PAUSED)
        }

        postListViewModel = ViewModelProvider(this, viewModelFactory)[ReaderPostListViewModel::class.java]

        if (isTopLevel) {
            // Discover sub-tabs are nested inside ReaderDiscoverTabsFragment, so look for the
            // ReaderFragment ancestor rather than the direct parent
            val readerFragment = generateSequence(parentFragment) { it.parentFragment }
                .firstOrNull { it is ReaderFragment } ?: requireParentFragment()
            readerViewModel = ViewModelProvider(readerFragment, viewModelFactory)[ReaderViewModel::class.java]
        }

        val photonWidth = DisplayUtils.getWindowPixelWidth(requireContext()) -
                resources.getDimensionPixelSize(R.dimen.reader_card_margin) * 2
        postListViewModel.start(
            args = buildArgs(arguments, savedInstanceState),
            readerViewModel = readerViewModel,
            photonWidth = photonWidth,
            photonHeight = (photonWidth / READER_FEATURED_IMAGE_ASPECT_RATIO).toInt()
        )
    }

    /*
     * builds the list's starting state from the fragment arguments, overridden by the saved state if any
     */
    private fun buildArgs(arguments: Bundle, savedInstanceState: Bundle?): ReaderPostListArgs {
        val state = savedInstanceState ?: arguments
        val listType = BundleCompat.getSerializable(
            state,
            ReaderConstants.ARG_POST_LIST_TYPE,
            ReaderPostListType::class.java
        ) ?: ReaderTypes.DEFAULT_POST_LIST_TYPE

        return ReaderPostListArgs(
            listType = listType,
            tag = BundleCompat.getSerializable(state, ReaderConstants.ARG_TAG, ReaderTag::class.java),
            originalTag = tagFragmentStartedWith,
            blogId = state.getLong(ReaderConstants.ARG_BLOG_ID),
            feedId = state.getLong(ReaderConstants.ARG_FEED_ID),
            isTopLevel = isTopLevel,
            isFilterable = isFilterableScreen,
            searchQuery = state.getString(ReaderConstants.ARG_SEARCH_QUERY),
            tagHistory = savedInstanceState?.getStringArrayList(KEY_TAG_PREVIEW_HISTORY).orEmpty(),
            hasRequestedPosts = savedInstanceState?.getBoolean(ReaderConstants.KEY_ALREADY_REQUESTED) ?: false,
            hasUpdatedPosts = savedInstanceState?.getBoolean(ReaderConstants.KEY_ALREADY_UPDATED) ?: false,
            restorePosition = savedInstanceState?.getInt(ReaderConstants.KEY_RESTORE_POSITION) ?: 0
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.let {
            postListViewModel.onViewRestored(it.getInt(ReaderConstants.KEY_RESTORE_POSITION))
        }

        if (isFilterableScreen) {
            initSubFilterViewModel(savedInstanceState)
        }

        setupObservers()

        if (isFollowingScreen) {
            subFilterViewModel!!.onUserComesToReader()
        }
    }

    private fun handleNavigationEventObserved(navTarget: ReaderNavigationEvents) {
        when (navTarget) {
            is ShowSitePickerForResult -> {
                ActivityLauncher.showSitePickerForResult(
                    this@ReaderPostListFragment,
                    navTarget.preselectedSite,
                    navTarget.mode
                )
            }

            is OpenEditorForReblog -> {
                ActivityLauncher.openEditorForReblog(
                    activity,
                    navTarget.site,
                    navTarget.post,
                    navTarget.source
                )
            }

            is ShowNoSitesToReblog -> {
                ReaderActivityLauncher.showNoSiteToReblog(requireActivity())
            }

            is ShowBookmarkedTab -> {
                ActivityLauncher.viewSavedPostsListInReader(requireActivity())
            }

            is ShowBookmarkedSavedOnlyLocallyDialog -> {
                showBookmarksSavedLocallyDialog(navTarget)
            }

            is ShowReportPost -> {
                ReaderActivityLauncher.openUrl(
                    requireActivity(),
                    ReaderUtils.getReportPostUrl(navTarget.url),
                    OpenUrlType.INTERNAL
                )
            }

            is ShowReportUser -> {
                ReaderActivityLauncher.openUrl(
                    requireActivity(),
                    ReaderUtils.getReportUserUrl(
                        navTarget.url,
                        navTarget.authorId
                    ),
                    OpenUrlType.INTERNAL
                )
            }

            is ShowLoginRequiredBottomSheet -> {
                ReaderLoginRequiredBottomSheetFragment.newInstance()
                    .show(childFragmentManager, ReaderLoginRequiredBottomSheetFragment.TAG)
            }

            else -> {
                error("Action not supported in ReaderPostListFragment $navTarget")
            }
        }
    }

    private fun setupObservers() {
        postListViewModel.navigationEvents.observe(
            viewLifecycleOwner
        ) { event: Event<ReaderNavigationEvents> ->
            event.applyIfNotHandled {
                handleNavigationEventObserved(this)
            }
        }

        postListViewModel.snackbarEvents.observe(
            viewLifecycleOwner
        ) { event: Event<SnackbarMessageHolder> ->
            event.applyIfNotHandled {
                showSnackbar(this)
            }
        }

        postListViewModel.preloadPostEvents.observe(
            viewLifecycleOwner
        ) { event: Event<PreLoadPostContent> ->
            event.applyIfNotHandled {
                addWebViewCachingFragment(this.blogId, this.postId)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                postListViewModel.uiEvents.collect { handleUiEvent(it) }
            }
        }
    }

    private fun handleUiEvent(event: ReaderPostListUiEvent) {
        when (event) {
            is ReaderPostListUiEvent.PostSelected -> onPostSelected(event.post)
            is ReaderPostListUiEvent.PostButtonClicked -> onButtonClicked(event.post, event.type)
            is ReaderPostListUiEvent.PostHeaderClicked -> ReaderActivityLauncher.showReaderBlogPreview(
                requireContext(),
                event.post,
                postListViewModel.source,
                readerTracker
            )
            is ReaderPostListUiEvent.VideoOverlayClicked ->
                ReaderActivityLauncher.showReaderVideoViewer(requireContext(), event.post.featuredVideo)
            is ReaderPostListUiEvent.TagFollowFailed -> ToastUtils.showToast(
                requireContext(),
                if (event.isFollowRequest) {
                    R.string.reader_toast_err_adding_tag
                } else {
                    R.string.reader_toast_err_removing_tag
                }
            )
            is ReaderPostListUiEvent.SetSubfilterFromTag -> subFilterViewModel?.setSubfilterFromTag(event.tag)
            ReaderPostListUiEvent.SetDefaultSubfilter -> subFilterViewModel?.setDefaultSubfilter(false)
            ReaderPostListUiEvent.NoConnection -> NetworkUtils.checkConnection(requireContext())
            ReaderPostListUiEvent.DataLoaded -> onDataLoaded()
            ReaderPostListUiEvent.BlogInfoFailed -> (activity as? OnBlogInfoFailedListener)?.onBlogInfoFailed()
            is ReaderPostListUiEvent.ShowSubscriptionSettings ->
                ReaderSubscriptionSettingsBottomSheetFragment.newInstance(
                    blogId = event.blogId,
                    blogName = event.blogName,
                    blogUrl = event.blogHost
                ).show(childFragmentManager, ReaderSubscriptionSettingsBottomSheetFragment.TAG)
        }
    }

    private fun showSnackbar(holder: SnackbarMessageHolder) {
        if (!isAdded || view == null) return
        getSnackbarParent()?.let { snackbarParent ->
            snackbarSequencer.enqueue(
                SnackbarItem(
                    SnackbarItem.Info(
                        view = snackbarParent,
                        textRes = holder.message,
                        duration = holder.duration,
                        isImportant = holder.isImportant
                    ),
                    if (holder.buttonTitle != null)
                        SnackbarItem.Action(
                            holder.buttonTitle
                        ) { holder.buttonAction.invoke() }
                    else
                        null
                )
            )
        }
    }

    private fun addWebViewCachingFragment(blogId: Long, postId: Long) {
        val tag = blogId.toString() + "" + postId

        if (parentFragmentManager.findFragmentByTag(tag) == null) {
            parentFragmentManager.beginTransaction()
                .add(ReaderPostWebViewCachingFragment.newInstance(blogId, postId), tag)
                .commit()
        }
    }

    private fun initSubFilterViewModel(savedInstanceState: Bundle?) {
        subFilterViewModel = getSubFilterViewModelForTag(
            fragment = this,
            tag = tagFragmentStartedWith!!,
            savedInstanceState = savedInstanceState
        )

        subFilterViewModel!!.currentSubFilter.observe(
            viewLifecycleOwner
        ) { subfilter ->
            postListViewModel.onSubfilterChanged(subfilter)
        }

        subFilterViewModel!!.readerModeInfo.observe(
            viewLifecycleOwner
        ) { readerModeInfo ->
            if (readerModeInfo != null) {
                postListViewModel.changeReaderMode(readerModeInfo)
            }
        }
    }

    override fun onPause() {
        super.onPause()

        if (bookmarksSavedLocallyDialog != null) {
            bookmarksSavedLocallyDialog!!.dismiss()
        }
        wasPaused = true

        postListViewModel.onFragmentPause(isTopLevel, isSearching, isFilterableScreen)
    }

    override fun onResume() {
        super.onResume()
        if (wasPaused) {
            AppLog.d(AppLog.T.READER, "reader post list > resumed from paused state")
            wasPaused = false

            postListViewModel.onResumeAfterPause(consumeAddedTagName(), getSiteIfBlogPreview())

            if (isTopLevel) {
                // Remove sticky event if not consumed
                EventBus.getDefault().removeStickyEvent(TagAdded::class.java)
            }
        }

        // the user may have logged in or out, which changes whether the self-hosted CTA shows
        if (isFilterableScreen) {
            postListViewModel.onSubfilterChanged(subFilterViewModel!!.getCurrentSubfilterValue())
        }

        postListViewModel.onFragmentResume(
            isTopLevel, isSearching, isFilterableScreen,
            if (isFilterableScreen) subFilterViewModel!!.getCurrentSubfilterValue() else null
        )
    }

    /*
     * returns the name of the tag the user just followed, if any, so the Following screen can switch to it
     */
    private fun consumeAddedTagName(): String? {
        if (!isFollowingScreen || postListViewModel.listType != ReaderPostListType.TAG_FOLLOWED) {
            return null
        }
        return EventBus.getDefault().getStickyEvent(TagAdded::class.java)?.let { addedTag ->
            EventBus.getDefault().removeStickyEvent(addedTag)
            addedTag.tagName
        }
    }

    private fun getSiteIfBlogPreview(): SubfilterListItem.Site? {
        if (isFilterableScreen && (postListViewModel.listType == ReaderPostListType.BLOG_PREVIEW)) {
            return subFilterViewModel!!.getCurrentSubfilterValue() as? SubfilterListItem.Site
        }
        return null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val activity: Activity? = activity
        if (activity != null) {
            val intent = Intent()
            intent.putExtra(ReaderTagsFeedFragment.RESULT_SHOULD_REFRESH_TAGS_FEED, true)
            activity.setResult(Activity.RESULT_OK, intent)
        }
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)

        // purge database and update followed tags/blog if necessary - note that we don't purge unless
        // there's a connection to avoid removing posts the user would expect to see offline
        if (postListViewModel.listType == ReaderPostListType.TAG_FOLLOWED &&
            NetworkUtils.isNetworkAvailable(activity)
        ) {
            purgeDatabaseIfNeeded()
        }

        postListViewModel.onStart()
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
        postListViewModel.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        AppLog.d(AppLog.T.READER, "reader post list > saving instance state")
        val args = postListViewModel.currentArgs(
            restorePosition = listState?.firstVisibleItemIndex ?: savedFirstVisibleIndex
        )

        args.tag?.let { outState.putSerializable(ReaderConstants.ARG_TAG, it) }
        args.originalTag?.let { outState.putSerializable(ReaderConstants.ARG_ORIGINAL_TAG, it) }

        if (args.listType == ReaderPostListType.TAG_PREVIEW) {
            outState.putStringArrayList(KEY_TAG_PREVIEW_HISTORY, ArrayList(args.tagHistory))
        }
        outState.putString(ReaderConstants.ARG_SEARCH_QUERY, args.searchQuery)

        outState.putLong(ReaderConstants.ARG_BLOG_ID, args.blogId)
        outState.putLong(ReaderConstants.ARG_FEED_ID, args.feedId)
        outState.putBoolean(ReaderConstants.KEY_WAS_PAUSED, wasPaused)
        outState.putBoolean(ReaderConstants.KEY_ALREADY_REQUESTED, args.hasRequestedPosts)
        outState.putBoolean(ReaderConstants.KEY_ALREADY_UPDATED, args.hasUpdatedPosts)
        outState.putInt(ReaderConstants.KEY_RESTORE_POSITION, args.restorePosition)
        outState.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, args.listType)
        outState.putBoolean(ReaderConstants.ARG_IS_TOP_LEVEL, isTopLevel)
        outState.putBoolean(ReaderConstants.ARG_IS_FILTERABLE, isFilterableScreen)

        if (isFilterableScreen && subFilterViewModel != null) {
            subFilterViewModel!!.onSaveInstanceState(outState)
        }

        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val actions = buildActions()
        val listState = LazyListState(savedFirstVisibleIndex, savedFirstVisibleOffset).also { listState = it }
        return ReaderPostListComposeView(requireContext(), listState) {
            AppThemeM3 {
                val uiState by postListViewModel.uiState.collectAsStateWithLifecycle()
                ReaderPostListScreen(
                    uiState = uiState,
                    listState = listState,
                    actions = actions
                )
            }
        }.apply {
            // the host activity's AppBarLayout uses this as its lift-on-scroll target
            id = R.id.reader_post_list
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listState?.let {
            savedFirstVisibleIndex = it.firstVisibleItemIndex
            savedFirstVisibleOffset = it.firstVisibleItemScrollOffset
        }
        listState = null
    }

    private fun buildActions() = ReaderPostListActions(
        onRefresh = {
            readerTracker.track(AnalyticsTracker.Stat.READER_PULL_TO_REFRESH)
            postListViewModel.onPullToRefresh()
        },
        onLoadMore = postListViewModel::onLoadMore,
        onScrollHandled = postListViewModel::onScrollHandled,
        onFirstItemVisibilityChanged = postListViewModel::onFirstItemVisibilityChanged,
        onNewPostsBarClicked = postListViewModel::reload,
        onNewPostsBarScrolledAway = postListViewModel::onNewPostsBarScrolledAway,
        onEmptyStateButtonClicked = postListViewModel::onEmptyStateButtonTapped,
        onGapMarkerClicked = postListViewModel::onGapMarkerClicked,
        onTagHeaderFollowClicked = postListViewModel::onTagHeaderFollowClicked,
        onSiteHeaderFollowClicked = postListViewModel::onSiteHeaderFollowClicked,
        onSiteHeaderSettingsClicked = postListViewModel::onSiteHeaderSettingsClicked,
        onXPostClicked = ::onPostSelected,
        onRemovedPostClicked = { post ->
            if (!post.isBookmarked) {
                onButtonClicked(post, ReaderPostCardActionType.BOOKMARK)
            }
        }
    )

    private val isSearching: Boolean
        get() = postListViewModel.listType == ReaderPostListType.SEARCH_RESULTS

    /*
     * called by ReaderSearchActivity, which owns the search field
     */
    fun onSearchSubmitted(query: String) = postListViewModel.onSearchSubmitted(query)

    fun onSearchCleared() = postListViewModel.onSearchCleared()

    /*
     * returns the parent view for snackbars - if this fragment is hosted in the main activity we want the
     * parent to be the main activity's CoordinatorLayout
     */
    private fun getSnackbarParent(): View? {
        val coordinator =
            requireActivity().findViewById<View>(R.id.coordinator_layout)
        return coordinator ?: view
    }

    private val isListShown: Boolean
        get() = view?.isShown == true

    private fun announceListStateForAccessibility() {
        if (isListShown) {
            @Suppress("DEPRECATION")
            requireView().announceForAccessibility(
                getString(
                    R.string.reader_acessibility_list_loaded,
                    postListViewModel.uiState.value.items.size
                )
            )
        }
    }

    private fun showBookmarksSavedLocallyDialog(holder: ShowBookmarkedSavedOnlyLocallyDialog) {
        bookmarksSavedLocallyDialog = MaterialAlertDialogBuilder(requireActivity())
            .setTitle(getString(holder.title))
            .setMessage(getString(holder.message))
            .setPositiveButton(
                holder.buttonLabel
            ) { _: DialogInterface?, _: Int -> holder.okButtonAction.invoke() }
            .setCancelable(false)
            .create()
        bookmarksSavedLocallyDialog!!.show()
    }

    /*
     * called when posts have been loaded from the database
     */
    private fun onDataLoaded() {
        if (!isAdded || !postListViewModel.hasPosts) {
            return
        }
        announceListStateForAccessibility()
    }

    override fun getScrollableViewForUniqueIdProvision(): View? = view

    /*
     * called by the activity when user hits the back button - returns true if the back button
     * is handled here and should be ignored by the activity
     */
    override fun onActivityBackPressed(): Boolean {
        return postListViewModel.goBackInTagHistory()
    }

    /*
    * called when user taps a post
    */
    private fun onPostSelected(post: ReaderPost) {
        if (!isAdded) {
            return
        }

        incrementInteractions(
            AnalyticsTracker.Stat.APP_REVIEWS_EVENT_INCREMENTED_BY_OPENING_READER_POST
        )

        if (post.isBookmarked) {
            trackBookmarkedPostSelected(post)
        }

        if (post.isDiscoverPost) {
            // "discover" posts that highlight another post should open the original (source) post when tapped
            handleDiscoverPostSelected(post)
        }
        else if (post.isXpost) {
            // if this is a cross-post, we want to show the original post
            handleCrossPostSelected(post)
        } else {
            when (val type = postListViewModel.listType) {
                ReaderPostListType.TAG_FOLLOWED,
                ReaderPostListType.TAG_PREVIEW -> {
                    postListViewModel.currentTag?.let { tag ->
                        ReaderActivityLauncher.showReaderPostPagerForTag(
                            requireActivity(),
                            tag,
                            type,
                            post.blogId,
                            post.postId
                        )
                    }
                }

                ReaderPostListType.BLOG_PREVIEW -> {
                    ReaderActivityLauncher.showReaderPostPagerForBlog(
                        requireActivity(),
                        post.blogId,
                        post.postId
                    )
                }

                ReaderPostListType.SEARCH_RESULTS -> {
                    readerTracker.trackPost(AnalyticsTracker.Stat.READER_SEARCH_RESULT_TAPPED, post)
                    ReaderActivityLauncher.showReaderPostDetail(requireActivity(), post.blogId, post.postId)
                }

                ReaderPostListType.TAGS_FEED -> {}
            }
        }
    }

    private fun trackBookmarkedPostSelected(post: ReaderPost) {
        if (postListViewModel.isBookmarksList) {
            readerTracker.trackBlog(
                AnalyticsTracker.Stat.READER_SAVED_POST_OPENED_FROM_SAVED_POST_LIST,
                post.blogId,
                post.feedId,
                post.isFollowedByCurrentUser,
                postListViewModel.source
            )
        } else {
            readerTracker.trackBlog(
                AnalyticsTracker.Stat.READER_SAVED_POST_OPENED_FROM_OTHER_POST_LIST,
                post.blogId,
                post.feedId,
                post.isFollowedByCurrentUser,
                postListViewModel.source
            )
        }
    }

    @Suppress("NestedBlockDepth")
    private fun handleDiscoverPostSelected(post: ReaderPost) {
        post.discoverData?.let { discoverData ->
            if (discoverData.discoverType == ReaderPostDiscoverData.DiscoverType.EDITOR_PICK) {
                if (discoverData.blogId != 0L && discoverData.postId != 0L) {
                    ReaderActivityLauncher.showReaderPostDetail(
                        requireActivity(),
                        discoverData.blogId,
                        discoverData.postId
                    )
                } else if (discoverData.hasPermalink()) {
                    if (seenUnseenWithCounterFeatureConfig.isEnabled()) {
                        postListViewModel.onExternalPostOpened(post)
                    }
                    // if we don't have a blogId/postId, we sadly resort to showing the post
                    // in a WebView activity - this will happen for non-JP self-hosted
                    ReaderActivityLauncher.openUrl(requireActivity(), discoverData.permaLink)
                }
            }
        }
    }

    private fun handleCrossPostSelected(post: ReaderPost) {
        ReaderActivityLauncher.showReaderPostDetail(
            requireActivity(),
            post.xpostBlogId,
            post.xpostPostId
        )
    }

    @Suppress("LongMethod")
    private fun onButtonClicked(post: ReaderPost, actionType: ReaderPostCardActionType) {
        val source = postListViewModel.source
        val isBookmarksList = postListViewModel.isBookmarksList

        when (actionType) {
            ReaderPostCardActionType.FOLLOW -> {
                postListViewModel.onFollowSiteClicked(
                    post,
                    isBookmarksList,
                    source
                )
            }

            ReaderPostCardActionType.SITE_NOTIFICATIONS -> postListViewModel.onSiteNotificationMenuClicked(
                post.blogId,
                post.postId,
                isBookmarksList,
                source
            )

            ReaderPostCardActionType.SHARE -> {
                readerTracker.trackBlog(
                    AnalyticsTracker.Stat.SHARED_ITEM_READER,
                    post.blogId,
                    post.feedId,
                    post.isFollowedByCurrentUser,
                    source
                )
                sharePost(post)
            }

            ReaderPostCardActionType.VISIT_SITE -> {
                readerTracker.track(AnalyticsTracker.Stat.READER_ARTICLE_VISITED)
                ReaderActivityLauncher.showReaderBlogOrFeedPreview(
                    requireActivity(),
                    post.blogId,
                    post.feedId,
                    post.isFollowedByCurrentUser,
                    source,
                    readerTracker
                )
            }

            ReaderPostCardActionType.LIKE -> postListViewModel.onLikeButtonClicked(
                post,
                isBookmarksList,
                source
            )

            ReaderPostCardActionType.REBLOG -> postListViewModel.onReblogButtonClicked(
                post,
                isBookmarksList,
                source
            )

            ReaderPostCardActionType.REPORT_POST -> postListViewModel.onReportPostButtonClicked(
                post,
                isBookmarksList,
                source
            )

            ReaderPostCardActionType.REPORT_USER -> postListViewModel.onReportUserButtonClicked(
                post,
                isBookmarksList,
                source
            )

            ReaderPostCardActionType.BLOCK_SITE -> postListViewModel.onBlockSiteButtonClicked(
                post,
                isBookmarksList,
                source
            )

            ReaderPostCardActionType.BLOCK_USER -> postListViewModel.onBlockUserButtonClicked(
                post,
                isBookmarksList,
                source
            )

            ReaderPostCardActionType.BOOKMARK -> postListViewModel.onBookmarkButtonClicked(
                post.blogId,
                post.postId,
                isBookmarksList,
                source
            )

            ReaderPostCardActionType.COMMENTS -> ReaderActivityLauncher.showReaderComments(
                requireContext(),
                post.blogId,
                post.postId,
                ThreadedCommentsActionSource.READER_POST_CARD.sourceDescription
            )

            ReaderPostCardActionType.TOGGLE_SEEN_STATUS -> if (seenUnseenWithCounterFeatureConfig.isEnabled()) {
                postListViewModel.onToggleSeenStatusClicked(
                    post,
                    isBookmarksList,
                    source
                )
            }

            ReaderPostCardActionType.SPACER_NO_ACTION -> {}
            ReaderPostCardActionType.READING_PREFERENCES -> {}
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSubscriptionUpdated(event: OnSubscriptionUpdated) {
        if (event.isError) {
            AppLog.e(
                AppLog.T.API,
                (ReaderPostListFragment::class.java.simpleName + ".onSubscriptionUpdated: "
                        + event.error.type + " - " + event.error.message)
            )
        } else {
            dispatcher.dispatch(AccountActionBuilder.newFetchSubscriptionsAction())
        }
    }

    @Suppress("SwallowedException")
    private fun sharePost(post: ReaderPost) {
        val url = (if (post.hasShortUrl()) post.shortUrl else post.url)

        val intent = Intent(Intent.ACTION_SEND)
        intent.setType("text/plain")
        intent.putExtra(Intent.EXTRA_TEXT, url)
        intent.putExtra(Intent.EXTRA_SUBJECT, post.title)

        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share_link)))
        } catch (ex: ActivityNotFoundException) {
            ToastUtils.showToast(activity, R.string.reader_toast_err_share_intent)
        }
    }

    /*
     * purge reader db if it hasn't been done yet
     */
    private fun purgeDatabaseIfNeeded() {
        if (!hasPurgedReaderDb) {
            AppLog.d(AppLog.T.READER, "reader post list > purging database")
            hasPurgedReaderDb = true
            ReaderDatabase.purgeAsync()
        }
    }

    override fun onScrollToTop() {
        val listState = listState ?: return
        if (listState.firstVisibleItemIndex > 0) {
            // animated scrolls need Compose's frame clock, which lifecycleScope doesn't provide
            viewLifecycleOwner.lifecycleScope.launch(AndroidUiDispatcher.Main) { listState.animateScrollToItem(0) }
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RequestCodes.SITE_PICKER && resultCode == Activity.RESULT_OK && data != null) {
            val siteLocalId = data.getIntExtra(
                ChooseSiteActivity.KEY_SITE_LOCAL_ID,
                SelectedSiteRepository.UNAVAILABLE
            )
            postListViewModel.onReblogSiteSelected(siteLocalId)
        }
    }

    private val isFollowingScreen: Boolean
        get() = tagFragmentStartedWith != null && tagFragmentStartedWith!!.isFollowedSites

    companion object {
        private const val READER_FEATURED_IMAGE_ASPECT_RATIO = 16 / 9f
        private const val KEY_TAG_PREVIEW_HISTORY = "tag_preview_history"
        private var hasPurgedReaderDb = false

        /*
         * show posts with a specific tag (either TAG_FOLLOWED or TAG_PREVIEW)
         */
        @JvmOverloads
        fun newInstanceForTag(
            tag: ReaderTag,
            listType: ReaderPostListType?,
            isTopLevel: Boolean = false,
            isFilterable: Boolean = false
        ): ReaderPostListFragment {
            AppLog.d(AppLog.T.READER, "reader post list > newInstance (tag)")

            val args = Bundle()
            // Tag this fragment is started with
            args.putSerializable(ReaderConstants.ARG_ORIGINAL_TAG, tag)
            // Tag this fragment is started with but also used for savedState
            args.putSerializable(ReaderConstants.ARG_TAG, tag)
            args.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, listType)
            args.putBoolean(ReaderConstants.ARG_IS_TOP_LEVEL, isTopLevel)
            args.putBoolean(ReaderConstants.ARG_IS_FILTERABLE, isFilterable)

            val fragment = ReaderPostListFragment()
            fragment.arguments = args
            ReaderTracker.trackTagLoaded(tag)

            return fragment
        }

        /*
         * arguments for the search results list, which ReaderSearchActivity hosts with AndroidFragment
         */
        fun searchArguments() = Bundle().apply {
            putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, ReaderPostListType.SEARCH_RESULTS)
            putBoolean(ReaderConstants.ARG_IS_TOP_LEVEL, false)
        }

        /*
         * show posts in a specific blog
         */
        fun newInstanceForBlog(blogId: Long): ReaderPostListFragment {
            AppLog.d(AppLog.T.READER, "reader post list > newInstance (blog)")

            val args = Bundle()
            args.putLong(ReaderConstants.ARG_BLOG_ID, blogId)
            args.putSerializable(
                ReaderConstants.ARG_POST_LIST_TYPE,
                ReaderPostListType.BLOG_PREVIEW
            )

            val fragment = ReaderPostListFragment()
            fragment.arguments = args

            return fragment
        }

        fun newInstanceForFeed(feedId: Long): ReaderPostListFragment {
            AppLog.d(AppLog.T.READER, "reader post list > newInstance (blog)")

            val args = Bundle()
            args.putLong(ReaderConstants.ARG_FEED_ID, feedId)
            args.putLong(ReaderConstants.ARG_BLOG_ID, feedId)
            args.putSerializable(
                ReaderConstants.ARG_POST_LIST_TYPE,
                ReaderPostListType.BLOG_PREVIEW
            )

            val fragment = ReaderPostListFragment()
            fragment.arguments = args

            return fragment
        }
    }
}
