package org.wordpress.android.ui.reader

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.ProgressBar
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import kotlinx.coroutines.launch
import com.google.android.material.R as MaterialR
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.datasets.ReaderBlogTable
import org.wordpress.android.datasets.ReaderDatabase
import org.wordpress.android.datasets.ReaderSearchTable
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.ReaderActionBuilder
import org.wordpress.android.fluxc.model.ReaderSiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload.SubscriptionAction
import org.wordpress.android.fluxc.store.AccountStore.OnSubscriptionUpdated
import org.wordpress.android.fluxc.store.ReaderStore
import org.wordpress.android.fluxc.store.ReaderStore.OnReaderSitesSearched
import org.wordpress.android.fluxc.store.ReaderStore.ReaderSearchSitesPayload
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostDiscoverData
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.networking.NetworkConnectionMonitor
import org.wordpress.android.ui.ActionableEmptyView
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.FilteredRecyclerView
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.ViewPagerFragment
import org.wordpress.android.ui.main.BottomNavController
import org.wordpress.android.ui.main.ChooseSiteActivity
import org.wordpress.android.ui.main.WPMainActivity.OnActivityBackPressedListener
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.ReaderActivityLauncher.OpenUrlType
import org.wordpress.android.ui.reader.ReaderEvents.TagAdded
import org.wordpress.android.ui.reader.ReaderInterfaces.OnFollowListener
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.adapters.ReaderPostListAdapter
import org.wordpress.android.ui.reader.adapters.ReaderSearchSuggestionAdapter
import org.wordpress.android.ui.reader.adapters.ReaderSearchSuggestionRecyclerAdapter
import org.wordpress.android.ui.reader.adapters.ReaderSiteSearchAdapter
import org.wordpress.android.ui.reader.adapters.ReaderSiteSearchAdapter.SiteSearchAdapterListener
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.OpenEditorForReblog
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBookmarkedSavedOnlyLocallyDialog
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBookmarkedTab
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowNoSitesToReblog
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
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyButton
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListUiEvent
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListViewModel
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
import org.wordpress.android.ui.reader.views.ReaderSiteHeaderView.OnBlogInfoFailedListener
import org.wordpress.android.ui.reader.views.ReaderSiteHeaderView.OnSubscriptionSettingsClickListener
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.config.SeenUnseenWithCounterFeatureConfig
import org.wordpress.android.util.extensions.getColorFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.widgets.AppReviewManager.incrementInteractions
import org.wordpress.android.widgets.RecyclerItemDecoration
import org.wordpress.android.widgets.WPSnackbar.Companion.make
import javax.inject.Inject

