package org.wordpress.android.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import androidx.activity.addCallback
import androidx.core.os.BundleCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.WPLaunchActivity
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenInReader
import org.wordpress.android.ui.deeplinks.DeepLinkOpenWebLinksWithJetpackHelper
import org.wordpress.android.ui.deeplinks.DeepLinkTrackingUtils
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureFullScreenOverlayFragment
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureFullScreenOverlayFragment.Companion.newInstance
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureFullScreenOverlayViewModel
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureOverlayActions
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureOverlayActions.ForwardToJetpack
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureCollectionOverlaySource
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.posts.EditorConstants
import org.wordpress.android.ui.posts.EditorLauncher
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.ui.reader.ReaderEvents.DoSignIn
import org.wordpress.android.ui.reader.ReaderEvents.PostSlugsRequestCompleted
import org.wordpress.android.ui.reader.ReaderEvents.UpdatePostsEnded
import org.wordpress.android.ui.reader.ReaderEvents.UpdatePostsStarted
import org.wordpress.android.ui.reader.ReaderPostDetailFragment.Companion.newInstance
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.actions.ReaderActions
import org.wordpress.android.ui.reader.actions.ReaderActions.OnRequestListener
import org.wordpress.android.ui.reader.actions.ReaderPostActions
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostIdList
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.tracker.ReaderTrackerType
import org.wordpress.android.ui.reader.usecases.ReaderGetReadingPreferencesSyncUseCase
import org.wordpress.android.ui.reader.utils.ReaderPostSeenStatusWrapper
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource
import org.wordpress.android.ui.uploads.UploadActionUseCase
import org.wordpress.android.ui.uploads.UploadUtils
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import org.wordpress.android.ui.utils.JetpackAppMigrationFlowUtils
import org.wordpress.android.ui.utils.PreMigrationDeepLinkData
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.FluxCUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.UrlUtilsWrapper
import org.wordpress.android.util.WPActivityUtils
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.util.config.SeenUnseenWithCounterFeatureConfig
import org.wordpress.android.util.extensions.onBackPressedCompat
import org.wordpress.android.widgets.WPSwipeSnackbar
import org.wordpress.android.widgets.WPViewPager2Transformer
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.regex.Pattern
import javax.inject.Inject

/*
* shows reader post detail fragments in a ViewPager - primarily used for easy swiping between
* posts with a specific tag or in a specific blog, but can also be used to show a single
* post detail.
*
* It also displays intercepted WordPress.com URls in the following forms
*
* http[s]://wordpress.com/read/blogs/{blogId}/posts/{postId}
* http[s]://wordpress.com/read/feeds/{feedId}/posts/{feedItemId}
* http[s]://{username}.wordpress.com/{year}/{month}/{day}/{postSlug}
*
* Will also handle jumping to the comments section, liking a commend and liking a post directly
*/
@AndroidEntryPoint
@Suppress("LargeClass")
class ReaderPostPagerActivity : BaseAppCompatActivity() {
    /**
     * Type of URL intercepted
     */
    private enum class InterceptType {
        READER_BLOG,
        READER_FEED,
        WPCOM_POST_SLUG
    }

    /**
     * operation to perform automatically when opened via deeplinking
     */
    enum class DirectOperation {
        COMMENT_JUMP,
        COMMENT_REPLY,
        COMMENT_LIKE,
        POST_LIKE,
    }

    private lateinit var viewPager: ViewPager2
    private var progressBar: ProgressBar? = null

    private var currentTag: ReaderTag? = null
    private var isFeed = false
    private var blogId: Long = 0
    private var postId: Long = 0
    private var commentId = 0
    private var directOperation: DirectOperation? = null
    private var interceptedUri: String? = null
    private var lastSelectedPosition = -1
    private var postListType: ReaderPostListType? = null

    private var postSlugsResolutionUnderway = false
    private var isRequestingMorePosts = false
    private var isSinglePostView = false
    private var isRelatedPostView = false

    private var backFromLogin = false

    private val trackedPositions = HashSet<Int>()

    @Inject
    lateinit var siteStore: SiteStore

    @Inject
    lateinit var readerTracker: ReaderTracker

    @Inject
    lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper

    @Inject
    lateinit var readerPostTableWrapper: ReaderPostTableWrapper

    @Inject
    lateinit var postStore: PostStore

    @Inject
    lateinit var dispatcher: Dispatcher

    @Inject
    lateinit var uploadActionUseCase: UploadActionUseCase

    @Inject
    lateinit var uploadUtilsWrapper: UploadUtilsWrapper

    @Inject
    lateinit var postSeenStatusWrapper: ReaderPostSeenStatusWrapper

