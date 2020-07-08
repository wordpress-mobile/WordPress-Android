package org.wordpress.android.ui.reader

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.elevation.ElevationOverlayProvider
import com.google.android.material.snackbar.Snackbar
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DEEP_LINKED_FALLBACK
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_ARTICLE_DETAIL_LIKED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_ARTICLE_DETAIL_UNLIKED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_ARTICLE_RENDERED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_POST_SAVED_FROM_DETAILS
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_POST_UNSAVED_FROM_DETAILS
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_SAVED_LIST_VIEWED_FROM_POST_DETAILS_NOTICE
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_USER_UNAUTHORIZED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_WPCOM_SIGN_IN_NEEDED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.SHARED_ITEM
import org.wordpress.android.datasets.ReaderBlogTable
import org.wordpress.android.datasets.ReaderLikeTable
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.AccountActionBuilder.newUpdateSubscriptionNotificationPostAction
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.site.PrivateAtomicCookie
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload.SubscriptionAction
import org.wordpress.android.fluxc.store.AccountStore.OnSubscriptionUpdated
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.FetchPrivateAtomicCookiePayload
import org.wordpress.android.fluxc.store.SiteStore.OnPrivateAtomicCookieFetched
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostDiscoverData
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.PagePostCreationSourcesDetail
import org.wordpress.android.ui.PrivateAtCookieRefreshProgressDialog
import org.wordpress.android.ui.PrivateAtCookieRefreshProgressDialog.PrivateAtCookieProgressDialogOnDismissListener
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.main.SitePickerActivity
import org.wordpress.android.ui.main.SitePickerAdapter.SitePickerMode.REBLOG_SELECT_MODE
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.ui.posts.BasicFragmentDialog
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.ui.reader.ReaderActivityLauncher.OpenUrlType
import org.wordpress.android.ui.reader.ReaderActivityLauncher.PhotoViewerOption
import org.wordpress.android.ui.reader.ReaderActivityLauncher.PhotoViewerOption.IS_PRIVATE_IMAGE
import org.wordpress.android.ui.reader.ReaderInterfaces.AutoHideToolbarListener
import org.wordpress.android.ui.reader.ReaderPostPagerActivity.DirectOperation
import org.wordpress.android.ui.reader.ReaderPostPagerActivity.DirectOperation.COMMENT_JUMP
import org.wordpress.android.ui.reader.ReaderPostPagerActivity.DirectOperation.COMMENT_LIKE
import org.wordpress.android.ui.reader.ReaderPostPagerActivity.DirectOperation.COMMENT_REPLY
import org.wordpress.android.ui.reader.ReaderPostPagerActivity.DirectOperation.POST_LIKE
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.actions.ReaderActions
import org.wordpress.android.ui.reader.actions.ReaderPostActions
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId
import org.wordpress.android.ui.reader.models.ReaderSimplePostList
import org.wordpress.android.ui.reader.utils.FeaturedImageUtils
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.reader.utils.ReaderVideoUtils
import org.wordpress.android.ui.reader.views.ReaderBookmarkButton
import org.wordpress.android.ui.reader.views.ReaderIconCountView
import org.wordpress.android.ui.reader.views.ReaderLikingUsersView
import org.wordpress.android.ui.reader.views.ReaderPostDetailHeaderView
import org.wordpress.android.ui.reader.views.ReaderSimplePostContainerView
import org.wordpress.android.ui.reader.views.ReaderSimplePostView
import org.wordpress.android.ui.reader.views.ReaderTagStrip
import org.wordpress.android.ui.reader.views.ReaderWebView
import org.wordpress.android.ui.reader.views.ReaderWebView.ReaderCustomViewListener
import org.wordpress.android.ui.reader.views.ReaderWebView.ReaderWebViewPageFinishedListener
import org.wordpress.android.ui.reader.views.ReaderWebView.ReaderWebViewUrlClickListener
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.AppLog.T.READER
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.HtmlUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.PermissionUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.WPPermissionUtils.READER_FILE_DOWNLOAD_PERMISSION_REQUEST_CODE
import org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper
import org.wordpress.android.util.WPUrlUtils
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout
import org.wordpress.android.widgets.WPScrollView
import org.wordpress.android.widgets.WPScrollView.ScrollDirectionListener
import org.wordpress.android.widgets.WPSnackbar
import org.wordpress.android.widgets.WPTextView
import java.util.EnumSet
import javax.inject.Inject

