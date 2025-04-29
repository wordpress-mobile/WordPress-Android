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
import android.widget.AutoCompleteTextView
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
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
import org.wordpress.android.fluxc.model.SiteModel
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
import org.wordpress.android.ui.main.WPMainActivity
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
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.widgets.AppReviewManager.incrementInteractions
import org.wordpress.android.widgets.RecyclerItemDecoration
import org.wordpress.android.widgets.WPSnackbar.Companion.make
import java.util.EnumSet
import javax.inject.Inject
import androidx.core.view.isVisible

class ReaderPostListFragment : ViewPagerFragment(), OnPostSelectedListener, OnFollowListener,
    OnPostListItemButtonListener, OnActivityBackPressedListener, OnScrollToTopListener {
    private val mTagPreviewHistory = ReaderHistoryStack("tag_preview_history")

    @Inject
    lateinit var mViewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var mAccountStore: AccountStore

    @Inject
    lateinit var mReaderStore: ReaderStore

    @Inject
    lateinit var mDispatcher: Dispatcher

    @Inject
    lateinit var mImageManager: ImageManager

    @Inject
    lateinit var mUiHelpers: UiHelpers

    @Inject
    lateinit var mNetworkUtilsWrapper: NetworkUtilsWrapper

    @Inject
    lateinit var mTagUpdateClientUtilsProvider: TagUpdateClientUtilsProvider

    @Inject
    lateinit var mQuickStartUtilsWrapper: QuickStartUtilsWrapper

    @Inject
    lateinit var mSeenUnseenWithCounterFeatureConfig: SeenUnseenWithCounterFeatureConfig

    @Inject
    lateinit var mJetpackBrandingUtils: JetpackBrandingUtils

    @Inject
    lateinit var mQuickStartRepository: QuickStartRepository

    @Inject
    lateinit var mReaderTracker: ReaderTracker

    @Inject
    lateinit var mSnackbarSequencer: SnackbarSequencer

    @Inject
    lateinit var mDisplayUtilsWrapper: DisplayUtilsWrapper

    private var mPostAdapter: ReaderPostAdapter? = null
    private var mSiteSearchAdapter: ReaderSiteSearchAdapter? = null
    private var mSearchSuggestionAdapter: ReaderSearchSuggestionAdapter? = null
    private var mSearchSuggestionRecyclerAdapter: ReaderSearchSuggestionRecyclerAdapter? = null
    private lateinit var mRecyclerView: FilteredRecyclerView
    private var mFirstLoad = true
    private lateinit var mNewPostsBar: View
    private var mActionableEmptyView: ActionableEmptyView? = null
    private lateinit var mProgress: ProgressBar
    private var mSearchTabs: TabLayout? = null
    private var mSearchView: SearchView? = null
    private lateinit var mSearchMenuItem: MenuItem
    private lateinit var mJetpackBanner: View
    private var mIsTopLevel = false
    private var mBottomNavController: BottomNavController? = null
    private var mCurrentTag: ReaderTag? = null
    private var mTagFragmentStartedWith: ReaderTag? = null
    private var mCurrentBlogId: Long = 0
    private var mCurrentFeedId: Long = 0
    private var mCurrentSearchQuery: String? = null
    private var mPostListType: ReaderPostListType? = null
    private var mLastTappedSiteSearchResult: ReaderSiteModel? = null
    private var mRestorePosition = 0
    private var mSiteSearchRestorePosition = 0
    private var mPostSearchAdapterPos = 0
    private var mSiteSearchAdapterPos = 0
    private var mSearchTabsPos = NO_POSITION
    private var mIsFilterableScreen = false
    private var mIsFiltered = false
    private var mReaderSubsActivityResultLauncher: ActivityResultLauncher<Intent>? = null
    private var mCurrentUpdateActions = HashSet<ReaderPostServiceStarter.UpdateAction>()

    /*
     * called by post adapter to load older posts when user scrolls to the last post
     */
    private val mDataRequestedListener: DataRequestedListener = object : DataRequestedListener {
        override fun onRequestData() {
            // skip if update is already in progress
            if (isUpdating) {
                return
            }

            // request older posts unless we already have the max # to show
            when (getPostListType()) {
                ReaderPostListType.TAG_FOLLOWED, ReaderPostListType.TAG_PREVIEW -> if (ReaderPostTable.getNumPostsWithTag(
                        mCurrentTag
                    )
                    < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY
                ) {
                    // request older posts
                    updatePostsWithTag(
                        currentTag,
                        ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER
                    )
                    mReaderTracker.track(AnalyticsTracker.Stat.READER_INFINITE_SCROLL)
                }

                ReaderPostListType.BLOG_PREVIEW -> {
                    val numPosts = if (mCurrentFeedId != 0L) {
                        ReaderPostTable.getNumPostsInFeed(mCurrentFeedId)
                    } else {
                        ReaderPostTable.getNumPostsInBlog(mCurrentBlogId)
                    }
                    if (numPosts < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY) {
                        updatePostsInCurrentBlogOrFeed(ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER)
                        mReaderTracker.track(AnalyticsTracker.Stat.READER_INFINITE_SCROLL)
                    }
                }

                ReaderPostListType.SEARCH_RESULTS -> {
                    val searchTag = ReaderUtils.getTagForSearchQuery(
                        mCurrentSearchQuery!!
                    )
                    val offset = ReaderPostTable.getNumPostsWithTag(searchTag)
                    if (offset < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY) {
                        updatePostsInCurrentSearch(offset)
                        mReaderTracker.track(AnalyticsTracker.Stat.READER_INFINITE_SCROLL)
                    }
                }

                else -> { // noop }
                }
            }
        }
    }

    private var mWasPaused = false
    private var mHasRequestedPosts = false
    private var mHasUpdatedPosts = false
    private var mIsAnimatingOutNewPostsBar = false
    private var mBookmarksSavedLocallyDialog: AlertDialog? = null
    private var mViewModel: ReaderPostListViewModel? = null

    // This VM is initialized only on the Following tab
    private var mSubFilterViewModel: SubFilterViewModel? = null
    private var mReaderViewModel: ReaderViewModel? = null

    fun getSelectedSite(): SiteModel? {
        return (activity as? WPMainActivity)?.selectedSite
    }

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)

        args?.let { arguments ->
            if (arguments.containsKey(ReaderConstants.ARG_TAG)) {
                mCurrentTag = BundleCompat.getSerializable(arguments, ReaderConstants.ARG_TAG, ReaderTag::class.java)
            }
            if (arguments.containsKey(ReaderConstants.ARG_ORIGINAL_TAG)) {
                mTagFragmentStartedWith =
                    BundleCompat.getSerializable(arguments, ReaderConstants.ARG_ORIGINAL_TAG, ReaderTag::class.java)
            }
            if (arguments.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType =
                    BundleCompat.getSerializable(
                        arguments,
                        ReaderConstants.ARG_POST_LIST_TYPE,
                        ReaderPostListType::class.java
                    )
            }

            if (arguments.containsKey(ReaderConstants.ARG_IS_TOP_LEVEL)) {
                mIsTopLevel = arguments.getBoolean(ReaderConstants.ARG_IS_TOP_LEVEL)
            }
            if (arguments.containsKey(ReaderConstants.ARG_IS_FILTERABLE)) {
                mIsFilterableScreen = arguments.getBoolean(ReaderConstants.ARG_IS_FILTERABLE)
            }

            mCurrentBlogId = arguments.getLong(ReaderConstants.ARG_BLOG_ID)
            mCurrentFeedId = arguments.getLong(ReaderConstants.ARG_FEED_ID)
            mCurrentSearchQuery = arguments.getString(ReaderConstants.ARG_SEARCH_QUERY)

            if (getPostListType() == ReaderPostListType.TAG_PREVIEW && hasCurrentTag()) {
                mTagPreviewHistory.push(currentTagName)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)

        savedInstanceState?.let { state ->
            AppLog.d(AppLog.T.READER, "reader post list > restoring instance state")
            if (state.containsKey(ReaderConstants.ARG_TAG)) {
                mCurrentTag =
                    BundleCompat.getSerializable(state, ReaderConstants.ARG_TAG, ReaderTag::class.java)
            }
            if (state.containsKey(ReaderConstants.ARG_BLOG_ID)) {
                mCurrentBlogId = state.getLong(ReaderConstants.ARG_BLOG_ID)
            }
            if (state.containsKey(ReaderConstants.ARG_FEED_ID)) {
                mCurrentFeedId = state.getLong(ReaderConstants.ARG_FEED_ID)
            }
            if (state.containsKey(ReaderConstants.ARG_SEARCH_QUERY)) {
                mCurrentSearchQuery = state.getString(ReaderConstants.ARG_SEARCH_QUERY)
            }
            if (state.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType =
                    BundleCompat.getSerializable(state, ReaderConstants.ARG_POST_LIST_TYPE, ReaderPostListType::class.java)
            }
            if (getPostListType() == ReaderPostListType.TAG_PREVIEW) {
                mTagPreviewHistory.restoreInstance(state)
            }
            if (state.containsKey(ReaderConstants.ARG_IS_TOP_LEVEL)) {
                mIsTopLevel = state.getBoolean(ReaderConstants.ARG_IS_TOP_LEVEL)
            }
            if (state.containsKey(ReaderConstants.ARG_IS_FILTERABLE)) {
                mIsFilterableScreen =
                    state.getBoolean(ReaderConstants.ARG_IS_FILTERABLE)
            }

            if (state.containsKey(ReaderConstants.ARG_ORIGINAL_TAG)) {
                mTagFragmentStartedWith =
                    BundleCompat.getSerializable(state, ReaderConstants.ARG_ORIGINAL_TAG, ReaderTag::class.java)
            }

            mRestorePosition = state.getInt(ReaderConstants.KEY_RESTORE_POSITION)
            mSiteSearchRestorePosition =
                state.getInt(ReaderConstants.KEY_SITE_SEARCH_RESTORE_POSITION)
            mWasPaused = state.getBoolean(ReaderConstants.KEY_WAS_PAUSED)
            mHasRequestedPosts =
                state.getBoolean(ReaderConstants.KEY_ALREADY_REQUESTED)
            mHasUpdatedPosts = state.getBoolean(ReaderConstants.KEY_ALREADY_UPDATED)
            mFirstLoad = state.getBoolean(ReaderConstants.KEY_FIRST_LOAD)
            mSearchTabsPos =
                state.getInt(ReaderConstants.KEY_ACTIVE_SEARCH_TAB, NO_POSITION)
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mViewModel = ViewModelProvider(this, mViewModelFactory)[ReaderPostListViewModel::class.java]
        if (mIsTopLevel) {
            mReaderViewModel = ViewModelProvider(
                requireParentFragment(),
                mViewModelFactory
            )[ReaderViewModel::class.java]
        }

        if (mIsFilterableScreen) {
            initSubFilterViewModel(savedInstanceState)
        }

        setupObservers()
        mViewModel!!.start(mReaderViewModel)

        if (isFollowingScreen) {
            mSubFilterViewModel!!.onUserComesToReader()
        }

        if (isSearching) {
            mRecyclerView.showAppBarLayout()
            mSearchMenuItem.expandActionView()
            mRecyclerView.setToolbarScrollFlags(0)
        }
    }

    private fun setupObservers() {
        mViewModel!!.navigationEvents.observe(
            viewLifecycleOwner
        ) { event: Event<ReaderNavigationEvents> ->
            event.applyIfNotHandled {
                when (val navTarget = this) {
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
                        ReaderActivityLauncher.showNoSiteToReblog(activity)
                    }

                    is ShowBookmarkedTab -> {
                        ActivityLauncher.viewSavedPostsListInReader(activity)
                    }

                    is ShowBookmarkedSavedOnlyLocallyDialog -> {
                        showBookmarksSavedLocallyDialog(navTarget)
                    }

                    is ShowReportPost -> {
                        ReaderActivityLauncher.openUrl(
                            context,
                            ReaderUtils.getReportPostUrl(navTarget.url),
                            OpenUrlType.INTERNAL
                        )
                    }

                    is ShowReportUser -> {
                        ReaderActivityLauncher.openUrl(
                            context,
                            ReaderUtils.getReportUserUrl(
                                navTarget.url,
                                navTarget.authorId
                            ),
                            OpenUrlType.INTERNAL
                        )
                    }

                    else -> {
                        throw IllegalStateException("Action not supported in ReaderPostListFragment $navTarget")
                    }
                }
            }
        }

        mViewModel!!.snackbarEvents.observe(
            viewLifecycleOwner
        ) { event: Event<SnackbarMessageHolder> ->
            event.applyIfNotHandled {
                showSnackbar(this)
            }
        }

        mViewModel!!.preloadPostEvents.observe(
            viewLifecycleOwner
        ) { event: Event<PreLoadPostContent> ->
            event.applyIfNotHandled {
                addWebViewCachingFragment(this.blogId, this.postId)
            }
        }

        mViewModel!!.refreshPosts.observe(
            viewLifecycleOwner
        ) { event: Event<Unit> ->
            event.applyIfNotHandled {
                refreshPosts()
            }
        }

        mViewModel!!.updateFollowStatus.observe(
            viewLifecycleOwner
        ) { readerData: FollowStatusChanged ->
            setFollowStatusForBlog(readerData)
        }
    }

    private fun toggleJetpackBannerIfEnabled(showIfEnabled: Boolean, animateOnScroll: Boolean) {
        if (!isAdded || view == null || !isSearching) return

        if (mJetpackBrandingUtils.shouldShowJetpackBranding()) {
            if (animateOnScroll) {
                val scrollView = mRecyclerView.internalRecyclerView
                mJetpackBrandingUtils.showJetpackBannerIfScrolledToTop(
                    mJetpackBanner,
                    scrollView
                )
                // Return early since the banner visibility was handled by showJetpackBannerIfScrolledToTop
                return
            }

            if (showIfEnabled && !mDisplayUtilsWrapper.isPhoneLandscape()) {
                showJetpackBanner()
            } else {
                hideJetpackBanner()
            }
        }
    }

    private fun showJetpackBanner() {
        mJetpackBanner.visibility = View.VISIBLE

        // Add bottom margin to search suggestions list and empty view.
        val jetpackBannerHeight = resources.getDimensionPixelSize(R.dimen.jetpack_banner_height)
        (mRecyclerView.searchSuggestionsRecyclerView.layoutParams as MarginLayoutParams).bottomMargin
        (mActionableEmptyView!!.layoutParams as MarginLayoutParams).bottomMargin = jetpackBannerHeight
    }

    private fun hideJetpackBanner() {
        mJetpackBanner.visibility = View.GONE

        // Remove bottom margin from search suggestions list and empty view.
        (mRecyclerView.searchSuggestionsRecyclerView.layoutParams as MarginLayoutParams).bottomMargin =
            0
        (mActionableEmptyView!!.layoutParams as MarginLayoutParams).bottomMargin = 0
    }

    private fun setFollowStatusForBlog(readerData: FollowStatusChanged) {
        if (!hasPostAdapter()) {
            return
        }
        postAdapter.setFollowStatusForBlog(readerData.blogId, readerData.following)
    }

    private fun showSnackbar(holder: SnackbarMessageHolder) {
        if (!isAdded || view == null) return
        mSnackbarSequencer.enqueue(
            SnackbarItem(
                SnackbarItem.Info(
                    snackbarParent!!,
                    holder.message,
                    holder.duration,
                    holder.isImportant
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

    private fun addWebViewCachingFragment(blogId: Long, postId: Long) {
        val tag = blogId.toString() + "" + postId

        if (parentFragmentManager.findFragmentByTag(tag) == null) {
            parentFragmentManager.beginTransaction()
                .add(ReaderPostWebViewCachingFragment.newInstance(blogId, postId), tag)
                .commit()
        }
    }

    private fun initSubFilterViewModel(savedInstanceState: Bundle?) {
        mSubFilterViewModel = getSubFilterViewModelForTag(
            fragment = this,
            tag = mTagFragmentStartedWith!!,
            savedInstanceState = savedInstanceState
        )

        mSubFilterViewModel!!.currentSubFilter.observe(
            viewLifecycleOwner
        ) {
            if (getPostListType() != ReaderPostListType.SEARCH_RESULTS) {
                if (shouldShowEmptyViewForSelfHostedCta()) {
                    setEmptyTitleDescriptionAndButton(false)
                    showEmptyView()
                }
            }
        }

        mSubFilterViewModel!!.readerModeInfo.observe(
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
                (readerModeInfo.tag != null && mCurrentTag != null && (readerModeInfo.tag != mCurrentTag))
                        || (mPostListType != readerModeInfo.listType)
                        || (mCurrentBlogId != readerModeInfo.blogId)
                        || (mCurrentFeedId != readerModeInfo.feedId)
                        || (readerModeInfo.isFirstLoad)

            if (changesDetected && !readerModeInfo.isFirstLoad) {
                trackTagLoaded(readerModeInfo.tag)
            }
        }

        if (onlyOnChanges && !changesDetected) return

        if (readerModeInfo.tag != null) {
            mCurrentTag = readerModeInfo.tag
        }

        mPostListType = readerModeInfo.listType
        mCurrentBlogId = readerModeInfo.blogId
        mCurrentFeedId = readerModeInfo.feedId
        mIsFiltered = readerModeInfo.isFiltered

        resetPostAdapter(mPostListType!!)
        if (readerModeInfo.requestNewerPosts) {
            updatePosts(false)
        }
    }

    override fun onPause() {
        super.onPause()

        if (mBookmarksSavedLocallyDialog != null) {
            mBookmarksSavedLocallyDialog!!.dismiss()
        }
        mWasPaused = true

        mViewModel!!.onFragmentPause(mIsTopLevel, isSearching, mIsFilterableScreen)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        /*
         * This is a workaround for https://github.com/wordpress-mobile/WordPress-Android/issues/11985.
         * The RecyclerView doesn't get redrawn correctly when the adapter finishes its initialization in onStart.
         */
        postAdapter.notifyDataSetChanged()
        if (mWasPaused) {
            AppLog.d(AppLog.T.READER, "reader post list > resumed from paused state")
            mWasPaused = false

            val currentSite: SubfilterListItem.Site?

            if (getPostListType() == ReaderPostListType.TAG_FOLLOWED) {
                resumeFollowedTag()
            } else if ((getSiteIfBlogPreview().also { currentSite = it }) != null) {
                resumeFollowedSite(currentSite!!)
            } else {
                refreshPosts()
            }

            if (mIsTopLevel) {
                // Remove sticky event if not consumed
                EventBus.getDefault().removeStickyEvent(TagAdded::class.java)
            }

            // if the user tapped a site to show site preview, it's possible they also changed the follow
            // status so tell the search adapter to check whether it has the correct follow status
            if (isSearching && mLastTappedSiteSearchResult != null) {
                siteSearchAdapter.checkFollowStatusForSite(mLastTappedSiteSearchResult!!)
                mLastTappedSiteSearchResult = null
            }

            if (isSearching) {
                return
            }
        }

        if (shouldShowEmptyViewForSelfHostedCta()) {
            setEmptyTitleDescriptionAndButton(false)
            showEmptyView()
        }

        mViewModel!!.onFragmentResume(
            mIsTopLevel, isSearching, mIsFilterableScreen,
            if (mIsFilterableScreen) mSubFilterViewModel!!.getCurrentSubfilterValue() else null
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
            mSubFilterViewModel!!.setSubfilterFromTag(newTag)
        } else if (isFollowingScreen && !ReaderTagTable.tagExists(currentTag)) {
            // user just removed a tag which was selected in the subfilter
            mSubFilterViewModel!!.setDefaultSubfilter(false)
        } else {
            // otherwise, refresh posts to make sure any changes are reflected and auto-update
            // posts in the current tag if it's time
            refreshPosts()
            updateCurrentTagIfTime()
        }
    }

    private fun getSiteIfBlogPreview(): SubfilterListItem.Site? {
        if (mIsFilterableScreen && (getPostListType() == ReaderPostListType.BLOG_PREVIEW)) {
            return mSubFilterViewModel!!.getCurrentSubfilterValue() as? SubfilterListItem.Site
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
            if (mIsFilterableScreen) {
                mSubFilterViewModel!!.setDefaultSubfilter(false)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // detect the bottom nav controller when this fragment is hosted in the main activity - this is used to
        // hide the bottom nav when the user searches from the reader
        if (context is BottomNavController) {
            mBottomNavController = context
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
        mReaderSubsActivityResultLauncher = registerForActivityResult(
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
                        mSubFilterViewModel!!.loadSubFilters()
                    }
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        mBottomNavController = null
    }

    override fun onStart() {
        super.onStart()
        mDispatcher.register(this)
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
        mNewPostsBar.clearAnimation()
        mDispatcher.unregister(this)
        EventBus.getDefault().unregister(this)
    }

    /*
     * ensures the adapter is created and posts are updated if they haven't already been
     */
    private fun checkPostAdapter() {
        if (isAdded && mRecyclerView.adapter == null) {
            mRecyclerView.adapter = postAdapter
            refreshPosts()
            if (!mHasRequestedPosts && NetworkUtils.isNetworkAvailable(
                    activity
                )
            ) {
                mHasRequestedPosts = true
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
        mPostListType = postListType
        mPostAdapter = null
        mRecyclerView.adapter = null
        mRecyclerView.adapter = postAdapter
        mRecyclerView.setSwipeToRefreshEnabled(isSwipeToRefreshSupported)
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
            if (isPostAdapterEmpty && (ReaderBlogTable.hasFollowedBlogs() || !mHasUpdatedPosts)) {
                updateCurrentTag()
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: FollowedBlogsFetched) {
        // refresh posts if user is viewing "Followed Sites"
        if (event.didChange()
            && getPostListType() == ReaderPostListType.TAG_FOLLOWED && hasCurrentTag()
            && (currentTag!!.isFollowedSites || currentTag!!.isDefaultInMemoryTag)
        ) {
            refreshPosts()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        AppLog.d(AppLog.T.READER, "reader post list > saving instance state")

        if (mCurrentTag != null) {
            outState.putSerializable(ReaderConstants.ARG_TAG, mCurrentTag)
        }

        if (mTagFragmentStartedWith != null) {
            outState.putSerializable(ReaderConstants.ARG_ORIGINAL_TAG, mTagFragmentStartedWith)
        }

        if (getPostListType() == ReaderPostListType.TAG_PREVIEW) {
            mTagPreviewHistory.saveInstance(outState)
        } else if (isSearching && mSearchView != null && mSearchView!!.query != null) {
            val query = mSearchView!!.query.toString()
            outState.putString(ReaderConstants.ARG_SEARCH_QUERY, query)
        }

        outState.putLong(ReaderConstants.ARG_BLOG_ID, mCurrentBlogId)
        outState.putLong(ReaderConstants.ARG_FEED_ID, mCurrentFeedId)
        outState.putBoolean(ReaderConstants.KEY_WAS_PAUSED, mWasPaused)
        outState.putBoolean(ReaderConstants.KEY_ALREADY_REQUESTED, mHasRequestedPosts)
        outState.putBoolean(ReaderConstants.KEY_ALREADY_UPDATED, mHasUpdatedPosts)
        outState.putBoolean(ReaderConstants.KEY_FIRST_LOAD, mFirstLoad)
        outState.putSerializable(ReaderConstants.KEY_CURRENT_UPDATE_ACTIONS, mCurrentUpdateActions)
        outState.putInt(ReaderConstants.KEY_RESTORE_POSITION, currentPosition)
        outState.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, getPostListType())
        outState.putBoolean(ReaderConstants.ARG_IS_TOP_LEVEL, mIsTopLevel)
        outState.putBoolean(ReaderConstants.ARG_IS_FILTERABLE, mIsFilterableScreen)

        if (isSearchTabsShowing()) {
            val tabPosition = searchTabsPosition
            outState.putInt(ReaderConstants.KEY_ACTIVE_SEARCH_TAB, tabPosition)
            val siteSearchPosition =
                if (tabPosition == TAB_SITES) currentPosition else mSiteSearchAdapterPos
            outState.putInt(ReaderConstants.KEY_SITE_SEARCH_RESTORE_POSITION, siteSearchPosition)
        }

        if (mIsFilterableScreen && mSubFilterViewModel != null) {
            mSubFilterViewModel!!.onSaveInstanceState(outState)
        }

        super.onSaveInstanceState(outState)
    }

    private val currentPosition: Int
        get() {
            return if (hasPostAdapter()) {
                mRecyclerView.currentPosition
            } else {
                -1
            }
        }

    private fun updatePosts(forced: Boolean) {
        if (!isAdded) {
            return
        }

        if (!NetworkUtils.checkConnection(activity)) {
            mRecyclerView.isRefreshing = false
            return
        }

        if (forced) {
            // Update the tags on post refresh since following some sites (like P2) will change followed tags and blogs
            ReaderUpdateServiceStarter.startService(
                context,
                EnumSet.of(UpdateTask.TAGS, UpdateTask.FOLLOWED_BLOGS)
            )
        }

        if (mFirstLoad) {
            // let onResume() take care of this logic, as the FilteredRecyclerView.FilterListener onLoadData
            // method is called on two moments: once for first time load, and then each time the swipe to
            // refresh gesture triggers a refresh.
            mRecyclerView.isRefreshing = false
            mFirstLoad = false
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
            mRecyclerView.isRefreshing = true
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
        mRecyclerView = rootView.findViewById(R.id.reader_recycler_view)

        mActionableEmptyView = rootView.findViewById(R.id.empty_custom_view)

        mRecyclerView.setLogT(AppLog.T.READER)
        mRecyclerView.setCustomEmptyView()
        mRecyclerView.setFilterListener(object : FilteredRecyclerView.FilterListener {
            override fun onLoadFilterCriteriaOptions(refresh: Boolean): List<FilterCriteria> {
                return emptyList()
            }

            override fun onLoadFilterCriteriaOptionsAsync(
                listener: FilterCriteriaAsyncLoaderListener, refresh: Boolean
            ) {
            }

            override fun onLoadData(forced: Boolean) {
                if (forced) {
                    mReaderTracker.track(AnalyticsTracker.Stat.READER_PULL_TO_REFRESH)
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
                        mTagUpdateClientUtilsProvider
                    )

                    val tag = ReaderUtils.getValidTagForSharedPrefs(
                        currentTag!!,
                        mIsTopLevel,
                        mRecyclerView,
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

        mRecyclerView.setBackgroundColor(
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
        mRecyclerView.addItemDecoration(
            RecyclerItemDecoration(
                spacingHorizontal,
                spacingVertical,
                false
            )
        )

        // add a proper item divider to the RecyclerView
        mRecyclerView.addItemDivider(R.drawable.default_list_divider)

        mRecyclerView.setToolbarBackgroundColor(0)
        mRecyclerView.setToolbarSpinnerDrawable(R.drawable.ic_dropdown_primary_30_24dp)

        if (mIsTopLevel) {
            mRecyclerView.setToolbarTitle(
                R.string.reader_screen_title,
                resources.getDimensionPixelSize(R.dimen.margin_extra_large)
            )
        } else {
            mRecyclerView.setToolbarLeftAndRightPadding(
                resources.getDimensionPixelSize(R.dimen.margin_medium),
                resources.getDimensionPixelSize(R.dimen.margin_extra_large)
            )
        }

        // add a menu to the filtered recycler toolbar
        if (mAccountStore.hasAccessToken() && isSearching) {
            setupRecyclerToolbar()
        }

        mRecyclerView.setSwipeToRefreshEnabled(isSwipeToRefreshSupported)

        // bar that appears at top after new posts are loaded
        mNewPostsBar = rootView.findViewById(R.id.layout_new_posts)
        mNewPostsBar.visibility = View.GONE
        mNewPostsBar.setOnClickListener {
            mRecyclerView.scrollRecycleViewToPosition(0)
            refreshPosts()
        }

        // progress bar that appears when loading more posts
        mProgress = rootView.findViewById(R.id.progress_footer)
        mProgress.visibility = View.GONE

        mJetpackBanner = rootView.findViewById(R.id.jetpack_banner)
        if (mJetpackBrandingUtils.shouldShowJetpackBranding()) {
            val screen: JetpackPoweredScreen = JetpackPoweredScreen.WithDynamicText.READER_SEARCH
            mJetpackBrandingUtils.initJetpackBannerAnimation(
                mJetpackBanner,
                mRecyclerView.internalRecyclerView
            )
            val jetpackBannerTextView =
                mJetpackBanner.findViewById<TextView>(R.id.jetpack_banner_text)
            jetpackBannerTextView.text = mUiHelpers.getTextOfUiString(
                requireContext(),
                mJetpackBrandingUtils.getBrandingTextForScreen(screen)
            )

            if (mJetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) {
                mJetpackBanner.setOnClickListener {
                    mJetpackBrandingUtils.trackBannerTapped(screen)
                    JetpackPoweredBottomSheetFragment()
                        .show(childFragmentManager, JetpackPoweredBottomSheetFragment.TAG)
                }
            }
        }

        if (savedInstanceState?.containsKey(ReaderConstants.KEY_CURRENT_UPDATE_ACTIONS) == true) {
            val actions =
                BundleCompat.getSerializable(savedInstanceState, ReaderConstants.KEY_CURRENT_UPDATE_ACTIONS, HashSet::class.java)
            if (actions is HashSet<*>) {
                @Suppress("UNCHECKED_CAST")
                mCurrentUpdateActions = actions as HashSet<ReaderPostServiceStarter.UpdateAction>
                updateProgressIndicators()
            }
        }

        return rootView
    }

    /*
     * adds a menu to the recycler toolbar containing search items - only called
     * for followed tags
     */
    private fun setupRecyclerToolbar() {
        val menu = mRecyclerView.addToolbarMenu(R.menu.reader_list)
        mSearchMenuItem = menu.findItem(R.id.menu_reader_search)

        mSearchView = mSearchMenuItem.actionView as SearchView?
        mSearchView!!.queryHint = getString(R.string.reader_hint_post_search)
        mSearchView!!.isSubmitButtonEnabled = false
        mSearchView!!.setIconifiedByDefault(true)
        mSearchView!!.isIconified = true

        // force the search view to take up as much horizontal space as possible (without this
        // it looks truncated on landscape)
        val maxWidth = DisplayUtils.getWindowPixelWidth(requireActivity())
        mSearchView!!.maxWidth = maxWidth

        // this is hacky, but we want to change the SearchView's autocomplete to show suggestions
        // after a single character is typed, and there's no less hacky way to do this...
        val view =
            mSearchView!!.findViewById<View>(com.google.android.material.R.id.search_src_text)
        if (view is AutoCompleteTextView) {
            view.threshold = 1
        }

        mSearchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                if (getPostListType() != ReaderPostListType.SEARCH_RESULTS) {
                    mReaderTracker.track(AnalyticsTracker.Stat.READER_SEARCH_LOADED)
                }
                resetPostAdapter(ReaderPostListType.SEARCH_RESULTS)
                populateSearchSuggestions(null)
                showSearchMessageOrSuggestions()
                // hide the bottom navigation when search is active
                if (mBottomNavController != null) {
                    mBottomNavController!!.onRequestHideBottomNavigation()
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

        mSearchView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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
        val hasPerformedSearch = !TextUtils.isEmpty(mCurrentSearchQuery)

        toggleJetpackBannerIfEnabled(showIfEnabled = true, animateOnScroll = false)

        // prevents suggestions from being shown after the search view has been collapsed
        if (!isSearching) {
            return
        }

        // prevents suggestions from being shown above search results after configuration changes
        if (mWasPaused && hasPerformedSearch) {
            return
        }

        if (!hasQuery || !hasPerformedSearch) {
            // clear posts and sites so only the suggestions or the empty view are visible
            postAdapter.clear()
            siteSearchAdapter.clear()

            hideSearchTabs()

            // clears the last performed query
            mCurrentSearchQuery = null

            val hasSuggestions =
                mSearchSuggestionRecyclerAdapter != null && mSearchSuggestionRecyclerAdapter!!.itemCount > 0

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
        ReaderSearchServiceStarter.startService(activity, mCurrentSearchQuery!!, offset)
    }

    /*
     * start a search for reader sites matching the current search query
     */
    private fun updateSitesInCurrentSearch(offset: Int) {
        if (searchTabsPosition == TAB_SITES) {
            if (offset == 0) {
                mRecyclerView.isRefreshing = true
            } else {
                showLoadingProgress(true)
            }
        }
        val payload = ReaderSearchSitesPayload(
            mCurrentSearchQuery!!,
            ReaderConstants.READER_MAX_SEARCH_RESULTS_TO_REQUEST,
            offset,
            false
        )
        mDispatcher.dispatch(ReaderActionBuilder.newReaderSearchSitesAction(payload))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            if (mCurrentSearchQuery != null) {
                submitSearchQuery(mCurrentSearchQuery!!)
            }
        }
    }

    private fun submitSearchQuery(query: String) {
        if (!isAdded) {
            return
        }

        mSearchView!!.clearFocus() // this will hide suggestions and the virtual keyboard
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

        mPostAdapter!!.setCurrentTag(searchTag)
        mCurrentSearchQuery = trimQuery
        updatePostsInCurrentSearch(0)
        updateSitesInCurrentSearch(0)

        toggleJetpackBannerIfEnabled(showIfEnabled = false, animateOnScroll = false)

        // track that the user performed a search
        if (trimQuery != "") {
            mReaderTracker.trackQuery(AnalyticsTracker.Stat.READER_SEARCH_PERFORMED, trimQuery)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReaderSitesSearched(event: OnReaderSitesSearched) {
        if (!isAdded || getPostListType() != ReaderPostListType.SEARCH_RESULTS) {
            return
        }

        if (!isUpdating) {
            mRecyclerView.isRefreshing = false
        }
        showLoadingProgress(false)

        val adapter = siteSearchAdapter
        if (event.isError) {
            adapter.clear()
        } else if (StringUtils.equals(event.searchTerm, mCurrentSearchQuery)) {
            adapter.setCanLoadMore(event.canLoadMore)
            if (event.offset == 0) {
                adapter.setSiteList(event.sites)
            } else {
                adapter.addSiteList(event.sites)
            }
            if (mSiteSearchRestorePosition > 0) {
                mRecyclerView.scrollRecycleViewToPosition(mSiteSearchRestorePosition)
            }
        }

        if (searchTabsPosition == TAB_SITES && adapter.isEmpty) {
            setEmptyTitleDescriptionAndButton(event.isError)
            showEmptyView()
        }

        mSiteSearchRestorePosition = 0
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
        mRecyclerView.showSearchSuggestions()
    }

    private fun hideSearchSuggestions() {
        mRecyclerView.hideSearchSuggestions()
    }

    /*
     * create the TabLayout that separates search results between POSTS and SITES and places it below
     * the FilteredRecyclerView's toolbar
     */
    private fun createSearchTabs() {
        if (mSearchTabs == null) {
            val rootView = requireView().findViewById<ViewGroup>(android.R.id.content)
            val inflater = LayoutInflater.from(activity)
            mSearchTabs = inflater.inflate(R.layout.reader_search_tabs, rootView) as TabLayout
            mSearchTabs!!.visibility = View.GONE
            mRecyclerView.appBarLayout.addView(mSearchTabs)
        }
    }

    private fun isSearchTabsShowing() = mSearchTabs?.isVisible ?: false

    private fun showSearchTabs() {
        if (!isAdded) {
            return
        }
        if (mSearchTabs == null) {
            createSearchTabs()
        }
        if (mSearchTabs!!.visibility != View.VISIBLE) {
            mSearchTabs!!.visibility = View.VISIBLE

            mPostSearchAdapterPos = 0
            mSiteSearchAdapterPos = 0

            mSearchTabs!!.addOnTabSelectedListener(object : OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    if (tab.position == TAB_POSTS) {
                        mRecyclerView.adapter = postAdapter
                        if (mPostSearchAdapterPos > 0) {
                            mRecyclerView.scrollRecycleViewToPosition(mPostSearchAdapterPos)
                        }
                        if (postAdapter.isEmpty) {
                            setEmptyTitleDescriptionAndButton(false)
                            showEmptyView()
                        } else {
                            hideEmptyView()
                        }
                    } else if (tab.position == TAB_SITES) {
                        mRecyclerView.adapter = siteSearchAdapter
                        if (mSiteSearchAdapterPos > 0) {
                            mRecyclerView.scrollRecycleViewToPosition(mSiteSearchAdapterPos)
                        }
                        if (siteSearchAdapter.isEmpty) {
                            setEmptyTitleDescriptionAndButton(false)
                            showEmptyView()
                        } else {
                            hideEmptyView()
                        }
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    if (tab.position == TAB_POSTS) {
                        mPostSearchAdapterPos = mRecyclerView.currentPosition
                    } else if (tab.position == TAB_SITES) {
                        mSiteSearchAdapterPos = mRecyclerView.currentPosition
                    }
                }

                override fun onTabReselected(tab: TabLayout.Tab) {
                    mRecyclerView.smoothScrollToPosition(0)
                }
            })

            if (mSearchTabsPos != NO_POSITION && mSearchTabsPos != mSearchTabs!!.selectedTabPosition) {
                val tab = mSearchTabs!!.getTabAt(mSearchTabsPos)
                tab?.select()
                mSearchTabsPos = NO_POSITION
            }
        }
    }

    private fun hideSearchTabs() {
        if (isAdded && mSearchTabs != null && mSearchTabs!!.isVisible) {
            mSearchTabs!!.visibility = View.GONE
            mSearchTabs!!.clearOnTabSelectedListeners()
            if (mSearchTabs!!.selectedTabPosition != TAB_POSTS) {
                mSearchTabs!!.getTabAt(TAB_POSTS)!!.select()
            }
            mRecyclerView.adapter = postAdapter
            mLastTappedSiteSearchResult = null
            showLoadingProgress(false)
        }
    }

    private val searchTabsPosition: Int
        get() = if (isSearchTabsShowing()) mSearchTabs!!.selectedTabPosition else -1

    private fun populateSearchSuggestions(query: String?) {
        populateSearchSuggestionAdapter(query)
        populateSearchSuggestionRecyclerAdapter(null) // always passing null as there's no need to filter
    }

    /*
     * create and assign the suggestion adapter for the search view
     */
    private fun createSearchSuggestionAdapter() {
        mSearchSuggestionAdapter = ReaderSearchSuggestionAdapter(activity)
        mSearchView!!.suggestionsAdapter = mSearchSuggestionAdapter

        mSearchView!!.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                return false
            }

            override fun onSuggestionClick(position: Int): Boolean {
                val query = mSearchSuggestionAdapter!!.getSuggestion(position)
                onSearchSuggestionClicked(query)
                return true
            }
        })

        mSearchSuggestionAdapter!!.setOnSuggestionDeleteClickListener { query: String? ->
            this.onSearchSuggestionDeleteClicked(
                query!!
            )
        }
        mSearchSuggestionAdapter!!.setOnSuggestionClearClickListener { this.onSearchSuggestionClearClicked() }
    }

    private fun populateSearchSuggestionAdapter(query: String?) {
        if (mSearchSuggestionAdapter == null) {
            createSearchSuggestionAdapter()
        }
        mSearchSuggestionAdapter!!.setFilter(query)
    }

    private fun createSearchSuggestionRecyclerAdapter() {
        mSearchSuggestionRecyclerAdapter = ReaderSearchSuggestionRecyclerAdapter()
        mRecyclerView.setSearchSuggestionAdapter(mSearchSuggestionRecyclerAdapter)

        mSearchSuggestionRecyclerAdapter!!.setOnSuggestionClickListener { query: String? ->
            onSearchSuggestionClicked(
                query
            )
        }
        mSearchSuggestionRecyclerAdapter!!.setOnSuggestionDeleteClickListener { query: String? ->
            onSearchSuggestionDeleteClicked(
                query!!
            )
        }
        mSearchSuggestionRecyclerAdapter!!.setOnSuggestionClearClickListener { onSearchSuggestionClearClicked() }
    }

    @Suppress("SameParameterValue")
    private fun populateSearchSuggestionRecyclerAdapter(query: String?) {
        if (mSearchSuggestionRecyclerAdapter == null) {
            createSearchSuggestionRecyclerAdapter()
        }
        mSearchSuggestionRecyclerAdapter!!.setQuery(query)
    }

    private fun onSearchSuggestionClicked(query: String?) {
        if (!TextUtils.isEmpty(query)) {
            mSearchView!!.setQuery(query, true)
        }
    }

    private fun onSearchSuggestionDeleteClicked(query: String) {
        ReaderSearchTable.deleteQueryString(query)

        mSearchSuggestionAdapter!!.reload()
        mSearchSuggestionRecyclerAdapter!!.reload()

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
        mReaderTracker.track(AnalyticsTracker.Stat.READER_SEARCH_HISTORY_CLEARED)
        ReaderSearchTable.deleteAllQueries()

        mSearchSuggestionAdapter!!.swapCursor(null)
        mSearchSuggestionRecyclerAdapter!!.swapCursor(null)

        showSearchMessageOrSuggestions()
    }

    private val isSearchViewExpanded: Boolean
        /*
             * is the search input showing?
             */
        get() = mSearchView != null && !mSearchView!!.isIconified

    private val isSearchViewEmpty: Boolean
        get() = mSearchView != null && mSearchView!!.query.isEmpty()

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: SearchPostsStarted) {
        if (!isAdded || getPostListType() != ReaderPostListType.SEARCH_RESULTS) {
            return
        }

        val updateAction =
            if (event.offset == 0) ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER else ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER
        setIsUpdating(true, updateAction)
        setEmptyTitleDescriptionAndButton(false)
        if (isPostAdapterEmpty) showEmptyView()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: SearchPostsEnded) {
        if (!isAdded || getPostListType() != ReaderPostListType.SEARCH_RESULTS) {
            return
        }

        val updateAction =
            if (event.offset == 0) ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER else ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER
        setIsUpdating(false, updateAction)

        // load the results if the search succeeded and it's the current search - note that success
        // means the search didn't fail, not necessarily that is has results - which is fine because
        // if there aren't results then refreshing will show the empty message
        if (event.didSucceed() && isSearching && event.query == mCurrentSearchQuery) {
            refreshPosts()
            showSearchTabs()
        } else {
            hideSearchTabs()
        }
    }

    private val snackbarParent: View?
        /*
             * returns the parent view for snackbars - if this fragment is hosted in the main activity we want the
             * parent to be the main activity's CoordinatorLayout
             */
        get() {
            val coordinator =
                requireActivity().findViewById<View>(R.id.coordinator_layout)
            if (coordinator != null) {
                return coordinator
            }
            return view
        }

    private fun setEmptyTitleDescriptionAndButton(requestFailed: Boolean) {
        if (!isAdded) {
            return
        }

        val heightToolbar = requireActivity().resources.getDimensionPixelSize(R.dimen.toolbar_height)
        val heightTabs = requireActivity().resources.getDimensionPixelSize(R.dimen.tab_height)
        mActionableEmptyView!!.updateLayoutForSearch(false, 0)
        mActionableEmptyView!!.subtitle.contentDescription = null
        var isImageHidden = false
        val title: String
        var description: String? = null
        var button: ActionableEmptyViewButtonType? = null

        // Ensure the default image is reset for empty views before applying logic
        mActionableEmptyView!!.image.setImageResource(R.drawable.illustration_reader_empty)

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
                ReaderPostListType.TAG_FOLLOWED -> if (currentTag!!.isFollowedSites || currentTag!!.isDefaultInMemoryTag) {
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

                ReaderPostListType.BLOG_PREVIEW -> title =
                    getString(R.string.reader_no_posts_in_blog)

                ReaderPostListType.SEARCH_RESULTS -> {
                    isImageHidden = true

                    if (isSearchViewEmpty || TextUtils.isEmpty(mCurrentSearchQuery)) {
                        title = getString(R.string.reader_label_post_search_explainer)
                        mActionableEmptyView!!.updateLayoutForSearch(true, heightToolbar)
                    } else if (isUpdating) {
                        title = ""
                        mActionableEmptyView!!.updateLayoutForSearch(true, heightToolbar)
                    } else {
                        title = getString(R.string.reader_empty_search_title)
                        val formattedQuery = "<em>$mCurrentSearchQuery</em>"
                        description = String.format(
                            getString(R.string.reader_empty_search_description),
                            formattedQuery
                        )
                        mActionableEmptyView!!.updateLayoutForSearch(
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
        mActionableEmptyView!!.image.visibility = View.VISIBLE
        mActionableEmptyView!!.title.setText(R.string.reader_empty_saved_posts_title)
        mActionableEmptyView!!.subtitle.text = ssb
        mActionableEmptyView!!.subtitle.contentDescription =
            getString(R.string.reader_empty_saved_posts_content_description)
        mActionableEmptyView!!.subtitle.visibility = View.VISIBLE
        mActionableEmptyView!!.button.setText(R.string.reader_empty_followed_blogs_button_subscriptions)
        mActionableEmptyView!!.button.visibility = View.VISIBLE
        mActionableEmptyView!!.button.setOnClickListener {
            setCurrentTagFromEmptyViewButton(
                ActionableEmptyViewButtonType.FOLLOWED
            )
        }
    }

    private fun shouldShowEmptyViewForSelfHostedCta(): Boolean {
        return mIsFilterableScreen &&
                !mAccountStore.hasAccessToken()
                && mSubFilterViewModel?.getCurrentSubfilterValue() is SiteAll
    }

    private fun setEmptyTitleAndDescriptionForSelfHostedCta() {
        if (!isAdded) {
            return
        }

        mActionableEmptyView!!.image.visibility = View.VISIBLE
        mActionableEmptyView!!.title.text =
            getString(R.string.reader_self_hosted_select_filter)
        mActionableEmptyView!!.subtitle.visibility = View.GONE
        mActionableEmptyView!!.button.visibility = View.GONE
    }

    private fun addBookmarkImageSpan(ssb: SpannableStringBuilder, imagePlaceholderPosition: Int) {
        val d = ContextCompat.getDrawable(
            requireContext(),
            R.drawable.ic_bookmark_grey_dark_18dp
        )
        d!!.setBounds(0, 0, (d.intrinsicWidth * 1.2).toInt(), (d.intrinsicHeight * 1.2).toInt())
        ssb.setSpan(
            ImageSpan(d), imagePlaceholderPosition, imagePlaceholderPosition + 2,
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

        mActionableEmptyView!!.image.visibility =
            if (!isUpdating && !isImageHidden) View.VISIBLE else View.GONE
        mActionableEmptyView!!.title.text = title

        if (description == null) {
            mActionableEmptyView!!.subtitle.visibility = View.GONE
        } else {
            mActionableEmptyView!!.subtitle.visibility = View.VISIBLE

            if (description.contains("<") && description.contains(">")) {
                mActionableEmptyView!!.subtitle.text = HtmlCompat.fromHtml(
                    description,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
            } else {
                mActionableEmptyView!!.subtitle.text = description
            }
        }

        if (button == null) {
            mActionableEmptyView!!.button.visibility = View.GONE
        } else {
            mActionableEmptyView!!.button.visibility = View.VISIBLE

            when (button) {
                ActionableEmptyViewButtonType.DISCOVER -> mActionableEmptyView!!.button.setText(
                    R.string.reader_no_followed_blogs_button_discover
                )

                ActionableEmptyViewButtonType.FOLLOWED -> mActionableEmptyView!!.button.setText(R.string.reader_empty_followed_blogs_button_subscriptions)
            }

            mActionableEmptyView!!.button.setOnClickListener {
                setCurrentTagFromEmptyViewButton(
                    button
                )
            }
        }
    }

    private fun showEmptyView() {
        if (isAdded) {
            mActionableEmptyView!!.visibility = View.VISIBLE
            mActionableEmptyView!!.announceEmptyStateForAccessibility()
        }
    }

    private fun hideEmptyView() {
        if (isAdded) {
            mActionableEmptyView!!.visibility = View.GONE
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

        mViewModel!!.onEmptyStateButtonTapped(tag!!)
    }

    private fun announceListStateForAccessibility() {
        if (view != null) {
            requireView().announceForAccessibility(
                getString(
                    R.string.reader_acessibility_list_loaded,
                    postAdapter.itemCount
                )
            )
        }
    }

    private fun showBookmarksSavedLocallyDialog(holder: ShowBookmarkedSavedOnlyLocallyDialog) {
        mBookmarksSavedLocallyDialog = MaterialAlertDialogBuilder(requireActivity())
            .setTitle(getString(holder.title))
            .setMessage(getString(holder.message))
            .setPositiveButton(
                holder.buttonLabel
            ) { _: DialogInterface?, _: Int -> holder.okButtonAction.invoke() }
            .setCancelable(false)
            .create()
        mBookmarksSavedLocallyDialog!!.show()
    }

    /*
     * called by post adapter when data has been loaded
     */
    private val mDataLoadedListener: DataLoadedListener = object : DataLoadedListener {
        override fun onDataLoaded(isEmpty: Boolean) {
            if (!isAdded || (isEmpty && !mHasUpdatedPosts)) {
                return
            }
            if (isEmpty) {
                if ((getPostListType() != ReaderPostListType.SEARCH_RESULTS) ||
                    (searchTabsPosition == TAB_SITES && siteSearchAdapter.isEmpty) ||
                    (searchTabsPosition == TAB_POSTS && postAdapter.isEmpty)
                ) {
                    setEmptyTitleDescriptionAndButton(false)
                    showEmptyView()
                }
            } else {
                hideEmptyView()
                announceListStateForAccessibility()
                if (mRestorePosition > 0) {
                    AppLog.d(AppLog.T.READER, "reader post list > restoring position")
                    mRecyclerView.scrollRecycleViewToPosition(mRestorePosition)
                }
                if (isSearching && !isSearchTabsShowing()) {
                    showSearchTabs()
                } else if (isSearching) {
                    toggleJetpackBannerIfEnabled(showIfEnabled = true, animateOnScroll = true)
                }
            }
            mRestorePosition = 0
        }
    }

    private val isBookmarksList: Boolean
        get() = getPostListType() == ReaderPostListType.TAG_FOLLOWED
                && (mCurrentTag != null && mCurrentTag!!.isBookmarked)

    private val postAdapter: ReaderPostAdapter
        get() {
            if (mPostAdapter == null) {
                AppLog.d(
                    AppLog.T.READER,
                    "reader post list > creating post adapter"
                )
                val context =
                    WPActivityUtils.getThemedContext(activity)
                mPostAdapter = ReaderPostAdapter(
                    context,
                    getPostListType(),
                    mImageManager,
                    mUiHelpers,
                    mNetworkUtilsWrapper,
                    mIsTopLevel,
                    this.lifecycleScope
                )
                mPostAdapter!!.setOnFollowListener(this)
                mPostAdapter!!.setOnPostSelectedListener(this)
                mPostAdapter!!.setOnPostListItemButtonListener(this)
                mPostAdapter!!.setOnDataLoadedListener(mDataLoadedListener)
                mPostAdapter!!.setOnDataRequestedListener(mDataRequestedListener)
                if (activity is OnBlogInfoLoadedListener) {
                    mPostAdapter!!.setOnBlogInfoLoadedListener(activity as OnBlogInfoLoadedListener?)
                }
                if (getPostListType().isTagType) {
                    mPostAdapter!!.setCurrentTag(currentTag)
                } else if (getPostListType() == ReaderPostListType.BLOG_PREVIEW) {
                    mPostAdapter!!.setCurrentBlogAndFeed(mCurrentBlogId, mCurrentFeedId)
                } else if (isSearching) {
                    val searchTag =
                        ReaderUtils.getTagForSearchQuery(
                            mCurrentSearchQuery!!
                        )
                    mPostAdapter!!.setCurrentTag(searchTag)
                }
            }
            return mPostAdapter!!
        }

    private val siteSearchAdapter: ReaderSiteSearchAdapter
        get() {
            if (mSiteSearchAdapter == null) {
                mSiteSearchAdapter = ReaderSiteSearchAdapter(object : SiteSearchAdapterListener {
                    override fun onSiteClicked(site: ReaderSiteModel) {
                        mLastTappedSiteSearchResult = site
                        ReaderActivityLauncher.showReaderBlogOrFeedPreview(
                            activity,
                            site.siteId,
                            site.feedId,
                            site.isFollowing,
                            mPostAdapter!!.source,
                            mReaderTracker
                        )
                    }

                    override fun onLoadMore(offset: Int) {
                        showLoadingProgress(true)
                        updateSitesInCurrentSearch(offset)
                    }
                })
            }
            return mSiteSearchAdapter!!
        }

    private fun hasPostAdapter(): Boolean {
        return (mPostAdapter != null)
    }

    private val isPostAdapterEmpty: Boolean
        get() = (mPostAdapter == null || mPostAdapter!!.isEmpty)

    private fun isCurrentTag(tag: ReaderTag?): Boolean {
        return ReaderTag.isSameTag(tag, mCurrentTag)
    }

    private fun isCurrentTagName(tagName: String?): Boolean {
        return (tagName != null && tagName.equals(currentTagName, ignoreCase = true))
    }

    private var currentTag: ReaderTag?
        get() = mCurrentTag
        private set(tag) {
            if (tag == null) {
                return
            }

            // skip if this is already the current tag and the post adapter is already showing it
            if (isCurrentTag(tag)
                && hasPostAdapter()
                && postAdapter.isCurrentTag(tag)
            ) {
                return
            }

            mCurrentTag = tag

            if (mIsFilterableScreen) {
                if (isFilterableTag(mCurrentTag) || mCurrentTag!!.isDefaultInMemoryTag) {
                    mSubFilterViewModel!!.onSubfilterReselected()
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
                            mIsFiltered
                        ),
                        false
                    )
                }
            }

            val validTag = ReaderUtils.getValidTagForSharedPrefs(
                tag,
                mIsTopLevel,
                mRecyclerView,
                ReaderUtils.getDefaultTagFromDbOrCreateInMemory(
                    requireActivity(),
                    mTagUpdateClientUtilsProvider
                )
            )

            when (getPostListType()) {
                ReaderPostListType.TAG_FOLLOWED ->                 // remember this as the current tag if viewing followed tag
                    AppPrefs.setReaderTag(validTag)

                ReaderPostListType.TAG_PREVIEW -> mTagPreviewHistory.push(tag.tagSlug)
                ReaderPostListType.BLOG_PREVIEW -> if (mIsTopLevel) {
                    AppPrefs.setReaderTag(validTag)
                }

                ReaderPostListType.SEARCH_RESULTS -> {}
                ReaderPostListType.TAGS_FEED -> {}
            }

            postAdapter.setCurrentTag(mCurrentTag)
            hideNewPostsBar()
            showLoadingProgress(false)
            updateCurrentTagIfTime()
        }

    private val currentTagName: String
        get() = (if (mCurrentTag != null) mCurrentTag!!.tagSlug else "")

    private fun hasCurrentTag(): Boolean {
        return mCurrentTag != null
    }

    override fun getScrollableViewForUniqueIdProvision(): View? {
        return mRecyclerView.internalRecyclerView
    }

    /*
     * called by the activity when user hits the back button - returns true if the back button
     * is handled here and should be ignored by the activity
     */
    override fun onActivityBackPressed(): Boolean {
        if (isSearchViewExpanded) {
            mSearchMenuItem.collapseActionView()
            return true
        } else {
            return goBackInTagHistory()
        }
    }

    /*
     * when previewing posts with a specific tag, a history of previewed tags is retained so
     * the user can navigate back through them - this is faster and requires less memory
     * than creating a new fragment for each previewed tag
     */
    private fun goBackInTagHistory(): Boolean {
        if (mTagPreviewHistory.empty()) {
            return false
        }

        var tagName = mTagPreviewHistory.pop()
        if (isCurrentTagName(tagName)) {
            if (mTagPreviewHistory.empty()) {
                return false
            }
            tagName = mTagPreviewHistory.pop()
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
            postAdapter.refresh()
        }
    }

    /*
     * same as above but clears posts before refreshing
     */
    private fun reloadPosts() {
        hideNewPostsBar()
        if (hasPostAdapter()) {
            postAdapter.reload()
        }
    }

    /*
     * reload the list of tags for the dropdown filter
     */
    private fun reloadTags() {
        if (isAdded) {
            mRecyclerView.refreshFilterCriteriaOptions()
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
        if (mCurrentFeedId != 0L) {
            ReaderPostServiceStarter.startServiceForFeed(activity, mCurrentFeedId, updateAction)
        } else {
            ReaderPostServiceStarter.startServiceForBlog(activity, mCurrentBlogId, updateAction)
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
        if (isPostAdapterEmpty) showEmptyView()
    }

    @Suppress("unused")
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
        mHasUpdatedPosts = true

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
            !isPostAdapterEmpty &&
            (!isAdded || !mRecyclerView.isFirstItemVisible)
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
            return
        }
        if (tag == null) {
            AppLog.w(AppLog.T.READER, "null tag passed to updatePostsWithTag")
            return
        }
        AppLog.d(
            AppLog.T.READER,
            "reader post list > updating tag " + tag.tagNameForLog + ", updateAction=" + updateAction.name
        )
        ReaderPostServiceStarter.startServiceForTag(activity, tag, updateAction)
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
                        if (isBookmarksList && isPostAdapterEmpty && isAdded) {
                            setEmptyTitleAndDescriptionForBookmarksList()
                            mActionableEmptyView!!.image.setImageResource(
                                R.drawable.illustration_reader_empty
                            )
                            showEmptyView()
                        } else if ((currentTag?.isListTopic() == true) && isPostAdapterEmpty && isAdded) {
                            mActionableEmptyView!!.image.setImageResource(
                                R.drawable.illustration_reader_empty
                            )
                            mActionableEmptyView!!.title.text =
                                getString(R.string.reader_empty_blogs_posts_in_custom_list)
                            mActionableEmptyView!!.image.visibility = View.VISIBLE
                            mActionableEmptyView!!.title.visibility = View.VISIBLE
                            mActionableEmptyView!!.button.visibility = View.GONE
                            mActionableEmptyView!!.subtitle.visibility = View.GONE
                            showEmptyView()
                        } else if (!isPostAdapterEmpty) {
                            hideEmptyView()
                        }
                    }
                }
            }
        }.start()
    }

    private val isUpdating: Boolean
        get() = mCurrentUpdateActions.size > 0

    /*
    * show/hide progress bar which appears at the bottom of the activity when loading more posts
    */
    private fun showLoadingProgress(showProgress: Boolean) {
        if (isAdded) {
            if (showProgress) {
                mProgress.bringToFront()
                mProgress.visibility = View.VISIBLE
            } else {
                mProgress.visibility = View.GONE
            }
        }
    }

    private fun clearCurrentUpdateActions() {
        if (!isAdded || !isUpdating) return

        mCurrentUpdateActions.clear()
        updateProgressIndicators()
    }

    private fun setIsUpdating(
        isUpdating: Boolean,
        updateAction: ReaderPostServiceStarter.UpdateAction
    ) {
        if (!isAdded) return
        val isUiUpdateNeeded = if (isUpdating) {
            mCurrentUpdateActions.add(updateAction)
        } else {
            mCurrentUpdateActions.remove(updateAction)
        }

        if (isUiUpdateNeeded) updateProgressIndicators()
    }

    private fun updateProgressIndicators() {
        if (!isUpdating) {
            // when there's no update in progress, hide the bottom and swipe-to-refresh progress bars
            showLoadingProgress(false)
            mRecyclerView.isRefreshing = false
        } else if (mCurrentUpdateActions.size == 1 && mCurrentUpdateActions.contains(
                ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER
            )
        ) {
            // if only older posts are being updated, show only the bottom progress bar
            showLoadingProgress(true)
            mRecyclerView.isRefreshing = false
        } else {
            // if anything else is being updated, show only the swipe-to-refresh progress bar
            showLoadingProgress(false)
            mRecyclerView.isRefreshing = true
        }

        // if swipe-to-refresh isn't active, keep it disabled during an update - this prevents
        // doing a refresh while another update is already in progress
        if (!mRecyclerView.isRefreshing) {
            mRecyclerView.setSwipeToRefreshEnabled(!isUpdating && isSwipeToRefreshSupported)
        }
    }

    private val isSwipeToRefreshSupported: Boolean
        /*
             * swipe-to-refresh isn't supported for search results since they're really brief snapshots
             * and are unlikely to show new posts due to the way they're sorted
             */
        get() = getPostListType() != ReaderPostListType.SEARCH_RESULTS

    /*
     * bar that appears at the top when new posts have been retrieved
     */
    private fun isNewPostsBarShowing() = mNewPostsBar.isVisible

    private fun showNewPostsBar() {
        if (!isAdded || isNewPostsBarShowing()) {
            return
        }

        AniUtils.startAnimation(mNewPostsBar, R.anim.reader_top_bar_in)
        mNewPostsBar.visibility = View.VISIBLE

        // assign the scroll listener to hide the bar when the recycler is scrolled, but don't assign
        // it right away since the user may be scrolling when the bar appears (which would cause it
        // to disappear as soon as it's displayed)
        mRecyclerView.postDelayed({
            if (isAdded && isNewPostsBarShowing()) {
                mRecyclerView.addOnScrollListener(mOnScrollListener)
            }
        }, 1000L)

        // remove the gap marker if it's showing, since it's no longer valid
        postAdapter.removeGapMarker()
    }

    private fun hideNewPostsBar() {
        if (!isAdded || !isNewPostsBarShowing() || mIsAnimatingOutNewPostsBar) {
            return
        }

        mIsAnimatingOutNewPostsBar = true

        // remove the onScrollListener assigned in showNewPostsBar()
        mRecyclerView.removeOnScrollListener(mOnScrollListener)

        val listener: Animation.AnimationListener = object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
            }

            override fun onAnimationEnd(animation: Animation) {
                if (isAdded) {
                    mNewPostsBar.visibility = View.GONE
                    mIsAnimatingOutNewPostsBar = false
                }
            }

            override fun onAnimationRepeat(animation: Animation) {
            }
        }
        AniUtils.startAnimation(mNewPostsBar, R.anim.reader_top_bar_out, listener)
    }

    /*
     * are we showing all posts with a specific tag (followed or previewed), or all
     * posts in a specific blog?
     */
    private fun getPostListType(): ReaderPostListType {
        return mPostListType ?: ReaderTypes.DEFAULT_POST_LIST_TYPE
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
            if (isBookmarksList) {
                mReaderTracker.trackBlog(
                    AnalyticsTracker.Stat.READER_SAVED_POST_OPENED_FROM_SAVED_POST_LIST,
                    post.blogId,
                    post.feedId,
                    post.isFollowedByCurrentUser,
                    mPostAdapter!!.source
                )
            } else {
                mReaderTracker.trackBlog(
                    AnalyticsTracker.Stat.READER_SAVED_POST_OPENED_FROM_OTHER_POST_LIST,
                    post.blogId,
                    post.feedId,
                    post.isFollowedByCurrentUser,
                    mPostAdapter!!.source
                )
            }
        }

        // "discover" posts that highlight another post should open the original (source) post when tapped
        if (post.isDiscoverPost) {
            val discoverData = post.discoverData
            if (discoverData != null
                && discoverData.discoverType == ReaderPostDiscoverData.DiscoverType.EDITOR_PICK
            ) {
                if (discoverData.blogId != 0L && discoverData.postId != 0L) {
                    ReaderActivityLauncher.showReaderPostDetail(
                        activity,
                        discoverData.blogId,
                        discoverData.postId
                    )
                    return
                } else if (discoverData.hasPermalink()) {
                    if (mSeenUnseenWithCounterFeatureConfig.isEnabled()) {
                        mViewModel!!.onExternalPostOpened(post)
                    }
                    // if we don't have a blogId/postId, we sadly resort to showing the post
                    // in a WebView activity - this will happen for non-JP self-hosted
                    ReaderActivityLauncher.openUrl(activity, discoverData.permaLink)
                    return
                }
            }
        }

        // if this is a cross-post, we want to show the original post
        if (post.isXpost) {
            ReaderActivityLauncher.showReaderPostDetail(
                activity,
                post.xpostBlogId,
                post.xpostPostId
            )
            return
        }

        when (val type = getPostListType()) {
            ReaderPostListType.TAG_FOLLOWED, ReaderPostListType.TAG_PREVIEW -> ReaderActivityLauncher.showReaderPostPagerForTag(
                activity,
                currentTag,
                type,
                post.blogId,
                post.postId
            )

            ReaderPostListType.BLOG_PREVIEW -> ReaderActivityLauncher.showReaderPostPagerForBlog(
                activity,
                post.blogId,
                post.postId
            )

            ReaderPostListType.SEARCH_RESULTS -> {
                mReaderTracker.trackPost(AnalyticsTracker.Stat.READER_SEARCH_RESULT_TAPPED, post)
                ReaderActivityLauncher.showReaderPostDetail(activity, post.blogId, post.postId)
            }

            ReaderPostListType.TAGS_FEED -> {}
        }
    }

    /*
     * scroll listener assigned to the recycler when the "new posts" bar is shown to hide
     * it upon scrolling
     */
    private val mOnScrollListener: RecyclerView.OnScrollListener =
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
            String.format("reader post list > tag %s displayed", tag.tagNameForLog)
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
     * [ReaderPostListFragment.mReaderTracker] field instance will not be yet available, and
     * as thus cannot be used, or else it will result in a [NullPointerException].
     */
    private fun trackTagLoaded(tag: ReaderTag?) {
        if (tag == null) {
            return
        }
        val stat = if (tag.isTagTopic) {
            AnalyticsTracker.Stat.READER_TAG_LOADED
        } else if (tag.isListTopic) {
            AnalyticsTracker.Stat.READER_LIST_LOADED
        } else {
            return
        }

        trackTag(stat, tag.tagSlug)
    }

    override fun onButtonClicked(post: ReaderPost, actionType: ReaderPostCardActionType) {
        when (actionType) {
            ReaderPostCardActionType.FOLLOW -> mViewModel!!.onFollowSiteClicked(
                post,
                isBookmarksList,
                mPostAdapter!!.source
            )

            ReaderPostCardActionType.SITE_NOTIFICATIONS -> mViewModel!!.onSiteNotificationMenuClicked(
                post.blogId,
                post.postId,
                isBookmarksList,
                mPostAdapter!!.source
            )

            ReaderPostCardActionType.SHARE -> {
                mReaderTracker.trackBlog(
                    AnalyticsTracker.Stat.SHARED_ITEM_READER,
                    post.blogId,
                    post.feedId,
                    post.isFollowedByCurrentUser,
                    mPostAdapter!!.source
                )
                sharePost(post)
            }

            ReaderPostCardActionType.VISIT_SITE -> {
                mReaderTracker.track(AnalyticsTracker.Stat.READER_ARTICLE_VISITED)
                ReaderActivityLauncher.openPost(context, post)
            }

            ReaderPostCardActionType.LIKE -> mViewModel!!.onLikeButtonClicked(
                post,
                isBookmarksList,
                mPostAdapter!!.source
            )

            ReaderPostCardActionType.REBLOG -> mViewModel!!.onReblogButtonClicked(
                post,
                isBookmarksList,
                mPostAdapter!!.source
            )

            ReaderPostCardActionType.REPORT_POST -> mViewModel!!.onReportPostButtonClicked(
                post,
                isBookmarksList,
                mPostAdapter!!.source
            )

            ReaderPostCardActionType.REPORT_USER -> mViewModel!!.onReportUserButtonClicked(
                post,
                isBookmarksList,
                mPostAdapter!!.source
            )

            ReaderPostCardActionType.BLOCK_SITE -> mViewModel!!.onBlockSiteButtonClicked(
                post,
                isBookmarksList,
                mPostAdapter!!.source
            )

            ReaderPostCardActionType.BLOCK_USER -> mViewModel!!.onBlockUserButtonClicked(
                post,
                isBookmarksList,
                mPostAdapter!!.source
            )

            ReaderPostCardActionType.BOOKMARK -> mViewModel!!.onBookmarkButtonClicked(
                post.blogId,
                post.postId,
                isBookmarksList,
                mPostAdapter!!.source
            )

            ReaderPostCardActionType.COMMENTS -> ReaderActivityLauncher.showReaderComments(
                requireContext(),
                post.blogId,
                post.postId,
                ThreadedCommentsActionSource.READER_POST_CARD.sourceDescription
            )

            ReaderPostCardActionType.TOGGLE_SEEN_STATUS -> if (mSeenUnseenWithCounterFeatureConfig.isEnabled()) {
                mViewModel!!.onToggleSeenStatusClicked(
                    post,
                    isBookmarksList,
                    mPostAdapter!!.source
                )
            }

            ReaderPostCardActionType.SPACER_NO_ACTION -> {}
            ReaderPostCardActionType.READING_PREFERENCES -> {}
        }
    }

    override fun onFollowTapped(view: View, blogName: String, blogId: Long, feedId: Long) {
        mDispatcher.dispatch(AccountActionBuilder.newFetchSubscriptionsAction())

        val blog = if (TextUtils.isEmpty(blogName))
            getString(R.string.reader_followed_blog_notifications_this)
        else
            blogName

        if (blogId > 0) {
            make(
                snackbarParent!!,
                HtmlCompat.fromHtml(
                    getString(R.string.reader_followed_blog_notifications, "<b>", blog, "</b>"),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                ),
                Snackbar.LENGTH_LONG
            ).setAction(
                getString(R.string.reader_followed_blog_notifications_action)
            ) {
                mReaderTracker.trackBlog(
                    AnalyticsTracker.Stat.FOLLOWED_BLOG_NOTIFICATIONS_READER_ENABLED,
                    blogId,
                    feedId
                )
                val payload = AddOrDeleteSubscriptionPayload(
                    blogId.toString(), SubscriptionAction.NEW
                )
                mDispatcher.dispatch(
                    AccountActionBuilder.newUpdateSubscriptionNotificationPostAction(
                        payload
                    )
                )
                ReaderBlogTable.setNotificationsEnabledByBlogId(blogId, true)
            }
                .show()
        }
    }

    override fun onFollowingTapped() {
        mDispatcher.dispatch(AccountActionBuilder.newFetchSubscriptionsAction())
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
            mDispatcher.dispatch(AccountActionBuilder.newFetchSubscriptionsAction())
        }
    }

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
        if (!mHasPurgedReaderDb) {
            AppLog.d(AppLog.T.READER, "reader post list > purging database")
            mHasPurgedReaderDb = true
            ReaderDatabase.purgeAsync()
        }
    }

    override fun onScrollToTop() {
        if (isAdded && currentPosition > 0) {
            mRecyclerView.smoothScrollToPosition(0)
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("deprecation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RequestCodes.SITE_PICKER && resultCode == Activity.RESULT_OK && data != null) {
            val siteLocalId = data.getIntExtra(
                ChooseSiteActivity.KEY_SITE_LOCAL_ID,
                SelectedSiteRepository.UNAVAILABLE
            )
            mViewModel!!.onReblogSiteSelected(siteLocalId)
        }
    }

    private val isFollowingScreen: Boolean
        get() = mTagFragmentStartedWith != null && mTagFragmentStartedWith!!.isFollowedSites

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
        private var mHasPurgedReaderDb = false

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