@Suppress("LargeClass")
class ReaderPostListFragment : ViewPagerFragment(), OnFollowListener, OnActivityBackPressedListener,
    OnScrollToTopListener {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var accountStore: AccountStore

    // This must be injected for site search to work
    @Inject
    lateinit var readerStore: ReaderStore

    @Inject
    lateinit var dispatcher: Dispatcher

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var seenUnseenWithCounterFeatureConfig: SeenUnseenWithCounterFeatureConfig

    @Inject
    lateinit var readerTracker: ReaderTracker

    @Inject
    lateinit var snackbarSequencer: SnackbarSequencer

    @Inject
    lateinit var networkConnectionMonitor: NetworkConnectionMonitor

    // Initialised to true so the first LiveData replay of "connected" (e.g. on a freshly restored fragment
    // with an active search) doesn't re-fire the search; only a genuine offline->online transition should.
    private var lastConnected = true

    private var postAdapter: ReaderPostListAdapter? = null
    private var siteSearchAdapter: ReaderSiteSearchAdapter? = null
    private var suggestionAdapter: ReaderSearchSuggestionAdapter? = null
    private var searchSuggestionRecyclerAdapter: ReaderSearchSuggestionRecyclerAdapter? = null

    private lateinit var recyclerView: FilteredRecyclerView
    private lateinit var newPostsBar: View
    private lateinit var progressBar: ProgressBar
    private var searchMenuItem: MenuItem? = null

    private lateinit var postListViewModel: ReaderPostListViewModel

    private var bottomNavController: BottomNavController? = null
    private var actionableEmptyView: ActionableEmptyView? = null
    private var searchTabs: TabLayout? = null
    private var searchView: SearchView? = null

    private var isTopLevel = false
    private var wasPaused = false
    private var isAnimatingOutNewPostsBar = false
    private var lastRenderedState: ReaderPostListUiState? = null

    private var lastTappedSiteSearchResult: ReaderSiteModel? = null
    private var tagFragmentStartedWith: ReaderTag? = null

    private var siteSearchRestorePosition = 0
    private var postSearchAdapterPos = 0
    private var siteSearchAdapterPos = 0
    private var searchTabsPos = NO_POSITION

    private var isFilterableScreen = false

    private var bookmarksSavedLocallyDialog: AlertDialog? = null

    // This VM is initialized only on the Following tab
    private var subFilterViewModel: SubFilterViewModel? = null
    private var readerViewModel: ReaderViewModel? = null

    /*
     * called when user clicks on subscription settings button in the blog header
     */
    private val subscriptionSettingsClickListener =
        OnSubscriptionSettingsClickListener { blogId, blogName, blogUrl ->
            ReaderSubscriptionSettingsBottomSheetFragment.newInstance(
                blogId = blogId,
                blogName = blogName,
                blogUrl = blogUrl
            ).show(childFragmentManager, ReaderSubscriptionSettingsBottomSheetFragment.TAG)
        }

    private val adapterListener = object : ReaderPostListAdapter.Listener {
        override val source: String
            get() = postListViewModel.source
        override val followListener: OnFollowListener
            get() = this@ReaderPostListFragment
        override val blogInfoFailedListener: OnBlogInfoFailedListener?
            get() = activity as? OnBlogInfoFailedListener
        override val subscriptionSettingsClickListener: OnSubscriptionSettingsClickListener
            get() = this@ReaderPostListFragment.subscriptionSettingsClickListener

        override fun onLastItemBound() = postListViewModel.onLoadMore()

        override fun onXPostClicked(post: ReaderPost) = onPostSelected(post)

        override fun onRemovedPostClicked(post: ReaderPost) {
            if (!post.isBookmarked) {
                onButtonClicked(post, ReaderPostCardActionType.BOOKMARK)
            }
        }

        override fun onTagHeaderFollowClicked() = postListViewModel.onTagHeaderFollowClicked()

        override fun onGapMarkerClicked() = postListViewModel.onGapMarkerClicked()
    }

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
            siteSearchRestorePosition = it.getInt(ReaderConstants.KEY_SITE_SEARCH_RESTORE_POSITION)
            wasPaused = it.getBoolean(ReaderConstants.KEY_WAS_PAUSED)
            searchTabsPos = it.getInt(ReaderConstants.KEY_ACTIVE_SEARCH_TAB, NO_POSITION)
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

        recyclerView.adapter = getPostAdapter()
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

        if (isSearching) {
            recyclerView.showAppBarLayout()
            // searchMenuItem is only set up when logged in, but logged-out users can still reach search
            searchMenuItem?.expandActionView()
            recyclerView.setToolbarScrollFlags(0)
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

        lastRenderedState = null
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { postListViewModel.uiState.collect { renderState(it) } }
                launch { postListViewModel.uiEvents.collect { handleUiEvent(it) } }
            }
        }

        networkConnectionMonitor.isConnected.observe(viewLifecycleOwner) { connected ->
            // Only resubmit the search when connectivity actually transitions to connected, so LiveData replay
            // on re-foregrounding doesn't re-fire the search.
            if (connected && !lastConnected) {
                postListViewModel.searchQuery?.let { submitSearchQuery(it) }
            }
            lastConnected = connected
        }
    }

    private fun renderState(state: ReaderPostListUiState) {
        val lastState = lastRenderedState
        lastRenderedState = state

        getPostAdapter().submitList(state.items) {
            // the diff can finish after the view is destroyed - keep the scroll for the next view
            if (view == null) return@submitList
            state.scrollToPosition?.let { position ->
                AppLog.d(AppLog.T.READER, "reader post list > scrolling to position $position")
                recyclerView.scrollRecycleViewToPosition(position)
                postListViewModel.onScrollHandled()
            }
        }

        if (lastState?.progress() != state.progress()) {
            updateProgressIndicators(state)
        }

        if (lastState?.newPostsBarVisible != state.newPostsBarVisible) {
            if (state.newPostsBarVisible) showNewPostsBar() else hideNewPostsBar()
        }

        // search shows its own empty states, since they depend on the search UI
        val isEmptyStateChanged = lastState == null || lastState.emptyState != state.emptyState ||
                lastState.isUpdating != state.isUpdating
        if (!isSearching && isEmptyStateChanged) {
            val emptyState = state.emptyState
            if (emptyState == null) {
                hideEmptyView()
            } else {
                setEmptyView(emptyState, state.isUpdating)
                showEmptyView()
            }
        }
    }

    private fun ReaderPostListUiState.progress() = Triple(isRefreshing, isLoadingMore, isUpdating)

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
            is ReaderPostListUiEvent.SearchStarted -> if (!event.hasPosts) showSearchEmptyState(false)
            is ReaderPostListUiEvent.SearchEnded -> if (event.showResults) showSearchTabs() else hideSearchTabs()
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
            if (!isSearching) {
                postListViewModel.onSubfilterChanged(subfilter)
            }
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

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        /*
         * This is a workaround for https://github.com/wordpress-mobile/WordPress-Android/issues/11985.
         * The RecyclerView doesn't get redrawn correctly when the adapter finishes its initialization in onStart.
         */
        getPostAdapter().notifyDataSetChanged()
        if (wasPaused) {
            AppLog.d(AppLog.T.READER, "reader post list > resumed from paused state")
            wasPaused = false

            postListViewModel.onResumeAfterPause(consumeAddedTagName(), getSiteIfBlogPreview())

            if (isTopLevel) {
                // Remove sticky event if not consumed
                EventBus.getDefault().removeStickyEvent(TagAdded::class.java)
            }

            // if the user tapped a site to show site preview, it's possible they also changed the follow
            // status so tell the search adapter to check whether it has the correct follow status
            if (isSearching && lastTappedSiteSearchResult != null) {
                getSiteSearchAdapter().checkFollowStatusForSite(lastTappedSiteSearchResult!!)
                lastTappedSiteSearchResult = null
            }

            if (isSearching) {
                return
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

        // detect the bottom nav controller when this fragment is hosted in the main activity - this is used to
        // hide the bottom nav when the user searches from the reader
        if (context is BottomNavController) {
            bottomNavController = context
        }

        val activity: Activity? = activity
        if (activity != null) {
            val intent = Intent()
            intent.putExtra(ReaderTagsFeedFragment.RESULT_SHOULD_REFRESH_TAGS_FEED, true)
            activity.setResult(Activity.RESULT_OK, intent)
        }
    }

    override fun onDetach() {
        super.onDetach()
        bottomNavController = null
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
        newPostsBar.clearAnimation()
        dispatcher.unregister(this)
        postListViewModel.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        AppLog.d(AppLog.T.READER, "reader post list > saving instance state")
        val args = postListViewModel.currentArgs(restorePosition = currentPosition)

        args.tag?.let { outState.putSerializable(ReaderConstants.ARG_TAG, it) }
        args.originalTag?.let { outState.putSerializable(ReaderConstants.ARG_ORIGINAL_TAG, it) }

        if (args.listType == ReaderPostListType.TAG_PREVIEW) {
            outState.putStringArrayList(KEY_TAG_PREVIEW_HISTORY, ArrayList(args.tagHistory))
        } else if (isSearching && searchView != null && searchView!!.query != null) {
            val query = searchView!!.query.toString()
            outState.putString(ReaderConstants.ARG_SEARCH_QUERY, query)
        }

        outState.putLong(ReaderConstants.ARG_BLOG_ID, args.blogId)
        outState.putLong(ReaderConstants.ARG_FEED_ID, args.feedId)
        outState.putBoolean(ReaderConstants.KEY_WAS_PAUSED, wasPaused)
        outState.putBoolean(ReaderConstants.KEY_ALREADY_REQUESTED, args.hasRequestedPosts)
        outState.putBoolean(ReaderConstants.KEY_ALREADY_UPDATED, args.hasUpdatedPosts)
        outState.putInt(ReaderConstants.KEY_RESTORE_POSITION, args.restorePosition)
        outState.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, args.listType)
        outState.putBoolean(ReaderConstants.ARG_IS_TOP_LEVEL, isTopLevel)
        outState.putBoolean(ReaderConstants.ARG_IS_FILTERABLE, isFilterableScreen)

        if (isSearchTabsShowing()) {
            val tabPosition = searchTabsPosition
            outState.putInt(ReaderConstants.KEY_ACTIVE_SEARCH_TAB, tabPosition)
            val siteSearchPosition =
                if (tabPosition == TAB_SITES) currentPosition else siteSearchAdapterPos
            outState.putInt(ReaderConstants.KEY_SITE_SEARCH_RESTORE_POSITION, siteSearchPosition)
        }

        if (isFilterableScreen && subFilterViewModel != null) {
            subFilterViewModel!!.onSaveInstanceState(outState)
        }

        super.onSaveInstanceState(outState)
    }

    // a detached fragment (e.g. an inactive main tab restored after process death) never creates its view
    private val currentPosition: Int
        get() = if (::recyclerView.isInitialized) recyclerView.currentPosition else -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView =
            inflater.inflate(R.layout.reader_fragment_post_cards, container, false) as ViewGroup
        recyclerView = rootView.findViewById(R.id.reader_recycler_view)

        actionableEmptyView = rootView.findViewById(R.id.empty_custom_view)

        setupRecycler()
        setupRecyclerFilterListener()

        // bar that appears at top after new posts are loaded
        newPostsBar = rootView.findViewById(R.id.layout_new_posts)
        newPostsBar.visibility = View.GONE
        isAnimatingOutNewPostsBar = false
        newPostsBar.setOnClickListener {
            recyclerView.scrollRecycleViewToPosition(0)
            postListViewModel.reload()
        }

        // progress bar that appears when loading more posts
        progressBar = rootView.findViewById(R.id.progress_footer)
        progressBar.visibility = View.GONE

        return rootView
    }

    private fun setupRecycler() {
        recyclerView.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(), R.color.reader_post_list_background
            )
        )

        // add the item decoration (dividers) to the recycler, skipping the first item if the first
        // item is the tag toolbar (shown when viewing posts in followed tags) - this is to avoid
        // having the tag toolbar take up more vertical space than necessary
        val spacingVerticalRes = R.dimen.reader_card_gutters
        val spacingHorizontal = resources.getDimensionPixelSize(R.dimen.reader_card_margin)
        val spacingVertical = resources.getDimensionPixelSize(spacingVerticalRes)
        recyclerView.addItemDecoration(
            RecyclerItemDecoration(
                spacingHorizontal,
                spacingVertical,
                false
            )
        )

        // add a proper item divider to the RecyclerView
        recyclerView.addItemDivider(R.drawable.default_list_divider)

        recyclerView.setToolbarBackgroundColor(0)

        if (isTopLevel) {
            recyclerView.setToolbarTitle(
                R.string.reader_screen_title,
                resources.getDimensionPixelSize(R.dimen.margin_extra_large)
            )
        } else {
            recyclerView.setToolbarLeftAndRightPadding(
                resources.getDimensionPixelSize(R.dimen.margin_medium),
                resources.getDimensionPixelSize(R.dimen.margin_extra_large)
            )
        }

        // add a menu to the filtered recycler toolbar
        if (accountStore.hasAccessToken() && isSearching) {
            setupRecyclerToolbar()
        }

        recyclerView.setSwipeToRefreshEnabled(isSwipeToRefreshSupported)

        // rows are diffed rather than redrawn with notifyDataSetChanged, so skip the change crossfade
        (recyclerView.internalRecyclerView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        // the view model decides whether to show the "new posts" bar based on whether the first item is visible
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                postListViewModel.onFirstItemVisibilityChanged(recyclerView.isFirstItemVisible)
            }
        })
    }

    private fun setupRecyclerFilterListener() {
        recyclerView.setFilterListener { forced ->
            if (forced) {
                readerTracker.track(AnalyticsTracker.Stat.READER_PULL_TO_REFRESH)
            }
            postListViewModel.onPullToRefresh()
        }
    }

    /*
     * adds a menu to the recycler toolbar containing search items - only called
     * for followed tags
     */
    private fun setupRecyclerToolbar() {
        val menu = recyclerView.addToolbarMenu(R.menu.reader_list)
        val searchMenuItem = menu.findItem(R.id.menu_reader_search)
        this.searchMenuItem = searchMenuItem

        searchView = searchMenuItem.actionView as SearchView?
        searchView!!.queryHint = getString(R.string.reader_hint_post_search)
        searchView!!.isSubmitButtonEnabled = false
        searchView!!.setIconifiedByDefault(true)
        searchView!!.isIconified = true

        // force the search view to take up as much horizontal space as possible (without this
        // it looks truncated on landscape)
        val maxWidth = DisplayUtils.getWindowPixelWidth(requireActivity())
        searchView!!.maxWidth = maxWidth

        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                if (!isSearching) {
                    readerTracker.track(AnalyticsTracker.Stat.READER_SEARCH_LOADED)
                }
                postListViewModel.onSearchExpanded()
                recyclerView.setSwipeToRefreshEnabled(isSwipeToRefreshSupported)
                populateSearchSuggestions(null)
                showSearchMessageOrSuggestions()
                // hide the bottom navigation when search is active
                if (bottomNavController != null) {
                    bottomNavController!!.onRequestHideBottomNavigation()
                }

                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                if (activity is ReaderSearchActivity) {
                    (requireActivity() as ReaderSearchActivity).finishWithRefreshSubscriptionsResult()
                }
                requireActivity().finish()
                return false
            }
        })

        searchView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                submitSearchQuery(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                populateSearchSuggestions(newText)
                showSearchMessageOrSuggestions()
                return true
            }
        }
        )
    }

    private fun showSearchMessageOrSuggestions() {
        val hasQuery = !isSearchViewEmpty
        val hasPerformedSearch = !TextUtils.isEmpty(postListViewModel.searchQuery)

        // prevents suggestions from being shown after the search view has been collapsed
        if (!isSearching) {
            return
        }

        // prevents suggestions from being shown above search results after configuration changes
        if (wasPaused && hasPerformedSearch) {
            return
        }

        if (!hasQuery || !hasPerformedSearch) {
            // clear posts and sites so only the suggestions or the empty view are visible, and clear
            // the last performed query
            postListViewModel.onSearchCleared()
            getSiteSearchAdapter().clear()

            hideSearchTabs()

            val hasSuggestions =
                searchSuggestionRecyclerAdapter != null && searchSuggestionRecyclerAdapter!!.itemCount > 0

            if (hasSuggestions) {
                hideSearchMessage()
                showSearchSuggestions()
            } else {
                showSearchMessage()
                hideSearchSuggestions()
            }
        }
    }

    private val isSearching: Boolean
        get() = postListViewModel.listType == ReaderPostListType.SEARCH_RESULTS

    /*
     * start a search for reader sites matching the current search query
     */
    private fun updateSitesInCurrentSearch(offset: Int) {
        val query = postListViewModel.searchQuery ?: return
        if (searchTabsPosition == TAB_SITES) {
            if (offset == 0) {
                recyclerView.isRefreshing = true
            } else {
                showLoadingProgress(true)
            }
        }
        val payload = ReaderSearchSitesPayload(
            query,
            ReaderConstants.READER_MAX_SEARCH_RESULTS_TO_REQUEST,
            offset,
            false
        )
        dispatcher.dispatch(ReaderActionBuilder.newReaderSearchSitesAction(payload))
    }

    private fun submitSearchQuery(query: String) {
        if (!isAdded) {
            return
        }

        searchView?.clearFocus() // this will hide suggestions and the virtual keyboard
        hideSearchMessage()
        hideSearchSuggestions()

        if (!NetworkUtils.isNetworkAvailable(context)) {
            showEmptyView()
        }

        // remember this query for future suggestions
        val trimQuery = query.trim()
        ReaderSearchTable.addOrUpdateQueryString(trimQuery)

        postListViewModel.onSearchSubmitted(trimQuery)
        updateSitesInCurrentSearch(0)

        // track that the user performed a search
        if (trimQuery != "") {
            readerTracker.trackQuery(AnalyticsTracker.Stat.READER_SEARCH_PERFORMED, trimQuery)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReaderSitesSearched(event: OnReaderSitesSearched) {
        if (!isAdded || !isSearching) {
            return
        }

        if (!postListViewModel.uiState.value.isUpdating) {
            recyclerView.isRefreshing = false
        }
        showLoadingProgress(false)

        val adapter = getSiteSearchAdapter()
        if (event.isError) {
            adapter.clear()
        } else if (StringUtils.equals(event.searchTerm, postListViewModel.searchQuery)) {
            adapter.setCanLoadMore(event.canLoadMore)
            if (event.offset == 0) {
                adapter.setSiteList(event.sites)
            } else {
                adapter.addSiteList(event.sites)
            }
            if (siteSearchRestorePosition > 0) {
                recyclerView.scrollRecycleViewToPosition(siteSearchRestorePosition)
            }
        }

        if (searchTabsPosition == TAB_SITES && adapter.isEmpty) {
            showSearchEmptyState(event.isError)
        }

        siteSearchRestorePosition = 0
    }

    /*
     * reuse "empty" view to let user know what they're querying
     */
    private fun showSearchMessage() {
        if (!isAdded) {
            return
        }

        showSearchEmptyState(false)
    }

    private fun hideSearchMessage() {
        hideEmptyView()
    }

    private fun showSearchSuggestions() {
        recyclerView.showSearchSuggestions()
    }

    private fun hideSearchSuggestions() {
        recyclerView.hideSearchSuggestions()
    }

    /*
     * create the TabLayout that separates search results between POSTS and SITES and places it below
     * the FilteredRecyclerView's toolbar
     */
    private fun createSearchTabs() {
        if (searchTabs == null) {
            val rootView = requireView().findViewById<ViewGroup>(android.R.id.content)
            val inflater = LayoutInflater.from(activity)
            searchTabs = inflater.inflate(R.layout.reader_search_tabs, rootView) as TabLayout
            searchTabs!!.visibility = View.GONE
            recyclerView.appBarLayout.addView(searchTabs)
        }
    }

    private fun isSearchTabsShowing() = searchTabs?.isVisible ?: false

    private fun showSearchTabs() {
        if (!isAdded) {
            return
        }
        if (searchTabs == null) {
            createSearchTabs()
        }
        if (searchTabs!!.visibility != View.VISIBLE) {
            searchTabs!!.visibility = View.VISIBLE

            postSearchAdapterPos = 0
            siteSearchAdapterPos = 0

            searchTabs!!.addOnTabSelectedListener(object : OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    if (tab.position == TAB_POSTS) {
                        recyclerView.adapter = getPostAdapter()
                        if (postSearchAdapterPos > 0) {
                            recyclerView.scrollRecycleViewToPosition(postSearchAdapterPos)
                        }
                        if (!postListViewModel.hasPosts) {
                            showSearchEmptyState(false)
                        } else {
                            hideEmptyView()
                        }
                    } else if (tab.position == TAB_SITES) {
                        recyclerView.adapter = getSiteSearchAdapter()
                        if (siteSearchAdapterPos > 0) {
                            recyclerView.scrollRecycleViewToPosition(siteSearchAdapterPos)
                        }
                        if (getSiteSearchAdapter().isEmpty) {
                            showSearchEmptyState(false)
                        } else {
                            hideEmptyView()
                        }
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    if (tab.position == TAB_POSTS) {
                        postSearchAdapterPos = recyclerView.currentPosition
                    } else if (tab.position == TAB_SITES) {
                        siteSearchAdapterPos = recyclerView.currentPosition
                    }
                }

                override fun onTabReselected(tab: TabLayout.Tab) {
                    recyclerView.smoothScrollToPosition(0)
                }
            })

            if (searchTabsPos != NO_POSITION && searchTabsPos != searchTabs!!.selectedTabPosition) {
                val tab = searchTabs!!.getTabAt(searchTabsPos)
                tab?.select()
                searchTabsPos = NO_POSITION
            }
        }
    }

    private fun hideSearchTabs() {
        if (isAdded && searchTabs != null && searchTabs!!.isVisible) {
            searchTabs!!.visibility = View.GONE
            searchTabs!!.clearOnTabSelectedListeners()
            if (searchTabs!!.selectedTabPosition != TAB_POSTS) {
                searchTabs!!.getTabAt(TAB_POSTS)!!.select()
            }
            recyclerView.adapter = getPostAdapter()
            lastTappedSiteSearchResult = null
            showLoadingProgress(false)
        }
    }

    private val searchTabsPosition: Int
        get() = if (isSearchTabsShowing()) searchTabs!!.selectedTabPosition else -1

    private fun populateSearchSuggestions(query: String?) {
        populateSearchSuggestionAdapter(query)
        populateSearchSuggestionRecyclerAdapter(null) // always passing null as there's no need to filter
    }

    /*
     * create and assign the suggestion adapter for the search view
     */
    private fun createSearchSuggestionAdapter() {
        suggestionAdapter = ReaderSearchSuggestionAdapter(activity)
        searchView!!.suggestionsAdapter = suggestionAdapter

        searchView!!.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                return false
            }

            override fun onSuggestionClick(position: Int): Boolean {
                val query = suggestionAdapter!!.getSuggestion(position)
                onSearchSuggestionClicked(query)
                return true
            }
        })

        suggestionAdapter!!.setOnSuggestionDeleteClickListener { query: String? ->
            this.onSearchSuggestionDeleteClicked(
                query!!
            )
        }
        suggestionAdapter!!.setOnSuggestionClearClickListener { this.onSearchSuggestionClearClicked() }
    }

    private fun populateSearchSuggestionAdapter(query: String?) {
        if (suggestionAdapter == null) {
            createSearchSuggestionAdapter()
        }
        suggestionAdapter!!.setFilter(query)
    }

    private fun createSearchSuggestionRecyclerAdapter() {
        searchSuggestionRecyclerAdapter = ReaderSearchSuggestionRecyclerAdapter()
        recyclerView.setSearchSuggestionAdapter(searchSuggestionRecyclerAdapter)

        searchSuggestionRecyclerAdapter!!.setOnSuggestionClickListener { query: String? ->
            onSearchSuggestionClicked(
                query
            )
        }
        searchSuggestionRecyclerAdapter!!.setOnSuggestionDeleteClickListener { query: String? ->
            onSearchSuggestionDeleteClicked(
                query!!
            )
        }
        searchSuggestionRecyclerAdapter!!.setOnSuggestionClearClickListener { onSearchSuggestionClearClicked() }
    }

    @Suppress("SameParameterValue")
    private fun populateSearchSuggestionRecyclerAdapter(query: String?) {
        if (searchSuggestionRecyclerAdapter == null) {
            createSearchSuggestionRecyclerAdapter()
        }
        searchSuggestionRecyclerAdapter!!.setQuery(query)
    }

    private fun onSearchSuggestionClicked(query: String?) {
        if (!TextUtils.isEmpty(query)) {
            searchView!!.setQuery(query, true)
        }
    }

    private fun onSearchSuggestionDeleteClicked(query: String) {
        ReaderSearchTable.deleteQueryString(query)

        suggestionAdapter!!.reload()
        searchSuggestionRecyclerAdapter!!.reload()

        showSearchMessageOrSuggestions()
    }

    private fun onSearchSuggestionClearClicked() {
        showClearSearchSuggestionsConfirmationDialog(requireContext())
    }

    private fun showClearSearchSuggestionsConfirmationDialog(context: Context) {
        MaterialAlertDialogBuilder(context)
            .setMessage(R.string.dlg_confirm_clear_search_history)
            .setCancelable(true)
            .setNegativeButton(R.string.no, null)
            .setPositiveButton(
                R.string.yes
            ) { _: DialogInterface?, _: Int -> clearSearchSuggestions() }
            .create()
            .show()
    }

    private fun clearSearchSuggestions() {
        readerTracker.track(AnalyticsTracker.Stat.READER_SEARCH_HISTORY_CLEARED)
        ReaderSearchTable.deleteAllQueries()

        suggestionAdapter!!.swapCursor(null)
        searchSuggestionRecyclerAdapter!!.swapCursor(null)

        showSearchMessageOrSuggestions()
    }

    private val isSearchViewExpanded: Boolean
        get() = searchView != null && !searchView!!.isIconified

    private val isSearchViewEmpty: Boolean
        get() = searchView != null && searchView!!.query.isEmpty()

    /*
     * returns the parent view for snackbars - if this fragment is hosted in the main activity we want the
     * parent to be the main activity's CoordinatorLayout
     */
    private fun getSnackbarParent(): View? {
        val coordinator =
            requireActivity().findViewById<View>(R.id.coordinator_layout)
        return coordinator ?: view
    }

    private fun showSearchEmptyState(requestFailed: Boolean) {
        if (!isAdded) return
        setEmptyView(
            postListViewModel.searchEmptyState(requestFailed, isSearchViewEmpty),
            postListViewModel.uiState.value.isUpdating
        )
        showEmptyView()
    }

    private fun setEmptyView(emptyState: ReaderPostListEmptyState, isUpdating: Boolean) {
        val emptyView = actionableEmptyView ?: return
        emptyView.updateLayoutForSearch(false, 0)
        emptyView.subtitle.contentDescription = null

        // Ensure the default image is reset for empty views before applying logic
        emptyView.image.setImageResource(0)

        when (emptyState) {
            ReaderPostListEmptyState.SelfHostedCta -> setEmptyViewForSelfHostedCta(emptyView)
            ReaderPostListEmptyState.Bookmarks -> setEmptyViewForBookmarksList(emptyView)
            else -> setEmptyViewForPosts(emptyView, emptyState, isUpdating)
        }
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun setEmptyViewForPosts(
        emptyView: ActionableEmptyView,
        emptyState: ReaderPostListEmptyState,
        isUpdating: Boolean
    ) {
        val heightToolbar = resources.getDimensionPixelSize(R.dimen.toolbar_height)
        val heightTabs = resources.getDimensionPixelSize(R.dimen.tab_height)
        var title: String? = null
        var description: String? = null
        var button: ReaderPostListEmptyButton? = null
        var isImageHidden = false
        when (emptyState) {
            // handled by setEmptyView()
            ReaderPostListEmptyState.SelfHostedCta, ReaderPostListEmptyState.Bookmarks -> Unit

            ReaderPostListEmptyState.NoConnection -> title = getString(R.string.reader_empty_posts_no_connection)
            is ReaderPostListEmptyState.RequestFailed -> title = getString(
                if (emptyState.isSearch) {
                    R.string.reader_empty_search_request_failed
                } else {
                    R.string.reader_empty_posts_request_failed
                }
            )

            ReaderPostListEmptyState.Updating -> title = getString(R.string.reader_empty_posts_in_tag_updating)
            ReaderPostListEmptyState.FollowedSitesNoRecentPosts -> {
                title = getString(R.string.reader_empty_followed_blogs_no_recent_posts_title)
                description = getString(R.string.reader_empty_followed_blogs_subscribed_no_recent_posts_description)
                button = ReaderPostListEmptyButton.DISCOVER
                isImageHidden = true
            }

            ReaderPostListEmptyState.NoFollowedSites -> {
                title = getString(R.string.reader_no_followed_blogs_title)
                description = getString(R.string.reader_no_followed_blogs_description)
                button = ReaderPostListEmptyButton.DISCOVER
                isImageHidden = true
            }

            ReaderPostListEmptyState.NoLikedPosts -> {
                title = getString(R.string.reader_empty_posts_liked_title)
                description = getString(R.string.reader_empty_posts_liked_description)
                button = ReaderPostListEmptyButton.FOLLOWED
            }

            ReaderPostListEmptyState.NoPostsInList ->
                title = getString(R.string.reader_empty_blogs_posts_in_custom_list)
            ReaderPostListEmptyState.NoPostsWithTag -> title = getString(R.string.reader_no_posts_with_this_tag)
            ReaderPostListEmptyState.NoPostsInBlog -> title = getString(R.string.reader_no_posts_in_blog)
            ReaderPostListEmptyState.SearchExplainer -> {
                title = getString(R.string.reader_label_post_search_explainer)
                isImageHidden = true
                emptyView.updateLayoutForSearch(true, heightToolbar)
            }

            ReaderPostListEmptyState.SearchUpdating -> {
                title = ""
                isImageHidden = true
                emptyView.updateLayoutForSearch(true, heightToolbar)
            }

            is ReaderPostListEmptyState.SearchNoResults -> {
                title = getString(R.string.reader_empty_search_title)
                description = String.format(
                    getString(R.string.reader_empty_search_description),
                    "<em>${emptyState.query}</em>"
                )
                isImageHidden = true
                emptyView.updateLayoutForSearch(true, heightToolbar + heightTabs)
            }
        }

        emptyView.image.visibility = if (!isUpdating && !isImageHidden) View.VISIBLE else View.GONE
        emptyView.title.text = title

        if (description == null) {
            emptyView.subtitle.visibility = View.GONE
        } else {
            emptyView.subtitle.visibility = View.VISIBLE
            emptyView.subtitle.text = if (description.contains("<") && description.contains(">")) {
                HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_LEGACY)
            } else {
                description
            }
        }

        if (button == null) {
            emptyView.button.visibility = View.GONE
        } else {
            emptyView.button.visibility = View.VISIBLE
            emptyView.button.setText(emptyButtonTextRes(button))
            emptyView.button.setOnClickListener { postListViewModel.onEmptyStateButtonTapped(button) }
        }
    }

    @StringRes
    private fun emptyButtonTextRes(button: ReaderPostListEmptyButton) = when (button) {
        ReaderPostListEmptyButton.DISCOVER -> R.string.reader_no_followed_blogs_button_discover
        ReaderPostListEmptyButton.FOLLOWED -> R.string.reader_empty_followed_blogs_button_subscriptions
    }

    /*
     * Currently, only local bookmarks are supported.  Show an empty view if the local database has no data.
     */
    private fun setEmptyViewForBookmarksList(emptyView: ActionableEmptyView) {
        // replace %s placeholder with bookmark outline icon
        val description = getString(R.string.reader_empty_saved_posts_description)
        val ssb = SpannableStringBuilder(description)
        val imagePlaceholderPosition = description.indexOf("%s")
        addBookmarkImageSpan(ssb, imagePlaceholderPosition)
        emptyView.image.visibility = View.VISIBLE
        emptyView.title.setText(R.string.reader_empty_saved_posts_title)
        emptyView.subtitle.text = ssb
        emptyView.subtitle.contentDescription =
            getString(R.string.reader_empty_saved_posts_content_description)
        emptyView.subtitle.visibility = View.VISIBLE
        emptyView.button.setText(R.string.reader_no_followed_blogs_button_discover)
        emptyView.button.visibility = View.VISIBLE
        emptyView.button.setOnClickListener {
            postListViewModel.onEmptyStateButtonTapped(ReaderPostListEmptyButton.DISCOVER)
        }
    }

    private fun setEmptyViewForSelfHostedCta(emptyView: ActionableEmptyView) {
        emptyView.image.visibility = View.VISIBLE
        emptyView.title.text = getString(R.string.reader_empty_subscriptions)
        emptyView.subtitle.visibility = View.GONE
        emptyView.button.setText(R.string.reader_no_followed_blogs_button_discover)
        emptyView.button.setOnClickListener {
            postListViewModel.onEmptyStateButtonTapped(ReaderPostListEmptyButton.DISCOVER)
        }
    }

    private fun addBookmarkImageSpan(ssb: SpannableStringBuilder, imagePlaceholderPosition: Int) {
        val drawable = ContextCompat.getDrawable(
            requireContext(),
            R.drawable.ic_bookmark_grey_dark_18dp
        )

        val tint = requireContext().getColorFromAttribute(MaterialR.attr.colorOnBackground)
        drawable?.setTint(tint)
        drawable?.alpha = BOOKMARK_IMAGE_ALPHA

        drawable!!.setBounds(
            0,
            0,
            (drawable.intrinsicWidth * BOOKMARK_IMAGE_MULTIPLIER).toInt(),
            (drawable.intrinsicHeight * BOOKMARK_IMAGE_MULTIPLIER).toInt()
        )
        ssb.setSpan(
            ImageSpan(drawable), imagePlaceholderPosition, imagePlaceholderPosition + 2,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun showEmptyView() {
        if (isAdded) {
            actionableEmptyView!!.visibility = View.VISIBLE
            actionableEmptyView!!.announceEmptyStateForAccessibility()
        }
    }

    private fun hideEmptyView() {
        if (isAdded) {
            actionableEmptyView!!.visibility = View.GONE
        }
    }

    private fun announceListStateForAccessibility() {
        if (view != null) {
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
        if (isSearching) {
            hideEmptyView()
            if (!isSearchTabsShowing()) {
                showSearchTabs()
            }
        }
    }

    private fun getPostAdapter(): ReaderPostListAdapter {
        return postAdapter ?: ReaderPostListAdapter(imageManager, readerTracker, adapterListener).also {
            AppLog.d(AppLog.T.READER, "reader post list > creating post adapter")
            it.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    // RecyclerView keeps the current first row in place when rows are inserted above it, so
                    // newer posts would land above the screen - keep the top in view instead
                    if (positionStart == 0 && recyclerView.isFirstItemVisible) {
                        recyclerView.scrollRecycleViewToPosition(0)
                    }
                }
            })
            postAdapter = it
        }
    }

    private fun getSiteSearchAdapter(): ReaderSiteSearchAdapter {
        if (siteSearchAdapter == null) {
            siteSearchAdapter = ReaderSiteSearchAdapter(object : SiteSearchAdapterListener {
                override fun onSiteClicked(site: ReaderSiteModel) {
                    lastTappedSiteSearchResult = site
                    ReaderActivityLauncher.showReaderBlogOrFeedPreview(
                        requireActivity(),
                        site.siteId,
                        site.feedId,
                        site.isFollowing,
                        postListViewModel.source,
                        readerTracker
                    )
                }

                override fun onLoadMore(offset: Int) {
                    showLoadingProgress(true)
                    updateSitesInCurrentSearch(offset)
                }
            })
        }
        return siteSearchAdapter!!
    }

    override fun getScrollableViewForUniqueIdProvision(): View? {
        return recyclerView.internalRecyclerView
    }

    /*
     * called by the activity when user hits the back button - returns true if the back button
     * is handled here and should be ignored by the activity
     */
    override fun onActivityBackPressed(): Boolean {
        return if (isSearchViewExpanded) {
            searchMenuItem?.collapseActionView()
            true
        } else {
            postListViewModel.goBackInTagHistory()
        }
    }

    /*
    * show/hide progress bar which appears at the bottom of the activity when loading more posts
    */
    private fun showLoadingProgress(showProgress: Boolean) {
        if (isAdded) {
            if (showProgress) {
                progressBar.bringToFront()
                progressBar.visibility = View.VISIBLE
            } else {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun updateProgressIndicators(state: ReaderPostListUiState) {
        // if only older posts are being updated, show only the bottom progress bar, otherwise
        // show only the swipe-to-refresh progress bar
        showLoadingProgress(state.isLoadingMore)
        recyclerView.isRefreshing = state.isRefreshing

        // if swipe-to-refresh isn't active, keep it disabled during an update - this prevents
        // doing a refresh while another update is already in progress
        if (!recyclerView.isRefreshing) {
            recyclerView.setSwipeToRefreshEnabled(!state.isUpdating && isSwipeToRefreshSupported)
        }
    }

    /*
     * swipe-to-refresh isn't supported for search results since they're really brief snapshots
     * and are unlikely to show new posts due to the way they're sorted
     */
    private val isSwipeToRefreshSupported: Boolean
        get() = !isSearching

    /*
     * bar that appears at the top when new posts have been retrieved
     */
    private fun isNewPostsBarShowing() = newPostsBar.isVisible

    private fun showNewPostsBar() {
        if (!isAdded || isNewPostsBarShowing()) {
            return
        }

        AniUtils.startAnimation(newPostsBar, R.anim.reader_top_bar_in)
        newPostsBar.visibility = View.VISIBLE

        // assign the scroll listener to hide the bar when the recycler is scrolled, but don't assign
        // it right away since the user may be scrolling when the bar appears (which would cause it
        // to disappear as soon as it's displayed)
        recyclerView.postDelayed({
            if (isAdded && isNewPostsBarShowing()) {
                recyclerView.addOnScrollListener(onRecyclerScrollListener)
            }
        }, RECYCLER_DELAY_MS)
    }

    private fun hideNewPostsBar() {
        if (!isAdded || !isNewPostsBarShowing() || isAnimatingOutNewPostsBar) {
            return
        }

        isAnimatingOutNewPostsBar = true

        // remove the onScrollListener assigned in showNewPostsBar()
        recyclerView.removeOnScrollListener(onRecyclerScrollListener)

        val listener: Animation.AnimationListener = object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                // noop
            }

            override fun onAnimationEnd(animation: Animation) {
                if (isAdded) {
                    newPostsBar.visibility = View.GONE
                    isAnimatingOutNewPostsBar = false
                }
            }

            override fun onAnimationRepeat(animation: Animation) {
                // noop
            }
        }
        AniUtils.startAnimation(newPostsBar, R.anim.reader_top_bar_out, listener)
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

    /*
     * scroll listener assigned to the recycler when the "new posts" bar is shown to hide
     * it upon scrolling
     */
    private val onRecyclerScrollListener: RecyclerView.OnScrollListener =
        object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                postListViewModel.onNewPostsBarScrolledAway()
            }
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

    override fun onFollowTapped(view: View, blogName: String, blogId: Long, feedId: Long) {
        dispatcher.dispatch(AccountActionBuilder.newFetchSubscriptionsAction())

        val blog = if (TextUtils.isEmpty(blogName))
            getString(R.string.reader_followed_blog_notifications_this)
        else
            blogName

        if (blogId > 0) {
            make(
                view = getSnackbarParent()!!,
                text = HtmlCompat.fromHtml(
                    getString(R.string.reader_followed_blog_notifications, "<b>", blog, "</b>"),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                ),
                duration = Snackbar.LENGTH_LONG
            ).setAction(
                getString(R.string.reader_followed_blog_notifications_action)
            ) {
                readerTracker.trackBlog(
                    AnalyticsTracker.Stat.FOLLOWED_BLOG_NOTIFICATIONS_READER_ENABLED,
                    blogId,
                    feedId
                )
                val payload = AddOrDeleteSubscriptionPayload(
                    blogId.toString(), SubscriptionAction.NEW
                )
                dispatcher.dispatch(
                    AccountActionBuilder.newUpdateSubscriptionNotificationPostAction(
                        payload
                    )
                )
                ReaderBlogTable.setNotificationsEnabledByBlogId(blogId, true)
            }.show()
        }
    }

    override fun onFollowingTapped() {
        dispatcher.dispatch(AccountActionBuilder.newFetchSubscriptionsAction())
    }

    override fun onFollowTappedWhenLoggedOut() {
        ReaderLoginRequiredBottomSheetFragment.newInstance()
            .show(childFragmentManager, ReaderLoginRequiredBottomSheetFragment.TAG)
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
        if (isAdded && currentPosition > 0) {
            recyclerView.smoothScrollToPosition(0)
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
        private const val TAB_POSTS = 0
        private const val TAB_SITES = 1
        private const val NO_POSITION = -1
        private const val RECYCLER_DELAY_MS = 1000L
        private const val BOOKMARK_IMAGE_MULTIPLIER = 1.2
        private const val BOOKMARK_IMAGE_ALPHA = 150
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

        fun newInstanceForSearch(): ReaderPostListFragment {
            AppLog.d(AppLog.T.READER, "reader post list > newInstance (search)")

            val args = Bundle()
            args.putSerializable(
                ReaderConstants.ARG_POST_LIST_TYPE,
                ReaderPostListType.SEARCH_RESULTS
            )
            args.putBoolean(ReaderConstants.ARG_IS_TOP_LEVEL, false)

            val fragment = ReaderPostListFragment()
            fragment.arguments = args
            return fragment
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
