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
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.Animation
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
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
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.datasets.ReaderSearchTable
import org.wordpress.android.datasets.ReaderTagTable
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
import org.wordpress.android.models.FilterCriteria
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostDiscoverData
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.networking.ConnectionChangeReceiver.ConnectionChangeEvent
import org.wordpress.android.ui.ActionableEmptyView
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.EmptyViewMessageType
import org.wordpress.android.ui.FilteredRecyclerView
import org.wordpress.android.ui.FilteredRecyclerView.FilterCriteriaAsyncLoaderListener
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.ViewPagerFragment
import org.wordpress.android.ui.main.BottomNavController
import org.wordpress.android.ui.main.ChooseSiteActivity
import org.wordpress.android.ui.main.WPMainActivity.OnActivityBackPressedListener
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.ui.reader.ReaderActivityLauncher.OpenUrlType
import org.wordpress.android.ui.reader.ReaderEvents.FollowedBlogsFetched
import org.wordpress.android.ui.reader.ReaderEvents.FollowedTagsFetched
import org.wordpress.android.ui.reader.ReaderEvents.SearchPostsEnded
import org.wordpress.android.ui.reader.ReaderEvents.SearchPostsStarted
import org.wordpress.android.ui.reader.ReaderEvents.TagAdded
import org.wordpress.android.ui.reader.ReaderEvents.UpdatePostsEnded
import org.wordpress.android.ui.reader.ReaderEvents.UpdatePostsStarted
import org.wordpress.android.ui.reader.ReaderInterfaces.DataLoadedListener
import org.wordpress.android.ui.reader.ReaderInterfaces.OnFollowListener
import org.wordpress.android.ui.reader.ReaderInterfaces.OnPostListItemButtonListener
import org.wordpress.android.ui.reader.ReaderInterfaces.OnPostSelectedListener
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.actions.ReaderActions
import org.wordpress.android.ui.reader.actions.ReaderActions.DataRequestedListener
import org.wordpress.android.ui.reader.adapters.ReaderPostAdapter
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
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter
import org.wordpress.android.ui.reader.services.search.ReaderSearchServiceStarter
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter
import org.wordpress.android.ui.reader.services.update.TagUpdateClientUtilsProvider
import org.wordpress.android.ui.reader.subfilter.SubFilterViewModel
import org.wordpress.android.ui.reader.subfilter.SubFilterViewModelProvider.Companion.getSubFilterViewModelForTag
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.SiteAll
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.tracker.ReaderTracker.Companion.trackTag
import org.wordpress.android.ui.reader.usecases.BookmarkPostState.PreLoadPostContent
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.FollowStatusChanged
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.reader.viewmodels.ReaderModeInfo
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListViewModel
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
import org.wordpress.android.ui.reader.views.ReaderSiteHeaderView.OnBlogInfoFailedListener
import org.wordpress.android.ui.reader.views.ReaderSiteHeaderView.OnBlogInfoLoadedListener
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.WPActivityUtils
import org.wordpress.android.util.config.SeenUnseenWithCounterFeatureConfig
import org.wordpress.android.util.extensions.getColorFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.widgets.AppReviewManager.incrementInteractions
import org.wordpress.android.widgets.RecyclerItemDecoration
import org.wordpress.android.widgets.WPSnackbar.Companion.make
import java.util.EnumSet
import java.util.Locale
import javax.inject.Inject

