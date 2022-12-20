package org.wordpress.android.ui.reader

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.ImageView.ScaleType.CENTER_CROP
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ListPopupWindow
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.ElevationOverlayProvider
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.databinding.ReaderFragmentPostDetailBinding
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
import org.wordpress.android.ui.avatars.AVATAR_LEFT_OFFSET_DIMEN
import org.wordpress.android.ui.avatars.AvatarItemDecorator
import org.wordpress.android.ui.avatars.TrainOfAvatarsAdapter
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem
import org.wordpress.android.ui.engagement.EngagementNavigationSource
import org.wordpress.android.ui.main.SitePickerActivity
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.ui.media.MediaPreviewActivity
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.ReaderActivityLauncher.OpenUrlType
import org.wordpress.android.ui.reader.ReaderActivityLauncher.PhotoViewerOption
import org.wordpress.android.ui.reader.ReaderPostPagerActivity.DirectOperation
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.actions.ReaderActions
import org.wordpress.android.ui.reader.actions.ReaderPostActions
import org.wordpress.android.ui.reader.adapters.CommentSnippetAdapter
import org.wordpress.android.ui.reader.adapters.ReaderMenuAdapter
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource.DIRECT_OPERATION
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource.READER_POST_DETAILS_COMMENTS
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.PrimaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.reader.utils.ReaderVideoUtils
import org.wordpress.android.ui.reader.viewmodels.ConversationNotificationsViewModel
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.CommentSnippetUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.TrainOfFacesUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ErrorUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.LoadingUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ReaderPostDetailsUiState
import org.wordpress.android.ui.reader.views.ReaderIconCountView
import org.wordpress.android.ui.reader.views.ReaderPostDetailsHeaderViewUiStateBuilder
import org.wordpress.android.ui.reader.views.ReaderSimplePostContainerView
import org.wordpress.android.ui.reader.views.ReaderWebView
import org.wordpress.android.ui.reader.views.ReaderWebView.ReaderCustomViewListener
import org.wordpress.android.ui.reader.views.ReaderWebView.ReaderWebViewPageFinishedListener
import org.wordpress.android.ui.reader.views.ReaderWebView.ReaderWebViewUrlClickListener
import org.wordpress.android.ui.reader.views.uistates.CommentSnippetItemState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.util.JetpackBrandingUtils.Screen.READER_POST_DETAIL
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.PermissionUtils
import org.wordpress.android.util.RtlUtils
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.WPPermissionUtils.READER_FILE_DOWNLOAD_PERMISSION_REQUEST_CODE
import org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper
import org.wordpress.android.util.config.CommentsSnippetFeatureConfig
import org.wordpress.android.util.config.LikesEnhancementsFeatureConfig
import org.wordpress.android.util.extensions.getColorFromAttribute
import org.wordpress.android.util.extensions.isDarkTheme
import org.wordpress.android.util.extensions.setVisible
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.PHOTO
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.observeEvent
import org.wordpress.android.widgets.WPScrollView
import org.wordpress.android.widgets.WPScrollView.ScrollDirectionListener
import org.wordpress.android.widgets.WPSnackbar
import org.wordpress.android.widgets.WPTextView
import java.net.HttpURLConnection
import java.util.EnumSet
import javax.inject.Inject