class ReaderPostDetailFragment : Fragment(),
        WPMainActivity.OnActivityBackPressedListener,
        ScrollDirectionListener,
        ReaderCustomViewListener,
        ReaderInterfaces.OnFollowListener,
        ReaderWebViewPageFinishedListener,
        ReaderWebViewUrlClickListener,
        BasicFragmentDialog.BasicDialogPositiveClickInterface,
        PrivateAtCookieProgressDialogOnDismissListener {
    private var postId: Long = 0
    private var blogId: Long = 0
    private var directOperation: DirectOperation? = null
    private var commentId: Int = 0
    private var isFeed: Boolean = false
    private var interceptedUri: String? = null
    private var post: ReaderPost? = null
    private var renderer: ReaderPostRenderer? = null
    private var postListType: ReaderPostListType = ReaderTypes.DEFAULT_POST_LIST_TYPE

    private val postHistory = ReaderPostHistory()

    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper
    private lateinit var scrollView: WPScrollView
    private lateinit var layoutFooter: ViewGroup
    private lateinit var readerWebView: ReaderWebView
    private lateinit var likingUsersView: ReaderLikingUsersView
    private lateinit var likingUsersDivider: View
    private lateinit var likingUsersLabel: View
    private lateinit var signInButton: WPTextView
    private lateinit var readerBookmarkButton: ReaderBookmarkButton

    private lateinit var globalRelatedPostsView: ReaderSimplePostContainerView
    private lateinit var localRelatedPostsView: ReaderSimplePostContainerView

    private var postSlugsResolutionUnderway: Boolean = false
    private var hasAlreadyUpdatedPost: Boolean = false
    private var hasAlreadyRequestedPost: Boolean = false
    private var isWebViewPaused: Boolean = false
    private var isRelatedPost: Boolean = false

    private var hasTrackedGlobalRelatedPosts: Boolean = false
    private var hasTrackedLocalRelatedPosts: Boolean = false

    private var toolbarHeight: Int = 0
    private var errorMessage: String? = null

    private var isToolbarShowing = true
    private var autoHideToolbarListener: AutoHideToolbarListener? = null

    private var fileForDownload: String? = null

    @Inject internal lateinit var accountStore: AccountStore
    @Inject internal lateinit var siteStore: SiteStore
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var readerFileDownloadManager: ReaderFileDownloadManager
    @Inject internal lateinit var featuredImageUtils: FeaturedImageUtils
    @Inject internal lateinit var privateAtomicCookie: PrivateAtomicCookie
    @Inject internal lateinit var readerCssProvider: ReaderCssProvider

    private val mSignInClickListener = View.OnClickListener {
        EventBus.getDefault()
                .post(ReaderEvents.DoSignIn())
    }

    /*
     * AsyncTask to retrieve this post from SQLite and display it
     */
    private var isPostTaskRunning = false

    val isCustomViewShowing: Boolean
        get() = view != null && readerWebView.isCustomViewShowing

    private val actionBar: ActionBar?
        get() {
            return if (isAdded && activity is AppCompatActivity) {
                (activity as AppCompatActivity).supportActionBar
            } else {
                AppLog.w(T.READER, "reader post detail > getActionBar returned null")
                null
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
            isFeed = args.getBoolean(ReaderConstants.ARG_IS_FEED)
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is AutoHideToolbarListener) {
            autoHideToolbarListener = context
        }
        toolbarHeight = context.resources.getDimensionPixelSize(R.dimen.toolbar_height)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.reader_fragment_post_detail, container, false)

        val swipeRefreshLayout = view.findViewById<CustomSwipeRefreshLayout>(R.id.swipe_to_refresh)

        // this fragment hides/shows toolbar with scrolling, which messes up ptr animation position
        // so we have to set it manually
        val swipeToRefreshOffset = resources.getDimensionPixelSize(R.dimen.toolbar_content_offset)
        swipeRefreshLayout.setProgressViewOffset(false, 0, swipeToRefreshOffset)

        swipeToRefreshHelper = buildSwipeToRefreshHelper(
                swipeRefreshLayout
        ) {
            if (isAdded) {
                updatePost()
            }
        }

        scrollView = view.findViewById(R.id.scroll_view_reader)
        scrollView.setScrollDirectionListener(this)

        layoutFooter = view.findViewById(R.id.layout_post_detail_footer)

        val elevationOverlayProvider = ElevationOverlayProvider(layoutFooter.context)
        val appbarElevation = resources.getDimension(R.dimen.appbar_elevation)
        val elevatedSurfaceColor = elevationOverlayProvider.compositeOverlayWithThemeSurfaceColorIfNeeded(
                appbarElevation
        )
        layoutFooter.setBackgroundColor(elevatedSurfaceColor)

        likingUsersView = view.findViewById(R.id.layout_liking_users_view)
        likingUsersDivider = view.findViewById(R.id.layout_liking_users_divider)
        likingUsersLabel = view.findViewById(R.id.text_liking_users_label)

        // setup the ReaderWebView
        readerWebView = view.findViewById(R.id.reader_webview)
        readerWebView.setCustomViewListener(this)
        readerWebView.setUrlClickListener(this)
        readerWebView.setPageFinishedListener(this)

        // hide footer and scrollView until the post is loaded
        layoutFooter.visibility = View.INVISIBLE
        scrollView.visibility = View.INVISIBLE

        val relatedPostsContainer = view.findViewById<View>(R.id.container_related_posts)
        globalRelatedPostsView = relatedPostsContainer.findViewById(R.id.related_posts_view_global)
        globalRelatedPostsView.setOnFollowListener(this)
        localRelatedPostsView = relatedPostsContainer.findViewById(R.id.related_posts_view_local)
        localRelatedPostsView.setOnFollowListener(this)

        signInButton = view.findViewById(R.id.nux_sign_in_button)
        signInButton.setOnClickListener(mSignInClickListener)

        readerBookmarkButton = view.findViewById(R.id.bookmark_button)

        val progress = view.findViewById<ProgressBar>(R.id.progress_loading)
        if (postSlugsResolutionUnderway) {
            progress.visibility = View.VISIBLE
        }

        showPost()

        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        if (view != null) {
            readerWebView.destroy()
        }
    }

    private fun hasPost(): Boolean {
        return post != null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.reader_detail, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        // browse & share require the post to have a URL (some feed-based posts don't have one)
        val postHasUrl = hasPost() && post!!.hasUrl()
        val mnuBrowse = menu.findItem(R.id.menu_browse)
        if (mnuBrowse != null) {
            mnuBrowse.isVisible = postHasUrl || interceptedUri != null
        }
        val mnuShare = menu.findItem(R.id.menu_share)
        if (mnuShare != null) {
            mnuShare.isVisible = postHasUrl
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_browse -> {
                if (hasPost()) {
                    ReaderActivityLauncher.openUrl(activity, post!!.url, OpenUrlType.EXTERNAL)
                } else if (interceptedUri != null) {
                    AnalyticsUtils.trackWithInterceptedUri(
                            DEEP_LINKED_FALLBACK,
                            interceptedUri
                    )
                    ReaderActivityLauncher.openUrl(activity, interceptedUri, OpenUrlType.EXTERNAL)
                    requireActivity().finish()
                }
                return true
            }
            R.id.menu_share -> {
                AnalyticsTracker.track(SHARED_ITEM)
                sharePage()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(ReaderConstants.ARG_IS_FEED, isFeed)
        outState.putLong(ReaderConstants.ARG_BLOG_ID, blogId)
        outState.putLong(ReaderConstants.ARG_POST_ID, postId)
        outState.putSerializable(ReaderConstants.ARG_DIRECT_OPERATION, directOperation)
        outState.putInt(ReaderConstants.ARG_COMMENT_ID, commentId)

        outState.putBoolean(ReaderConstants.ARG_IS_RELATED_POST, isRelatedPost)
        outState.putString(ReaderConstants.ARG_INTERCEPTED_URI, interceptedUri)
        outState.putBoolean(
                ReaderConstants.KEY_POST_SLUGS_RESOLUTION_UNDERWAY,
                postSlugsResolutionUnderway
        )
        outState.putBoolean(ReaderConstants.KEY_ALREADY_UPDATED, hasAlreadyUpdatedPost)
        outState.putBoolean(ReaderConstants.KEY_ALREADY_REQUESTED, hasAlreadyRequestedPost)

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

        super.onSaveInstanceState(outState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        restoreState(savedInstanceState)
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            isFeed = it.getBoolean(ReaderConstants.ARG_IS_FEED)
            blogId = it.getLong(ReaderConstants.ARG_BLOG_ID)
            postId = it.getLong(ReaderConstants.ARG_POST_ID)
            directOperation = it
                    .getSerializable(ReaderConstants.ARG_DIRECT_OPERATION) as? DirectOperation
            commentId = it.getInt(ReaderConstants.ARG_COMMENT_ID)
            isRelatedPost = it.getBoolean(ReaderConstants.ARG_IS_RELATED_POST)
            interceptedUri = it.getString(ReaderConstants.ARG_INTERCEPTED_URI)
            postSlugsResolutionUnderway = it.getBoolean(ReaderConstants.KEY_POST_SLUGS_RESOLUTION_UNDERWAY)
            hasAlreadyUpdatedPost = it.getBoolean(ReaderConstants.KEY_ALREADY_UPDATED)
            hasAlreadyRequestedPost = it.getBoolean(ReaderConstants.KEY_ALREADY_REQUESTED)
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

    override fun onFollowTapped(view: View, blogName: String, blogId: Long) {
        dispatcher.dispatch(AccountActionBuilder.newFetchSubscriptionsAction())

        if (isAdded) {
            val blog = if (TextUtils.isEmpty(blogName))
                getString(R.string.reader_followed_blog_notifications_this)
            else
                blogName

            WPSnackbar.make(
                    view, Html.fromHtml(
                    getString(
                            R.string.reader_followed_blog_notifications,
                            "<b>", blog, "</b>"
                    )
            ), Snackbar.LENGTH_LONG
            )
                    .setAction(
                            getString(R.string.reader_followed_blog_notifications_action)
                    ) {
                        AnalyticsUtils.trackWithSiteId(
                                Stat.FOLLOWED_BLOG_NOTIFICATIONS_READER_ENABLED,
                                blogId
                        )
                        val payload = AddOrDeleteSubscriptionPayload(
                                blogId.toString(), SubscriptionAction.NEW
                        )
                        dispatcher.dispatch(newUpdateSubscriptionNotificationPostAction(payload))
                        ReaderBlogTable.setNotificationsEnabledByBlogId(blogId, true)
                    }
                    .show()
        }
    }

    override fun onFollowingTapped() {
        dispatcher.dispatch(AccountActionBuilder.newFetchSubscriptionsAction())
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSubscriptionUpdated(event: OnSubscriptionUpdated) {
        if (event.isError) {
            AppLog.e(
                    T.API,
                    "${ReaderPostDetailFragment::class.java.simpleName}.onSubscriptionUpdated: " +
                            "${event.error.type} - ${event.error.message}"
            )
        } else {
            dispatcher.dispatch(AccountActionBuilder.newFetchSubscriptionsAction())
        }
    }

    /*
     * changes the like on the passed post
     */
    private fun togglePostLike() {
        if (hasPost()) {
            setPostLike(post?.isLikedByCurrentUser == false)
        }
    }

    private fun initBookmarkButton() {
        if (!canShowFooter()) {
            return
        }

        updateBookmarkView()
        readerBookmarkButton.setOnClickListener { toggleBookmark() }
    }

    private fun updateBookmarkView() {
        if (!isAdded || !hasPost()) {
            return
        }

        if (!canShowBookmarkButton()) {
            readerBookmarkButton.visibility = View.GONE
        } else {
            readerBookmarkButton.visibility = View.VISIBLE
            readerBookmarkButton.updateIsBookmarkedState(post!!.isBookmarked)
        }
    }

    /*
     * triggered when user taps the bookmark post button
     */
    private fun toggleBookmark() {
        val post = this.post
        if (!isAdded || post == null) {
            return
        }

        if (post.isBookmarked) {
            ReaderPostActions.removeFromBookmarked(post)
            AnalyticsTracker.track(READER_POST_UNSAVED_FROM_DETAILS)
        } else {
            ReaderPostActions.addToBookmarked(post)
            AnalyticsTracker.track(READER_POST_SAVED_FROM_DETAILS)
            if (AppPrefs.shouldShowBookmarksSavedLocallyDialog()) {
                AppPrefs.setBookmarksSavedLocallyDialogShown()
                showBookmarksSavedLocallyDialog()
            } else {
                // show snackbar when not in saved posts list
                showBookmarkSnackbar()
            }
        }

        this.post = ReaderPostTable.getBlogPost(post.blogId, post.postId, false)

        updateBookmarkView()
    }

    private fun showBookmarksSavedLocallyDialog() {
        val basicFragmentDialog = BasicFragmentDialog()
        basicFragmentDialog.initialize(
                BOOKMARKS_SAVED_LOCALLY_DIALOG,
                getString(R.string.reader_save_posts_locally_dialog_title),
                getString(R.string.reader_save_posts_locally_dialog_message),
                getString(R.string.dialog_button_ok), null, null
        )
        fragmentManager?.let {
            basicFragmentDialog.show(it, BOOKMARKS_SAVED_LOCALLY_DIALOG)
        }
    }

    override fun onPositiveClicked(instanceTag: String) {
        when (instanceTag) {
            BOOKMARKS_SAVED_LOCALLY_DIALOG -> showBookmarkSnackbar()
        }
    }

    private fun showBookmarkSnackbar() {
        if (!isAdded) {
            return
        }

        WPSnackbar.make(requireView(), R.string.reader_bookmark_snack_title, Snackbar.LENGTH_LONG)
                .setAction(
                        R.string.reader_bookmark_snack_btn
                ) {
                    AnalyticsTracker
                            .track(READER_SAVED_LIST_VIEWED_FROM_POST_DETAILS_NOTICE)
                    ActivityLauncher.viewSavedPostsListInReader(activity)
                }
                .show()
    }

    /*
     * changes the like on the passed post
     */
    private fun setPostLike(isAskingToLike: Boolean) {
        val post = this.post
        if (!isAdded || post == null || !NetworkUtils.checkConnection(activity)) {
            return
        }

        if (isAskingToLike != ReaderPostTable.isPostLikedByCurrentUser(post)) {
            val likeCount = requireView().findViewById<ReaderIconCountView>(R.id.count_likes)
            likeCount.isSelected = isAskingToLike
            ReaderAnim.animateLikeButton(likeCount.imageView, isAskingToLike)

            val success = ReaderPostActions.performLikeAction(
                    post, isAskingToLike,
                    accountStore.account.userId
            )
            if (!success) {
                likeCount.isSelected = !isAskingToLike
                return
            }

            // get the post again since it has changed, then refresh to show changes
            this.post = ReaderPostTable.getBlogPost(post.blogId, post.postId, false)
            refreshLikes()
            refreshIconCounts()
        }

        if (isAskingToLike) {
            AnalyticsUtils.trackWithReaderPostDetails(
                    READER_ARTICLE_DETAIL_LIKED,
                    post
            )
        } else {
            AnalyticsUtils.trackWithReaderPostDetails(
                    READER_ARTICLE_DETAIL_UNLIKED,
                    post
            )
        }
    }

    /**
     * display the standard Android share chooser to share this post
     */
    private fun sharePage() {
        val post = this.post
        if (!isAdded || post == null) {
            return
        }

        val url = if (post.hasShortUrl()) post.shortUrl else post.url

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, url)
        intent.putExtra(Intent.EXTRA_SUBJECT, post.title)
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share_link)))
        } catch (ex: android.content.ActivityNotFoundException) {
            ToastUtils.showToast(activity, R.string.reader_toast_err_share_intent)
        }
    }

    /*
     * replace the current post with the passed one
     */
    private fun replacePost(blogId: Long, postId: Long, clearCommentOperation: Boolean) {
        isFeed = false
        this.blogId = blogId
        this.postId = postId

        if (clearCommentOperation) {
            directOperation = null
            commentId = 0
        }

        hasAlreadyRequestedPost = false
        hasAlreadyUpdatedPost = false
        hasTrackedGlobalRelatedPosts = false
        hasTrackedLocalRelatedPosts = false

        // hide views that would show info for the previous post - these will be re-displayed
        // with the correct info once the new post loads
        globalRelatedPostsView.visibility = View.GONE
        localRelatedPostsView.visibility = View.GONE

        likingUsersView.visibility = View.GONE
        likingUsersDivider.visibility = View.GONE
        likingUsersLabel.visibility = View.GONE

        // clear the webView - otherwise it will remain scrolled to where the user scrolled to
        readerWebView.clearContent()

        // make sure the toolbar and footer are showing
        showToolbar(true)
        showFooter(true)

        // now show the passed post
        showPost()
    }

    /*
     * request posts related to the current one - only available for wp.com
     */
    private fun requestRelatedPosts() {
        if (hasPost() && post?.isWP == true) {
            ReaderPostActions.requestRelatedPosts(post)
        }
    }

    /*
     * related posts were retrieved
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ReaderEvents.RelatedPostsUpdated) {
        val post = this.post
        if (!isAdded || post == null) {
            return
        }

        // make sure this event is for the current post
        if (event.sourcePostId == post.postId && event.sourceSiteId == post.blogId) {
            if (event.hasLocalRelatedPosts()) {
                showRelatedPosts(event.localRelatedPosts, false)
            }
            if (event.hasGlobalRelatedPosts()) {
                showRelatedPosts(event.globalRelatedPosts, true)
            }
        }
    }

    /*
     * show the passed list of related posts - can be either global (related posts from
     * across wp.com) or local (related posts from the same site as the current post)
     */
    private fun showRelatedPosts(relatedPosts: ReaderSimplePostList, isGlobal: Boolean) {
        // tapping a related post should open the related post detail
        val listener = ReaderSimplePostView.OnSimplePostClickListener { _, siteId, postId ->
            showRelatedPostDetail(
                    siteId,
                    postId,
                    isGlobal
            )
        }

        // different container views for global/local related posts
        val relatedPostsView = if (isGlobal) globalRelatedPostsView else localRelatedPostsView
        relatedPostsView.showPosts(relatedPosts, post!!.blogName, isGlobal, listener)

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

    /*
     * user clicked a single related post - if we're already viewing a related post, add it to the
     * history stack so the user can back-button through the history - otherwise start a new detail
     * activity for this related post
     */
    private fun showRelatedPostDetail(blogId: Long, postId: Long, isGlobal: Boolean) {
        val stat = if (isGlobal)
            Stat.READER_GLOBAL_RELATED_POST_CLICKED
        else
            Stat.READER_LOCAL_RELATED_POST_CLICKED
        AnalyticsUtils.trackWithReaderPostDetails(stat, blogId, postId)

        if (isRelatedPost) {
            postHistory.push(ReaderBlogIdPostId(post!!.blogId, post!!.postId))
            replacePost(blogId, postId, true)
        } else {
            ReaderActivityLauncher.showReaderPostDetail(
                    activity,
                    false,
                    blogId,
                    postId, null,
                    0,
                    true, null
            )
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
        if (!hasPost() || !post!!.isWP) {
            setRefreshing(false)
            return
        }

        val numLikesBefore = ReaderLikeTable.getNumLikesForPost(post)

        val resultListener = ReaderActions.UpdateResultListener { result ->
            val post = this.post
            if (isAdded && post != null) {
                // if the post has changed, reload it from the db and update the like/comment counts
                if (result.isNewOrChanged) {
                    this.post = ReaderPostTable.getBlogPost(post.blogId, post.postId, false)
                    refreshIconCounts()
                }
                // refresh likes if necessary - done regardless of whether the post has changed
                // since it's possible likes weren't stored until the post was updated
                if (result != ReaderActions.UpdateResult.FAILED && numLikesBefore != ReaderLikeTable.getNumLikesForPost(
                                post
                        )) {
                    refreshLikes()
                }

                setRefreshing(false)

                if (directOperation != null && directOperation == DirectOperation.POST_LIKE) {
                    doLikePost()
                }
            }
        }
        ReaderPostActions.updatePost(post!!, resultListener)
    }

    private fun refreshIconCounts() {
        val post = this.post
        if (!isAdded || post == null || !canShowFooter()) {
            return
        }

        val countLikes = requireView().findViewById<ReaderIconCountView>(R.id.count_likes)
        val countComments = requireView().findViewById<ReaderIconCountView>(R.id.count_comments)
        val reblogButton = requireView().findViewById<ReaderIconCountView>(R.id.reblog)

        if (canBeReblogged()) {
            reblogButton.setCount(0)
            reblogButton.visibility = View.VISIBLE
            reblogButton.setOnClickListener {
                val sites = siteStore.visibleSitesAccessedViaWPCom
                when (sites.count()) {
                    0 -> ReaderActivityLauncher.showNoSiteToReblog(activity)
                    1 -> {
                        sites.firstOrNull()?.let {
                            ActivityLauncher.openEditorForReblog(
                                    activity,
                                    it,
                                    this.post,
                                    PagePostCreationSourcesDetail.POST_FROM_DETAIL_REBLOG
                            )
                        } ?: ToastUtils.showToast(activity, R.string.reader_reblog_error)
                    }
                    else -> {
                        sites.firstOrNull()?.let {
                            ActivityLauncher.showSitePickerForResult(this, it, REBLOG_SELECT_MODE)
                        } ?: ToastUtils.showToast(activity, R.string.reader_reblog_error)
                    }
                }
            }
        } else {
            reblogButton.visibility = View.GONE
            reblogButton.setOnClickListener(null)
        }

        if (canShowCommentCount()) {
            countComments.setCount(post.numReplies)
            countComments.visibility = View.VISIBLE
            countComments.setOnClickListener {
                ReaderActivityLauncher.showReaderComments(
                        activity,
                        post.blogId,
                        post.postId
                )
            }
        } else {
            countComments.visibility = View.GONE
            countComments.setOnClickListener(null)
        }

        if (canShowLikeCount()) {
            countLikes.setCount(post.numLikes)
            countLikes.contentDescription = ReaderUtils.getLongLikeLabelText(
                    activity,
                    post.numLikes,
                    post.isLikedByCurrentUser
            )
            countLikes.visibility = View.VISIBLE
            countLikes.isSelected = post.isLikedByCurrentUser
            if (!accountStore.hasAccessToken()) {
                countLikes.isEnabled = false
            } else if (post.canLikePost()) {
                countLikes.setOnClickListener { togglePostLike() }
            }
            // if we know refreshLikes() is going to show the liking users, force liking user
            // views to take up space right now
            if (post.numLikes > 0 && likingUsersView.visibility == View.GONE) {
                likingUsersView.visibility = View.INVISIBLE
                likingUsersDivider.visibility = View.INVISIBLE
                likingUsersLabel.visibility = View.INVISIBLE
            }
        } else {
            countLikes.visibility = View.GONE
            countLikes.setOnClickListener(null)
        }
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

        if (post?.canLikePost() == false) {
            ToastUtils.showToast(activity, R.string.reader_toast_err_cannot_like_post)
            return
        }

        setPostLike(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RequestCodes.SITE_PICKER -> {
                if (resultCode == Activity.RESULT_OK) {
                    val siteLocalId = data?.getIntExtra(SitePickerActivity.KEY_LOCAL_ID, -1) ?: -1
                    val site = siteStore.getSiteByLocalId(siteLocalId)
                    ActivityLauncher.openEditorForReblog(
                            activity,
                            site,
                            this.post,
                            PagePostCreationSourcesDetail.POST_FROM_DETAIL_REBLOG
                    )
                }
            }
        }
    }

    /*
     * show latest likes for this post
     */
    private fun refreshLikes() {
        val post = this.post
        if (!isAdded || post == null || !post.canLikePost()) {
            return
        }

        // nothing more to do if no likes
        if (post.numLikes == 0) {
            likingUsersView.visibility = View.GONE
            likingUsersDivider.visibility = View.GONE
            likingUsersLabel.visibility = View.GONE
            return
        }

        // clicking likes view shows activity displaying all liking users
        likingUsersView.setOnClickListener {
            ReaderActivityLauncher.showReaderLikingUsers(
                    activity,
                    post.blogId,
                    post.postId
            )
        }

        likingUsersDivider.visibility = View.VISIBLE
        likingUsersLabel.visibility = View.VISIBLE
        likingUsersView.visibility = View.VISIBLE
        likingUsersView.showLikingUsers(post, accountStore.account.userId)
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

        val postContent = post?.text
        val isPrivatePost = post?.isPrivate == true
        val options = EnumSet.noneOf(PhotoViewerOption::class.java)
        if (isPrivatePost) {
            options.add(IS_PRIVATE_IMAGE)
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
     * called when the post doesn't exist in local db, need to get it from server
     */
    private fun requestPost() {
        val progress = requireView().findViewById<ProgressBar>(R.id.progress_loading)
        progress.visibility = View.VISIBLE
        progress.bringToFront()

        val listener = object : ReaderActions.OnRequestListener {
            override fun onSuccess() {
                hasAlreadyRequestedPost = true
                if (isAdded) {
                    progress.visibility = View.GONE
                    showPost()
                    EventBus.getDefault().post(ReaderEvents.SinglePostDownloaded())
                }
            }

            override fun onFailure(statusCode: Int) {
                hasAlreadyRequestedPost = true
                if (isAdded) {
                    progress.visibility = View.GONE
                    onRequestFailure(statusCode)
                }
            }
        }

        if (isFeed) {
            ReaderPostActions.requestFeedPost(blogId, postId, listener)
        } else {
            ReaderPostActions.requestBlogPost(blogId, postId, listener)
        }
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

        if (event.statusCode == 200) {
            replacePost(event.blogId, event.postId, false)
        } else {
            onRequestFailure(event.statusCode)
        }
    }

    private fun onRequestFailure(statusCode: Int) {
        val errMsgResId: Int
        if (!NetworkUtils.isNetworkAvailable(activity)) {
            errMsgResId = R.string.no_network_message
        } else {
            when (statusCode) {
                401, 403 -> {
                    val offerSignIn = WPUrlUtils.isWordPressCom(interceptedUri) && !accountStore.hasAccessToken()

                    if (!offerSignIn) {
                        errMsgResId = if (interceptedUri == null)
                            R.string.reader_err_get_post_not_authorized
                        else
                            R.string.reader_err_get_post_not_authorized_fallback
                        signInButton.visibility = View.GONE
                    } else {
                        errMsgResId = if (interceptedUri == null)
                            R.string.reader_err_get_post_not_authorized_signin
                        else
                            R.string.reader_err_get_post_not_authorized_signin_fallback
                        signInButton.visibility = View.VISIBLE
                        AnalyticsUtils.trackWithReaderPostDetails(
                                READER_WPCOM_SIGN_IN_NEEDED,
                                post
                        )
                    }
                    AnalyticsUtils.trackWithReaderPostDetails(
                            READER_USER_UNAUTHORIZED,
                            post
                    )
                }
                404 -> errMsgResId = R.string.reader_err_get_post_not_found
                else -> errMsgResId = R.string.reader_err_get_post_generic
            }
        }
        showError(getString(errMsgResId))
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
                AppLog.e(READER, "Drawable not found. See issue #11576", e)
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

        if (isPostTaskRunning) {
            AppLog.w(T.READER, "reader post detail > show post task already running")
            return
        }

        ShowPostTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPrivateAtomicCookieFetched(event: OnPrivateAtomicCookieFetched) {
        if (!isAdded) {
            return
        }

        if (event.isError) {
            AppLog.e(
                    READER,
                    "Failed to load private AT cookie. $event.error.type - $event.error.message"
            )
            WPSnackbar.make(
                    requireView(),
                    string.media_accessing_failed,
                    Snackbar.LENGTH_LONG
            ).show()
        } else {
            CookieManager.getInstance().setCookie(
                    privateAtomicCookie.getDomain(), privateAtomicCookie.getCookieContent()
            )
        }

        PrivateAtCookieRefreshProgressDialog.dismissIfNecessary(fragmentManager)
        if (renderer != null) {
            renderer!!.beginRender()
        }
    }

    override fun onCookieProgressDialogCancelled() {
        if (!isAdded) {
            return
        }

        WPSnackbar.make(
                requireView(),
                string.media_accessing_failed,
                Snackbar.LENGTH_LONG
        ).show()
        if (renderer != null) {
            renderer!!.beginRender()
        }
    }

    // TODO replace this inner async task with a coroutine
    @SuppressLint("StaticFieldLeak")
    private inner class ShowPostTask : AsyncTask<Void, Void, Boolean>() {
        override fun onPreExecute() {
            isPostTaskRunning = true
        }

        override fun onCancelled() {
            isPostTaskRunning = false
        }

        override fun doInBackground(vararg params: Void): Boolean? {
            post = if (isFeed)
                ReaderPostTable.getFeedPost(blogId, postId, false)
            else
                ReaderPostTable.getBlogPost(blogId, postId, false)
            if (post == null) {
                return false
            }

            // "discover" Editor Pick posts should open the original (source) post
            if (post!!.isDiscoverPost) {
                val discoverData = post!!.discoverData
                if (discoverData != null &&
                        discoverData.discoverType == ReaderPostDiscoverData.DiscoverType.EDITOR_PICK &&
                        discoverData.blogId != 0L &&
                        discoverData.postId != 0L
                ) {
                    isFeed = false
                    blogId = discoverData.blogId
                    postId = discoverData.postId
                    post = ReaderPostTable.getBlogPost(blogId, postId, false)
                    if (post == null) {
                        return false
                    }
                }
            }

            return true
        }

        override fun onPostExecute(result: Boolean) {
            isPostTaskRunning = false

            if (!isAdded) {
                return
            }

            // make sure options menu reflects whether we now have a post
            activity!!.invalidateOptionsMenu()

            if (!result) {
                // post couldn't be loaded which means it doesn't exist in db, so request it from
                // the server if it hasn't already been requested
                if (!hasAlreadyRequestedPost) {
                    AppLog.i(T.READER, "reader post detail > post not found, requesting it")
                    requestPost()
                } else if (!TextUtils.isEmpty(errorMessage)) {
                    // post has already been requested and failed, so restore previous error message
                    showError(errorMessage)
                }
                return
            } else {
                showError(null)
            }

            if (directOperation != null) {
                when (directOperation) {
                    COMMENT_JUMP, COMMENT_REPLY, COMMENT_LIKE -> {
                        ReaderActivityLauncher.showReaderComments(
                                activity, post!!.blogId, post!!.postId,
                                directOperation, commentId.toLong(), interceptedUri
                        )
                        activity?.finish()
                        activity?.overridePendingTransition(0, 0)
                        return
                    }
                    POST_LIKE -> {
                    }
                }
                // Liking needs to be handled "later" after the post has been updated from the server so,
                // nothing special to do here
            }

            AnalyticsUtils.trackWithReaderPostDetails(
                    READER_ARTICLE_RENDERED,
                    post
            )

            readerWebView.setIsPrivatePost(post!!.isPrivate)
            readerWebView.setBlogSchemeIsHttps(UrlUtils.isHttps(post!!.blogUrl))

            val txtTitle = view!!.findViewById<TextView>(R.id.text_title)
            val txtDateline = view!!.findViewById<TextView>(R.id.text_dateline)

            val tagStrip = view!!.findViewById<ReaderTagStrip>(R.id.tag_strip)
            val headerView = view!!.findViewById<ReaderPostDetailHeaderView>(R.id.header_view)
            headerView.setOnFollowListener(this@ReaderPostDetailFragment)
            if (!canShowFooter()) {
                layoutFooter.visibility = View.GONE
            }

            // add padding to the scrollView to make room for the top and bottom toolbars - this also
            // ensures the scrollbar matches the content so it doesn't disappear behind the toolbars
            val topPadding = if (autoHideToolbarListener != null) toolbarHeight else 0
            val bottomPadding = if (canShowFooter()) layoutFooter.height else 0
            scrollView.setPadding(0, topPadding, 0, bottomPadding)

            // scrollView was hidden in onCreateView, show it now that we have the post
            scrollView.visibility = View.VISIBLE

            // render the post in the webView
            renderer = ReaderPostRenderer(readerWebView, post, featuredImageUtils, readerCssProvider)

            // if the post is from private atomic site postpone render until we have a special access cookie
            if (post!!.isPrivateAtomic && privateAtomicCookie.isCookieRefreshRequired()) {
                PrivateAtCookieRefreshProgressDialog.showIfNecessary(fragmentManager, this@ReaderPostDetailFragment)
                dispatcher.dispatch(
                        SiteActionBuilder.newFetchPrivateAtomicCookieAction(
                                FetchPrivateAtomicCookiePayload(post!!.blogId)
                        )
                )
            } else if (post!!.isPrivateAtomic && privateAtomicCookie.exists()) {
                // make sure we add cookie to the cookie manager if it exists before starting render
                CookieManager.getInstance().setCookie(
                        privateAtomicCookie.getDomain(), privateAtomicCookie.getCookieContent()
                )
                renderer!!.beginRender()
            } else {
                renderer!!.beginRender()
            }

            // if we're showing just the excerpt, also show a footer which links to the full post
            if (post!!.shouldShowExcerpt()) {
                val excerptFooter = view!!.findViewById<ViewGroup>(R.id.excerpt_footer)
                excerptFooter.visibility = View.VISIBLE

                val blogName = "<font color='" + HtmlUtils.colorResToHtmlColor(
                        activity!!, R.color
                        .link_reader
                ) + "'>" + post!!.blogName + "</font>"
                val linkText = String.format(
                        WordPress.getContext().getString(R.string.reader_excerpt_link),
                        blogName
                )

                val txtExcerptFooter = excerptFooter.findViewById<TextView>(R.id.text_excerpt_footer)
                txtExcerptFooter.text = Html.fromHtml(linkText)

                txtExcerptFooter.setOnClickListener { v ->
                    ReaderActivityLauncher.openUrl(
                            v.context,
                            post!!.url
                    )
                }
            }

            txtTitle.text = if (post!!.hasTitle()) post!!.title else getString(R.string.reader_untitled_post)

            val timestamp = DateTimeUtils.javaDateToTimeSpan(
                    post!!.displayDate,
                    WordPress.getContext()
            )
            txtDateline.text = timestamp

            headerView.setPost(post!!, accountStore.hasAccessToken())
            tagStrip.setPost(post!!)

            if (canShowFooter() && layoutFooter.visibility != View.VISIBLE) {
                AniUtils.fadeIn(layoutFooter, AniUtils.Duration.LONG)
            }

            refreshIconCounts()
            initBookmarkButton()
        }
    }

    /*
     * called by the web view when the content finishes loading - likes aren't displayed
     * until this is triggered, to avoid having them appear before the webView content
     */
    override fun onPageFinished(view: WebView, url: String?) {
        if (!isAdded) {
            return
        }

        if (url != null && url == "about:blank") {
            // brief delay before showing comments/likes to give page time to render
            view.postDelayed(Runnable {
                if (!isAdded) {
                    return@Runnable
                }
                refreshLikes()
                if (!hasAlreadyUpdatedPost) {
                    hasAlreadyUpdatedPost = true
                    updatePost()
                }
                requestRelatedPosts()
            }, 300)
        } else {
            AppLog.w(T.READER, "reader post detail > page finished - " + url!!)
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
        // full screen video has just been shown so hide the ActionBar
        val actionBar = actionBar
        actionBar?.hide()
    }

    override fun onCustomViewHidden() {
        // user returned from full screen video so re-display the ActionBar
        val actionBar = actionBar
        actionBar?.show()
    }

    fun hideCustomView() {
        if (view != null) {
            readerWebView.hideCustomView()
        }
    }

    override fun onUrlClick(url: String): Boolean {
        // if this is a "wordpress://blogpreview?" link, show blog preview for the blog - this is
        // used for Discover posts that highlight a blog
        if (ReaderUtils.isBlogPreviewUrl(url)) {
            val siteId = ReaderUtils.getBlogIdFromBlogPreviewUrl(url)
            if (siteId != 0L) {
                ReaderActivityLauncher.showReaderBlogPreview(activity, siteId)
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

    /*
     * returns True if the passed URL should be opened in the external browser app
     */
    private fun shouldOpenExternal(url: String): Boolean {
        // open YouTube videos in external app so they launch the YouTube player
        if (ReaderVideoUtils.isYouTubeVideoLink(url)) {
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
        return showPhotoViewer(imageUrl, view, x, y)
    }

    override fun onFileDownloadClick(fileUrl: String?): Boolean {
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
            readerFileDownloadManager.downloadFile(fileForDownload!!)
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
        if (!isToolbarShowing && -distanceY >= MIN_SCROLL_DISTANCE_Y) {
            showToolbar(true)
            showFooter(true)
        }
    }

    override fun onScrollDown(distanceY: Float) {
        if (isToolbarShowing &&
                distanceY >= MIN_SCROLL_DISTANCE_Y &&
                scrollView.canScrollDown() &&
                scrollView.canScrollUp() &&
                scrollView.scrollY > toolbarHeight) {
            showToolbar(false)
            showFooter(false)
        }
    }

    override fun onScrollCompleted() {
        if (!isToolbarShowing && (!scrollView.canScrollDown() || !scrollView.canScrollUp())) {
            showToolbar(true)
            showFooter(true)
        }

        trackRelatedPostsIfShowing()
    }

    private fun showToolbar(show: Boolean) {
        isToolbarShowing = show
        if (autoHideToolbarListener != null) {
            autoHideToolbarListener!!.onShowHideToolbar(show)
        }
    }

    private fun showFooter(show: Boolean) {
        if (isAdded && canShowFooter()) {
            AniUtils.animateBottomBar(layoutFooter, show)
        }
    }

    /*
     * can we show the footer bar which contains the like & comment counts?
     */
    private fun canShowFooter(): Boolean {
        return canShowLikeCount() || canShowCommentCount() || canShowBookmarkButton()
    }

    /**
     * Returns true if the blog post can be reblogged
     */
    private fun canBeReblogged(): Boolean {
        this.post?.let {
            if (!it.isPrivate && accountStore.hasAccessToken()) {
                return true
            }
        }
        return false
    }

    private fun canShowCommentCount(): Boolean {
        val post = this.post ?: return false
        return if (!accountStore.hasAccessToken()) {
            post.numReplies > 0
        } else post.isWP &&
                !post.isDiscoverPost &&
                (post.isCommentsOpen || post.numReplies > 0)
    }

    private fun canShowBookmarkButton(): Boolean {
        return hasPost() && !post!!.isDiscoverPost
    }

    private fun canShowLikeCount(): Boolean {
        if (post == null) {
            return false
        }
        return if (!accountStore.hasAccessToken()) {
            post!!.numLikes > 0
        } else post!!.canLikePost() || post!!.numLikes > 0
    }

    private fun setRefreshing(refreshing: Boolean) {
        swipeToRefreshHelper.isRefreshing = refreshing
    }

    companion object {
        private const val BOOKMARKS_SAVED_LOCALLY_DIALOG = "bookmarks_saved_locally_dialog"

        // min scroll distance before toggling toolbar
        private const val MIN_SCROLL_DISTANCE_Y = 10f

        fun newInstance(blogId: Long, postId: Long): ReaderPostDetailFragment {
            return newInstance(false, blogId, postId, null, 0, false, null, null, false)
        }

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