    @Inject
    lateinit var seenUnseenWithCounterFeatureConfig: SeenUnseenWithCounterFeatureConfig

    @Inject
    lateinit var urlUtilsWrapper: UrlUtilsWrapper

    @Inject
    lateinit var deepLinkTrackingUtils: DeepLinkTrackingUtils

    @Inject
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Inject
    lateinit var deepLinkOpenWebLinksWithJetpackHelper: DeepLinkOpenWebLinksWithJetpackHelper

    @Inject
    lateinit var jetpackAppMigrationFlowUtils: JetpackAppMigrationFlowUtils
    private var jetpackFullScreenViewModel: JetpackFeatureFullScreenOverlayViewModel? = null

    @Inject
    lateinit var mAccountStore: AccountStore

    @Inject
    lateinit var jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper

    @Inject
    lateinit var getReadingPreferencesSyncUseCase: ReaderGetReadingPreferencesSyncUseCase

    @Suppress("LongMethod")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        jetpackFullScreenViewModel =
            ViewModelProvider(this)[JetpackFeatureFullScreenOverlayViewModel::class.java]

        setContentView(R.layout.reader_activity_post_pager)

        // Start migration flow passing deep link data if requirements are met
        if (jetpackAppMigrationFlowUtils.shouldShowMigrationFlow()) {
            val deepLinkData = PreMigrationDeepLinkData(
                intent.action,
                intent.data
            )
            jetpackAppMigrationFlowUtils.startJetpackMigrationFlow(deepLinkData)
            finish()
            return
        }

        viewPager = findViewById(R.id.viewpager)
        // lint complains about OFFSCREEN_PAGE_LIMIT, even through it's a valid constant (?)
        @SuppressLint("WrongConstant")
        viewPager.offscreenPageLimit = OFFSCREEN_PAGE_LIMIT

        progressBar = findViewById(R.id.progress_loading)

        if (savedInstanceState != null) {
            isFeed = savedInstanceState.getBoolean(ReaderConstants.ARG_IS_FEED)
            blogId = savedInstanceState.getLong(ReaderConstants.ARG_BLOG_ID)
            postId = savedInstanceState.getLong(ReaderConstants.ARG_POST_ID)
            directOperation = BundleCompat.getSerializable(
                savedInstanceState,
                ReaderConstants.ARG_DIRECT_OPERATION,
                DirectOperation::class.java
            )
            commentId = savedInstanceState.getInt(ReaderConstants.ARG_COMMENT_ID)
            isSinglePostView = savedInstanceState.getBoolean(ReaderConstants.ARG_IS_SINGLE_POST)
            isRelatedPostView = savedInstanceState.getBoolean(ReaderConstants.ARG_IS_RELATED_POST)
            interceptedUri = savedInstanceState.getString(ReaderConstants.ARG_INTERCEPTED_URI)
            if (savedInstanceState.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                postListType =
                    BundleCompat.getSerializable(
                        savedInstanceState,
                        ReaderConstants.ARG_POST_LIST_TYPE,
                        ReaderPostListType::class.java
                    )
            }
            if (savedInstanceState.containsKey(ReaderConstants.ARG_TAG)) {
                currentTag =
                    BundleCompat.getSerializable(
                        savedInstanceState,
                        ReaderConstants.ARG_TAG,
                        ReaderTag::class.java
                    )
            }
            postSlugsResolutionUnderway =
                savedInstanceState.getBoolean(ReaderConstants.KEY_POST_SLUGS_RESOLUTION_UNDERWAY)
            if (savedInstanceState.containsKey(ReaderConstants.KEY_TRACKED_POSITIONS)) {
                BundleCompat.getSerializable(
                    savedInstanceState,
                    ReaderConstants.KEY_TRACKED_POSITIONS,
                    HashSet::class.java
                )?.let { positions ->
                    @Suppress("UNCHECKED_CAST")
                    trackedPositions.addAll(positions as HashSet<Int>)
                }
            }
        } else {
            isFeed = intent.getBooleanExtra(ReaderConstants.ARG_IS_FEED, false)
            blogId = intent.getLongExtra(ReaderConstants.ARG_BLOG_ID, 0)
            postId = intent.getLongExtra(ReaderConstants.ARG_POST_ID, 0)
            commentId = intent.getIntExtra(ReaderConstants.ARG_COMMENT_ID, 0)
            isSinglePostView = intent.getBooleanExtra(ReaderConstants.ARG_IS_SINGLE_POST, false)
            isRelatedPostView = intent.getBooleanExtra(ReaderConstants.ARG_IS_RELATED_POST, false)
            interceptedUri = intent.getStringExtra(ReaderConstants.ARG_INTERCEPTED_URI)
            if (intent.hasExtra(ReaderConstants.ARG_DIRECT_OPERATION)) {
                directOperation = BundleCompat.getSerializable(
                    intent.extras!!,
                    ReaderConstants.ARG_DIRECT_OPERATION,
                    DirectOperation::class.java
                )
            }
            if (intent.hasExtra(ReaderConstants.ARG_POST_LIST_TYPE)) {
                postListType =
                    BundleCompat.getSerializable(
                        intent.extras!!,
                        ReaderConstants.ARG_POST_LIST_TYPE,
                        ReaderPostListType::class.java
                    )
            }
            if (intent.hasExtra(ReaderConstants.ARG_TAG)) {
                currentTag = BundleCompat.getSerializable(
                    intent.extras!!,
                    ReaderConstants.ARG_TAG,
                    ReaderTag::class.java
                )
            }
        }