@AndroidEntryPoint
class ReaderPostDetailFragment : ViewPagerFragment(),
        WPMainActivity.OnActivityBackPressedListener,
        ScrollDirectionListener,
        ReaderCustomViewListener,
        ReaderWebViewPageFinishedListener,
        ReaderWebViewUrlClickListener,
        PrivateAtCookieProgressDialogOnDismissListener,
        ReaderInterfaces.AutoHideToolbarListener {
    private var postId: Long = 0
    private var blogId: Long = 0
    private var directOperation: DirectOperation? = null
    private var commentId: Int = 0
    private var interceptedUri: String? = null
    private var renderer: ReaderPostRenderer? = null
    private var postListType: ReaderPostListType = ReaderTypes.DEFAULT_POST_LIST_TYPE

    private val postHistory = ReaderPostHistory()

    private var bookmarksSavedLocallyDialog: AlertDialog? = null
    private var moreMenuPopup: ListPopupWindow? = null
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper
    private lateinit var scrollView: WPScrollView
    private lateinit var layoutFooter: ViewGroup
    private lateinit var readerWebView: ReaderWebView

    private lateinit var likeFacesTrain: View
    private lateinit var likeProgressBar: ProgressBar
    private lateinit var likeEmptyStateText: TextView
    private lateinit var likeFacesRecycler: RecyclerView

    private lateinit var commentsSnippetContainer: View
    private lateinit var followConversationContainer: View
    private lateinit var commentsNumTitle: TextView
    private lateinit var commentSnippetRecycler: RecyclerView

    private lateinit var excerptFooter: ViewGroup
    private lateinit var textExcerptFooter: TextView

    private lateinit var signInButton: WPTextView

    private lateinit var appBar: AppBarLayout
    private lateinit var toolBar: Toolbar

    private lateinit var globalRelatedPostsView: ReaderSimplePostContainerView
    private lateinit var localRelatedPostsView: ReaderSimplePostContainerView

    private var postSlugsResolutionUnderway: Boolean = false
    private var hasAlreadyUpdatedPost: Boolean = false
    private var isWebViewPaused: Boolean = false

    private var isRelatedPost: Boolean = false

    private var hasTrackedGlobalRelatedPosts: Boolean = false
    private var hasTrackedLocalRelatedPosts: Boolean = false

    private var errorMessage: String? = null

    private var fileForDownload: String? = null

    private val viewModel: ReaderPostDetailViewModel by viewModels()
    private lateinit var conversationViewModel: ConversationNotificationsViewModel

    @Inject internal lateinit var accountStore: AccountStore
    @Inject internal lateinit var siteStore: SiteStore
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var readerFileDownloadManager: ReaderFileDownloadManager
    @Inject internal lateinit var privateAtomicCookie: PrivateAtomicCookie
    @Inject internal lateinit var readerCssProvider: ReaderCssProvider
    @Inject internal lateinit var imageManager: ImageManager
    @Inject lateinit var postDetailsHeaderViewUiStateBuilder: ReaderPostDetailsHeaderViewUiStateBuilder
    @Inject lateinit var readerUtilsWrapper: ReaderUtilsWrapper
    @Inject lateinit var viewModelFactory: Factory
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var readerTracker: ReaderTracker
    @Inject lateinit var likesEnhancementsFeatureConfig: LikesEnhancementsFeatureConfig
    @Inject lateinit var contextProvider: ContextProvider
    @Inject lateinit var commentsSnippetFeatureConfig: CommentsSnippetFeatureConfig
    @Inject lateinit var jetpackBrandingUtils: JetpackBrandingUtils

    private val mSignInClickListener = View.OnClickListener {
        EventBus.getDefault()
                .post(ReaderEvents.DoSignIn())
    }

    val isCustomViewShowing: Boolean
        get() = view != null && readerWebView.isCustomViewShowing

    private val appBarLayoutOffsetChangedListener =
            AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
                val collapsingToolbarLayout = appBarLayout
                        .findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar)
                val toolbar = appBarLayout.findViewById<Toolbar>(R.id.toolbar_main)

                context?.let { context ->
                    val menu: Menu = toolbar.menu
                    val menuBrowse: MenuItem? = menu.findItem(R.id.menu_browse)
                    val menuShare: MenuItem? = menu.findItem(R.id.menu_share)
                    val menuMore: MenuItem? = menu.findItem(R.id.menu_more)

                    val collapsingToolbarHeight = collapsingToolbarLayout.height
                    val isCollapsed = (collapsingToolbarHeight + verticalOffset) <=
                            collapsingToolbarLayout.scrimVisibleHeightTrigger
                    val isDarkTheme = context.resources.configuration.isDarkTheme()

                    val colorAttr = if (isCollapsed || isDarkTheme) {
                        R.attr.colorOnSurface
                    } else {
                        R.attr.colorSurface
                    }
                    val color = context.getColorFromAttribute(colorAttr)
                    val colorFilter = BlendModeColorFilterCompat
                            .createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_ATOP)

                    toolbar.setTitleTextColor(color)
                    toolbar.navigationIcon?.colorFilter = colorFilter

                    menuBrowse?.icon?.colorFilter = colorFilter
                    menuShare?.icon?.colorFilter = colorFilter
                    menuMore?.icon?.colorFilter = colorFilter
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        if (savedInstanceState != null) {
            postHistory.restoreInstance(savedInstanceState)
        }
    }

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)
        if (args != null) {
            blogId = args.getLong(ReaderConstants.ARG_BLOG_ID)
            postId = args.getLong(ReaderConstants.ARG_POST_ID)
            directOperation = args.getSerializable(ReaderConstants.ARG_DIRECT_OPERATION) as? DirectOperation
            commentId = args.getInt(ReaderConstants.ARG_COMMENT_ID)
            isRelatedPost = args.getBoolean(ReaderConstants.ARG_IS_RELATED_POST)
            interceptedUri = args.getString(ReaderConstants.ARG_INTERCEPTED_URI)
            if (args.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                this.postListType = args.getSerializable(ReaderConstants.ARG_POST_LIST_TYPE) as ReaderPostListType
            }
            postSlugsResolutionUnderway = args.getBoolean(ReaderConstants.KEY_POST_SLUGS_RESOLUTION_UNDERWAY)
        }
    }

    override fun getScrollableViewForUniqueIdProvision(): View? {
        return scrollView
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.reader_fragment_post_detail, container, false)

        initSwipeRefreshLayout(view)
        initAppBar(view)
        initScrollView(view)
        initWebView(view)
        initLikeFacesTrain(view)
        initCommentSnippetView(view)
        initExcerptFooter(view)
        initRelatedPostsView(view)
        initLayoutFooter(view)
        initSignInButton(view)
        initProgressView(view)

        return view
    }

    private fun initSwipeRefreshLayout(view: View) {
        val swipeRefreshLayout = view.findViewById<CustomSwipeRefreshLayout>(R.id.swipe_to_refresh)

        // this fragment hides/shows toolbar with scrolling, which messes up ptr animation position
        // so we have to set it manually
        val swipeToRefreshOffset = resources.getDimensionPixelSize(R.dimen.toolbar_content_offset)
        swipeRefreshLayout.setProgressViewOffset(false, 0, swipeToRefreshOffset)

        swipeToRefreshHelper = buildSwipeToRefreshHelper(swipeRefreshLayout) {
            if (isAdded) {
                if (commentsSnippetFeatureConfig.isEnabled()) {
                    conversationViewModel.onRefresh()
                }
                updatePost()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun initAppBar(view: View) {
        appBar = view.findViewById(R.id.appbar_with_collapsing_toolbar_layout)
        toolBar = appBar.findViewById(R.id.toolbar_main)

        toolBar.setVisible(true)
        appBar.addOnOffsetChangedListener(appBarLayoutOffsetChangedListener)

        // Fixes collapsing toolbar layout being obscured by the status bar when drawn behind it
        ViewCompat.setOnApplyWindowInsetsListener(appBar) { _: View, insets: WindowInsetsCompat ->
            val insetTop = insets.systemWindowInsetTop
            if (insetTop > 0) {
                toolBar.setPadding(0, insetTop, 0, 0)
            }
            insets.consumeSystemWindowInsets()
        }

        // Fixes viewpager not displaying menu items for first fragment
        toolBar.inflateMenu(R.menu.reader_detail)

        // for related posts, show an X in the toolbar which closes the activity
        if (isRelatedPost) {
            toolBar.setNavigationIcon(R.drawable.ic_cross_white_24dp)
            toolBar.setNavigationOnClickListener { requireActivity().finish() }
            toolBar.setTitle(R.string.reader_title_related_post_detail)
        } else {
            toolBar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
            toolBar.setNavigationOnClickListener { requireActivity().onBackPressed() }
        }
    }

    private fun initScrollView(view: View) {
        scrollView = view.findViewById(R.id.scroll_view_reader)
        scrollView.setScrollDirectionListener(this)
        scrollView.visibility = View.INVISIBLE
    }

    private fun initWebView(view: View) {
        // setup the ReaderWebView
        readerWebView = view.findViewById(R.id.reader_webview)
        readerWebView.setCustomViewListener(this)
        readerWebView.setUrlClickListener(this)
        readerWebView.setPageFinishedListener(this)
    }

    private fun initLikeFacesTrain(view: View) {
        likeFacesTrain = view.findViewById(R.id.liker_faces_container)
        likeFacesRecycler = view.findViewById(R.id.likes_recycler)
        likeProgressBar = view.findViewById(R.id.progress_bar)
        likeEmptyStateText = view.findViewById(R.id.empty_state_text)
    }

    private fun initCommentSnippetView(view: View) {
        commentsSnippetContainer = view.findViewById(R.id.comments_snippet)
        commentsNumTitle = view.findViewById(R.id.comments_number_title)
        followConversationContainer = view.findViewById(R.id.follow_conversation_container)
        commentSnippetRecycler = view.findViewById(R.id.comments_recycler)
    }

    private fun initExcerptFooter(view: View) {
        excerptFooter = view.findViewById(R.id.excerpt_footer)
        textExcerptFooter = excerptFooter.findViewById(R.id.text_excerpt_footer)
    }

    private fun initRelatedPostsView(view: View) {
        val relatedPostsContainer = view.findViewById<View>(R.id.container_related_posts)
        globalRelatedPostsView = relatedPostsContainer.findViewById(R.id.related_posts_view_global)
        localRelatedPostsView = relatedPostsContainer.findViewById(R.id.related_posts_view_local)
    }

    private fun initLayoutFooter(view: View) {
        layoutFooter = view.findViewById(R.id.layout_post_detail_footer)
        val elevationOverlayProvider = ElevationOverlayProvider(layoutFooter.context)
        val appbarElevation = resources.getDimension(R.dimen.appbar_elevation)
        val elevatedSurfaceColor = elevationOverlayProvider.compositeOverlayWithThemeSurfaceColorIfNeeded(
                appbarElevation
        )
        layoutFooter.setBackgroundColor(elevatedSurfaceColor)
        layoutFooter.visibility = View.INVISIBLE
    }

    private fun initSignInButton(view: View) {
        signInButton = view.findViewById(R.id.nux_sign_in_button)
        signInButton.setOnClickListener(mSignInClickListener)
    }

    private fun initProgressView(view: View) {
        val progress = view.findViewById<ProgressBar>(R.id.progress_loading)
        if (postSlugsResolutionUnderway) {
            progress.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        if (isVisible) {
            replaceActivityToolbarWithCollapsingToolbar()
        }
    }

    private fun replaceActivityToolbarWithCollapsingToolbar() {
        val activity = activity as? AppCompatActivity
        activity?.supportActionBar?.hide()

        toolBar.setVisible(true)
        activity?.setSupportActionBar(toolBar)

        activity?.supportActionBar?.setDisplayShowTitleEnabled(isRelatedPost)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = ReaderFragmentPostDetailBinding.bind(view)

        initLikeFacesRecycler(savedInstanceState)
        initCommentSnippetRecycler(savedInstanceState)
        initViewModel(binding, savedInstanceState)
        restoreState(savedInstanceState)
        setHasOptionsMenu(true)

        showPost()
    }

    private fun initLikeFacesRecycler(savedInstanceState: Bundle?) {
        if (!likesEnhancementsFeatureConfig.isEnabled()) return
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        savedInstanceState?.getParcelable<Parcelable>(ReaderPostDetailFragment.KEY_LIKERS_LIST_STATE)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        likeFacesRecycler.addItemDecoration(
                AvatarItemDecorator(
                RtlUtils.isRtl(activity),
                contextProvider.getContext(),
                AVATAR_LEFT_OFFSET_DIMEN)
        )

        likeFacesRecycler.layoutManager = layoutManager
    }

    private fun initCommentSnippetRecycler(savedInstanceState: Bundle?) {
        if (!commentsSnippetFeatureConfig.isEnabled()) return
        val layoutManager = LinearLayoutManager(activity)

        savedInstanceState?.getParcelable<Parcelable>(ReaderPostDetailFragment.KEY_COMMENTS_SNIPPET_LIST_STATE)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        commentSnippetRecycler.layoutManager = layoutManager
    }

    private fun initViewModel(binding: ReaderFragmentPostDetailBinding, savedInstanceState: Bundle?) {
        conversationViewModel = ViewModelProvider(this, viewModelFactory).get(
                ConversationNotificationsViewModel::class.java
        )

        initObservers(binding)

        val bundle = savedInstanceState ?: arguments
        val isFeed = bundle?.getBoolean(ReaderConstants.ARG_IS_FEED) ?: false
        val interceptedUri = bundle?.getString(ReaderConstants.ARG_INTERCEPTED_URI)

        if (commentsSnippetFeatureConfig.isEnabled()) {
            conversationViewModel.start(blogId, postId, READER_POST_DETAILS_COMMENTS)
        }
        viewModel.start(isRelatedPost = isRelatedPost, isFeed = isFeed, interceptedUri = interceptedUri)
    }

    @Suppress("LongMethod")
    private fun initObservers(binding: ReaderFragmentPostDetailBinding) {
        viewModel.uiState.observe(viewLifecycleOwner, {
            uiHelpers.updateVisibility(binding.textError, it.errorVisible)
            uiHelpers.updateVisibility(binding.progressLoading, it.loadingVisible)
            when (it) {
                is LoadingUiState -> Unit // Do Nothing
                is ReaderPostDetailsUiState -> renderUiState(it, binding)
                is ErrorUiState -> {
                    uiHelpers.updateVisibility(signInButton, it.signInButtonVisibility)
                    val message = it.message?.let { msg -> uiHelpers.getTextOfUiString(requireContext(), msg) }
                    showError(message?.toString())
                }
            }
        })

        if (likesEnhancementsFeatureConfig.isEnabled()) {
            viewModel.likesUiState.observe(viewLifecycleOwner, { state ->
                manageLikesUiState(state)
            })
        }

        viewModel.refreshPost.observeEvent(viewLifecycleOwner, {} /* Do nothing */)

        viewModel.snackbarEvents.observeEvent(viewLifecycleOwner, { it.showSnackbar(binding) })

        viewModel.navigationEvents.observeEvent(viewLifecycleOwner, { it.handleNavigationEvent() })

        if (commentsSnippetFeatureConfig.isEnabled()) {
            conversationViewModel.snackbarEvents.observe(viewLifecycleOwner, { event ->
                if (!isAdded) return@observe

                val fm: FragmentManager = childFragmentManager
                val bottomSheet =
                        fm.findFragmentByTag(NOTIFICATIONS_BOTTOM_SHEET_TAG) as? CommentNotificationsBottomSheetFragment
                if (bottomSheet != null) return@observe
                event.applyIfNotHandled {
                    this.showSnackbar(binding)
                }
            })

            conversationViewModel.showBottomSheetEvent.observeEvent(viewLifecycleOwner, { showBottomSheetData ->
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
            })

            conversationViewModel.updateFollowUiState.observe(viewLifecycleOwner, { uiState ->
                manageFollowConversationUiState(uiState, binding)
            })

            viewModel.commentSnippetState.observe(viewLifecycleOwner, { state ->
                manageCommentSnippetUiState(state)
            })
        }

        viewModel.showJetpackPoweredBottomSheet.observeEvent(viewLifecycleOwner) {
            JetpackPoweredBottomSheetFragment
                    .newInstance()
                    .show(childFragmentManager, JetpackPoweredBottomSheetFragment.TAG)
        }
    }

    private fun manageFollowConversationUiState(
        uiState: FollowConversationUiState,
        binding: ReaderFragmentPostDetailBinding
    ) {
        if (!isAdded) return

        with(binding) {
            val bellItem = commentsSnippet.bellIcon
            val followContainer = commentsSnippet.followConversationContainer

            val shimmerView: ShimmerFrameLayout = commentsSnippet.shimmerViewContainer
            val followText = commentsSnippet.followConversation
            followText.setOnClickListener(
                    if (uiState.onFollowTapped != null) {
                        View.OnClickListener { uiState.onFollowTapped.invoke() }
                    } else {
                        null
                    }
            )
            bellItem.setOnClickListener {
                uiState.onManageNotificationsTapped.invoke()
            }
            followContainer.isEnabled = uiState.flags.isMenuEnabled
            followText.isEnabled = uiState.flags.isMenuEnabled
            bellItem.isEnabled = uiState.flags.isMenuEnabled
            if (uiState.flags.showMenuShimmer) {
                if (!shimmerView.isShimmerVisible) {
                    shimmerView.showShimmer(true)
                } else if (!shimmerView.isShimmerStarted) {
                    shimmerView.startShimmer()
                }
            } else {
                shimmerView.hideShimmer()
            }
            shimmerView.visibility = if (uiState.flags.isFollowMenuVisible) View.VISIBLE else View.GONE
            bellItem.visibility = if (uiState.flags.isBellMenuVisible) View.VISIBLE else View.GONE
        }
    }

    private fun manageLikesUiState(state: TrainOfFacesUiState) {
        if (!isAdded) return

        with(requireActivity()) {
            if (this.isFinishing) return@with

            val shouldSkipAnimation = likeFacesTrain.visibility == View.GONE && state.goingToShowFaces

            setupLikeFacesTrain(
                    state.engageItemsList,
                    state.showLoading,
                    shouldSkipAnimation
            )

            likeProgressBar.visibility = if (state.showLoading) View.VISIBLE else View.GONE
            likeFacesTrain.visibility = if (state.showLikeFacesTrainContainer) View.VISIBLE else View.GONE

            if (state.showEmptyState) {
                uiHelpers.setTextOrHide(likeEmptyStateText, state.emptyStateTitle?.let {
                    getString(R.string.like_faces_error_loading_message, uiHelpers.getTextOfUiString(this, it))
                })
                likeEmptyStateText.visibility = View.VISIBLE
            } else {
                likeEmptyStateText.visibility = View.GONE
            }

            likeFacesTrain.contentDescription = uiHelpers.getTextOfUiString(
                    contextProvider.getContext(),
                    state.contentDescription
            )

            likeFacesTrain.setOnClickListener {
                if (!isAdded) return@setOnClickListener

                viewModel.onLikeFacesClicked()
            }
        }
    }

    private fun manageCommentSnippetUiState(state: CommentSnippetUiState) {
        if (!isAdded) return

        with(requireActivity()) {
            if (this.isFinishing) return@with

            uiHelpers.updateVisibility(commentsSnippetContainer, commentsSnippetFeatureConfig.isEnabled())
            uiHelpers.updateVisibility(followConversationContainer, state.showFollowConversation)
            commentsNumTitle.text = readerUtilsWrapper.getTextForCommentSnippet(state.commentsNumber)

            setupCommentSnippetAdapter(this, state.snippetItems)
        }
    }

    private fun setupCommentSnippetAdapter(context: Context, items: List<CommentSnippetItemState>) {
        val adapter = commentSnippetRecycler.adapter as? CommentSnippetAdapter ?: CommentSnippetAdapter(
                context,
                viewModel.post
        ).also {
            commentSnippetRecycler.adapter = it
        }

        val recyclerViewState = commentSnippetRecycler.layoutManager?.onSaveInstanceState()
        adapter.loadData(items)
        commentSnippetRecycler.layoutManager?.onRestoreInstanceState(recyclerViewState)
    }

    private fun setupLikeFacesTrain(items: List<TrainOfAvatarsItem>, loading: Boolean, shouldSkipAnimation: Boolean) {
        likeFacesRecycler.visibility = if (loading) View.GONE else View.VISIBLE

        if (shouldSkipAnimation) {
            likeFacesRecycler.itemAnimator = null
        } else if (likeFacesRecycler.itemAnimator == null) {
            likeFacesRecycler.itemAnimator = DefaultItemAnimator()
        }

        var adapter = likeFacesRecycler.adapter as? TrainOfAvatarsAdapter

        if (adapter == null) {
            adapter = TrainOfAvatarsAdapter(
                    imageManager,
                    uiHelpers
            )

            likeFacesRecycler.adapter = adapter
        }

        val recyclerViewState = likeFacesRecycler.layoutManager?.onSaveInstanceState()
        adapter.loadData(items)
        likeFacesRecycler.layoutManager?.onRestoreInstanceState(recyclerViewState)
    }

    private fun renderUiState(state: ReaderPostDetailsUiState, binding: ReaderFragmentPostDetailBinding) {
        onPostExecuteShowPost()

        if (jetpackBrandingUtils.shouldShowJetpackBranding()) {
            binding.jetpackBadge.root.isVisible = true
            if (jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) {
                binding.jetpackBadge.jetpackPoweredBadge.setOnClickListener {
                    jetpackBrandingUtils.trackBadgeTapped(READER_POST_DETAIL)
                    viewModel.showJetpackPoweredBottomSheet()
                }
            }
        }

        binding.headerView.updatePost(state.headerUiState)
        showOrHideMoreMenu(state)

        updateFeaturedImage(state.featuredImageUiState, binding)
        updateExcerptFooter(state.excerptFooterUiState)

        with(binding.layoutPostDetailFooter) {
            updateActionButton(state.postId, state.blogId, state.actions.likeAction, countLikes)
            updateActionButton(state.postId, state.blogId, state.actions.reblogAction, reblog)
            updateActionButton(state.postId, state.blogId, state.actions.commentsAction, countComments)
            updateActionButton(state.postId, state.blogId, state.actions.bookmarkAction, bookmark)
        }

        state.localRelatedPosts?.let { showRelatedPosts(it) }
        state.globalRelatedPosts?.let { showRelatedPosts(it) }
    }

    // TODO: Update using UiState/ NavigationEvent
    @Suppress("ForbiddenComment")
    private fun onPostExecuteShowPost() {
        // make sure options menu reflects whether we now have a post
        activity?.invalidateOptionsMenu()

        viewModel.post?.let {
            if (handleDirectOperation()) return

            scrollView.visibility = View.VISIBLE
            layoutFooter.visibility = View.VISIBLE
        }
    }

    @Suppress("ComplexMethod", "LongMethod")
    private fun ReaderNavigationEvents.handleNavigationEvent() {
        when (this) {
            is ReaderNavigationEvents.ShowMediaPreview -> MediaPreviewActivity
                    .showPreview(requireContext(), site, featuredImage)

            is ReaderNavigationEvents.ShowPostsByTag -> ReaderActivityLauncher.showReaderTagPreview(
                    context,
                    this.tag,
                    ReaderTracker.SOURCE_POST_DETAIL,
                    readerTracker
            )

            is ReaderNavigationEvents.ShowBlogPreview -> ReaderActivityLauncher.showReaderBlogOrFeedPreview(
                    context,
                    this.siteId,
                    this.feedId,
                    this.isFollowed,
                    ReaderTracker.SOURCE_POST_DETAIL,
                    readerTracker
            )

            is ReaderNavigationEvents.SharePost -> ReaderActivityLauncher.sharePost(context, post)

            is ReaderNavigationEvents.OpenPost -> ReaderActivityLauncher.openPost(context, post)

            is ReaderNavigationEvents.ShowReportPost ->
                ReaderActivityLauncher.openUrl(context, readerUtilsWrapper.getReportPostUrl(url), OpenUrlType.INTERNAL)

            is ReaderNavigationEvents.ShowReportUser -> ReaderActivityLauncher.openUrl(
                    context,
                    readerUtilsWrapper.getReportUserUrl(url, authorId),
                    OpenUrlType.INTERNAL
            )

            is ReaderNavigationEvents.ShowReaderComments -> ReaderActivityLauncher.showReaderCommentsForResult(
                    this@ReaderPostDetailFragment,
                    blogId,
                    postId,
                    this.source.sourceDescription
            )

            is ReaderNavigationEvents.ShowNoSitesToReblog -> ReaderActivityLauncher.showNoSiteToReblog(activity)

            is ReaderNavigationEvents.ShowSitePickerForResult ->
                ActivityLauncher
                        .showSitePickerForResult(this@ReaderPostDetailFragment, this.preselectedSite, this.mode)

            is ReaderNavigationEvents.OpenEditorForReblog ->
                ActivityLauncher.openEditorForReblog(activity, this.site, this.post, this.source)

            is ReaderNavigationEvents.ShowBookmarkedTab -> ActivityLauncher.viewSavedPostsListInReader(activity)

            is ReaderNavigationEvents.ShowBookmarkedSavedOnlyLocallyDialog -> showBookmarkSavedLocallyDialog(this)

            is ReaderNavigationEvents.OpenUrl -> ReaderActivityLauncher.openUrl(requireContext(), url)

            is ReaderNavigationEvents.ShowRelatedPostDetails ->
                showRelatedPostDetail(postId = this.postId, blogId = this.blogId)

            is ReaderNavigationEvents.ReplaceRelatedPostDetailsWithHistory ->
                replaceRelatedPostDetailWithHistory(postId = this.postId, blogId = this.blogId)

            is ReaderNavigationEvents.ShowPostInWebView -> showPostInWebView(post)
            is ReaderNavigationEvents.ShowEngagedPeopleList -> {
                ActivityLauncher.viewPostLikesListActivity(
                        activity,
                        this.siteId,
                        this.postId,
                        this.headerData,
                        EngagementNavigationSource.LIKE_READER_LIST
                )
            }
            is ReaderNavigationEvents.ShowPostDetail,
            is ReaderNavigationEvents.ShowVideoViewer,
            is ReaderNavigationEvents.ShowReaderSubs -> Unit // Do Nothing
        }
    }

    private fun updateFeaturedImage(
        state: ReaderPostDetailsUiState.ReaderPostFeaturedImageUiState?,
        binding: ReaderFragmentPostDetailBinding
    ) {
        val featuredImage = binding.appbarWithCollapsingToolbarLayout.featuredImage
        featuredImage.setVisible(state != null)
        state?.let {
            featuredImage.layoutParams.height = it.height
            it.url?.let { url ->
                imageManager.load(featuredImage, PHOTO, url, CENTER_CROP)
                featuredImage.setOnClickListener {
                    viewModel.onFeaturedImageClicked(blogId = state.blogId, featuredImageUrl = url)
                }
            }
        }
    }

    private fun updateExcerptFooter(state: ReaderPostDetailsUiState.ExcerptFooterUiState?) {
        // if we're showing just the excerpt, show a footer which links to the full post
        excerptFooter.setVisible(state != null)
        state?.let {
            uiHelpers.setTextOrHide(textExcerptFooter, state.visitPostExcerptFooterLinkText)
            textExcerptFooter.setOnClickListener {
                state.postLink?.let { link -> viewModel.onVisitPostExcerptFooterClicked(postLink = link) }
            }
        }
    }

    private fun updateActionButton(postId: Long, blogId: Long, state: PrimaryAction, view: View) {
        if (view is ReaderIconCountView) {
            view.setCount(state.count)
        }
        view.isEnabled = state.isEnabled
        view.isSelected = state.isSelected
        view.contentDescription = state.contentDescription?.let { uiHelpers.getTextOfUiString(view.context, it) }
        view.setOnClickListener { state.onClicked?.invoke(postId, blogId, state.type) }
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

    private fun showOrHideMoreMenu(
        state: ReaderPostDetailsUiState
    ) {
        val moreMenu: View? = toolBar.findViewById(R.id.menu_more)
        moreMenu?.let {
            state.moreMenuItems?.let {
                if (moreMenuPopup == null) {
                    createMoreMenuPopup(it, moreMenu)
                }
                moreMenuPopup?.show()
            } ?: moreMenuPopup?.dismiss()
        }
    }

    private fun createMoreMenuPopup(actions: List<ReaderPostCardAction>, v: View) {
        readerTracker.track(AnalyticsTracker.Stat.READER_ARTICLE_DETAIL_MORE_TAPPED)
        moreMenuPopup = ListPopupWindow(v.context)
        moreMenuPopup?.let {
            it.width = v.context.resources.getDimensionPixelSize(R.dimen.menu_item_width)
            it.setAdapter(ReaderMenuAdapter(v.context, uiHelpers, actions))
            it.setDropDownGravity(Gravity.END)
            it.anchorView = v
            it.isModal = true
            it.setOnItemClickListener { _, _, position, _ ->
                viewModel.onMoreMenuItemClicked(actions[position].type)
            }
            it.setOnDismissListener {
                viewModel.onMoreMenuDismissed()
                moreMenuPopup = null
            }
        }
    }

    private fun SnackbarMessageHolder.showSnackbar(binding: ReaderFragmentPostDetailBinding) {
        val snackbar = WPSnackbar.make(
                binding.layoutPostDetailContainer,
                uiHelpers.getTextOfUiString(requireContext(), this.message),
                Snackbar.LENGTH_LONG
        )
        if (this.buttonTitle != null) {
            snackbar.setAction(uiHelpers.getTextOfUiString(requireContext(), this.buttonTitle)) {
                this.buttonAction.invoke()
            }
        }
        snackbar.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (view != null) {
            readerWebView.destroy()
        }
        bookmarksSavedLocallyDialog?.dismiss()
        moreMenuPopup?.dismiss()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.reader_detail, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        // browse & share require the post to have a URL (some feed-based posts don't have one)
        val postHasUrl = viewModel.post?.hasUrl() == true
        val mnuBrowse = menu.findItem(R.id.menu_browse)
        if (mnuBrowse != null) {
            mnuBrowse.isVisible = postHasUrl || viewModel.interceptedUri != null
        }
        val mnuShare = menu.findItem(R.id.menu_share)
        if (mnuShare != null) {
            mnuShare.isVisible = postHasUrl
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_browse -> {
                if (viewModel.hasPost) {
                    readerTracker.track(AnalyticsTracker.Stat.READER_ARTICLE_VISITED)
                    ReaderActivityLauncher.openPost(context, viewModel.post)
                } else if (viewModel.interceptedUri != null) {
                    readerTracker.trackUri(AnalyticsTracker.Stat.DEEP_LINKED_FALLBACK, viewModel.interceptedUri!!)
                    ReaderActivityLauncher.openUrl(activity, viewModel.interceptedUri, OpenUrlType.EXTERNAL)
                    requireActivity().finish()
                }
                return true
            }
            R.id.menu_share -> {
                readerTracker.track(AnalyticsTracker.Stat.SHARED_ITEM)
                ReaderActivityLauncher.sharePost(context, viewModel.post)
                return true
            }
            R.id.menu_more -> {
                viewModel.onMoreButtonClicked()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
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
        outState.putBoolean(
                ReaderConstants.KEY_POST_SLUGS_RESOLUTION_UNDERWAY,
                postSlugsResolutionUnderway
        )
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

        if (!errorMessage.isNullOrEmpty()) {
            outState.putString(ReaderConstants.KEY_ERROR_MESSAGE, errorMessage)
        }

        if (commentsSnippetFeatureConfig.isEnabled()) {
            commentSnippetRecycler.layoutManager?.let {
                outState.putParcelable(KEY_COMMENTS_SNIPPET_LIST_STATE, it.onSaveInstanceState())
            }
        }

        super.onSaveInstanceState(outState)
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            blogId = it.getLong(ReaderConstants.ARG_BLOG_ID)
            postId = it.getLong(ReaderConstants.ARG_POST_ID)
            directOperation = it
                    .getSerializable(ReaderConstants.ARG_DIRECT_OPERATION) as? DirectOperation
            commentId = it.getInt(ReaderConstants.ARG_COMMENT_ID)
            isRelatedPost = it.getBoolean(ReaderConstants.ARG_IS_RELATED_POST)
            interceptedUri = it.getString(ReaderConstants.ARG_INTERCEPTED_URI)
            postSlugsResolutionUnderway = it.getBoolean(ReaderConstants.KEY_POST_SLUGS_RESOLUTION_UNDERWAY)
            hasAlreadyUpdatedPost = it.getBoolean(ReaderConstants.KEY_ALREADY_UPDATED)
            hasTrackedGlobalRelatedPosts = it.getBoolean(ReaderConstants.KEY_ALREADY_TRACKED_GLOBAL_RELATED_POSTS)
            hasTrackedLocalRelatedPosts = it.getBoolean(ReaderConstants.KEY_ALREADY_TRACKED_LOCAL_RELATED_POSTS)
            if (it.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                this.postListType = it.getSerializable(ReaderConstants.ARG_POST_LIST_TYPE) as ReaderPostListType
            }
            if (it.containsKey(ReaderConstants.KEY_ERROR_MESSAGE)) {
                errorMessage = it.getString(ReaderConstants.KEY_ERROR_MESSAGE)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
        EventBus.getDefault().register(this)
        activity?.registerReceiver(
                readerFileDownloadManager,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
        EventBus.getDefault().unregister(this)
        activity?.unregisterReceiver(readerFileDownloadManager)
    }

    /*
     * called by the activity when user hits the back button - returns true if the back button
     * is handled here and should be ignored by the activity
     */
    override fun onActivityBackPressed(): Boolean {
        return goBackInPostHistory()
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

        // hide views that would show info for the previous post - these will be re-displayed
        // with the correct info once the new post loads
        globalRelatedPostsView.visibility = View.GONE
        localRelatedPostsView.visibility = View.GONE
        if (likesEnhancementsFeatureConfig.isEnabled()) {
            likeFacesTrain.visibility = View.GONE
        }
        if (commentsSnippetFeatureConfig.isEnabled()) {
            commentsSnippetContainer.visibility = View.GONE
        }

        // clear the webView - otherwise it will remain scrolled to where the user scrolled to
        readerWebView.clearContent()

        // now show the passed post
        showPost()
    }

    /*
     * show the passed list of related posts - can be either global (related posts from
     * across wp.com) or local (related posts from the same site as the current post)
     */
    private fun showRelatedPosts(state: ReaderPostDetailsUiState.RelatedPostsUiState) {
        // different container views for global/local related posts
        val relatedPostsView = if (state.isGlobal) globalRelatedPostsView else localRelatedPostsView
        relatedPostsView.showPosts(state)

        // fade in this related posts view
        if (relatedPostsView.visibility != View.VISIBLE) {
            AniUtils.fadeIn(relatedPostsView, AniUtils.Duration.MEDIUM)
        }

        trackRelatedPostsIfShowing()
    }

    /*
     * track that related posts have loaded and are scrolled into view if we haven't
     * already tracked it
     */
    private fun trackRelatedPostsIfShowing() {
        if (!hasTrackedGlobalRelatedPosts && isVisibleAndScrolledIntoView(globalRelatedPostsView)) {
            hasTrackedGlobalRelatedPosts = true
            AppLog.d(T.READER, "reader post detail > global related posts rendered")
            globalRelatedPostsView.trackRailcarRender()
        }

        if (!hasTrackedLocalRelatedPosts && isVisibleAndScrolledIntoView(localRelatedPostsView)) {
            hasTrackedLocalRelatedPosts = true
            AppLog.d(T.READER, "reader post detail > local related posts rendered")
            localRelatedPostsView.trackRailcarRender()
        }
    }

    /*
     * returns True if the passed view is visible and has been scrolled into view - assumes
     * that the view is a child of mScrollView
     */
    private fun isVisibleAndScrolledIntoView(view: View?): Boolean {
        if (view != null && view.visibility == View.VISIBLE) {
            val scrollBounds = Rect()
            scrollView.getHitRect(scrollBounds)
            return view.getLocalVisibleRect(scrollBounds)
        }
        return false
    }

    private fun showRelatedPostDetail(postId: Long, blogId: Long) {
        ReaderActivityLauncher.showReaderPostDetail(
                activity,
                false,
                blogId,
                postId, null,
                0,
                true, null
        )
    }

    private fun replaceRelatedPostDetailWithHistory(postId: Long, blogId: Long) {
        viewModel.post?.let {
            postHistory.push(ReaderBlogIdPostId(it.blogId, it.postId))
            replacePost(blogId, postId, true)
        }
    }

    /*
     * if the fragment is maintaining a backstack of posts, navigate to the previous one
     */
    fun goBackInPostHistory(): Boolean {
        return if (!postHistory.isEmpty()) {
            val ids = postHistory.pop()
            replacePost(ids.blogId, ids.postId, true)
            true
        } else {
            false
        }
    }

    /*
     * get the latest version of this post
     */
    private fun updatePost() {
        if (!viewModel.hasPost || viewModel.post?.isWP == false) {
            setRefreshing(false)
            return
        }

        val resultListener = ReaderActions.UpdateResultListener { result ->
            val post = viewModel.post
            if (isAdded && post != null) {
                if (commentsSnippetFeatureConfig.isEnabled()) {
                    conversationViewModel.onUpdatePost(post.blogId, post.postId)
                    viewModel.onRefreshCommentsData(post.blogId, post.postId)
                }

                // if the post has changed, reload it from the db and update the like/comment counts
                if (result.isNewOrChanged) {
                    viewModel.post = ReaderPostTable.getBlogPost(post.blogId, post.postId, false)
                    viewModel.post?.let {
                        if (likesEnhancementsFeatureConfig.isEnabled()) {
                            viewModel.onRefreshLikersData(it)
                        }
                        viewModel.onUpdatePost(it)
                    }
                }

                setRefreshing(false)

                if (directOperation != null && directOperation == DirectOperation.POST_LIKE) {
                    doLikePost()
                }
            }
        }
        viewModel.post?.let { ReaderPostActions.updatePost(it, resultListener) }
    }

    private fun doLikePost() {
        if (!isAdded) {
            return
        }

        if (!accountStore.hasAccessToken()) {
            WPSnackbar.make(
                    requireView(), R.string.reader_snackbar_err_cannot_like_post_logged_out,
                    Snackbar.LENGTH_INDEFINITE
            ).setAction(R.string.sign_in, mSignInClickListener).show()
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
                            SitePickerActivity.KEY_SITE_LOCAL_ID,
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

        val postContent = viewModel.post?.text
        val isPrivatePost = viewModel.post?.isPrivate == true
        val options = EnumSet.noneOf(PhotoViewerOption::class.java)
        if (isPrivatePost) {
            options.add(PhotoViewerOption.IS_PRIVATE_IMAGE)
        }

        ReaderActivityLauncher.showReaderPhotoViewer(
                activity,
                imageUrl,
                postContent,
                sourceView,
                options,
                startX,
                startY
        )

        return true
    }

    /*
     * post slugs resolution to IDs has completed
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ReaderEvents.PostSlugsRequestCompleted) {
        postSlugsResolutionUnderway = false

        if (!isAdded) {
            return
        }

        val progress = requireView().findViewById<ProgressBar>(R.id.progress_loading)
        progress.visibility = View.GONE

        if (event.statusCode == HttpURLConnection.HTTP_OK) {
            replacePost(event.blogId, event.postId, false)
        } else {
            onRequestFailure(event.statusCode)
        }
    }

    private fun onRequestFailure(statusCode: Int) {
        if (!NetworkUtils.isNetworkAvailable(activity)) {
            showError(getString(R.string.no_network_message))
        } else {
            when (statusCode) {
                HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN ->
                    viewModel.onNotAuthorisedRequestFailure()

                HttpURLConnection.HTTP_NOT_FOUND -> showError(getString(R.string.reader_err_get_post_not_found))
                else -> showError(getString(R.string.reader_err_get_post_generic))
            }
        }
    }

    /*
     * shows an error message in the middle of the screen - used when requesting post fails
     */
    private fun showError(errorMessage: String?) {
        if (!isAdded) {
            return
        }

        val txtError = requireView().findViewById<TextView>(R.id.text_error)
        txtError.text = errorMessage

        context?.let {
            val icon: Drawable? = try {
                ContextCompat.getDrawable(it, R.drawable.ic_notice_48dp)
            } catch (e: Resources.NotFoundException) {
                AppLog.e(T.READER, "Drawable not found. See issue #11576", e)
                null
            }
            icon?.let {
                txtError.setCompoundDrawablesRelativeWithIntrinsicBounds(null, icon, null, null)
            }
        }

        if (errorMessage == null) {
            txtError.visibility = View.GONE
        } else if (txtError.visibility != View.VISIBLE) {
            AniUtils.fadeIn(txtError, AniUtils.Duration.MEDIUM)
        }
        this.errorMessage = errorMessage
    }

    private fun showPost() {
        if (postSlugsResolutionUnderway) {
            AppLog.w(T.READER, "reader post detail > post request already running")
            return
        }

        if (commentsSnippetFeatureConfig.isEnabled()) {
            conversationViewModel.onUpdatePost(blogId, postId)
        }

        viewModel.onShowPost(blogId = blogId, postId = postId)
    }

    @Suppress("unused", "DEPRECATION")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPrivateAtomicCookieFetched(event: OnPrivateAtomicCookieFetched) {
        if (!isAdded) {
            return
        }

        if (event.isError) {
            AppLog.e(T.READER, "Failed to load private AT cookie. $event.error.type - $event.error.message")
            WPSnackbar.make(
                    requireView(),
                    R.string.media_accessing_failed,
                    Snackbar.LENGTH_LONG
            ).show()
        } else {
            CookieManager.getInstance().setCookie(
                    privateAtomicCookie.getDomain(), privateAtomicCookie.getCookieContent()
            )
        }

        PrivateAtCookieRefreshProgressDialog.dismissIfNecessary(fragmentManager)
        renderer?.beginRender()
    }

    override fun onCookieProgressDialogCancelled() {
        if (!isAdded) {
            return
        }

        WPSnackbar.make(
                requireView(),
                R.string.media_accessing_failed,
                Snackbar.LENGTH_LONG
        ).show()
        renderer?.beginRender()
    }

    private fun handleDirectOperation() = when (directOperation) {
        DirectOperation.COMMENT_JUMP, DirectOperation.COMMENT_REPLY, DirectOperation.COMMENT_LIKE -> {
            viewModel.post?.let {
                ReaderActivityLauncher.showReaderComments(
                        activity, it.blogId, it.postId,
                        directOperation, commentId.toLong(), viewModel.interceptedUri,
                        DIRECT_OPERATION.sourceDescription
                )
            }

            activity?.finish()
            activity?.overridePendingTransition(0, 0)
            true
        }
        // Like needs to be handled "later" after the post has been updated from the server, nothing special to do here.
        DirectOperation.POST_LIKE -> false
        null -> false
    }

    @Suppress("DEPRECATION")
    private fun ReaderPostDetailFragment.showPostInWebView(post: ReaderPost) {
        readerWebView.setIsPrivatePost(post.isPrivate)
        readerWebView.setBlogSchemeIsHttps(UrlUtils.isHttps(post.blogUrl))
        renderer = ReaderPostRenderer(readerWebView, viewModel.post, readerCssProvider)

        // if the post is from private atomic site postpone render until we have a special access cookie
        if (post.isPrivateAtomic && privateAtomicCookie.isCookieRefreshRequired()) {
            PrivateAtCookieRefreshProgressDialog.showIfNecessary(fragmentManager, this@ReaderPostDetailFragment)
            requestPrivateAtomicCookie()
        } else if (post.isPrivateAtomic && privateAtomicCookie.exists()) {
            // make sure we add cookie to the cookie manager if it exists before starting render
            CookieManager.getInstance().setCookie(
                    privateAtomicCookie.getDomain(), privateAtomicCookie.getCookieContent()
            )
            renderer?.beginRender()
        } else {
            renderer?.beginRender()
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

        if (url != null && url == "about:blank") {
            // brief delay before showing related posts to give page time to render
            view.postDelayed(Runnable {
                if (!isAdded) {
                    return@Runnable
                }
                if (!hasAlreadyUpdatedPost) {
                    hasAlreadyUpdatedPost = true
                    updatePost()
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
            }, 300)
        } else {
            url?.let { AppLog.w(T.READER, "reader post detail > page finished - $it") }
        }
    }

    /*
     * return the container view that should host the full screen video
     */
    override fun onRequestCustomView(): ViewGroup? {
        return if (isAdded) {
            requireView().findViewById<View>(R.id.layout_custom_view_container) as ViewGroup
        } else {
            null
        }
    }

    /*
     * return the container view that should be hidden when full screen video is shown
     */
    override fun onRequestContentView(): ViewGroup? {
        return if (isAdded) {
            requireView().findViewById<View>(R.id.layout_post_detail_container) as ViewGroup
        } else {
            null
        }
    }

    override fun onCustomViewShown() {
        // full screen video has just been shown so hide the AppBar
        readerTracker.track(Stat.READER_ARTICLE_CUSTOM_VIEW_SHOWN)
        onShowHideToolbar(false)
    }

    override fun onCustomViewHidden() {
        // user returned from full screen video so re-display the AppBar
        readerTracker.track(Stat.READER_ARTICLE_CUSTOM_VIEW_HIDDEN)
        onShowHideToolbar(true)
    }

    fun hideCustomView() {
        if (view != null) {
            readerWebView.hideCustomView()
        }
    }

    override fun onUrlClick(url: String): Boolean {
        readerTracker.track(Stat.READER_ARTICLE_LINK_TAPPED)
        // if this is a "wordpress://blogpreview?" link, show blog preview for the blog - this is
        // used for Discover posts that highlight a blog
        if (ReaderUtils.isBlogPreviewUrl(url)) {
            val siteId = ReaderUtils.getBlogIdFromBlogPreviewUrl(url)
            if (siteId != 0L) {
                ReaderActivityLauncher.showReaderBlogPreview(
                        activity,
                        siteId,
                        viewModel.post?.isFollowedByCurrentUser,
                        ReaderTracker.SOURCE_POST_DETAIL,
                        readerTracker
                )
            }
            return true
        }

        if (isFile(url)) {
            onFileDownloadClick(url)
        } else {
            val openUrlType = if (shouldOpenExternal(url)) OpenUrlType.EXTERNAL else OpenUrlType.INTERNAL
            ReaderActivityLauncher.openUrl(activity, url, openUrlType)
        }
        return true
    }

    override fun onPageJumpClick(pageJump: String?): Boolean {
        readerTracker.track(Stat.READER_ARTICLE_PAGE_JUMP_TAPPED)
        val wasJsEnabled = readerWebView.settings.javaScriptEnabled

        readerWebView.settings.javaScriptEnabled = true

        readerWebView.evaluateJavascript("document.getElementById('$pageJump').offsetTop") { result ->
            // Note that 'result' can be the string 'null' in case the page jump identifier is not found on page
            val offsetTop = StringUtils.stringToInt(result, -1)
            if (offsetTop >= 0) {
                val yOffset = (resources.displayMetrics.density * offsetTop).toInt()
                scrollView.smoothScrollTo(0, yOffset)
            } else {
                ToastUtils.showToast(activity, R.string.reader_toast_err_page_jump_not_found)
            }
        }

        readerWebView.settings.javaScriptEnabled = wasJsEnabled
        return true
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
        if (Uri.parse(url).queryParameterNames.any { it.contains("wp-story") }) {
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
        readerTracker.track(Stat.READER_ARTICLE_IMAGE_TAPPED)
        return showPhotoViewer(imageUrl, view, x, y)
    }

    override fun onFileDownloadClick(fileUrl: String?): Boolean {
        readerTracker.track(Stat.READER_ARTICLE_FILE_DOWNLOAD_TAPPED)
        return if (activity != null &&
                fileUrl != null &&
                PermissionUtils.checkAndRequestStoragePermission(
                        this,
                        READER_FILE_DOWNLOAD_PERMISSION_REQUEST_CODE
                )) {
            readerFileDownloadManager.downloadFile(fileUrl)
            true
        } else {
            fileForDownload = fileUrl
            false
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        val activity = activity
        if (activity != null &&
                requestCode == READER_FILE_DOWNLOAD_PERMISSION_REQUEST_CODE &&
                grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
    }

    override fun onScrollDown(distanceY: Float) {
    }

    override fun onScrollCompleted() {
        trackRelatedPostsIfShowing()
    }

    private fun setRefreshing(refreshing: Boolean) {
        swipeToRefreshHelper.isRefreshing = refreshing
    }

    override fun onShowHideToolbar(show: Boolean) {
        if (isAdded) {
            AniUtils.animateTopBar(appBar, show)
        }
    }

    companion object {
        private const val KEY_LIKERS_LIST_STATE = "likers_list_state"
        private const val KEY_COMMENTS_SNIPPET_LIST_STATE = "comments_snippet_list_state"
        private const val NOTIFICATIONS_BOTTOM_SHEET_TAG = "NOTIFICATIONS_BOTTOM_SHEET_TAG"

        fun newInstance(blogId: Long, postId: Long): ReaderPostDetailFragment {
            return newInstance(false, blogId, postId, null, 0, false, null, null, false)
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
            postSlugsResolutionUnderway: Boolean
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
            args.putBoolean(
                    ReaderConstants.KEY_POST_SLUGS_RESOLUTION_UNDERWAY,
                    postSlugsResolutionUnderway
            )

            val fragment = ReaderPostDetailFragment()
            fragment.arguments = args

            return fragment
        }
    }
}