@Suppress("LargeClass")
class ReaderPostListFragment : ViewPagerFragment(), OnPostSelectedListener, OnFollowListener,
    OnPostListItemButtonListener, OnActivityBackPressedListener, OnScrollToTopListener {
    private val tagPreviewHistory = ReaderHistoryStack("tag_preview_history")

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
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Inject
    lateinit var tagUpdateClientUtilsProvider: TagUpdateClientUtilsProvider

    @Inject
    lateinit var quickStartUtilsWrapper: QuickStartUtilsWrapper

    @Inject
    lateinit var seenUnseenWithCounterFeatureConfig: SeenUnseenWithCounterFeatureConfig

    @Inject
    lateinit var jetpackBrandingUtils: JetpackBrandingUtils

    @Inject
    lateinit var quickStartRepository: QuickStartRepository

    @Inject
    lateinit var readerTracker: ReaderTracker

    @Inject
    lateinit var snackbarSequencer: SnackbarSequencer

    @Inject
    lateinit var displayUtilsWrapper: DisplayUtilsWrapper

    private var readerPostAdapter: ReaderPostAdapter? = null
    private var siteSearchAdapter: ReaderSiteSearchAdapter? = null
    private var suggestionAdapter: ReaderSearchSuggestionAdapter? = null
    private var searchSuggestionRecyclerAdapter: ReaderSearchSuggestionRecyclerAdapter? = null

    private lateinit var recyclerView: FilteredRecyclerView
    private lateinit var newPostsBar: View
    private lateinit var progressBar: ProgressBar
    private lateinit var searchMenuItem: MenuItem
    private lateinit var jetpackBanner: View

    private lateinit var postListViewModel: ReaderPostListViewModel

    private var bottomNavController: BottomNavController? = null
    private var actionableEmptyView: ActionableEmptyView? = null
    private var searchTabs: TabLayout? = null
    private var searchView: SearchView? = null

    private var firstLoad = true
    private var isTopLevel = false
    private var wasPaused = false
    private var hasRequestedPosts = false
    private var hasUpdatedPosts = false
    private var isAnimatingOutNewPostsBar = false

    private var currentReaderTag: ReaderTag? = null
    private var currentBlogId: Long = 0
    private var currentFeedId: Long = 0
    private var currentSearchQuery: String? = null
    private var readerPostListType: ReaderPostListType? = null
    private var lastTappedSiteSearchResult: ReaderSiteModel? = null
    private var tagFragmentStartedWith: ReaderTag? = null

    private var restorePosition = 0
    private var siteSearchRestorePosition = 0
    private var postSearchAdapterPos = 0
    private var siteSearchAdapterPos = 0
    private var searchTabsPos = NO_POSITION

    private var isFilterableScreen = false
    private var isFiltered = false
    private var readerSubsActivityResultLauncher: ActivityResultLauncher<Intent>? = null
    private var currentUpdateActions = HashSet<ReaderPostServiceStarter.UpdateAction>()

    private var bookmarksSavedLocallyDialog: AlertDialog? = null

    // This VM is initialized only on the Following tab
    private var subFilterViewModel: SubFilterViewModel? = null
    private var readerViewModel: ReaderViewModel? = null

    /*
     * called by post adapter to load older posts when user scrolls to the last post
     */
    private val dataRequestedListener: DataRequestedListener = object : DataRequestedListener {
        override fun onRequestData() {
            // skip if update is already in progress
            if (isUpdating) {
                return
            }

            // request older posts unless we already have the max # to show
            when (getPostListType()) {
                ReaderPostListType.TAG_FOLLOWED,
                ReaderPostListType.TAG_PREVIEW -> {
                    if (ReaderPostTable.getNumPostsWithTag(currentReaderTag) <
                        ReaderConstants.READER_MAX_POSTS_TO_DISPLAY
                    ) {
                        // request older posts
                        updatePostsWithTag(
                            currentTag,
                            ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER
                        )
                        readerTracker.track(AnalyticsTracker.Stat.READER_INFINITE_SCROLL)
                    }
                }

                ReaderPostListType.BLOG_PREVIEW -> {
                    val numPosts = if (currentFeedId != 0L) {
                        ReaderPostTable.getNumPostsInFeed(currentFeedId)
                    } else {
                        ReaderPostTable.getNumPostsInBlog(currentBlogId)
                    }
                    if (numPosts < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY) {
                        updatePostsInCurrentBlogOrFeed(ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER)
                        readerTracker.track(AnalyticsTracker.Stat.READER_INFINITE_SCROLL)
                    }
                }

                ReaderPostListType.SEARCH_RESULTS -> {
                    val searchTag = ReaderUtils.getTagForSearchQuery(
                        currentSearchQuery!!
                    )
                    val offset = ReaderPostTable.getNumPostsWithTag(searchTag)
                    if (offset < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY) {
                        updatePostsInCurrentSearch(offset)
                        readerTracker.track(AnalyticsTracker.Stat.READER_INFINITE_SCROLL)
                    }
                }

                else -> {
                    // noop
                }
            }
        }
    }

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)

        args?.let { arguments ->
            if (arguments.containsKey(ReaderConstants.ARG_TAG)) {
                currentReaderTag = BundleCompat.getSerializable(
                    arguments,
                    ReaderConstants.ARG_TAG,
                    ReaderTag::class.java
                )
            }
            if (arguments.containsKey(ReaderConstants.ARG_ORIGINAL_TAG)) {
                tagFragmentStartedWith =
                    BundleCompat.getSerializable(
                        arguments,
                        ReaderConstants.ARG_ORIGINAL_TAG,
                        ReaderTag::class.java
                    )
            }
            if (arguments.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                readerPostListType =
                    BundleCompat.getSerializable(
                        arguments,
                        ReaderConstants.ARG_POST_LIST_TYPE,
                        ReaderPostListType::class.java
                    )
            }

            if (arguments.containsKey(ReaderConstants.ARG_IS_TOP_LEVEL)) {
                isTopLevel = arguments.getBoolean(ReaderConstants.ARG_IS_TOP_LEVEL)
            }
            if (arguments.containsKey(ReaderConstants.ARG_IS_FILTERABLE)) {
                isFilterableScreen = arguments.getBoolean(ReaderConstants.ARG_IS_FILTERABLE)
            }

            currentBlogId = arguments.getLong(ReaderConstants.ARG_BLOG_ID)
            currentFeedId = arguments.getLong(ReaderConstants.ARG_FEED_ID)
            currentSearchQuery = arguments.getString(ReaderConstants.ARG_SEARCH_QUERY)

            if (getPostListType() == ReaderPostListType.TAG_PREVIEW && hasCurrentTag()) {
                tagPreviewHistory.push(currentTagName)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)

        savedInstanceState?.let {
            restoreState(it)
        }
    }

    private fun restoreState(state: Bundle) {
        AppLog.d(AppLog.T.READER, "reader post list fragment > restoring instance state")
        if (state.containsKey(ReaderConstants.ARG_TAG)) {
            currentReaderTag =
                BundleCompat.getSerializable(state, ReaderConstants.ARG_TAG, ReaderTag::class.java)
        }
        if (state.containsKey(ReaderConstants.ARG_BLOG_ID)) {
            currentBlogId = state.getLong(ReaderConstants.ARG_BLOG_ID)
        }
        if (state.containsKey(ReaderConstants.ARG_FEED_ID)) {
            currentFeedId = state.getLong(ReaderConstants.ARG_FEED_ID)
        }
        if (state.containsKey(ReaderConstants.ARG_SEARCH_QUERY)) {
            currentSearchQuery = state.getString(ReaderConstants.ARG_SEARCH_QUERY)
        }
        if (state.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
            readerPostListType =
                BundleCompat.getSerializable(
                    state,
                    ReaderConstants.ARG_POST_LIST_TYPE,
                    ReaderPostListType::class.java
                )
        }
        if (getPostListType() == ReaderPostListType.TAG_PREVIEW) {
            tagPreviewHistory.restoreInstance(state)
        }
        if (state.containsKey(ReaderConstants.ARG_IS_TOP_LEVEL)) {
            isTopLevel = state.getBoolean(ReaderConstants.ARG_IS_TOP_LEVEL)
        }
        if (state.containsKey(ReaderConstants.ARG_IS_FILTERABLE)) {
            isFilterableScreen =
                state.getBoolean(ReaderConstants.ARG_IS_FILTERABLE)
        }

        if (state.containsKey(ReaderConstants.ARG_ORIGINAL_TAG)) {
            tagFragmentStartedWith =
                BundleCompat.getSerializable(
                    state,
                    ReaderConstants.ARG_ORIGINAL_TAG,
                    ReaderTag::class.java
                )
        }

        restorePosition = state.getInt(ReaderConstants.KEY_RESTORE_POSITION)
        siteSearchRestorePosition =
            state.getInt(ReaderConstants.KEY_SITE_SEARCH_RESTORE_POSITION)
        wasPaused = state.getBoolean(ReaderConstants.KEY_WAS_PAUSED)
        hasRequestedPosts =
            state.getBoolean(ReaderConstants.KEY_ALREADY_REQUESTED)
        hasUpdatedPosts = state.getBoolean(ReaderConstants.KEY_ALREADY_UPDATED)
        firstLoad = state.getBoolean(ReaderConstants.KEY_FIRST_LOAD)
        searchTabsPos =
            state.getInt(ReaderConstants.KEY_ACTIVE_SEARCH_TAB, NO_POSITION)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postListViewModel = ViewModelProvider(this, viewModelFactory)[ReaderPostListViewModel::class.java]

        if (isTopLevel) {
            readerViewModel = ViewModelProvider(
                requireParentFragment(),
                viewModelFactory
            )[ReaderViewModel::class.java]
        }

        if (isFilterableScreen) {
            initSubFilterViewModel(savedInstanceState)
        }

        setupObservers()
        postListViewModel.start(readerViewModel)

        if (isFollowingScreen) {
            subFilterViewModel!!.onUserComesToReader()
        }

        if (isSearching) {
            recyclerView.showAppBarLayout()
            searchMenuItem.expandActionView()
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

        postListViewModel.refreshPosts.observe(
            viewLifecycleOwner
        ) { event: Event<Unit> ->
            event.applyIfNotHandled {
                refreshPosts()
            }
        }

        postListViewModel.updateFollowStatus.observe(
            viewLifecycleOwner
        ) { readerData: FollowStatusChanged ->
            setFollowStatusForBlog(readerData)
        }
    }

    private fun toggleJetpackBannerIfEnabled(showIfEnabled: Boolean, animateOnScroll: Boolean) {
        if (!isAdded || view == null || !isSearching) return

        if (jetpackBrandingUtils.shouldShowJetpackBranding()) {
            if (animateOnScroll) {
                val scrollView = recyclerView.internalRecyclerView
                jetpackBrandingUtils.showJetpackBannerIfScrolledToTop(
                    jetpackBanner,
                    scrollView
                )
                // Return early since the banner visibility was handled by showJetpackBannerIfScrolledToTop
                return
            }

            if (showIfEnabled && !displayUtilsWrapper.isPhoneLandscape()) {
                showJetpackBanner()
            } else {
                hideJetpackBanner()
            }
        }
    }

    private fun showJetpackBanner() {
        jetpackBanner.visibility = View.VISIBLE

        // Add bottom margin to search suggestions list and empty view.
        val jetpackBannerHeight = resources.getDimensionPixelSize(R.dimen.jetpack_banner_height)
        (recyclerView.searchSuggestionsRecyclerView.layoutParams as MarginLayoutParams).bottomMargin
        (actionableEmptyView!!.layoutParams as MarginLayoutParams).bottomMargin =
            jetpackBannerHeight
    }

    private fun hideJetpackBanner() {
        jetpackBanner.visibility = View.GONE

        // Remove bottom margin from search suggestions list and empty view.
        (recyclerView.searchSuggestionsRecyclerView.layoutParams as MarginLayoutParams).bottomMargin =
            0
        (actionableEmptyView!!.layoutParams as MarginLayoutParams).bottomMargin = 0
    }

    private fun setFollowStatusForBlog(readerData: FollowStatusChanged) {
        if (!hasPostAdapter()) {
            return
        }
        getPostAdapter().setFollowStatusForBlog(readerData.blogId, readerData.following)
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
        ) {
            if (getPostListType() != ReaderPostListType.SEARCH_RESULTS) {
                if (shouldShowEmptyViewForSelfHostedCta()) {
                    setEmptyTitleDescriptionAndButton(false)
                    showEmptyView()
                }
            }
        }

        subFilterViewModel!!.readerModeInfo.observe(
            viewLifecycleOwner
        ) { readerModeInfo: ReaderModeInfo? ->
            if (readerModeInfo != null) {
                changeReaderMode(readerModeInfo, true)
            }
        }
    }

    private fun changeReaderMode(readerModeInfo: ReaderModeInfo, onlyOnChanges: Boolean) {
        var changesDetected = false

        if (onlyOnChanges) {
            changesDetected =
                (readerModeInfo.tag != null && currentReaderTag != null && (readerModeInfo.tag != currentReaderTag))
                        || (readerPostListType != readerModeInfo.listType)
                        || (currentBlogId != readerModeInfo.blogId)
                        || (currentFeedId != readerModeInfo.feedId)
                        || (readerModeInfo.isFirstLoad)

            if (changesDetected && !readerModeInfo.isFirstLoad) {
                trackTagLoaded(readerModeInfo.tag)
            }
        }

        if (onlyOnChanges && !changesDetected) return

        if (readerModeInfo.tag != null) {
            currentReaderTag = readerModeInfo.tag
        }

        readerPostListType = readerModeInfo.listType
        currentBlogId = readerModeInfo.blogId
        currentFeedId = readerModeInfo.feedId
        isFiltered = readerModeInfo.isFiltered

        resetPostAdapter(readerPostListType!!)
        if (readerModeInfo.requestNewerPosts) {
            updatePosts(false)
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

            val currentSite: SubfilterListItem.Site?

            if (getPostListType() == ReaderPostListType.TAG_FOLLOWED) {
                resumeFollowedTag()
            } else if ((getSiteIfBlogPreview().also { currentSite = it }) != null) {
                resumeFollowedSite(currentSite!!)
            } else {
                refreshPosts()
            }

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

        if (shouldShowEmptyViewForSelfHostedCta()) {
            setEmptyTitleDescriptionAndButton(false)
            showEmptyView()
        }

        postListViewModel.onFragmentResume(
            isTopLevel, isSearching, isFilterableScreen,
            if (isFilterableScreen) subFilterViewModel!!.getCurrentSubfilterValue() else null
        )
    }

    /*
     * called when fragment is resumed and we're looking at posts in a followed tag
     */
    private fun resumeFollowedTag() {
        val addedTag = EventBus.getDefault().getStickyEvent(
            TagAdded::class.java
        )
        if (isFollowingScreen && addedTag != null) {
            EventBus.getDefault().removeStickyEvent(addedTag)
            // user just added a tag so switch to it.
            val newTag = ReaderUtils.getTagFromTagName(addedTag.tagName, ReaderTagType.FOLLOWED)
            subFilterViewModel!!.setSubfilterFromTag(newTag)
        } else if (isFollowingScreen && !ReaderTagTable.tagExists(currentTag)) {
            // user just removed a tag which was selected in the subfilter
            subFilterViewModel!!.setDefaultSubfilter(false)
        } else {
            // otherwise, refresh posts to make sure any changes are reflected and auto-update
            // posts in the current tag if it's time
            refreshPosts()
            updateCurrentTagIfTime()
        }
    }

    private fun getSiteIfBlogPreview(): SubfilterListItem.Site? {
        if (isFilterableScreen && (getPostListType() == ReaderPostListType.BLOG_PREVIEW)) {
            return subFilterViewModel!!.getCurrentSubfilterValue() as? SubfilterListItem.Site
        }
        return null
    }

    private fun resumeFollowedSite(currentSite: SubfilterListItem.Site) {
        var isSiteStillAvailable = false
        val blog = currentSite.blog
        if ((blog.hasFeedUrl() && ReaderBlogTable.isFollowedFeed(blog.feedId))
            || ReaderBlogTable.isFollowedBlog(blog.blogId)
        ) {
            isSiteStillAvailable = true
        }

        if (isSiteStillAvailable) {
            refreshPosts()
        } else {
            if (isFilterableScreen) {
                subFilterViewModel!!.setDefaultSubfilter(false)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // detect the bottom nav controller when this fragment is hosted in the main activity - this is used to
        // hide the bottom nav when the user searches from the reader
        if (context is BottomNavController) {
            bottomNavController = context
        }

        initReaderSubsActivityResultLauncher()

        val activity: Activity? = activity
        if (activity != null) {
            val intent = Intent()
            intent.putExtra(ReaderTagsFeedFragment.RESULT_SHOULD_REFRESH_TAGS_FEED, true)
            activity.setResult(Activity.RESULT_OK, intent)
        }
    }

    private fun initReaderSubsActivityResultLauncher() {
        readerSubsActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data != null) {
                    val shouldRefreshSubscriptions =
                        data.getBooleanExtra(
                            ReaderSubsActivity.RESULT_SHOULD_REFRESH_SUBSCRIPTIONS,
                            false
                        )
                    if (shouldRefreshSubscriptions) {
                        subFilterViewModel!!.loadSubFilters()
                    }
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        bottomNavController = null
        readerPostAdapter?.setOnBlogInfoLoadedListener(null)
        readerPostAdapter?.setOnBlogInfoFailedListener(null)
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
        EventBus.getDefault().register(this)

        reloadTags()

        // purge database and update followed tags/blog if necessary - note that we don't purge unless
        // there's a connection to avoid removing posts the user would expect to see offline
        if (getPostListType() == ReaderPostListType.TAG_FOLLOWED && NetworkUtils.isNetworkAvailable(
                activity
            )
        ) {
            purgeDatabaseIfNeeded()
        }

        checkPostAdapter()
    }

    override fun onStop() {
        super.onStop()
        newPostsBar.clearAnimation()
        dispatcher.unregister(this)
        EventBus.getDefault().unregister(this)
    }

    /*
     * ensures the adapter is created and posts are updated if they haven't already been
     */
    private fun checkPostAdapter() {
        if (isAdded && recyclerView.adapter == null) {
            recyclerView.adapter = getPostAdapter()
            refreshPosts()
            if (!hasRequestedPosts && NetworkUtils.isNetworkAvailable(
                    activity
                )
            ) {
                hasRequestedPosts = true
                if (getPostListType().isTagType) {
                    updateCurrentTagIfTime()
                } else if (getPostListType() == ReaderPostListType.BLOG_PREVIEW) {
                    updatePostsInCurrentBlogOrFeed(ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER)
                }
            }
        }
    }

    /*
     * reset the post adapter to initial state and create it again using the passed list type
     */
    private fun resetPostAdapter(postListType: ReaderPostListType) {
        readerPostListType = postListType
        readerPostAdapter = null
        recyclerView.adapter = null
        recyclerView.adapter = getPostAdapter()
        recyclerView.setSwipeToRefreshEnabled(isSwipeToRefreshSupported)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: FollowedTagsFetched) {
        if (getPostListType() == ReaderPostListType.TAG_FOLLOWED) {
            if (event.didChange()) {
                // reload the tag filter since tags have changed or we just opened the fragment
                reloadTags()
            }

            // update the current tag if the list fragment is empty - this will happen if
            // the tag table was previously empty (ie: first run)
            if (isPostAdapterEmpty() && (ReaderBlogTable.hasFollowedBlogs() || !hasUpdatedPosts)) {
                updateCurrentTag()
            }
        }
    }

    @Suppress("unused", "ComplexCondition")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: FollowedBlogsFetched) {
        // refresh posts if user is viewing "Followed Sites"
        if (event.didChange()
            && (getPostListType() == ReaderPostListType.TAG_FOLLOWED && hasCurrentTag())
            && (currentTag!!.isFollowedSites || currentTag!!.isDefaultInMemoryTag)
        ) {
            refreshPosts()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        AppLog.d(AppLog.T.READER, "reader post list > saving instance state")

        if (currentReaderTag != null) {
            outState.putSerializable(ReaderConstants.ARG_TAG, currentReaderTag)
        }

        if (tagFragmentStartedWith != null) {
            outState.putSerializable(ReaderConstants.ARG_ORIGINAL_TAG, tagFragmentStartedWith)
        }

        if (getPostListType() == ReaderPostListType.TAG_PREVIEW) {
            tagPreviewHistory.saveInstance(outState)
        } else if (isSearching && searchView != null && searchView!!.query != null) {
            val query = searchView!!.query.toString()
            outState.putString(ReaderConstants.ARG_SEARCH_QUERY, query)
        }

        outState.putLong(ReaderConstants.ARG_BLOG_ID, currentBlogId)
        outState.putLong(ReaderConstants.ARG_FEED_ID, currentFeedId)
        outState.putBoolean(ReaderConstants.KEY_WAS_PAUSED, wasPaused)
        outState.putBoolean(ReaderConstants.KEY_ALREADY_REQUESTED, hasRequestedPosts)
        outState.putBoolean(ReaderConstants.KEY_ALREADY_UPDATED, hasUpdatedPosts)
        outState.putBoolean(ReaderConstants.KEY_FIRST_LOAD, firstLoad)
        outState.putSerializable(ReaderConstants.KEY_CURRENT_UPDATE_ACTIONS, currentUpdateActions)
        outState.putInt(ReaderConstants.KEY_RESTORE_POSITION, currentPosition)
        outState.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, getPostListType())
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

    private val currentPosition: Int
        get() {
            return if (hasPostAdapter()) {
                recyclerView.currentPosition
            } else {
                -1
            }
        }

    private fun updatePosts(forced: Boolean) {
        if (!isAdded) {
            return
        }

        if (!NetworkUtils.checkConnection(activity)) {
            recyclerView.isRefreshing = false
            return
        }

        if (forced) {
            // Update the tags on post refresh since following some sites (like P2) will change followed tags and blogs
            ReaderUpdateServiceStarter.startService(
                context,
                EnumSet.of(UpdateTask.TAGS, UpdateTask.FOLLOWED_BLOGS)
            )
        }

        if (firstLoad) {
            // let onResume() take care of this logic, as the FilteredRecyclerView.FilterListener onLoadData
            // method is called on two moments: once for first time load, and then each time the swipe to
            // refresh gesture triggers a refresh.
            recyclerView.isRefreshing = false
            firstLoad = false
        } else {
            val updateAction = if (forced)
                ReaderPostServiceStarter.UpdateAction.REQUEST_REFRESH
            else
                ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER
            when (getPostListType()) {
                ReaderPostListType.TAG_FOLLOWED, ReaderPostListType.TAG_PREVIEW -> updatePostsWithTag(
                    currentTag, updateAction
                )

                ReaderPostListType.BLOG_PREVIEW -> updatePostsInCurrentBlogOrFeed(updateAction)
                ReaderPostListType.SEARCH_RESULTS -> {}
                ReaderPostListType.TAGS_FEED -> {}
            }
            // make sure swipe-to-refresh progress shows since this is a manual refresh
            recyclerView.isRefreshing = true
        }
        if (currentTag != null && currentTag!!.isBookmarked) {
            ReaderPostTable.purgeUnbookmarkedPostsWithBookmarkTag()
            refreshPosts()
        }
    }

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
        newPostsBar.setOnClickListener {
            recyclerView.scrollRecycleViewToPosition(0)
            refreshPosts()
        }

        // progress bar that appears when loading more posts
        progressBar = rootView.findViewById(R.id.progress_footer)
        progressBar.visibility = View.GONE

        jetpackBanner = rootView.findViewById(R.id.jetpack_banner)
        setupJetpackBanner()

        if (savedInstanceState?.containsKey(ReaderConstants.KEY_CURRENT_UPDATE_ACTIONS) == true) {
            val actions =
                BundleCompat.getSerializable(
                    savedInstanceState,
                    ReaderConstants.KEY_CURRENT_UPDATE_ACTIONS,
                    HashSet::class.java
                )
            if (actions is HashSet<*>) {
                @Suppress("UNCHECKED_CAST")
                currentUpdateActions = actions as HashSet<ReaderPostServiceStarter.UpdateAction>
                updateProgressIndicators()
            }
        }

        return rootView
    }

    private fun setupRecycler() {
        recyclerView.setLogT(AppLog.T.READER)
        recyclerView.setCustomEmptyView()

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
        recyclerView.setToolbarSpinnerDrawable(R.drawable.ic_dropdown_primary_30_24dp)

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
    }

    private fun setupRecyclerFilterListener() {
        recyclerView.setFilterListener(object : FilteredRecyclerView.FilterListener {
            override fun onLoadFilterCriteriaOptions(refresh: Boolean): List<FilterCriteria> {
                return emptyList()
            }

            override fun onLoadFilterCriteriaOptionsAsync(
                listener: FilterCriteriaAsyncLoaderListener, refresh: Boolean
            ) {
                // noop
            }

            override fun onLoadData(forced: Boolean) {
                if (forced) {
                    readerTracker.track(AnalyticsTracker.Stat.READER_PULL_TO_REFRESH)
                }
                updatePosts(forced)
            }

            override fun onFilterSelected(position: Int, criteria: FilterCriteria) {
                onTagChanged(criteria as ReaderTag)
            }

            override fun onRecallSelection(): FilterCriteria {
                if (hasCurrentTag()) {
                    val defaultTag = ReaderUtils.getDefaultTagFromDbOrCreateInMemory(
                        requireActivity(),
                        tagUpdateClientUtilsProvider
                    )

                    val tag = ReaderUtils.getValidTagForSharedPrefs(
                        currentTag!!,
                        isTopLevel,
                        recyclerView,
                        defaultTag
                    )

                    return tag
                } else {
                    AppLog.w(
                        AppLog.T.READER,
                        "reader post list > no current tag in onRecallSelection"
                    )
                    return ReaderUtils.getDefaultTag()
                }
            }

            override fun onShowEmptyViewMessage(emptyViewMsgType: EmptyViewMessageType): String? {
                return null
            }

            override fun onShowCustomEmptyView(emptyViewMsgType: EmptyViewMessageType) {
                setEmptyTitleDescriptionAndButton(
                    EmptyViewMessageType.NETWORK_ERROR == emptyViewMsgType
                            || EmptyViewMessageType.PERMISSION_ERROR == emptyViewMsgType
                            || EmptyViewMessageType.GENERIC_ERROR == emptyViewMsgType
                )
            }
        })
    }

    /*
     * adds a menu to the recycler toolbar containing search items - only called
     * for followed tags
     */
    private fun setupRecyclerToolbar() {
        val menu = recyclerView.addToolbarMenu(R.menu.reader_list)
        searchMenuItem = menu.findItem(R.id.menu_reader_search)

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
                if (getPostListType() != ReaderPostListType.SEARCH_RESULTS) {
                    readerTracker.track(AnalyticsTracker.Stat.READER_SEARCH_LOADED)
                }
                resetPostAdapter(ReaderPostListType.SEARCH_RESULTS)
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

    private fun setupJetpackBanner() {
        if (jetpackBrandingUtils.shouldShowJetpackBranding()) {
            val screen: JetpackPoweredScreen = JetpackPoweredScreen.WithDynamicText.READER_SEARCH
            jetpackBrandingUtils.initJetpackBannerAnimation(
                jetpackBanner,
                recyclerView.internalRecyclerView
            )
            val jetpackBannerTextView =
                jetpackBanner.findViewById<TextView>(R.id.jetpack_banner_text)
            jetpackBannerTextView.text = uiHelpers.getTextOfUiString(
                requireContext(),
                jetpackBrandingUtils.getBrandingTextForScreen(screen)
            )

            if (jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) {
                jetpackBanner.setOnClickListener {
                    jetpackBrandingUtils.trackBannerTapped(screen)
                    JetpackPoweredBottomSheetFragment()
                        .show(childFragmentManager, JetpackPoweredBottomSheetFragment.TAG)
                }
            }
        }
    }

    private fun showSearchMessageOrSuggestions() {
        val hasQuery = !isSearchViewEmpty
        val hasPerformedSearch = !TextUtils.isEmpty(currentSearchQuery)

        toggleJetpackBannerIfEnabled(showIfEnabled = true, animateOnScroll = false)

        // prevents suggestions from being shown after the search view has been collapsed
        if (!isSearching) {
            return
        }

        // prevents suggestions from being shown above search results after configuration changes
        if (wasPaused && hasPerformedSearch) {
            return
        }

        if (!hasQuery || !hasPerformedSearch) {
            // clear posts and sites so only the suggestions or the empty view are visible
            getPostAdapter().clear()
            getSiteSearchAdapter().clear()

            hideSearchTabs()

            // clears the last performed query
            currentSearchQuery = null

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
        get() = getPostListType() == ReaderPostListType.SEARCH_RESULTS

    /*
    * start the search service to search for posts matching the current query - the passed
    * offset is used during infinite scroll, pass zero for initial search
    */
    private fun updatePostsInCurrentSearch(offset: Int) {
        ReaderSearchServiceStarter.startService(activity, currentSearchQuery!!, offset)
    }

    /*
     * start a search for reader sites matching the current search query
     */
    private fun updateSitesInCurrentSearch(offset: Int) {
        if (searchTabsPosition == TAB_SITES) {
            if (offset == 0) {
                recyclerView.isRefreshing = true
            } else {
                showLoadingProgress(true)
            }
        }
        val payload = ReaderSearchSitesPayload(
            currentSearchQuery!!,
            ReaderConstants.READER_MAX_SEARCH_RESULTS_TO_REQUEST,
            offset,
            false
        )
        dispatcher.dispatch(ReaderActionBuilder.newReaderSearchSitesAction(payload))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            if (currentSearchQuery != null) {
                submitSearchQuery(currentSearchQuery!!)
            }
        }
    }

    private fun submitSearchQuery(query: String) {
        if (!isAdded) {
            return
        }

        searchView!!.clearFocus() // this will hide suggestions and the virtual keyboard
        hideSearchMessage()
        hideSearchSuggestions()

        if (!NetworkUtils.isNetworkAvailable(context)) {
            showEmptyView()
        }

        // remember this query for future suggestions
        val trimQuery = query.trim { it <= ' ' }
        ReaderSearchTable.addOrUpdateQueryString(trimQuery)

        // remove cached results for this search - search results are ephemeral so each search
        // should be treated as a "fresh" one
        val searchTag = ReaderUtils.getTagForSearchQuery(trimQuery)
        ReaderPostTable.deletePostsWithTag(searchTag)

        getPostAdapter().setCurrentTag(searchTag)
        currentSearchQuery = trimQuery
        updatePostsInCurrentSearch(0)
        updateSitesInCurrentSearch(0)

        toggleJetpackBannerIfEnabled(showIfEnabled = false, animateOnScroll = false)

        // track that the user performed a search
        if (trimQuery != "") {
            readerTracker.trackQuery(AnalyticsTracker.Stat.READER_SEARCH_PERFORMED, trimQuery)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReaderSitesSearched(event: OnReaderSitesSearched) {
        if (!isAdded || getPostListType() != ReaderPostListType.SEARCH_RESULTS) {
            return
        }

        if (!isUpdating) {
            recyclerView.isRefreshing = false
        }
        showLoadingProgress(false)

        val adapter = getSiteSearchAdapter()
        if (event.isError) {
            adapter.clear()
        } else if (StringUtils.equals(event.searchTerm, currentSearchQuery)) {
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
            setEmptyTitleDescriptionAndButton(event.isError)
            showEmptyView()
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

        setEmptyTitleDescriptionAndButton(false)
        showEmptyView()
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
                        if (getPostAdapter().isEmpty) {
                            setEmptyTitleDescriptionAndButton(false)
                            showEmptyView()
                        } else {
                            hideEmptyView()
                        }
                    } else if (tab.position == TAB_SITES) {
                        recyclerView.adapter = getSiteSearchAdapter()
                        if (siteSearchAdapterPos > 0) {
                            recyclerView.scrollRecycleViewToPosition(siteSearchAdapterPos)
                        }
                        if (getSiteSearchAdapter().isEmpty) {
                            setEmptyTitleDescriptionAndButton(false)
                            showEmptyView()
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

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: SearchPostsStarted) {
        if (!isAdded || getPostListType() != ReaderPostListType.SEARCH_RESULTS) {
            return
        }

        val updateAction = if (event.offset == 0) {
            ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER
        } else {
            ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER
        }
        setIsUpdating(true, updateAction)
        setEmptyTitleDescriptionAndButton(false)
        if (isPostAdapterEmpty()) showEmptyView()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: SearchPostsEnded) {
        if (!isAdded || getPostListType() != ReaderPostListType.SEARCH_RESULTS) {
            return
        }

        val updateAction = if (event.offset == 0) {
            ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER
        } else {
            ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER
        }
        setIsUpdating(false, updateAction)

        // load the results if the search succeeded and it's the current search - note that success
        // means the search didn't fail, not necessarily that is has results - which is fine because
        // if there aren't results then refreshing will show the empty message
        if (event.didSucceed() && isSearching && event.query == currentSearchQuery) {
            refreshPosts()
            showSearchTabs()
        } else {
            hideSearchTabs()
        }
    }

    /*
     * returns the parent view for snackbars - if this fragment is hosted in the main activity we want the
     * parent to be the main activity's CoordinatorLayout
     */
    private fun getSnackbarParent(): View? {
        val coordinator =
            requireActivity().findViewById<View>(R.id.coordinator_layout)
        return coordinator ?: view
    }

    @Suppress("LongMethod", "NestedBlockDepth", "CyclomaticComplexMethod", "ReturnCount")
    private fun setEmptyTitleDescriptionAndButton(requestFailed: Boolean) {
        if (!isAdded) {
            return
        }

        val heightToolbar =
            requireActivity().resources.getDimensionPixelSize(R.dimen.toolbar_height)
        val heightTabs = requireActivity().resources.getDimensionPixelSize(R.dimen.tab_height)
        actionableEmptyView!!.updateLayoutForSearch(false, 0)
        actionableEmptyView!!.subtitle.contentDescription = null
        var isImageHidden = false
        val title: String
        var description: String? = null
        var button: ActionableEmptyViewButtonType? = null

        // Ensure the default image is reset for empty views before applying logic
        actionableEmptyView!!.image.setImageResource(0)

        if (shouldShowEmptyViewForSelfHostedCta()) {
            setEmptyTitleAndDescriptionForSelfHostedCta()
            return
        } else if (getPostListType() == ReaderPostListType.TAG_FOLLOWED && currentTag!!.isBookmarked) {
            setEmptyTitleAndDescriptionForBookmarksList()
            return
        } else if (!NetworkUtils.isNetworkAvailable(activity)) {
            clearCurrentUpdateActions()
            title = getString(R.string.reader_empty_posts_no_connection)
        } else if (requestFailed) {
            title = if (isSearching) {
                getString(R.string.reader_empty_search_request_failed)
            } else {
                getString(R.string.reader_empty_posts_request_failed)
            }
        } else if (isUpdating && getPostListType() != ReaderPostListType.SEARCH_RESULTS) {
            title = getString(R.string.reader_empty_posts_in_tag_updating)
        } else {
            when (getPostListType()) {
                ReaderPostListType.TAG_FOLLOWED -> {
                    if (currentTag!!.isFollowedSites || currentTag!!.isDefaultInMemoryTag) {
                        isImageHidden = true

                        if (ReaderBlogTable.hasFollowedBlogs()) {
                            title =
                                getString(R.string.reader_empty_followed_blogs_no_recent_posts_title)
                            description = getString(
                                R.string.reader_empty_followed_blogs_subscribed_no_recent_posts_description
                            )
                        } else {
                            title = getString(R.string.reader_no_followed_blogs_title)
                            description = getString(R.string.reader_no_followed_blogs_description)
                        }

                        button = ActionableEmptyViewButtonType.DISCOVER
                    } else if (currentTag!!.isPostsILike) {
                        title = getString(R.string.reader_empty_posts_liked_title)
                        description = getString(R.string.reader_empty_posts_liked_description)
                        button = ActionableEmptyViewButtonType.FOLLOWED
                    } else if (currentTag!!.isListTopic) {
                        title = getString(R.string.reader_empty_blogs_posts_in_custom_list)
                    } else {
                        title = getString(R.string.reader_no_posts_with_this_tag)
                    }
                }

                ReaderPostListType.BLOG_PREVIEW -> title =
                    getString(R.string.reader_no_posts_in_blog)

                ReaderPostListType.SEARCH_RESULTS -> {
                    isImageHidden = true

                    if (isSearchViewEmpty || TextUtils.isEmpty(currentSearchQuery)) {
                        title = getString(R.string.reader_label_post_search_explainer)
                        actionableEmptyView!!.updateLayoutForSearch(true, heightToolbar)
                    } else if (isUpdating) {
                        title = ""
                        actionableEmptyView!!.updateLayoutForSearch(true, heightToolbar)
                    } else {
                        title = getString(R.string.reader_empty_search_title)
                        val formattedQuery = "<em>$currentSearchQuery</em>"
                        description = String.format(
                            getString(R.string.reader_empty_search_description),
                            formattedQuery
                        )
                        actionableEmptyView!!.updateLayoutForSearch(
                            true,
                            heightToolbar + heightTabs
                        )
                    }
                }

                ReaderPostListType.TAG_PREVIEW -> title =
                    getString(R.string.reader_no_posts_with_this_tag)

                else -> title = getString(R.string.reader_no_posts_with_this_tag)
            }
        }

        setEmptyTitleDescriptionAndButton(title, description, button, isImageHidden)
    }

    /*
     * Currently, only local bookmarks are supported.  Show an empty view if the local database has no data.
     */
    private fun setEmptyTitleAndDescriptionForBookmarksList() {
        // replace %s placeholder with bookmark outline icon
        val description = getString(R.string.reader_empty_saved_posts_description)
        val ssb = SpannableStringBuilder(description)
        val imagePlaceholderPosition = description.indexOf("%s")
        addBookmarkImageSpan(ssb, imagePlaceholderPosition)
        actionableEmptyView!!.image.visibility = View.VISIBLE
        actionableEmptyView!!.title.setText(R.string.reader_empty_saved_posts_title)
        actionableEmptyView!!.subtitle.text = ssb
        actionableEmptyView!!.subtitle.contentDescription =
            getString(R.string.reader_empty_saved_posts_content_description)
        actionableEmptyView!!.subtitle.visibility = View.VISIBLE
        actionableEmptyView!!.button.setText(R.string.reader_no_followed_blogs_button_discover)
        actionableEmptyView!!.button.visibility = View.VISIBLE
        actionableEmptyView!!.button.setOnClickListener {
            setCurrentTagFromEmptyViewButton(
                ActionableEmptyViewButtonType.DISCOVER
            )
        }
    }

    private fun shouldShowEmptyViewForSelfHostedCta(): Boolean {
        return isFilterableScreen &&
                !accountStore.hasAccessToken()
                && subFilterViewModel?.getCurrentSubfilterValue() is SiteAll
    }

    private fun setEmptyTitleAndDescriptionForSelfHostedCta() {
        if (!isAdded) {
            return
        }

        actionableEmptyView!!.image.visibility = View.VISIBLE
        actionableEmptyView!!.title.text =
            getString(R.string.reader_empty_subscriptions)
        actionableEmptyView!!.subtitle.visibility = View.GONE
        actionableEmptyView!!.button.setText(
            R.string.reader_no_followed_blogs_button_discover
        )
        actionableEmptyView!!.button.setOnClickListener {
            setCurrentTagFromEmptyViewButton(
                ActionableEmptyViewButtonType.DISCOVER
            )
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

    private fun setEmptyTitleDescriptionAndButton(
        title: String, description: String?,
        button: ActionableEmptyViewButtonType?,
        isImageHidden: Boolean
    ) {
        if (!isAdded) {
            return
        }

        actionableEmptyView!!.image.visibility =
            if (!isUpdating && !isImageHidden) View.VISIBLE else View.GONE
        actionableEmptyView!!.title.text = title

        if (description == null) {
            actionableEmptyView!!.subtitle.visibility = View.GONE
        } else {
            actionableEmptyView!!.subtitle.visibility = View.VISIBLE

            if (description.contains("<") && description.contains(">")) {
                actionableEmptyView!!.subtitle.text = HtmlCompat.fromHtml(
                    description,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
            } else {
                actionableEmptyView!!.subtitle.text = description
            }
        }

        if (button == null) {
            actionableEmptyView!!.button.visibility = View.GONE
        } else {
            actionableEmptyView!!.button.visibility = View.VISIBLE

            when (button) {
                ActionableEmptyViewButtonType.DISCOVER -> {
                    actionableEmptyView!!.button.setText(
                        R.string.reader_no_followed_blogs_button_discover
                    )
                }

                ActionableEmptyViewButtonType.FOLLOWED -> {
                    actionableEmptyView!!.button.setText(R.string.reader_empty_followed_blogs_button_subscriptions)
                }
            }

            actionableEmptyView!!.button.setOnClickListener {
                setCurrentTagFromEmptyViewButton(
                    button
                )
            }
        }
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

    private fun setCurrentTagFromEmptyViewButton(button: ActionableEmptyViewButtonType) {
        var tag: ReaderTag? = when (button) {
            ActionableEmptyViewButtonType.DISCOVER -> ReaderUtils.getTagFromEndpoint(
                ReaderTag.DISCOVER_PATH
            )

            ActionableEmptyViewButtonType.FOLLOWED -> ReaderUtils.getTagFromEndpoint(
                ReaderTag.FOLLOWING_PATH
            )
        }
        if (tag == null) {
            tag = ReaderUtils.getDefaultTag()
        }

        postListViewModel.onEmptyStateButtonTapped(tag!!)
    }

    private fun announceListStateForAccessibility() {
        if (view != null) {
            requireView().announceForAccessibility(
                getString(
                    R.string.reader_acessibility_list_loaded,
                    getPostAdapter().itemCount
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
     * called by post adapter when data has been loaded
     */
    private val mDataLoadedListener: DataLoadedListener = object : DataLoadedListener {
        @Suppress("ComplexCondition")
        override fun onDataLoaded(isEmpty: Boolean) {
            if (!isAdded || (isEmpty && !hasUpdatedPosts)) {
                return
            }
            if (isEmpty) {
                if ((getPostListType() != ReaderPostListType.SEARCH_RESULTS) ||
                    (searchTabsPosition == TAB_SITES && getSiteSearchAdapter().isEmpty) ||
                    (searchTabsPosition == TAB_POSTS && getPostAdapter().isEmpty)
                ) {
                    setEmptyTitleDescriptionAndButton(false)
                    showEmptyView()
                }
            } else {
                hideEmptyView()
                announceListStateForAccessibility()
                if (restorePosition > 0) {
                    AppLog.d(AppLog.T.READER, "reader post list > restoring position")
                    recyclerView.scrollRecycleViewToPosition(restorePosition)
                }
                if (isSearching && !isSearchTabsShowing()) {
                    showSearchTabs()
                } else if (isSearching) {
                    toggleJetpackBannerIfEnabled(showIfEnabled = true, animateOnScroll = true)
                }
            }
            restorePosition = 0
        }
    }

    private val isBookmarksList: Boolean
        get() = getPostListType() == ReaderPostListType.TAG_FOLLOWED
                && (currentReaderTag != null && currentReaderTag!!.isBookmarked)

    private fun getPostAdapter(): ReaderPostAdapter {
        if (readerPostAdapter == null) {
            AppLog.d(
                AppLog.T.READER,
                "reader post list > creating post adapter"
            )
            readerPostAdapter = ReaderPostAdapter(
                WPActivityUtils.getThemedContext(activity),
                getPostListType(),
                imageManager,
                uiHelpers,
                networkUtilsWrapper,
                isTopLevel,
                lifecycleScope
            )

            readerPostAdapter!!.setOnFollowListener(this)
            readerPostAdapter!!.setOnPostSelectedListener(this)
            readerPostAdapter!!.setOnPostListItemButtonListener(this)
            readerPostAdapter!!.setOnDataLoadedListener(mDataLoadedListener)
            readerPostAdapter!!.setOnDataRequestedListener(dataRequestedListener)
            if (activity is OnBlogInfoLoadedListener) {
                readerPostAdapter!!.setOnBlogInfoLoadedListener(activity as OnBlogInfoLoadedListener?)
            }
            if (activity is OnBlogInfoFailedListener) {
                readerPostAdapter!!.setOnBlogInfoFailedListener(activity as OnBlogInfoFailedListener?)
            }
            if (getPostListType().isTagType) {
                readerPostAdapter!!.setCurrentTag(currentTag)
            } else if (getPostListType() == ReaderPostListType.BLOG_PREVIEW) {
                readerPostAdapter!!.setCurrentBlogAndFeed(currentBlogId, currentFeedId)
            } else if (isSearching) {
                val searchTag =
                    ReaderUtils.getTagForSearchQuery(
                        currentSearchQuery ?: ""
                    )
                readerPostAdapter!!.setCurrentTag(searchTag)
            }
        }
        return readerPostAdapter!!
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
                        getPostAdapter().source,
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

    private fun hasPostAdapter(): Boolean {
        return (readerPostAdapter != null)
    }

    private fun isPostAdapterEmpty(): Boolean {
        return readerPostAdapter == null || readerPostAdapter!!.isEmpty
    }

    private fun isCurrentTag(tag: ReaderTag?): Boolean {
        return ReaderTag.isSameTag(tag, currentReaderTag)
    }

    private fun isCurrentTagName(tagName: String?): Boolean {
        return (tagName != null && tagName.equals(currentTagName, ignoreCase = true))
    }

    private var currentTag: ReaderTag?
        get() = currentReaderTag
        private set(tag) {
            if (tag == null) {
                return
            }

            // skip if this is already the current tag and the post adapter is already showing it
            if (isCurrentTag(tag)
                && hasPostAdapter()
                && getPostAdapter().isCurrentTag(tag)
            ) {
                return
            }

            currentReaderTag = tag

            if (isFilterableScreen) {
                if (isFilterableTag(currentReaderTag) || currentReaderTag!!.isDefaultInMemoryTag) {
                    subFilterViewModel!!.onSubfilterReselected()
                } else {
                    changeReaderMode(
                        ReaderModeInfo(
                            tag,
                            ReaderPostListType.TAG_FOLLOWED,
                            0,
                            0,
                            false,
                            null,
                            false,
                            isFiltered
                        ),
                        false
                    )
                }
            }

            val validTag = ReaderUtils.getValidTagForSharedPrefs(
                tag,
                isTopLevel,
                recyclerView,
                ReaderUtils.getDefaultTagFromDbOrCreateInMemory(
                    requireActivity(),
                    tagUpdateClientUtilsProvider
                )
            )

            when (getPostListType()) {
                ReaderPostListType.TAG_FOLLOWED -> {
                    // remember this as the current tag if viewing followed tag
                    AppPrefs.setReaderTag(validTag)
                }

                ReaderPostListType.TAG_PREVIEW -> {
                    tagPreviewHistory.push(tag.tagSlug)
                }

                ReaderPostListType.BLOG_PREVIEW -> {
                    if (isTopLevel) {
                        AppPrefs.setReaderTag(validTag)
                    }
                }

                ReaderPostListType.SEARCH_RESULTS -> {}
                ReaderPostListType.TAGS_FEED -> {}
            }

            getPostAdapter().setCurrentTag(currentReaderTag)
            hideNewPostsBar()
            showLoadingProgress(false)
            updateCurrentTagIfTime()
        }

    private val currentTagName: String
        get() = (if (currentReaderTag != null) currentReaderTag!!.tagSlug else "")

    private fun hasCurrentTag(): Boolean {
        return currentReaderTag != null
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
            searchMenuItem.collapseActionView()
            true
        } else {
            goBackInTagHistory()
        }
    }

    @Suppress("ReturnCount")
    private fun goBackInTagHistory(): Boolean {
        if (tagPreviewHistory.empty()) {
            return false
        }

        var tagName = tagPreviewHistory.pop().orEmpty()
        if (isCurrentTagName(tagName)) {
            if (tagPreviewHistory.empty()) {
                return false
            }
            tagName = tagPreviewHistory.pop().orEmpty()
        }

        val newTag = ReaderUtils.getTagFromTagName(tagName, ReaderTagType.FOLLOWED)
        currentTag = newTag

        return true
    }

    /*
     * refresh adapter so latest posts appear
     */
    private fun refreshPosts() {
        hideNewPostsBar()
        if (hasPostAdapter()) {
            getPostAdapter().refresh()
        }
    }

    /*
     * same as above but clears posts before refreshing
     */
    private fun reloadPosts() {
        hideNewPostsBar()
        if (hasPostAdapter()) {
            getPostAdapter().reload()
        }
    }

    /*
     * reload the list of tags for the dropdown filter
     */
    private fun reloadTags() {
        if (isAdded) {
            recyclerView.refreshFilterCriteriaOptions()
        }
    }

    /*
     * get posts for the current blog from the server
     */
    private fun updatePostsInCurrentBlogOrFeed(updateAction: ReaderPostServiceStarter.UpdateAction) {
        if (!NetworkUtils.isNetworkAvailable(activity)) {
            AppLog.i(
                AppLog.T.READER,
                "reader post list > network unavailable, canceled blog update"
            )
            return
        }
        if (currentFeedId != 0L) {
            ReaderPostServiceStarter.startServiceForFeed(activity, currentFeedId, updateAction)
        } else {
            ReaderPostServiceStarter.startServiceForBlog(activity, currentBlogId, updateAction)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: UpdatePostsStarted) {
        if (!isAdded) {
            return
        }
        // check if the event is related to this instance of the ReaderPostListFragment
        if (event.readerTag != null && !isCurrentTag(event.readerTag)) {
            return
        }

        setIsUpdating(true, event.action)
        setEmptyTitleDescriptionAndButton(false)
        if (isPostAdapterEmpty()) {
            showEmptyView()
        }
    }

    @Suppress("unused", "ReturnCount", "ComplexCondition")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: UpdatePostsEnded) {
        if (!isAdded) {
            return
        }
        // check if the event is related to this instance of the ReaderPostListFragment
        if (event.readerTag != null && !isCurrentTag(event.readerTag)) {
            return
        }
        setIsUpdating(false, event.action)
        hasUpdatedPosts = true

        // don't show new posts if user is searching - posts will automatically
        // appear when search is exited
        if (isSearchViewExpanded || isSearching) {
            return
        }

        // determine whether to show the "new posts" bar - when this is shown, the newly
        // downloaded posts aren't displayed until the user taps the bar - only appears
        // when there are new posts in a followed tag and the user has scrolled the list
        // beyond the first post
        if (event.result == ReaderActions.UpdateResult.HAS_NEW &&
            event.action == ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER &&
            getPostListType() == ReaderPostListType.TAG_FOLLOWED &&
            !isPostAdapterEmpty() &&
            !recyclerView.isFirstItemVisible
        ) {
            showNewPostsBar()
        } else if (event.result.isNewOrChanged
            || event.action == ReaderPostServiceStarter.UpdateAction.REQUEST_REFRESH
        ) {
            refreshPosts()
        } else {
            val requestFailed = (event.result == ReaderActions.UpdateResult.FAILED)
            setEmptyTitleDescriptionAndButton(requestFailed)
            // if we requested posts in order to fill a gap but the request failed or didn't
            // return any posts, reload the adapter so the gap marker is reset (hiding its
            // progress bar)
            if (event.action == ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER_THAN_GAP) {
                reloadPosts()
            }
        }
    }

    /*
     * get latest posts for this tag from the server
     */
    private fun updatePostsWithTag(
        tag: ReaderTag?,
        updateAction: ReaderPostServiceStarter.UpdateAction
    ) {
        if (!isAdded) {
            return
        }

        if (!NetworkUtils.isNetworkAvailable(activity)) {
            AppLog.i(AppLog.T.READER, "reader post list > network unavailable, canceled tag update")
        } else if (tag == null) {
            AppLog.w(AppLog.T.READER, "null tag passed to updatePostsWithTag")
        } else {
            AppLog.d(
                AppLog.T.READER,
                "reader post list > updating tag " + tag.tagNameForLog + ", updateAction=" + updateAction.name
            )
            ReaderPostServiceStarter.startServiceForTag(activity, tag, updateAction)
        }
    }

    private fun updateCurrentTag() {
        updatePostsWithTag(currentTag, ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER)
    }

    /*
     * update the current tag if it's time to do so - note that the check is done in the
     * background since it can be expensive and this is called when the fragment is
     * resumed, which on slower devices can result in a janky experience
     */
    private fun updateCurrentTagIfTime() {
        if (!isAdded || !hasCurrentTag()) {
            return
        }
        object : Thread() {
            override fun run() {
                if (ReaderTagTable.shouldAutoUpdateTag(currentTag) && isAdded) {
                    // Check the fragment is attached right after `shouldAutoUpdateTag`
                    val activity = activity ?: return
                    activity.runOnUiThread { updateCurrentTag() }
                } else {
                    // Check the fragment is attached to the activity when this Thread starts.
                    val activity = activity ?: return
                    activity.runOnUiThread {
                        if (isBookmarksList && isPostAdapterEmpty() && isAdded) {
                            setEmptyTitleAndDescriptionForBookmarksList()
                            showEmptyView()
                        } else if ((currentTag?.isListTopic() == true) && isPostAdapterEmpty() && isAdded) {
                            actionableEmptyView!!.title.text =
                                getString(R.string.reader_empty_blogs_posts_in_custom_list)
                            actionableEmptyView!!.image.visibility = View.VISIBLE
                            actionableEmptyView!!.title.visibility = View.VISIBLE
                            actionableEmptyView!!.button.visibility = View.GONE
                            actionableEmptyView!!.subtitle.visibility = View.GONE
                            showEmptyView()
                        } else if (!isPostAdapterEmpty()) {
                            hideEmptyView()
                        }
                    }
                }
            }
        }.start()
    }

    private val isUpdating: Boolean
        get() = currentUpdateActions.size > 0

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

    private fun clearCurrentUpdateActions() {
        if (!isAdded || !isUpdating) return

        currentUpdateActions.clear()
        updateProgressIndicators()
    }

    private fun setIsUpdating(
        isUpdating: Boolean,
        updateAction: ReaderPostServiceStarter.UpdateAction
    ) {
        if (!isAdded) return
        val isUiUpdateNeeded = if (isUpdating) {
            currentUpdateActions.add(updateAction)
        } else {
            currentUpdateActions.remove(updateAction)
        }

        if (isUiUpdateNeeded) updateProgressIndicators()
    }

    private fun updateProgressIndicators() {
        if (!isUpdating) {
            // when there's no update in progress, hide the bottom and swipe-to-refresh progress bars
            showLoadingProgress(false)
            recyclerView.isRefreshing = false
        } else if (currentUpdateActions.size == 1 && currentUpdateActions.contains(
                ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER
            )
        ) {
            // if only older posts are being updated, show only the bottom progress bar
            showLoadingProgress(true)
            recyclerView.isRefreshing = false
        } else {
            // if anything else is being updated, show only the swipe-to-refresh progress bar
            showLoadingProgress(false)
            recyclerView.isRefreshing = true
        }

        // if swipe-to-refresh isn't active, keep it disabled during an update - this prevents
        // doing a refresh while another update is already in progress
        if (!recyclerView.isRefreshing) {
            recyclerView.setSwipeToRefreshEnabled(!isUpdating && isSwipeToRefreshSupported)
        }
    }

    /*
     * swipe-to-refresh isn't supported for search results since they're really brief snapshots
     * and are unlikely to show new posts due to the way they're sorted
     */
    private val isSwipeToRefreshSupported: Boolean
        get() = getPostListType() != ReaderPostListType.SEARCH_RESULTS

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

        // remove the gap marker if it's showing, since it's no longer valid
        getPostAdapter().removeGapMarker()
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
     * are we showing all posts with a specific tag (followed or previewed), or all
     * posts in a specific blog?
     */
    private fun getPostListType(): ReaderPostListType {
        return readerPostListType ?: ReaderTypes.DEFAULT_POST_LIST_TYPE
    }

    /*
    * called from adapter when user taps a post
    */
    override fun onPostSelected(post: ReaderPost?) {
        if (!isAdded || post == null) {
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
            when (val type = getPostListType()) {
                ReaderPostListType.TAG_FOLLOWED,
                ReaderPostListType.TAG_PREVIEW -> {
                    currentTag?.let { tag ->
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
        if (isBookmarksList) {
            readerTracker.trackBlog(
                AnalyticsTracker.Stat.READER_SAVED_POST_OPENED_FROM_SAVED_POST_LIST,
                post.blogId,
                post.feedId,
                post.isFollowedByCurrentUser,
                getPostAdapter().source
            )
        } else {
            readerTracker.trackBlog(
                AnalyticsTracker.Stat.READER_SAVED_POST_OPENED_FROM_OTHER_POST_LIST,
                post.blogId,
                post.feedId,
                post.isFollowedByCurrentUser,
                getPostAdapter().source
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
                hideNewPostsBar()
            }
        }

    /*
     * called when user selects a tag from the tag toolbar
     */
    private fun onTagChanged(tag: ReaderTag) {
        if (!isAdded || isCurrentTag(tag)) {
            return
        }
        // clear 'post removed from saved posts' undo items
        if (getPostListType() == ReaderPostListType.TAG_FOLLOWED) {
            ReaderPostTable.purgeUnbookmarkedPostsWithBookmarkTag()
        }

        trackTagLoaded(tag)
        AppLog.d(
            AppLog.T.READER,
            String.format(
                Locale.US,
                "reader post list > tag %s displayed",
                tag.tagNameForLog
            )
        )
        currentTag = tag
    }

    /**
     * WARNING: Do not replace the static reader tracker with the corresponding instance reader tracker
     * as this will result into a [NullPointerException] crash on specific scenarios.
     *
     *
     * This is because this method is also being triggered through the static
     * [ReaderPostListFragment.newInstanceForTag] method, which means that the
     * [ReaderPostListFragment.readerTracker] field instance will not be yet available, and
     * as thus cannot be used, or else it will result in a [NullPointerException].
     */
    private fun trackTagLoaded(tag: ReaderTag?) {
        if (tag == null) {
            return
        }
        val stat = if (tag.isFreshlyPressed) {
            AnalyticsTracker.Stat.READER_FRESHLY_PRESSED_LOADED
        } else if (tag.isTagTopic) {
            AnalyticsTracker.Stat.READER_TAG_LOADED
        } else if (tag.isListTopic) {
            AnalyticsTracker.Stat.READER_LIST_LOADED
        } else {
            return
        }

        trackTag(stat, tag.tagSlug)
    }

    @Suppress("LongMethod")
    override fun onButtonClicked(post: ReaderPost, actionType: ReaderPostCardActionType) {
        val source = getPostAdapter().source

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
                ReaderActivityLauncher.openPost(requireActivity(), post)
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

    private fun isFilterableTag(tag: ReaderTag?): Boolean {
        return tag != null && tag.isFilterable
    }

    private enum class ActionableEmptyViewButtonType {
        DISCOVER,
        FOLLOWED
    }

    companion object {
        private const val TAB_POSTS = 0
        private const val TAB_SITES = 1
        private const val NO_POSITION = -1
        private const val RECYCLER_DELAY_MS = 1000L
        private const val BOOKMARK_IMAGE_MULTIPLIER = 1.2
        private const val BOOKMARK_IMAGE_ALPHA = 150
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
            fragment.trackTagLoaded(tag)

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