        if (postListType == null) {
            postListType = ReaderPostListType.TAG_FOLLOWED
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                trackPostAtPositionIfNeeded(position)
                // pause the previous web view - otherwise embedded content will continue to play
                if (lastSelectedPosition > -1 && lastSelectedPosition != position) {
                    pagerAdapter?.getFragmentAtPosition(lastSelectedPosition)?.pauseWebView()
                }
                // unpause this web view if it was previously paused
                pagerAdapter?.getFragmentAtPosition(position)?.resumeWebViewIfPaused()
                lastSelectedPosition = position
            }
        })

        onBackPressedDispatcher.addCallback(this) {
            pagerAdapter?.getFragmentAtPosition(lastSelectedPosition)?.let { fragment ->
                // if full screen video is showing, hide the custom view rather than navigate back
                if (fragment.isCustomViewShowing) {
                    fragment.hideCustomView()
                } else if (!fragment.goBackInPostHistory()) {
                    onBackPressedDispatcher.onBackPressedCompat(this)
                }
            }
        }

        viewPager.setPageTransformer(
            WPViewPager2Transformer(WPViewPager2Transformer.TransformType.SlideOver)
        )

        observeOverlayEvents()
    }

    @Suppress("DEPRECATION")
    override fun onCreateView(
        parent: View?, name: String, context: Context,
        attrs: AttributeSet
    ): View? {
        // enable full screen for Android 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            window.setDecorFitsSystemWindows(false)
            val controller =
                WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        return super.onCreateView(parent, name, context, attrs)
    }

    private fun observeOverlayEvents() {
        jetpackFullScreenViewModel!!.action.observe(
            this
        ) { action: JetpackFeatureOverlayActions? ->
            if (action is ForwardToJetpack) {
                if (!deepLinkOpenWebLinksWithJetpackHelper.handleOpenLinksInJetpackIfPossible()) {
                    finishDeepLinkRequestFromOverlay(intent.action!!, intent.data!!)
                } else {
                    WPActivityUtils.disableReaderDeeplinks(this)
                    ActivityLauncher.openJetpackForDeeplink(
                        this, intent.action,
                        UriWrapper(intent.data!!)
                    )
                    finish()
                }
            } else {
                finishDeepLinkRequestFromOverlay(intent.action!!, intent.data!!)
            }
        }
    }

    private fun handleDeepLinking() {
        val action = intent.action
        val uri = intent.data

        var host: String? = ""
        if (uri != null) {
            host = uri.host
        }

        if (uri == null || jetpackFeatureRemovalPhaseHelper.shouldRemoveJetpackFeatures()
            || jetpackFeatureRemovalPhaseHelper.shouldShowStaticPage()
        ) {
            readerTracker.trackDeepLink(AnalyticsTracker.Stat.DEEP_LINKED, action!!, host!!, uri)
            // invalid uri so, just show the entry screen
            if (jetpackFeatureRemovalPhaseHelper.shouldShowStaticPage()) {
                val intent = Intent(this, WPMainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra(WPMainActivity.ARG_OPEN_PAGE, WPMainActivity.ARG_READER)
                startActivity(intent)
            } else {
                val intent = Intent(this, WPLaunchActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra(WPMainActivity.ARG_OPEN_PAGE, WPMainActivity.ARG_READER)
                startActivity(intent)
            }
            finish()
            return
        }

        if (!checkAndShowOpenWebLinksWithJetpackOverlayIfNeeded()) {
            finishDeepLinkRequest(action!!, uri)
        }
    }

    private fun finishDeepLinkRequestFromOverlay(action: String, uri: Uri) {
        finishDeepLinkRequest(action, uri)
        // We interrupted the normal flow to show the overly, we now need to rerun these methods on a dismiss action
        loadPosts(blogId, postId)
        backFromLogin = false
    }

    @Suppress("NestedBlockDepth", "MagicNumber")
    private fun finishDeepLinkRequest(action: String, uri: Uri) {
        var interceptType = InterceptType.READER_BLOG
        var blogIdentifier: String? = null // can be an id or a slug
        var postIdentifier: String? = null // can be an id or a slug

        interceptedUri = uri.toString()

        val segments = uri.pathSegments

        // Handled URLs look like this: http[s]://wordpress.com/read/feeds/{feedId}/posts/{feedItemId}
        // or http[s]://wordpress.com/reader/feeds/{feedId}/posts/{feedItemId}
        // with the first segment being 'read' or 'reader'.
        if (segments != null) {
            // Builds stripped URI for tracking purposes
            val wrappedUri = UriWrapper(uri)
            if (segments[0] == "read" || segments[0] == "reader") {
                if (segments.size > 2) {
                    blogIdentifier = segments[2]

                    if (segments[1] == "blogs") {
                        interceptType = InterceptType.READER_BLOG
                    } else if (segments[1] == "feeds") {
                        interceptType = InterceptType.READER_FEED
                        isFeed = true
                    }
                }

                if (segments.size > 4 && segments[3] == "posts") {
                    postIdentifier = segments[4]
                }

                parseFragment(uri)
                deepLinkTrackingUtils.track(action, OpenInReader(wrappedUri), wrappedUri)
                showPost(interceptType, blogIdentifier, postIdentifier)
                return
            } else if (segments.size >= 4) {
                blogIdentifier = uri.host
                try {
                    postIdentifier = URLEncoder.encode(segments[3], "UTF-8")
                } catch (e: UnsupportedEncodingException) {
                    AppLog.e(AppLog.T.READER, e)
                    ToastUtils.showToast(this, R.string.error_generic)
                }

                parseFragment(uri)
                detectLike(uri)

                interceptType = InterceptType.WPCOM_POST_SLUG
                deepLinkTrackingUtils.track(action, OpenInReader(wrappedUri), wrappedUri)
                showPost(interceptType, blogIdentifier, postIdentifier)
                return
            }
        }

        // at this point, just show the entry screen
        val intent = Intent(this, WPLaunchActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    @Suppress("MagicNumber")
    private fun showPost(
        interceptType: InterceptType,
        blogIdentifier: String?,
        postIdentifier: String?
    ) {
        if (!blogIdentifier.isNullOrEmpty() && !postIdentifier.isNullOrEmpty()) {
            isSinglePostView = true
            isRelatedPostView = false

            when (interceptType) {
                InterceptType.READER_BLOG -> if (parseIds(
                        blogIdentifier,
                        postIdentifier
                    )
                ) {
                    readerTracker.trackBlogPost(
                        AnalyticsTracker.Stat.READER_BLOG_POST_INTERCEPTED,
                        blogId,
                        postId
                    )
                    // IDs have now been set so, let ReaderPostPagerActivity normally display the post
                } else {
                    ToastUtils.showToast(this, R.string.error_generic)
                }

                InterceptType.READER_FEED -> if (parseIds(blogIdentifier, postIdentifier)) {
                    readerTracker.trackFeedPost(
                        AnalyticsTracker.Stat.READER_FEED_POST_INTERCEPTED,
                        blogId,
                        postId
                    )
                    // IDs have now been set so, let ReaderPostPagerActivity normally display the post
                } else {
                    ToastUtils.showToast(this, R.string.error_generic)
                }

                InterceptType.WPCOM_POST_SLUG -> {
                    readerTracker.trackBlogPost(
                        AnalyticsTracker.Stat.READER_WPCOM_BLOG_POST_INTERCEPTED,
                        blogIdentifier,
                        postIdentifier,
                        commentId
                    )

                    // try to get the post from the local db
                    val post = ReaderPostTable.getBlogPost(blogIdentifier, postIdentifier, true)
                    if (post != null) {
                        // set the IDs and let ReaderPostPagerActivity normally display the post
                        blogId = post.blogId
                        postId = post.postId
                    } else {
                        // not stored locally, so request it
                        ReaderPostActions.requestBlogPost(
                            blogIdentifier, postIdentifier,
                            object : OnRequestListener<String?> {
                                override fun onSuccess(blogUrl: String?) {
                                    postSlugsResolutionUnderway = false

                                    // the scheme is removed to match the query pattern in ReaderPostTable
                                    // .getBlogPost
                                    val primaryBlogIdentifier = urlUtilsWrapper.removeScheme(
                                        blogUrl!!
                                    )

                                    // getBlogPost utilizes the primaryBlogIdentifier instead of blogIdentifier
                                    // since
                                    // the custom and *.wordpress.com domains need to be used interchangeably since
                                    // they can both be used as the primary domain when identifying the blog_url
                                    // in the ReaderPostTable query.
                                    val readerPost =
                                        ReaderPostTable.getBlogPost(
                                            primaryBlogIdentifier, postIdentifier,
                                            true
                                        )
                                    val slugsResolved = if (readerPost != null)
                                        PostSlugsRequestCompleted(
                                            200, readerPost.blogId,
                                            readerPost.postId
                                        )
                                    else
                                        PostSlugsRequestCompleted(200, 0, 0)
                                    // notify that the slug resolution request has completed
                                    EventBus.getDefault().post(slugsResolved)

                                    // post wasn't available locally earlier so, track it now
                                    if (readerPost != null) {
                                        trackPost(readerPost.blogId, readerPost.postId)
                                    }
                                }

                                override fun onFailure(statusCode: Int) {
                                    postSlugsResolutionUnderway = false
                                    // notify that the slug resolution request has completed
                                    EventBus.getDefault()
                                        .post(PostSlugsRequestCompleted(statusCode, 0, 0))
                                }
                            })
                        postSlugsResolutionUnderway = true
                    }
                }
            }
        } else {
            ToastUtils.showToast(this, R.string.error_generic)
        }
    }

    private fun parseIds(blogIdentifier: String, postIdentifier: String): Boolean {
        try {
            blogId = blogIdentifier.toLong()
            postId = postIdentifier.toLong()
            return true
        } catch (e: NumberFormatException) {
            AppLog.e(AppLog.T.READER, e)
            return false
        }
    }

    @Suppress("ReturnCount")
    private fun checkAndShowOpenWebLinksWithJetpackOverlayIfNeeded(): Boolean {
        if (!isSignedInWPComOrHasWPOrgSite) return false

        if (!deepLinkOpenWebLinksWithJetpackHelper.shouldShowOpenLinksInJetpackOverlay()) return false

        deepLinkOpenWebLinksWithJetpackHelper.onOverlayShown()
        newInstance(
            null,
            isSiteCreationOverlay = false,
            isDeepLinkOverlay = true,
            siteCreationSource = SiteCreationSource.UNSPECIFIED,
            isFeatureCollectionOverlay = false,
            featureCollectionOverlaySource = JetpackFeatureCollectionOverlaySource.UNSPECIFIED
        )
            .show(supportFragmentManager, JetpackFeatureFullScreenOverlayFragment.TAG)
        return true
    }

    private val isSignedInWPComOrHasWPOrgSite: Boolean
        get() {
            return FluxCUtils.isSignedInWPComOrHasWPOrgSite(mAccountStore, siteStore)
        }

    /**
     * Parse the URL fragment and interpret it as an operation to perform. For example, a "#comments" fragment is
     * interpreted as a direct jump into the comments section of the post.
     *
     * @param uri the full URI input, including the fragment
     */
    @Suppress("ReturnCount")
    private fun parseFragment(uri: Uri?) {
        // default to do-nothing w.r.t. comments
        directOperation = null

        if (uri == null || uri.fragment == null) {
            return
        }

        val fragment: CharSequence = uri.fragment ?: ""

        val fragmentCommentsPattern = Pattern.compile("comments", Pattern.CASE_INSENSITIVE)
        val fragmentCommentIdPattern = Pattern.compile("comment-(\\d+)", Pattern.CASE_INSENSITIVE)
        val fragmentRespondPattern = Pattern.compile("respond", Pattern.CASE_INSENSITIVE)

        // check for the general "#comments" fragment to jump to the comments section
        val commentsMatcher = fragmentCommentsPattern.matcher(fragment)
        if (commentsMatcher.matches()) {
            directOperation = DirectOperation.COMMENT_JUMP
            commentId = 0
            return
        }

        // check for the "#respond" fragment to jump to the reply box
        val respondMatcher = fragmentRespondPattern.matcher(fragment)
        if (respondMatcher.matches()) {
            directOperation = DirectOperation.COMMENT_REPLY

            // check whether we are to reply to a specific comment
            val replyToCommentId = uri.getQueryParameter("replytocom")
            if (replyToCommentId != null) {
                try {
                    commentId = replyToCommentId.toInt()
                } catch (e: NumberFormatException) {
                    AppLog.e(
                        AppLog.T.UTILS,
                        "replytocom cannot be converted to int$replyToCommentId", e
                    )
                }
            }

            return
        }

        // check for the "#comment-xyz" fragment to jump to a specific comment
        val commentIdMatcher = fragmentCommentIdPattern.matcher(fragment)
        if (commentIdMatcher.find() && commentIdMatcher.groupCount() > 0) {
            commentIdMatcher.group(1)?.toInt() ?: 0
            directOperation = DirectOperation.COMMENT_JUMP
        }
    }

    /**
     * Parse the URL query parameters and detect attempt to like a post or a comment
     *
     * @param uri the full URI input, including the query parameters
     */
    private fun detectLike(uri: Uri) {
        // check whether we are to like something
        val doLike = "1" == uri.getQueryParameter("like")
        val likeActor = uri.getQueryParameter("like_actor")

        if (doLike && likeActor != null && likeActor.trim { it <= ' ' }.isNotEmpty()) {
            directOperation = DirectOperation.POST_LIKE

            // check whether we are to like a specific comment
            val likeCommentId = uri.getQueryParameter("commentid")
            if (likeCommentId != null) {
                try {
                    commentId = likeCommentId.toInt()
                    directOperation = DirectOperation.COMMENT_LIKE
                } catch (e: NumberFormatException) {
                    AppLog.e(
                        AppLog.T.UTILS,
                        "commentid cannot be converted to int$likeCommentId", e
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AppLog.d(AppLog.T.READER, "TRACK READER ReaderPostPagerActivity > START Count")
        readerTracker.start(ReaderTrackerType.PAGED_POST)
        EventBus.getDefault().register(this)

        // We register the dispatcher in order to receive the OnPostUploaded event and show the snackbar
        dispatcher.register(this)

        if (!hasPagerAdapter() || backFromLogin) {
            if (ActivityUtils.isDeepLinking(intent) || (ReaderConstants.ACTION_VIEW_POST
                        == intent.action)
            ) {
                handleDeepLinking()
            }

            loadPosts(blogId, postId)

            // clear up the back-from-login flag anyway
            backFromLogin = false
        }
    }

    override fun onPause() {
        super.onPause()
        AppLog.d(AppLog.T.READER, "TRACK READER ReaderPostPagerActivity > STOP Count")
        readerTracker.stop(ReaderTrackerType.PAGED_POST)
        EventBus.getDefault().unregister(this)
        dispatcher.unregister(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun hasPagerAdapter() = viewPager.adapter != null

    private val pagerAdapter: PostPagerAdapter?
        get() = viewPager.adapter as? PostPagerAdapter

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(ReaderConstants.ARG_IS_SINGLE_POST, isSinglePostView)
        outState.putBoolean(ReaderConstants.ARG_IS_RELATED_POST, isRelatedPostView)
        outState.putString(ReaderConstants.ARG_INTERCEPTED_URI, interceptedUri)

        outState.putSerializable(ReaderConstants.ARG_DIRECT_OPERATION, directOperation)
        outState.putInt(ReaderConstants.ARG_COMMENT_ID, commentId)

        if (hasCurrentTag()) {
            outState.putSerializable(ReaderConstants.ARG_TAG, currentTag)
        }
        if (postListType != null) {
            outState.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, postListType)
        }

        val id = adapterCurrentBlogIdPostId
        if (id != null) {
            outState.putLong(ReaderConstants.ARG_BLOG_ID, id.blogId)
            outState.putLong(ReaderConstants.ARG_POST_ID, id.postId)
        }

        outState.putBoolean(
            ReaderConstants.KEY_POST_SLUGS_RESOLUTION_UNDERWAY,
            postSlugsResolutionUnderway
        )

        if (trackedPositions.size > 0) {
            outState.putSerializable(ReaderConstants.KEY_TRACKED_POSITIONS, trackedPositions)
        }

        super.onSaveInstanceState(outState)
    }

    private val adapterCurrentBlogIdPostId: ReaderBlogIdPostId?
        get() = pagerAdapter?.currentBlogIdPostId

    private fun getAdapterBlogIdPostIdAtPosition(position: Int) =
        pagerAdapter?.getBlogIdPostIdAtPosition(position)

    /*
     * perform analytics tracking and bump the page view for the post at the passed position
     * if it hasn't already been done
     */
    private fun trackPostAtPositionIfNeeded(position: Int) {
        if (!hasPagerAdapter() || trackedPositions.contains(position)) {
            return
        }

        val idPair = getAdapterBlogIdPostIdAtPosition(position) ?: return

        AppLog.d(
            AppLog.T.READER,
            "reader pager > tracking post at position $position"
        )
        trackedPositions.add(position)

        trackPost(idPair.blogId, idPair.postId)
    }

    /*
     * perform analytics tracking and bump the page view for the post
     */
    private fun trackPost(blogId: Long, postId: Long) {
        // bump the page view
        ReaderPostActions.bumpPageViewForPost(siteStore, blogId, postId)

        if (seenUnseenWithCounterFeatureConfig.isEnabled()) {
            val currentPost = ReaderPostTable.getBlogPost(blogId, postId, true)
            if (currentPost != null) {
                postSeenStatusWrapper.markPostAsSeenSilently(currentPost)
            }
        }

        // analytics tracking
        readerTracker.trackPost(
            AnalyticsTracker.Stat.READER_ARTICLE_OPENED,
            readerPostTableWrapper.getBlogPost(blogId, postId, true),
            getReadingPreferencesSyncUseCase.invoke()
        )
    }

    /*
     * loads the blogId/postId pairs used to populate the pager adapter - passed blogId/postId will
     * be made active after loading unless gotoNext=true, in which case the post after the passed
     * one will be made active
     */
    private fun loadPosts(blogId: Long, postId: Long) {
        object : Thread() {
            override fun run() {
                val idList: ReaderBlogIdPostIdList
                if (isSinglePostView) {
                    idList = ReaderBlogIdPostIdList()
                    idList.add(ReaderBlogIdPostId(blogId, postId))
                } else {
                    val maxPosts = ReaderConstants.READER_MAX_POSTS_TO_DISPLAY
                    idList =
                        when (postListType) {
                            ReaderPostListType.TAG_FOLLOWED,
                            ReaderPostListType.TAG_PREVIEW ->
                                ReaderPostTable.getBlogIdPostIdsWithTag(currentTag, maxPosts)

                            ReaderPostListType.BLOG_PREVIEW -> ReaderPostTable.getBlogIdPostIdsInBlog(
                                blogId,
                                maxPosts
                            )

                            ReaderPostListType.SEARCH_RESULTS -> return
                            else -> return
                        }
                }

                val currentPosition = viewPager.currentItem
                val newPosition = idList.indexOf(blogId, postId)

                runOnUiThread {
                    if (isFinishing) {
                        return@runOnUiThread
                    }
                    AppLog.d(
                        AppLog.T.READER,
                        "reader pager > creating adapter"
                    )
                    val adapter = PostPagerAdapter(idList)
                    viewPager.adapter = adapter

                    // set the current position without smooth scrolling - otherwise the previous post in
                    // the list may briefly appear
                    if (adapter.isValidPosition(newPosition)) {
                        viewPager.setCurrentItem(newPosition, false)
                        trackPostAtPositionIfNeeded(newPosition)
                    } else if (adapter.isValidPosition(currentPosition)) {
                        viewPager.setCurrentItem(currentPosition, false)
                        trackPostAtPositionIfNeeded(currentPosition)
                    }

                    // let the user know they can swipe between posts
                    if (adapter.itemCount > 1 && !AppPrefs.isReaderSwipeToNavigateShown()) {
                        WPSwipeSnackbar.show(viewPager)
                        AppPrefs.setReaderSwipeToNavigateShown(true)
                    }
                }
            }
        }.start()
    }

    private fun hasCurrentTag() = currentTag != null

    /*
     * called when user scrolls towards the last posts - requests older posts with the
     * current tag or in the current blog
     */
    private fun requestMorePosts() {
        if (isRequestingMorePosts) {
            return
        }

        AppLog.d(AppLog.T.READER, "reader pager > requesting older posts")
        when (postListType) {
            ReaderPostListType.TAG_PREVIEW,
            ReaderPostListType.TAG_FOLLOWED ->
                ReaderPostServiceStarter.startServiceForTag(
                    this,
                    currentTag,
                    ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER
                )

            ReaderPostListType.BLOG_PREVIEW -> ReaderPostServiceStarter.startServiceForBlog(
                this,
                blogId,
                ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER
            )

            ReaderPostListType.SEARCH_RESULTS -> {}

            ReaderPostListType.TAGS_FEED -> {}

            else -> {}
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: UpdatePostsStarted?) {
        if (isFinishing) {
            return
        }

        isRequestingMorePosts = true
        progressBar!!.visibility = View.VISIBLE
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: UpdatePostsEnded) {
        if (isFinishing) {
            return
        }

        val adapter = pagerAdapter ?: return

        isRequestingMorePosts = false
        progressBar?.visibility = View.GONE

        if (event.result == ReaderActions.UpdateResult.HAS_NEW) {
            AppLog.d(AppLog.T.READER, "reader pager > older posts received")
            // remember which post to keep active
            val id = adapter.currentBlogIdPostId
            val blogId = (id?.blogId ?: 0)
            val postId = (id?.postId ?: 0)
            loadPosts(blogId, postId)
        } else {
            AppLog.d(AppLog.T.READER, "reader pager > all posts loaded")
            adapter.allPostsLoaded = true
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: DoSignIn?) {
        if (isFinishing) {
            return
        }

        readerTracker.trackUri(AnalyticsTracker.Stat.READER_SIGN_IN_INITIATED, interceptedUri!!)
        ActivityLauncher.loginWithoutMagicLink(this)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            RequestCodes.EDIT_POST -> {
                if (resultCode != RESULT_OK || data == null || isFinishing) {
                    return
                }
                val localId = data.getIntExtra(EditorConstants.EXTRA_POST_LOCAL_ID, 0)
                val site = data.extras?.let {
                    BundleCompat.getSerializable(
                        it,
                        WordPress.SITE,
                        SiteModel::class.java
                    )
                }
                val post = postStore.getPostByLocalPostId(localId)

                if (EditorLauncher.checkToRestart(data)) {
                    ActivityLauncher.editPostOrPageForResult(
                        data,
                        this@ReaderPostPagerActivity, site,
                        data.getIntExtra(EditorConstants.EXTRA_POST_LOCAL_ID, 0)
                    )

                    // a restart will happen so, no need to continue here
                    return
                }

                val snackbarAttachView = findViewById<View>(R.id.coordinator)
                if (site != null && post != null && snackbarAttachView != null) {
                    uploadUtilsWrapper.handleEditPostResultSnackbars(
                        this,
                        snackbarAttachView,
                        data,
                        post,
                        site,
                        uploadActionUseCase.getUploadAction(post),
                        { _: View? ->
                            UploadUtils.publishPost(
                                this@ReaderPostPagerActivity,
                                post,
                                site,
                                dispatcher
                            )
                        })
                }
            }

            RequestCodes.DO_LOGIN -> if (resultCode == RESULT_OK) {
                backFromLogin = true
            }

            RequestCodes.NO_REBLOG_SITE -> if (resultCode == RESULT_OK) {
                finish() // Finish activity to make My Site page visible
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostUploaded(event: OnPostUploaded) {
        val site = siteStore.getSiteByLocalId(selectedSiteRepository.getSelectedSiteLocalId())
        val snackbarAttachView = findViewById<View>(R.id.coordinator)
        if (site != null && event.post != null && snackbarAttachView != null) {
            uploadUtilsWrapper.onPostUploadedSnackbarHandler(
                this,
                snackbarAttachView,
                event.isError,
                event.isFirstTimePublish,
                event.post,
                null,
                site
            )
        }
    }

    /**
     * ViewPager2 adapter containing post detail fragments
     */
    private inner class PostPagerAdapter(
        private val idList: ReaderBlogIdPostIdList,
    ) : FragmentStateAdapter(this@ReaderPostPagerActivity) {
        var allPostsLoaded: Boolean = false

        fun canRequestMostPosts(): Boolean {
            return !allPostsLoaded &&
                    !isSinglePostView &&
                    (idList.size < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY)
                    && NetworkUtils.isNetworkAvailable(this@ReaderPostPagerActivity)
        }

        fun isValidPosition(position: Int) = position in 0..< itemCount

        override fun getItemCount() = idList.size

        override fun createFragment(position: Int): Fragment {
            if ((position == itemCount - 1) && canRequestMostPosts()) {
                requestMorePosts()
            }

            return newInstance(
                isFeed = isFeed,
                blogId = idList[position].blogId,
                postId = idList[position].postId,
                directOperation = directOperation,
                commentId = commentId,
                isRelatedPost = isRelatedPostView,
                interceptedUri = interceptedUri,
                postListType = postListType,
                postSlugsResolutionUnderway = postSlugsResolutionUnderway
            )
        }

        val currentBlogIdPostId: ReaderBlogIdPostId?
            get() = getBlogIdPostIdAtPosition(viewPager.currentItem)

        fun getBlogIdPostIdAtPosition(position: Int): ReaderBlogIdPostId? {
            return if (isValidPosition(position)) {
                idList[position]
            } else {
                null
            }
        }

        /**
         * In ViewPager2 the FragmentManager by default have assigned tags to fragments like this:
         *  Fragment in 1st position has a tag of "f0"
         *  Fragment in 2nd position has a tag of "f1"
         *  etc.
         */
        fun getFragmentAtPosition(position: Int) =
                supportFragmentManager.findFragmentByTag("f$position") as? ReaderPostDetailFragment
    }

    companion object {
        private const val OFFSCREEN_PAGE_LIMIT = 2
    }
}
