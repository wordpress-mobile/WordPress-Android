package org.wordpress.android.ui.reader

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.databinding.ReaderPostDetailBodyBinding
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.site.PrivateAtomicCookie
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.FetchPrivateAtomicCookiePayload
import org.wordpress.android.fluxc.store.SiteStore.OnPrivateAtomicCookieFetched
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.PrivateAtCookieRefreshProgressDialog
import org.wordpress.android.ui.PrivateAtCookieRefreshProgressDialog.PrivateAtCookieProgressDialogOnDismissListener
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.ViewPagerFragment
import org.wordpress.android.ui.compose.utils.showMessage
import org.wordpress.android.ui.engagement.EngagementNavigationSource
import org.wordpress.android.ui.main.ChooseSiteActivity
import org.wordpress.android.ui.media.MediaPreviewActivity
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.ReaderActivityLauncher.OpenUrlType
import org.wordpress.android.ui.reader.ReaderActivityLauncher.PhotoViewerOption
import org.wordpress.android.ui.reader.ReaderPostPagerActivity.DirectOperation
import org.wordpress.android.ui.reader.ReaderPostRenderer.ReaderPostMessageListener
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource.DIRECT_OPERATION
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource.READER_POST_DETAILS_COMMENTS
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences
import org.wordpress.android.ui.reader.tracker.ReaderReadingPreferencesTracker
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.tracker.ReaderTracker.Companion.SOURCE_POST_DETAIL_TOOLBAR
import org.wordpress.android.ui.reader.usecases.ReaderGetReadingPreferencesSyncUseCase
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.reader.utils.ReaderVideoUtils
import org.wordpress.android.ui.reader.viewmodels.ConversationNotificationsViewModel
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.PostRefreshed
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ReaderPostDetailsUiState
import org.wordpress.android.ui.reader.views.ReaderWebView
import org.wordpress.android.ui.reader.views.ReaderWebView.ReaderCustomViewListener
import org.wordpress.android.ui.reader.views.ReaderWebView.ReaderWebViewPageFinishedListener
import org.wordpress.android.ui.reader.views.ReaderWebView.ReaderWebViewUrlClickListener
import org.wordpress.android.ui.reader.views.compose.detail.ReaderPostDetailChromeState
import org.wordpress.android.ui.reader.views.compose.detail.ReaderPostDetailFooter
import org.wordpress.android.ui.reader.views.compose.detail.ReaderPostDetailFooterActions
import org.wordpress.android.ui.reader.views.compose.detail.ReaderPostDetailHeader
import org.wordpress.android.ui.reader.views.compose.detail.ReaderPostDetailNextPost
import org.wordpress.android.ui.reader.views.compose.detail.ReaderPostDetailScreen
import org.wordpress.android.ui.reader.views.compose.detail.ReaderPostDetailScreenActions
import org.wordpress.android.ui.reader.views.compose.detail.ReaderPostDetailTheme
import org.wordpress.android.ui.reader.views.compose.detail.ReaderPostDetailTopBarState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.PermissionUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.WPPermissionUtils.READER_FILE_DOWNLOAD_PERMISSION_REQUEST_CODE
import org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper
import org.wordpress.android.util.config.CommentsSnippetFeatureConfig
import org.wordpress.android.util.config.LikesEnhancementsFeatureConfig
import org.wordpress.android.util.config.ReaderReadingPreferencesFeatureConfig
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.viewmodel.observeEvent
import org.wordpress.android.widgets.WPScrollView
import org.wordpress.android.widgets.WPScrollView.ScrollDirectionListener
import java.util.EnumSet
import javax.inject.Inject

@AndroidEntryPoint
@Suppress("LargeClass")
class ReaderPostDetailFragment : ViewPagerFragment(),
    ScrollDirectionListener,
    ReaderCustomViewListener,
    ReaderWebViewPageFinishedListener,
    ReaderWebViewUrlClickListener,
    PrivateAtCookieProgressDialogOnDismissListener {
    private var postId: Long = 0
    private var blogId: Long = 0
    private var directOperation: DirectOperation? = null
    private var commentId: Int = 0
    private var interceptedUri: String? = null
    private var renderer: ReaderPostRenderer? = null
    private var postListType: ReaderPostListType = ReaderTypes.DEFAULT_POST_LIST_TYPE

    private val postHistory = ReaderPostHistory()

    private var bookmarksSavedLocallyDialog: AlertDialog? = null
    private lateinit var scrollView: WPScrollView
    private lateinit var readerWebView: ReaderWebView
    private lateinit var readerProgressBar: ProgressBar

    /** Where the WebView's fullscreen video goes, laid over the whole screen while it shows */
    private lateinit var customViewContainer: FrameLayout

    private lateinit var chrome: ReaderPostDetailChromeState
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper

    /** Where each related posts section was last laid out, keyed by whether it's the global one */
    private val relatedPostsCoordinates = mutableMapOf<Boolean, LayoutCoordinates>()

    // the status code of a failed request for the post's slug, which leaves this page without a post
    private var slugFailureStatus: Int? = null
    private var hasAlreadyUpdatedPost: Boolean = false
    private var isWebViewPaused: Boolean = false
    private var webViewLayoutListener: View.OnLayoutChangeListener? = null
    private var renderWatchdogJob: Job? = null
    private var renderStartedAtMs: Long = 0

    /**
     * Set when a render starts and consumed by the [onPageFinished] that follows it, so the
     * first-render work runs once per render. Keyed off the render rather than off the URL the
     * WebView reports: [ReaderPostRenderer] loads the article with a real base URL so embeds get
     * a referrer, and changing that URL must not be able to silently disable this again.
     *
     * Once per *render*, not once per post - a post that renders twice runs the work twice. Only
     * the refresh of the post is latched per post, by [hasAlreadyUpdatedPost]; anything added here that must
     * not repeat needs a latch of its own.
     */
    private var isAwaitingRenderedPageFinish: Boolean = false

    private var isRelatedPost: Boolean = false

    private var hasTrackedGlobalRelatedPosts: Boolean = false
    private var hasTrackedLocalRelatedPosts: Boolean = false

    private var fileForDownload: String? = null

    private val viewModel: ReaderPostDetailViewModel by viewModels()
    private lateinit var conversationViewModel: ConversationNotificationsViewModel

    /**
     * Hides fullscreen video, then steps back through the related posts opened in place, before back leaves the
     * screen. Only the page on screen is resumed - the pager holds its neighbours at STARTED - so the neighbours'
     * callbacks stay disabled.
     */
    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (chrome.isCustomViewShowing) {
                hideCustomView()
            } else {
                goBackInPostHistory()
            }
            updateBackCallback()
        }
    }

    @Inject
    internal lateinit var accountStore: AccountStore

    @Inject
    internal lateinit var siteStore: SiteStore

    @Inject
    internal lateinit var dispatcher: Dispatcher

    @Inject
    internal lateinit var readerFileDownloadManager: ReaderFileDownloadManager

    @Inject
    internal lateinit var privateAtomicCookie: PrivateAtomicCookie

    @Inject
    internal lateinit var readerCssProvider: ReaderCssProvider

    @Inject
    lateinit var readerUtilsWrapper: ReaderUtilsWrapper

    @Inject
    lateinit var viewModelFactory: Factory

    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var readerTracker: ReaderTracker

    @Inject
    lateinit var likesEnhancementsFeatureConfig: LikesEnhancementsFeatureConfig

    @Inject
    lateinit var commentsSnippetFeatureConfig: CommentsSnippetFeatureConfig

    @Inject
    lateinit var readingPreferencesFeatureConfig: ReaderReadingPreferencesFeatureConfig

    @Inject
    lateinit var getReadingPreferences: ReaderGetReadingPreferencesSyncUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        if (savedInstanceState != null) {
            postHistory.restoreInstance(savedInstanceState)
        }
        conversationViewModel =
            ViewModelProvider(this, viewModelFactory)[ConversationNotificationsViewModel::class.java]
    }

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)
        if (args != null) {
            blogId = args.getLong(ReaderConstants.ARG_BLOG_ID)
            postId = args.getLong(ReaderConstants.ARG_POST_ID)
            directOperation = args.getSerializableCompat(ReaderConstants.ARG_DIRECT_OPERATION)
            commentId = args.getInt(ReaderConstants.ARG_COMMENT_ID)
            isRelatedPost = args.getBoolean(ReaderConstants.ARG_IS_RELATED_POST)
            interceptedUri = args.getString(ReaderConstants.ARG_INTERCEPTED_URI)
            if (args.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                postListType = requireNotNull(
                    args.getSerializableCompat(ReaderConstants.ARG_POST_LIST_TYPE)
                )
            }
            if (args.containsKey(ARG_SLUG_FAILURE_STATUS)) slugFailureStatus = args.getInt(ARG_SLUG_FAILURE_STATUS)
        }
    }

    @Suppress("RedundantNullableReturnType")
    override fun getScrollableViewForUniqueIdProvision(): View? {
        return scrollView
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // The reading theme's View style still matters to the Compose chrome: the more menu resolves its colors
        // from the context's attributes, and the dialogs and sheets this screen opens are themed by it
        val readingPreferences = getReadingPreferences()
        val theme = readingPreferences.theme
        val themedContext = ContextThemeWrapper(requireContext(), theme.style)
        val themeValues = ReaderReadingPreferences.ThemeValues.from(themedContext, theme)

        val bodyBinding = ReaderPostDetailBodyBinding.inflate(inflater.cloneInContext(themedContext))
        scrollView = bodyBinding.scrollViewReader
        scrollView.setScrollDirectionListener(this)
        scrollView.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { view, _, scrollY, _, _ ->
                val range = (view.getChildAt(0)?.height ?: 0) - view.height
                chrome.readingProgress = if (range > 0) scrollY.toFloat() / range else 0f
            }
        )
        readerWebView = bodyBinding.readerWebview
        readerWebView.customViewListener = this
        readerWebView.urlClickListener = this
        readerWebView.pageFinishedListener = this
        readerWebView.shareQuoteListener = ::shareQuote
        readerProgressBar = bodyBinding.readerProgressBar
        customViewContainer = FrameLayout(themedContext).apply {
            setBackgroundColor(CUSTOM_VIEW_BACKGROUND)
            visibility = View.GONE
        }
        swipeToRefreshHelper = buildSwipeToRefreshHelper(bodyBinding.swipeToRefresh) {
            if (isAdded) {
                if (commentsSnippetFeatureConfig.isEnabled()) {
                    conversationViewModel.onRefresh()
                }
                viewModel.onRefreshPost()
            }
        }
        chrome = ReaderPostDetailChromeState(
            canScroll = { scrollView.canScrollVertically(1) || scrollView.canScrollVertically(-1) }
        )

        bodyBinding.headerComposeView.setContent {
            ReaderPostDetailTheme(theme, themeValues) { Header(readingPreferences) }
        }
        val isLikesEnabled = likesEnhancementsFeatureConfig.isEnabled()
        val isSnippetEnabled = commentsSnippetFeatureConfig.isEnabled()
        bodyBinding.footerComposeView.setContent {
            ReaderPostDetailTheme(theme, themeValues) {
                Footer(readingPreferences, isLikesEnabled, isSnippetEnabled)
            }
        }

        // hides the toolbar of NotificationsDetailActivity, which this screen's own top bar replaces
        (activity as? AppCompatActivity)?.supportActionBar?.hide()

        return createRootView(themedContext, theme, themeValues, body = bodyBinding.root)
    }

    private fun createRootView(
        themedContext: Context,
        theme: ReaderReadingPreferences.Theme,
        themeValues: ReaderReadingPreferences.ThemeValues,
        body: View,
    ) = ComposeView(themedContext).apply {
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            chrome.topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            insets
        }
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ReaderPostDetailTheme(theme, themeValues) {
                val uiState by viewModel.uiState.observeAsState()
                val isPostBlocked by viewModel.postBlocked.observeAsState(false)
                ReaderPostDetailScreen(
                    chrome = chrome,
                    uiState = uiState,
                    topBarState = ReaderPostDetailTopBarState.from(
                        uiState = uiState,
                        isPostBlocked = isPostBlocked,
                        postHasUrl = viewModel.post?.hasUrl() == true,
                        hasInterceptedUri = viewModel.interceptedUri != null,
                        isReadingPreferencesEnabled = readingPreferencesFeatureConfig.isEnabled(),
                    ),
                    isRelatedPost = isRelatedPost,
                    actions = screenActions,
                    body = body,
                    customViewContainer = customViewContainer,
                )
            }
        }
    }

    @Composable
    private fun Header(readingPreferences: ReaderReadingPreferences) {
        // only the header's own state, so the rest of the post's state changing (a like, the more menu) skips it
        val headerState = remember {
            viewModel.uiState.map { (it as? ReaderPostDetailsUiState)?.headerUiState }.distinctUntilChanged()
        }.observeAsState().value
        headerState?.let { ReaderPostDetailHeader(it, readingPreferences, viewModel::handleHeaderAction) }
    }

    @Composable
    private fun Footer(
        readingPreferences: ReaderReadingPreferences,
        isLikesEnabled: Boolean,
        isSnippetEnabled: Boolean,
    ) {
        val uiState by viewModel.uiState.observeAsState()
        val likesState = if (isLikesEnabled) viewModel.likesUiState.observeAsState().value else null
        val commentSnippetState = if (isSnippetEnabled) viewModel.commentSnippetState.observeAsState().value else null
        val followState = if (isSnippetEnabled) {
            conversationViewModel.updateFollowUiState.observeAsState().value
        } else {
            null
        }
        val nextPost by produceState<ReaderPostDetailNextPost?>(null, chrome.hasWebViewContent) {
            value = if (chrome.hasWebViewContent) loadNextPost() else null
        }
        ReaderPostDetailFooter(
            detailsState = chrome.currentDetailsState(uiState),
            hasWebViewContent = chrome.hasWebViewContent,
            likesState = likesState,
            commentSnippetState = commentSnippetState,
            followConversationState = followState,
            readingPreferences = readingPreferences,
            actions = footerActions,
            nextPost = nextPost,
        )
    }

    /**
     * The page's own post, from its arguments: [blogId] and [postId] change when a related post replaces it in place,
     * while its position in the pager doesn't
     */
    private val pageIds: Pair<Long, Long>
        get() = requireArguments().let {
            it.getLong(ReaderConstants.ARG_BLOG_ID) to it.getLong(ReaderConstants.ARG_POST_ID)
        }

    private suspend fun loadNextPost(): ReaderPostDetailNextPost? {
        val (pageBlogId, pagePostId) = pageIds
        val ids = (activity as? ReaderPostPagerActivity)?.getNextPostIds(pageBlogId, pagePostId) ?: return null
        val isFeed = requireArguments().getBoolean(ReaderConstants.ARG_IS_FEED)
        val imageSize = resources.getDimensionPixelSize(R.dimen.reader_next_post_image_size)
        return withContext(Dispatchers.IO) {
            val post = if (isFeed) {
                ReaderPostTable.getFeedPost(ids.blogId, ids.postId, true)
            } else {
                ReaderPostTable.getBlogPost(ids.blogId, ids.postId, true)
            } ?: return@withContext null
            ReaderPostDetailNextPost(
                title = post.title,
                blogName = post.blogName.orEmpty(),
                imageUrl = post.getFeaturedImageForDisplay(imageSize, imageSize).takeIf { it.isNotEmpty() },
            )
        }
    }

    private val screenActions = ReaderPostDetailScreenActions(
        onNavigationClick = {
            // the X on a related post closes it, while back can step through its post history
            if (isRelatedPost) {
                requireActivity().finish()
            } else {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        },
        onReadingPreferencesClick = {
            ReaderReadingPreferencesDialogFragment.show(
                childFragmentManager,
                ReaderReadingPreferencesTracker.Source.POST_DETAIL_TOOLBAR,
            )
        },
        onBrowseClick = ::onBrowseClicked,
        onShareClick = ::onShareClicked,
        onMoreClick = {
            readerTracker.track(AnalyticsTracker.Stat.READER_ARTICLE_DETAIL_MORE_TAPPED)
            viewModel.onMoreButtonClicked()
        },
        // lambdas rather than bound references, which would read the view model before the fragment is attached
        onMoreDismiss = { viewModel.onMoreMenuDismissed() },
        onMoreItemClick = { type -> viewModel.onMoreMenuItemClicked(type) },
        onSignInClick = ::signIn,
        onOpenInBrowserClick = {
            viewModel.interceptedUri?.let { uri ->
                readerTracker.trackUri(AnalyticsTracker.Stat.DEEP_LINKED_FALLBACK, uri)
                ReaderActivityLauncher.openUrl(requireActivity(), uri, OpenUrlType.EXTERNAL)
                requireActivity().finish()
            }
        },
    )

    private val footerActions = ReaderPostDetailFooterActions(
        onExcerptFooterClick = { link -> viewModel.onVisitPostExcerptFooterClicked(postLink = link) },
        onLikesClick = {
            if (isAdded) viewModel.onLikesClicked()
        },
        onTagsExpandToggled = { readerTracker.track(AnalyticsTracker.Stat.READER_CHIPS_MORE_TOGGLED) },
        onCommentImageClick = ::showCommentImage,
        onCommentClick = { viewModel.onSnippetCommentClicked(it.blogId, it.postId, it.commentId) },
        onRelatedPostsPositioned = { isGlobal, coordinates ->
            relatedPostsCoordinates[isGlobal] = coordinates
            trackRelatedPostsIfShowing()
        },
        onNextPostClick = { (activity as? ReaderPostPagerActivity)?.showNextPost() },
    )

    private fun showCommentImage(imageUrl: String) {
        val options = EnumSet.noneOf(PhotoViewerOption::class.java)
        if (viewModel.post?.isPrivate == true) {
            options.add(PhotoViewerOption.IS_PRIVATE_IMAGE)
        }
        ReaderActivityLauncher.showReaderPhotoViewer(requireActivity(), imageUrl, null, null, options, 0, 0)
    }

    private fun signIn() {
        viewModel.interceptedUri?.let { readerTracker.trackUri(AnalyticsTracker.Stat.READER_SIGN_IN_INITIATED, it) }
        ActivityLauncher.loginToWpCom(requireActivity())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)
        initViewModel(savedInstanceState)
        restoreState(savedInstanceState)

        showPost()
    }

    override fun onResume() {
        super.onResume()
        updateBackCallback()
    }

    override fun onPause() {
        super.onPause()
        updateBackCallback()
    }

    private fun updateBackCallback() {
        backCallback.isEnabled = isResumed && (chrome.isCustomViewShowing || !postHistory.isEmpty())
    }

    private fun initViewModel(savedInstanceState: Bundle?) {
        initObservers()

        val bundle = savedInstanceState ?: arguments
        val isFeed = bundle?.getBoolean(ReaderConstants.ARG_IS_FEED) ?: false
        val interceptedUri = bundle?.getString(ReaderConstants.ARG_INTERCEPTED_URI)

        if (commentsSnippetFeatureConfig.isEnabled()) {
            conversationViewModel.start(blogId, postId, READER_POST_DETAILS_COMMENTS)
        }
        viewModel.start(isRelatedPost = isRelatedPost, isFeed = isFeed, interceptedUri = interceptedUri)
    }

    private fun initObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) {
            if (it is ReaderPostDetailsUiState) onPostExecuteShowPost()
        }

        viewModel.postRefreshed.observeEvent(viewLifecycleOwner, ::onPostRefreshed)

        // the view model only refreshes the post's likes, comments and actions after a card action while this is
        // observed
        viewModel.refreshPost.observeEvent(viewLifecycleOwner) {
            // Do nothing
        }

        viewModel.snackbarEvents.observeEvent(viewLifecycleOwner) { it.showSnackbar() }

        viewModel.navigationEvents.observeEvent(viewLifecycleOwner) { it.handleNavigationEvent() }

        if (commentsSnippetFeatureConfig.isEnabled()) {
            conversationViewModel.snackbarEvents.observe(viewLifecycleOwner) { event ->
                if (!isAdded) return@observe

                val fm: FragmentManager = childFragmentManager
                val bottomSheet =
                    fm.findFragmentByTag(NOTIFICATIONS_BOTTOM_SHEET_TAG) as? CommentNotificationsBottomSheetFragment
                if (bottomSheet != null) return@observe
                event.applyIfNotHandled {
                    this.showSnackbar()
                }
            }

            conversationViewModel.showBottomSheetEvent.observeEvent(viewLifecycleOwner) { showBottomSheetData ->
                if (!isAdded) return@observeEvent

                val fm: FragmentManager = childFragmentManager
                var bottomSheet =
                    fm.findFragmentByTag(NOTIFICATIONS_BOTTOM_SHEET_TAG) as? CommentNotificationsBottomSheetFragment
                if (showBottomSheetData.show && bottomSheet == null) {
                    bottomSheet = CommentNotificationsBottomSheetFragment.newInstance(
                        showBottomSheetData.isReceivingNotifications,
                        true
                    )
                    bottomSheet.show(fm, NOTIFICATIONS_BOTTOM_SHEET_TAG)
                } else if (!showBottomSheetData.show && bottomSheet != null) {
                    bottomSheet.dismiss()
                }
            }
        }

        viewModel.showJetpackPoweredBottomSheet.observeEvent(viewLifecycleOwner) {
            JetpackPoweredBottomSheetFragment
                .newInstance()
                .show(childFragmentManager, JetpackPoweredBottomSheetFragment.TAG)
        }
    }

    /**
     * Registers an [View.OnLayoutChangeListener] on [readerWebView] that waits for
     * the WebView to obtain a positive height after rendering HTML content. Once
     * detected, the listener reveals the parts below the body (tags, likers and
     * comments), so they don't briefly show near the top and then jump below the fold.
     *
     * Registered when the render *starts* rather than from [onPageFinished]. The page's
     * load event waits on every subresource - third-party embed scripts and iframes
     * included - so gating the article chrome on it meant a single slow embed could keep
     * tags, comments, related posts and the likers row hidden over a fully painted
     * article. The WebView gaining height is the signal we actually care about.
     */
    private fun registerWebViewLayoutListener() {
        removeWebViewLayoutListener()
        webViewLayoutListener = View.OnLayoutChangeListener {
                _, _, _, _, _, _, _, _, _ ->
            if (readerWebView.height > 0 && !chrome.hasWebViewContent) {
                onWebViewContentShown()
            }
        }
        readerWebView.addOnLayoutChangeListener(webViewLayoutListener)
    }

    /**
     * Called once the WebView has laid out with content. Hides the render spinner and
     * reveals the parts of the detail screen that are deliberately held back until the
     * article body is on screen.
     */
    private fun onWebViewContentShown() {
        chrome.hasWebViewContent = true
        removeWebViewLayoutListener()
        cancelRenderWatchdog()
        readerProgressBar.visibility = View.GONE
    }

    private fun removeWebViewLayoutListener() {
        webViewLayoutListener?.let {
            readerWebView.removeOnLayoutChangeListener(it)
        }
        webViewLayoutListener = null
    }

    /**
     * Starts the render and arms its backstop together. The watchdog measures the render, so it
     * must not be started before one that is still waiting on something else: a private Atomic
     * post postpones rendering until its access cookie arrives, and that wait has its own
     * progress dialog. Arming the two together also keeps [renderStartedAtMs] - and so the
     * elapsed time logged by [onPageFinished] - measuring the render rather than the network.
     */
    private fun beginRenderWithWatchdog() {
        startRenderWatchdog()
        isAwaitingRenderedPageFinish = true
        renderer?.beginRender()
    }

    /**
     * Starts the render clock, and the backstop for a render that never produces content.
     * Nothing in the article HTML should block painting any more, but the spinner must not be
     * able to spin forever: if the layout listener has not fired by [RENDER_WATCHDOG_MS], give
     * up on it and log how long we waited so the next report is diagnosable.
     *
     * A re-render over an article already on screen gets no backstop. There is no spinner up for
     * it to take down, and nothing could cancel it either - the layout listener removes itself on
     * the first paint and is only re-registered while the body has no content - so it would
     * do nothing but warn about a render that in fact finished.
     */
    private fun startRenderWatchdog() {
        renderStartedAtMs = System.currentTimeMillis()
        if (!chrome.hasWebViewContent) {
            armSpinnerBackstop(RENDER_WATCHDOG_MS)
        }
    }

    /**
     * Arms the single backstop that hides the spinner if [onWebViewContentShown] never runs,
     * replacing any previous one. The timeout differs by what we are still waiting for: a whole
     * render, or just the paint that follows a document that has already finished loading.
     */
    private fun armSpinnerBackstop(timeoutMs: Long) {
        cancelRenderWatchdog()
        renderWatchdogJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(timeoutMs)
            AppLog.w(
                T.READER,
                "reader post detail > no rendered content after ${timeoutMs}ms, hiding spinner"
            )
            readerProgressBar.visibility = View.GONE
        }
    }

    private fun cancelRenderWatchdog() {
        renderWatchdogJob?.cancel()
        renderWatchdogJob = null
    }

    // TODO: Update using UiState/ NavigationEvent
    @Suppress("ForbiddenComment")
    private fun onPostExecuteShowPost() {
        viewModel.post?.let {
            if (handleDirectOperation()) return

            scrollView.visibility = View.VISIBLE
            chrome.isPostShown = true
        }
    }

    @Suppress("ComplexMethod", "LongMethod")
    private fun ReaderNavigationEvents.handleNavigationEvent() {
        if (!isAdded) {
            return
        }
        when (this) {
            is ReaderNavigationEvents.ShowMediaPreview -> MediaPreviewActivity
                .showPreview(requireContext(), site, featuredImage)

            is ReaderNavigationEvents.ShowPostsByTag -> ReaderActivityLauncher.showReaderTagPreview(
                requireContext(),
                this.tag,
                ReaderTracker.SOURCE_POST_DETAIL,
                readerTracker
            )

            is ReaderNavigationEvents.ShowBlogPreview -> ReaderActivityLauncher.showReaderBlogOrFeedPreview(
                requireContext(),
                this.siteId,
                this.feedId,
                this.isFollowed,
                ReaderTracker.SOURCE_POST_DETAIL,
                readerTracker
            )

            is ReaderNavigationEvents.SharePost -> ReaderActivityLauncher.sharePost(requireContext(), post)

            is ReaderNavigationEvents.OpenPost -> ReaderActivityLauncher.openPost(requireContext(), post)

            is ReaderNavigationEvents.ShowReportPost -> {
                ReaderActivityLauncher.openUrl(
                    requireActivity(),
                    readerUtilsWrapper.getReportPostUrl(url),
                    OpenUrlType.INTERNAL
                )
            }

            is ReaderNavigationEvents.ShowReportUser -> ReaderActivityLauncher.openUrl(
                requireContext(),
                readerUtilsWrapper.getReportUserUrl(url, authorId),
                OpenUrlType.INTERNAL
            )

            is ReaderNavigationEvents.ShowReaderComments -> ReaderActivityLauncher.showReaderCommentsForResult(
                this@ReaderPostDetailFragment,
                blogId,
                postId,
                this.source.sourceDescription,
                this.commentId
            )

            is ReaderNavigationEvents.ShowNoSitesToReblog ->
                ReaderActivityLauncher.showNoSiteToReblog(requireActivity())

            is ReaderNavigationEvents.ShowSitePickerForResult ->
                ActivityLauncher
                    .showSitePickerForResult(this@ReaderPostDetailFragment, this.preselectedSite, this.mode)

            is ReaderNavigationEvents.OpenEditorForReblog ->
                ActivityLauncher.openEditorForReblog(requireActivity(), this.site, this.post, this.source)

            is ReaderNavigationEvents.ShowBookmarkedTab ->
                ActivityLauncher.viewSavedPostsListInReader(requireActivity())

            is ReaderNavigationEvents.ShowBookmarkedSavedOnlyLocallyDialog ->
                showBookmarkSavedLocallyDialog(this)

            is ReaderNavigationEvents.OpenUrl ->
                ReaderActivityLauncher.openUrl(requireContext(), url)

            is ReaderNavigationEvents.ShowRelatedPostDetails ->
                showRelatedPostDetail(postId = this.postId, blogId = this.blogId)

            is ReaderNavigationEvents.ReplaceRelatedPostDetailsWithHistory ->
                replaceRelatedPostDetailWithHistory(postId = this.postId, blogId = this.blogId)

            is ReaderNavigationEvents.ShowPostInWebView -> showPostInWebView(post)
            is ReaderNavigationEvents.ShowEngagedPeopleList -> {
                ActivityLauncher.viewPostLikesListActivity(
                    requireActivity(),
                    this.siteId,
                    this.postId,
                    this.headerData,
                    EngagementNavigationSource.LIKE_READER_LIST
                )
            }

            ReaderNavigationEvents.ShowReadingPreferences ->
                ReaderReadingPreferencesDialogFragment.show(
                    childFragmentManager,
                    ReaderReadingPreferencesTracker.Source.POST_DETAIL_MORE_MENU,
                )

            is ReaderNavigationEvents.ShowAuthorProfile -> showAuthorProfile(this)

            is ReaderNavigationEvents.ShowPostDetail,
            is ReaderNavigationEvents.ShowReaderSubs -> Unit // Do Nothing
            is ReaderNavigationEvents.ShowLoginRequiredBottomSheet -> showLoginRequiredBottomSheet()
        }
    }

    private fun showAuthorProfile(event: ReaderNavigationEvents.ShowAuthorProfile) {
        ReaderAuthorProfileBottomSheetFragment.newInstance(
            authorName = event.authorName,
            authorAvatar = event.authorAvatar,
            blogName = event.blogName,
            blogUrl = event.blogUrl,
        ).show(childFragmentManager, ReaderAuthorProfileBottomSheetFragment.TAG)
    }

    private fun showLoginRequiredBottomSheet() {
        ReaderLoginRequiredBottomSheetFragment.newInstance()
            .show(childFragmentManager, ReaderLoginRequiredBottomSheetFragment.TAG)
    }

    private fun showBookmarkSavedLocallyDialog(
        bookmarkDialog: ReaderNavigationEvents.ShowBookmarkedSavedOnlyLocallyDialog
    ) {
        if (bookmarksSavedLocallyDialog == null) {
            MaterialAlertDialogBuilder(requireActivity())
                .setTitle(getString(bookmarkDialog.title))
                .setMessage(getString(bookmarkDialog.message))
                .setPositiveButton(getString(bookmarkDialog.buttonLabel)) { _, _ ->
                    bookmarkDialog.okButtonAction.invoke()
                }
                .setOnDismissListener {
                    bookmarksSavedLocallyDialog = null
                }
                .setCancelable(false)
                .create()
                .let {
                    bookmarksSavedLocallyDialog = it
                    it.show()
                }
        }
    }

    /**
     * Shows a snackbar in place of any already showing, as the View-based snackbars did, rather than queueing it
     * behind that one
     */
    private fun replaceSnackbar(show: suspend SnackbarHostState.() -> Unit) {
        if (view == null) return
        val hostState = chrome.snackbarHostState
        viewLifecycleOwner.lifecycleScope.launch {
            hostState.currentSnackbarData?.dismiss()
            hostState.show()
        }
    }

    private fun SnackbarMessageHolder.showSnackbar() {
        val context = requireContext()
        replaceSnackbar { showMessage(this@showSnackbar, context, uiHelpers) }
    }

    /**
     * An indefinite snackbar gets a dismiss button, since a Compose snackbar can't be swiped away
     */
    private fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        isIndefinite: Boolean = false,
        action: () -> Unit = {},
    ) = replaceSnackbar {
        val result = showSnackbar(
            message = message,
            actionLabel = actionLabel,
            withDismissAction = isIndefinite,
            duration = if (isIndefinite) SnackbarDuration.Indefinite else SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) action()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeWebViewLayoutListener()
        relatedPostsCoordinates.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (view != null) {
            readerWebView.destroy()
        }
        bookmarksSavedLocallyDialog?.dismiss()
    }

    private fun onBrowseClicked() {
        val interceptedUri = viewModel.interceptedUri
        val post = viewModel.post
        if (post != null) {
            readerTracker.track(AnalyticsTracker.Stat.READER_ARTICLE_VISITED)
            ReaderActivityLauncher.openPost(requireContext(), post)
        } else if (interceptedUri != null) {
            readerTracker.trackUri(AnalyticsTracker.Stat.DEEP_LINKED_FALLBACK, interceptedUri)
            ReaderActivityLauncher.openUrl(requireActivity(), interceptedUri, OpenUrlType.EXTERNAL)
            requireActivity().finish()
        }
    }

    private fun onShareClicked() {
        val post = viewModel.post ?: return
        readerTracker.trackBlog(
            AnalyticsTracker.Stat.SHARED_ITEM_READER,
            post.blogId,
            post.feedId,
            post.isFollowedByCurrentUser,
            SOURCE_POST_DETAIL_TOOLBAR,
        )
        ReaderActivityLauncher.sharePost(requireContext(), post)
    }

    /*
     * shares the selected text in quotes, followed by the post's link
     */
    private fun shareQuote(text: String) {
        val post = viewModel.post ?: return
        val url = if (post.hasShortUrl()) post.shortUrl else post.url
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, getString(R.string.reader_share_quote_text, text, url))
        try {
            startActivity(Intent.createChooser(intent, null))
        } catch (e: ActivityNotFoundException) {
            AppLog.w(T.READER, "No app to share a quote with: $e")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(ReaderConstants.ARG_IS_FEED, viewModel.isFeed)
        outState.putLong(ReaderConstants.ARG_BLOG_ID, blogId)
        outState.putLong(ReaderConstants.ARG_POST_ID, postId)
        outState.putSerializable(ReaderConstants.ARG_DIRECT_OPERATION, directOperation)
        outState.putInt(ReaderConstants.ARG_COMMENT_ID, commentId)

        outState.putBoolean(ReaderConstants.ARG_IS_RELATED_POST, isRelatedPost)
        outState.putString(ReaderConstants.ARG_INTERCEPTED_URI, viewModel.interceptedUri)
        outState.putBoolean(ReaderConstants.KEY_ALREADY_UPDATED, hasAlreadyUpdatedPost)

        outState.putBoolean(
            ReaderConstants.KEY_ALREADY_TRACKED_GLOBAL_RELATED_POSTS,
            hasTrackedGlobalRelatedPosts
        )
        outState.putBoolean(
            ReaderConstants.KEY_ALREADY_TRACKED_LOCAL_RELATED_POSTS,
            hasTrackedLocalRelatedPosts
        )

        outState.putSerializable(
            ReaderConstants.ARG_POST_LIST_TYPE,
            this.postListType
        )

        postHistory.saveInstance(outState)

        super.onSaveInstanceState(outState)
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            blogId = it.getLong(ReaderConstants.ARG_BLOG_ID)
            postId = it.getLong(ReaderConstants.ARG_POST_ID)
            directOperation = it.getSerializableCompat(ReaderConstants.ARG_DIRECT_OPERATION)
            commentId = it.getInt(ReaderConstants.ARG_COMMENT_ID)
            isRelatedPost = it.getBoolean(ReaderConstants.ARG_IS_RELATED_POST)
            interceptedUri = it.getString(ReaderConstants.ARG_INTERCEPTED_URI)
            hasAlreadyUpdatedPost = it.getBoolean(ReaderConstants.KEY_ALREADY_UPDATED)
            hasTrackedGlobalRelatedPosts = it.getBoolean(ReaderConstants.KEY_ALREADY_TRACKED_GLOBAL_RELATED_POSTS)
            hasTrackedLocalRelatedPosts = it.getBoolean(ReaderConstants.KEY_ALREADY_TRACKED_LOCAL_RELATED_POSTS)
            if (it.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                postListType = requireNotNull(it.getSerializableCompat(ReaderConstants.ARG_POST_LIST_TYPE))
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity?.registerReceiver(
                readerFileDownloadManager,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextWrapper.RECEIVER_NOT_EXPORTED
            )
        } else {
            activity?.registerReceiver(
                readerFileDownloadManager,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
        activity?.unregisterReceiver(readerFileDownloadManager)
    }

    /*
     * replace the current post with the passed one
     */
    private fun replacePost(blogId: Long, postId: Long, clearCommentOperation: Boolean) {
        viewModel.isFeed = false
        this.blogId = blogId
        this.postId = postId

        if (clearCommentOperation) {
            directOperation = null
            commentId = 0
        }

        hasAlreadyUpdatedPost = false
        hasTrackedGlobalRelatedPosts = false
        hasTrackedLocalRelatedPosts = false

        // hides what's below the body until the new post's state arrives and its body is laid out
        chrome.replacedState = viewModel.uiState.value
        chrome.hasWebViewContent = false
        removeWebViewLayoutListener()

        // clear the webView - otherwise it will remain scrolled to where the user scrolled to
        readerWebView.clearContent()

        // now show the passed post
        showPost()
    }

    /*
     * track that related posts have loaded and are scrolled into view if we haven't
     * already tracked it
     */
    private fun trackRelatedPostsIfShowing() {
        val state = chrome.currentDetailsState(viewModel.uiState.value) ?: return
        if (!hasTrackedGlobalRelatedPosts && isScrolledIntoView(isGlobal = true)) {
            hasTrackedGlobalRelatedPosts = true
            AppLog.d(T.READER, "reader post detail > global related posts rendered")
            state.globalRelatedPosts?.railcarJsonStrings?.forEach { it?.let(readerTracker::trackRailcar) }
        }

        if (!hasTrackedLocalRelatedPosts && isScrolledIntoView(isGlobal = false)) {
            hasTrackedLocalRelatedPosts = true
            AppLog.d(T.READER, "reader post detail > local related posts rendered")
            state.localRelatedPosts?.railcarJsonStrings?.forEach { it?.let(readerTracker::trackRailcar) }
        }
    }

    /*
     * returns True if the related posts section is showing and at least partly scrolled into view
     */
    private fun isScrolledIntoView(isGlobal: Boolean): Boolean {
        val coordinates = relatedPostsCoordinates[isGlobal]?.takeIf { it.isAttached }
        if (coordinates == null || view == null || !scrollView.isShown) return false
        val bounds = coordinates.boundsInWindow()
        val location = IntArray(2)
        scrollView.getLocationInWindow(location)
        val visibleTop = location[1]
        val visibleBottom = visibleTop + scrollView.height
        return bounds.height > 0 && bounds.bottom > visibleTop && bounds.top < visibleBottom
    }

    private fun showRelatedPostDetail(postId: Long, blogId: Long) {
        ReaderActivityLauncher.showReaderPostDetail(
            context = requireActivity(),
            isFeed = false,
            blogId = blogId,
            postId = postId,
            directOperation = null,
            isRelatedPost = true,
        )
    }

    private fun replaceRelatedPostDetailWithHistory(postId: Long, blogId: Long) {
        viewModel.post?.let {
            postHistory.push(ReaderBlogIdPostId(it.blogId, it.postId))
            replacePost(blogId, postId, true)
            updateBackCallback()
        }
    }

    /*
     * if the fragment is maintaining a backstack of posts, navigate to the previous one
     */
    private fun goBackInPostHistory() {
        if (!postHistory.isEmpty()) {
            val ids = postHistory.pop()
            replacePost(ids.blogId, ids.postId, true)
        }
    }

    private fun onPostRefreshed(refreshed: PostRefreshed) {
        swipeToRefreshHelper.isRefreshing = false
        val post = refreshed.post ?: return
        if (commentsSnippetFeatureConfig.isEnabled()) {
            conversationViewModel.onUpdatePost(post.blogId, post.postId)
        }
        // Only a changed body needs the WebView again. The refresh also covers the like and comment counts, and
        // reloading an identical article for one of those would blank it, collapse it and lose the reader's
        // place - with no spinner, since the render spinner stays down over content already up.
        if (refreshed.hasNewBody) {
            showPostInWebView(post)
        }
        if (directOperation == DirectOperation.POST_LIKE) {
            doLikePost()
        }
    }

    @Suppress("ReturnCount")
    private fun doLikePost() {
        if (!isAdded) {
            return
        }

        if (!accountStore.hasAccessToken()) {
            showSnackbar(
                message = getString(R.string.reader_snackbar_err_cannot_like_post_logged_out),
                actionLabel = getString(R.string.sign_in),
                isIndefinite = true,
                action = ::signIn,
            )
            return
        }

        viewModel.post?.let {
            if (!it.canLikePost()) {
                ToastUtils.showToast(activity, R.string.reader_toast_err_cannot_like_post)
                return
            }

            if (!it.isLikedByCurrentUser) {
                viewModel.onButtonClicked(it.postId, it.blogId, ReaderPostCardActionType.LIKE)
            }
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RequestCodes.SITE_PICKER -> {
                if (resultCode == Activity.RESULT_OK) {
                    val siteLocalId = data?.getIntExtra(
                        ChooseSiteActivity.KEY_SITE_LOCAL_ID,
                        SelectedSiteRepository.UNAVAILABLE
                    ) ?: SelectedSiteRepository.UNAVAILABLE
                    viewModel.onReblogSiteSelected(siteLocalId)
                }
            }
            RequestCodes.READER_FOLLOW_CONVERSATION -> {
                if (resultCode == Activity.RESULT_OK && commentsSnippetFeatureConfig.isEnabled() && data != null) {
                    val flags = data.getParcelableExtra<FollowConversationStatusFlags>(
                        FOLLOW_CONVERSATION_UI_STATE_FLAGS_KEY
                    )
                    flags?.let {
                        conversationViewModel.onUserNavigateFromComments(it)
                    }
                }
                viewModel.onUserNavigateFromComments()
            }
        }
    }

    @Suppress("ReturnCount")
    private fun showPhotoViewer(
        imageUrl: String,
        sourceView: View,
        startX: Int,
        startY: Int
    ): Boolean {
        if (!isAdded || imageUrl.isEmpty()) {
            return false
        }

        // make sure this is a valid web image (could be file: or data:)
        if (!imageUrl.startsWith("http")) {
            return false
        }

        val postContent = viewModel.post?.text.orEmpty()
        val isPrivatePost = viewModel.post?.isPrivate == true
        val options = EnumSet.noneOf(PhotoViewerOption::class.java)
        if (isPrivatePost) {
            options.add(PhotoViewerOption.IS_PRIVATE_IMAGE)
        }

        ReaderActivityLauncher.showReaderPhotoViewer(
            requireActivity(),
            imageUrl,
            postContent,
            sourceView,
            options,
            startX,
            startY
        )

        return true
    }

    private val galleryDetector = ReaderGalleryDetector()

    private fun showPhotoViewerWithGalleryCheck(
        imageUrl: String,
        sourceView: View,
        startX: Int,
        startY: Int
    ) {
        if (!isAdded || imageUrl.isEmpty() || !imageUrl.startsWith("http")) {
            return
        }
        galleryDetector.detectGallery(readerWebView, imageUrl) { galleryUrls ->
            if (!isAdded) return@detectGallery
            if (galleryUrls != null) {
                showPhotoViewerForGallery(imageUrl, sourceView, startX, startY, galleryUrls)
            } else {
                showPhotoViewer(imageUrl, sourceView, startX, startY)
            }
        }
    }

    private fun showPhotoViewerForGallery(
        imageUrl: String,
        sourceView: View,
        startX: Int,
        startY: Int,
        galleryUrls: ArrayList<String>
    ) {
        val isPrivatePost = viewModel.post?.isPrivate == true
        val options = EnumSet.noneOf(PhotoViewerOption::class.java)
        if (isPrivatePost) {
            options.add(PhotoViewerOption.IS_PRIVATE_IMAGE)
        }

        ReaderActivityLauncher.showReaderPhotoViewer(
            requireActivity(),
            imageUrl,
            null,
            sourceView,
            options,
            startX,
            startY,
            galleryUrls
        )
    }

    private fun showPost() {
        slugFailureStatus?.let {
            viewModel.onPostSlugRequestFailed(it)
            return
        }

        if (commentsSnippetFeatureConfig.isEnabled()) {
            conversationViewModel.onUpdatePost(blogId, postId)
        }

        viewModel.onShowPost(blogId = blogId, postId = postId)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPrivateAtomicCookieFetched(event: OnPrivateAtomicCookieFetched) {
        if (!isAdded) {
            return
        }

        if (event.isError) {
            AppLog.e(T.READER, "Failed to load private AT cookie. $event.error.type - $event.error.message")
            showSnackbar(getString(R.string.media_accessing_failed))
        } else {
            CookieManager.getInstance().setCookie(
                privateAtomicCookie.getDomain(), privateAtomicCookie.getCookieContent()
            )
        }

        PrivateAtCookieRefreshProgressDialog.dismissIfNecessary(childFragmentManager)
        beginRenderWithWatchdog()
    }

    override fun onCookieProgressDialogCancelled() {
        if (!isAdded) {
            return
        }

        showSnackbar(getString(R.string.media_accessing_failed))
        beginRenderWithWatchdog()
    }

    // overridePendingTransition is deprecated in SDK 34 in favor of overrideActivityTransition, but the latter requires
    // SDK 34. overridePendingTransition still works on Android 14 so using it should be safe for now.
    @Suppress("DEPRECATION")
    private fun handleDirectOperation() = when (directOperation) {
        DirectOperation.COMMENT_JUMP, DirectOperation.COMMENT_REPLY, DirectOperation.COMMENT_LIKE -> {
            viewModel.post?.let {
                context?.let { nonNullContext ->
                    ReaderActivityLauncher.showReaderComments(
                        context = nonNullContext,
                        blogId = it.blogId,
                        postId = it.postId,
                        directOperation = directOperation,
                        commentId = commentId.toLong(),
                        interceptedUri = viewModel.interceptedUri,
                        source = DIRECT_OPERATION.sourceDescription
                    )
                }
            }

            activity?.finish()
            activity?.overridePendingTransition(0, 0)
            true
        }
        // Like needs to be handled "later" after the post has been updated from the server, nothing special to do here.
        DirectOperation.POST_LIKE -> false
        null -> false
    }

    private fun showPostInWebView(post: ReaderPost) {
        readerWebView.isPrivatePost = post.isPrivate
        readerWebView.blogSchemeIsHttps = UrlUtils.isHttps(post.blogUrl)
        // Watch for the body painting from here rather than from onPageFinished. The backstop is
        // armed with the render itself in [beginRenderWithWatchdog], not here - a private Atomic
        // post postpones its render until the access cookie arrives.
        // A re-render over an article already on screen - a refresh found a newer version of the post -
        // stays silent: the spinner would otherwise cover content the reader is reading.
        if (!chrome.hasWebViewContent) {
            readerProgressBar.visibility = View.VISIBLE
            registerWebViewLayoutListener()
        }
        renderer = ReaderPostRenderer(
            readerWebView,
            post,
            readerCssProvider,
            getReadingPreferences()
        ).also {
            it.setPostMessageListener(object : ReaderPostMessageListener {
                override fun onArticleTextCopied() {
                    viewModel.onArticleTextCopied()
                }

                override fun onArticleTextHighlighted() {
                    viewModel.onArticleTextHighlighted()
                }

                override fun onFragmentLinkClicked(url: String) {
                    onUrlClick(url)
                }
            })
        }

        // if the post is from private atomic site postpone render until we have a special access cookie
        if (post.isPrivateAtomic && privateAtomicCookie.isCookieRefreshRequired()) {
            PrivateAtCookieRefreshProgressDialog.showIfNecessary(childFragmentManager)
            requestPrivateAtomicCookie()
        } else if (post.isPrivateAtomic && privateAtomicCookie.exists()) {
            // make sure we add cookie to the cookie manager if it exists before starting render
            CookieManager.getInstance().setCookie(
                privateAtomicCookie.getDomain(), privateAtomicCookie.getCookieContent()
            )
            beginRenderWithWatchdog()
        } else {
            beginRenderWithWatchdog()
        }
    }

    private fun requestPrivateAtomicCookie() {
        dispatcher.dispatch(
            viewModel.post?.let {
                SiteActionBuilder.newFetchPrivateAtomicCookieAction(FetchPrivateAtomicCookiePayload(it.blogId))
            }
        )
    }

    /*
     * called by the web view when the content finishes loading - related posts aren't displayed
     * until this is triggered, to avoid having them appear before the webView content
     */
    override fun onPageFinished(view: WebView, url: String?) {
        if (!isAdded) {
            return
        }

        // The blank page clearContent() loads to drop the previous post. It doesn't go through
        // the renderer, so attach the layout listener for it, but nothing else here applies:
        // there is no article to measure, inject into, or react to, and the render that follows
        // arms its own watchdog.
        if (url == ReaderWebView.BLANK_URL) {
            if (!chrome.hasWebViewContent) {
                registerWebViewLayoutListener()
            }
            return
        }

        if (renderStartedAtMs > 0) {
            val elapsedMs = System.currentTimeMillis() - renderStartedAtMs
            AppLog.d(T.READER, "reader post detail > page finished after ${elapsedMs}ms - $url")
        }
        injectFragmentLinkInterceptor(view)

        // The document has finished loading, but the WebView has not necessarily laid out yet -
        // the body typically paints a few hundred ms later. Hiding the spinner here would uncover
        // an empty article for that whole window, so hand over to the layout listener and keep
        // only a short backstop for the post that never paints at all. A re-render over content
        // already on screen needs neither: the listener has done its job and the spinner is down.
        if (!chrome.hasWebViewContent) {
            armSpinnerBackstop(PAINT_GRACE_MS)
            registerWebViewLayoutListener()
        }

        // Only the page-finished that belongs to a render we started does the first-render work,
        // which also keeps the repeat calls a page full of iframes can produce from redoing it.
        if (isAwaitingRenderedPageFinish) {
            isAwaitingRenderedPageFinish = false
            // brief delay before showing related posts to give page time to render
            view.postDelayed(Runnable {
                if (!isAdded) {
                    return@Runnable
                }
                if (!hasAlreadyUpdatedPost) {
                    hasAlreadyUpdatedPost = true
                    viewModel.onRefreshPost()
                }
                viewModel.post?.let {
                    if (likesEnhancementsFeatureConfig.isEnabled()) {
                        viewModel.onRefreshLikersData(it)
                    }
                    if (commentsSnippetFeatureConfig.isEnabled()) {
                        viewModel.onRefreshCommentsData(it.blogId, it.postId)
                    }
                    viewModel.onRelatedPostsRequested(it)
                }
            }, ON_PAGE_FINISHED_DELAY_MILLIS)
        }
    }

    /**
     * Injects a JS click interceptor that catches taps on
     * same-page fragment links (e.g. footnote back-references)
     * and routes them through the wvHandler message bridge so
     * native scroll-to-element handling is used instead of
     * WebView-internal navigation.
     */
    private fun injectFragmentLinkInterceptor(view: WebView) {
        val postUrl = viewModel.post?.url
            ?.trimEnd('/') ?: return
        val safePostUrl = postUrl
            .replace("\\", "\\\\")
            .replace("'", "\\'")
        view.evaluateJavascript(
            "(function(){" +
                "if(window._fragInterceptorAdded)return;" +
                "window._fragInterceptorAdded=true;" +
                "var base='$safePostUrl'.toLowerCase();" +
                "document.addEventListener('click',function(e){" +
                "var el=e.target;" +
                "while(el&&el.tagName!=='A')el=el.parentElement;" +
                "if(!el||!el.href)return;" +
                "var i=el.href.indexOf('#');" +
                "if(i===-1)return;" +
                "var u=el.href.substring(0,i)" +
                ".replace(/\\/$/,'').toLowerCase();" +
                "if(u===base||u===''){" +
                "e.preventDefault();" +
                "wvHandler.postMessage('fragmentLink:'+el.href);" +
                "}" +
                "},true);" +
                "})()",
            null
        )
    }

    /*
     * return the container view that should host the full screen video
     */
    override fun onRequestCustomView(): ViewGroup? {
        return if (isAdded && view != null) customViewContainer else null
    }

    /*
     * return the container view that should be hidden when full screen video is shown
     */
    override fun onRequestContentView(): ViewGroup? {
        return if (isAdded && view != null) scrollView else null
    }

    override fun onCustomViewShown() {
        // full screen video has just been shown, which hides the top and bottom bars
        readerTracker.track(AnalyticsTracker.Stat.READER_ARTICLE_CUSTOM_VIEW_SHOWN)
        chrome.isCustomViewShowing = true
        updateBackCallback()
    }

    override fun onCustomViewHidden() {
        // user returned from full screen video, so show the bars again - including if scrolling had hidden them
        readerTracker.track(AnalyticsTracker.Stat.READER_ARTICLE_CUSTOM_VIEW_HIDDEN)
        chrome.isCustomViewShowing = false
        chrome.scrollConnection.reset()
        updateBackCallback()
    }

    private fun hideCustomView() {
        if (view != null) {
            readerWebView.hideCustomView()
        }
    }

    override fun onUrlClick(url: String): Boolean {
        readerTracker.track(AnalyticsTracker.Stat.READER_ARTICLE_LINK_TAPPED)

        // Handle same-page fragment links (e.g., footnotes).
        // Footnote hrefs resolve to the post's own URL with
        // a fragment (e.g., "https://example.com/post/#fn-id").
        val fragment = getPostUrlFragment(url)
        if (fragment != null) {
            scrollToElement(fragment) {
                val openUrlType = if (shouldOpenExternal(url)) {
                    OpenUrlType.EXTERNAL
                } else {
                    OpenUrlType.INTERNAL
                }
                ReaderActivityLauncher.openUrl(
                    requireActivity(), url, openUrlType
                )
            }
            return true
        }

        when {
            ReaderUtils.isBlogPreviewUrl(url) -> onBlogPreviewUrlClick(url)
            ReaderUtils.isTagUrl(url) -> viewModel.onTagItemClicked(ReaderUtils.getTagFromTagUrl(url))
            isFile(url) -> onFileDownloadClick(url)
            else -> {
                val openUrlType = if (shouldOpenExternal(url)) OpenUrlType.EXTERNAL else OpenUrlType.INTERNAL
                ReaderActivityLauncher.openUrl(requireActivity(), url, openUrlType)
            }
        }

        return true
    }

    // if this is a "wordpress://blogpreview?" link, show blog preview for the blog - this is
    // used for Discover posts that highlight a blog
    private fun onBlogPreviewUrlClick(url: String) {
        val siteId = ReaderUtils.getBlogIdFromBlogPreviewUrl(url)
        if (siteId != 0L) {
            val isFollowed = viewModel.post?.isFollowedByCurrentUser == true
            ReaderActivityLauncher.showReaderBlogPreview(
                requireContext(),
                siteId,
                isFollowed,
                ReaderTracker.SOURCE_POST_DETAIL,
                readerTracker
            )
        }
    }

    override fun onPageJumpClick(pageJump: String): Boolean {
        readerTracker.track(AnalyticsTracker.Stat.READER_ARTICLE_PAGE_JUMP_TAPPED)
        scrollToElement(pageJump) {
            ToastUtils.showToast(activity, R.string.reader_toast_err_page_jump_not_found)
        }
        return true
    }

    /**
     * If the URL is a same-page fragment link for the current post
     * (e.g., a footnote), returns the fragment identifier.
     * Returns null otherwise.
     */
    private fun getPostUrlFragment(url: String): String? {
        val postUrl = viewModel.post?.url?.trimEnd('/')
        val fragmentIndex = url.indexOf('#')
        val hasFragment = !postUrl.isNullOrEmpty()
            && fragmentIndex in 0 until url.length - 1
            && url.substring(0, fragmentIndex).trimEnd('/') == postUrl
        return if (hasFragment) url.substring(fragmentIndex + 1) else null
    }

    private fun scrollToElement(elementId: String, onNotFound: () -> Unit) {
        val safeId = elementId.replace("\\", "\\\\").replace("'", "\\'")
        readerWebView.evaluateJavascript(
            "(function(){var e=document.getElementById('$safeId');" +
                "return e?e.getBoundingClientRect().top+window.pageYOffset:-1})()"
        ) { result ->
            val offsetTop = result?.toDoubleOrNull()?.toInt() ?: -1
            if (offsetTop >= 0) {
                chrome.scrollConnection.hide()
                var webViewTop = 0
                var v: View? = readerWebView
                while (v != null && v != scrollView) {
                    webViewTop += v.top
                    v = v.parent as? View
                }
                val yOffset = webViewTop +
                    (resources.displayMetrics.density * offsetTop).toInt()
                scrollView.smoothScrollTo(0, yOffset)
            } else {
                onNotFound()
            }
        }
    }

    /*
     * returns True if the passed URL should be opened in the external browser app
     */
    @Suppress("ReturnCount")
    private fun shouldOpenExternal(url: String): Boolean {
        // open YouTube videos in external app so they launch the YouTube player
        if (ReaderVideoUtils.isYouTubeVideoLink(url)) {
            return true
        }

        // Open Stories links in external browser so they have more fullscreen play real estate
        if (url.toUri().queryParameterNames.any { it.contains("wp-story") }) {
            return true
        }

        // if the mime type starts with "application" open it externally - this will either
        // open it in the associated app or the default browser (which will enable the user
        // to download it)
        return isFile(url)

        // open all other urls using an AuthenticatedWebViewActivity
    }

    private fun isFile(url: String): Boolean {
        val mimeType = UrlUtils.getUrlMimeType(url)
        return mimeType != null && mimeType.startsWith("application")
    }

    override fun onImageUrlClick(imageUrl: String, view: View, x: Int, y: Int): Boolean {
        readerTracker.track(AnalyticsTracker.Stat.READER_ARTICLE_IMAGE_TAPPED)
        showPhotoViewerWithGalleryCheck(imageUrl, view, x, y)
        return true
    }

    override fun onFileDownloadClick(fileUrl: String): Boolean {
        readerTracker.track(AnalyticsTracker.Stat.READER_ARTICLE_FILE_DOWNLOAD_TAPPED)
        return if (activity != null &&
            PermissionUtils.checkAndRequestFileDownloadPermission(
                this,
                READER_FILE_DOWNLOAD_PERMISSION_REQUEST_CODE
            )
        ) {
            readerFileDownloadManager.downloadFile(fileUrl)
            true
        } else {
            fileForDownload = fileUrl
            false
        }
    }

    @Suppress("OVERRIDE_DEPRECATION", "ComplexCondition")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        val activity = activity
        if (activity != null &&
            requestCode == READER_FILE_DOWNLOAD_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            fileForDownload?.let { readerFileDownloadManager.downloadFile(it) }
        }
        fileForDownload = null
    }

    fun pauseWebView() {
        if (view == null) {
            AppLog.w(T.READER, "reader post detail > attempt to pause null webView")
        } else if (!isWebViewPaused) {
            AppLog.d(T.READER, "reader post detail > pausing webView")
            readerWebView.hideCustomView()
            readerWebView.onPause()
            isWebViewPaused = true
        }
    }

    fun resumeWebViewIfPaused() {
        if (view == null) {
            AppLog.w(T.READER, "reader post detail > attempt to resume null webView")
        } else if (isWebViewPaused) {
            AppLog.d(T.READER, "reader post detail > resuming paused webView")
            readerWebView.onResume()
            isWebViewPaused = false
        }
    }

    override fun onScrollUp(distanceY: Float) {
        // Do nothing
    }

    override fun onScrollDown(distanceY: Float) {
        // Do nothing
    }

    override fun onScrollCompleted() {
        trackRelatedPostsIfShowing()
    }

    companion object {
        private const val ON_PAGE_FINISHED_DELAY_MILLIS = 300L

        /**
         * How long the render spinner may stay up without the WebView producing content
         * before we hide it anyway. Generous enough not to pre-empt a slow but working
         * render, short enough that the screen can't look stuck indefinitely.
         */
        private const val RENDER_WATCHDOG_MS = 15_000L

        /**
         * How long to wait for the body to paint once the document has finished loading. Several
         * times the delay measured between onPageFinished and the WebView gaining height, while
         * still clearing the spinner promptly for a post whose body is empty and never will.
         */
        private const val PAINT_GRACE_MS = 2_000L

        private const val ARG_SLUG_FAILURE_STATUS = "slug_failure_status"
        private const val CUSTOM_VIEW_BACKGROUND = 0x80000000.toInt()
        private const val NOTIFICATIONS_BOTTOM_SHEET_TAG = "NOTIFICATIONS_BOTTOM_SHEET_TAG"

        fun newInstance(blogId: Long, postId: Long): ReaderPostDetailFragment {
            return newInstance(false, blogId, postId, null, 0, false, null, null, null)
        }

        @Suppress("LongParameterList")
        fun newInstance(
            isFeed: Boolean,
            blogId: Long,
            postId: Long,
            directOperation: DirectOperation?,
            commentId: Int,
            isRelatedPost: Boolean,
            interceptedUri: String?,
            postListType: ReaderPostListType?,
            slugFailureStatus: Int?
        ): ReaderPostDetailFragment {
            AppLog.d(T.READER, "reader post detail > newInstance")

            val args = Bundle()
            args.putBoolean(ReaderConstants.ARG_IS_FEED, isFeed)
            args.putLong(ReaderConstants.ARG_BLOG_ID, blogId)
            args.putLong(ReaderConstants.ARG_POST_ID, postId)
            args.putBoolean(ReaderConstants.ARG_IS_RELATED_POST, isRelatedPost)
            args.putSerializable(ReaderConstants.ARG_DIRECT_OPERATION, directOperation)
            args.putInt(ReaderConstants.ARG_COMMENT_ID, commentId)
            args.putString(ReaderConstants.ARG_INTERCEPTED_URI, interceptedUri)
            if (postListType != null) {
                args.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, postListType)
            }
            slugFailureStatus?.let { args.putInt(ARG_SLUG_FAILURE_STATUS, it) }

            val fragment = ReaderPostDetailFragment()
            fragment.arguments = args

            return fragment
        }
    }
}
