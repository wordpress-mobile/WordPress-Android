package org.wordpress.android.ui.reader

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.os.BundleCompat
import androidx.fragment.compose.AndroidFragment
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.datasets.ReaderBlogTable
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.posts.EditorConstants
import org.wordpress.android.ui.posts.EditorLauncher
import org.wordpress.android.ui.reader.ReaderInterfaces.OnBlogInfoFailedListener
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.views.compose.enableNestedScrollInterop
import org.wordpress.android.ui.uploads.UploadActionUseCase
import org.wordpress.android.ui.uploads.UploadUtils
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.extensions.onBackPressedCompat
import org.wordpress.android.util.extensions.setContent
import javax.inject.Inject

/*
* serves as the host for ReaderPostListFragment when showing blog preview & tag preview
*/
@AndroidEntryPoint
class ReaderPostListActivity : BaseAppCompatActivity(), OnBlogInfoFailedListener {
    private var source: String? = null
    private var postListType: ReaderPostListType = ReaderTypes.DEFAULT_POST_LIST_TYPE
    private var siteId: Long = 0
    private var listFragment: ReaderPostListFragment? = null

    @Inject
    lateinit var siteStore: SiteStore

    @Inject
    lateinit var postStore: PostStore

    @Inject
    lateinit var dispatcher: Dispatcher

    @Inject
    lateinit var uploadActionUseCase: UploadActionUseCase

    @Inject
    lateinit var uploadUtilsWrapper: UploadUtilsWrapper

    @Inject
    lateinit var readerTracker: ReaderTracker

    @Inject
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Inject
    lateinit var appLogWrapper: AppLogWrapper

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        source = intent.getStringExtra(ReaderConstants.ARG_SOURCE)
        postListType = if (intent.hasExtra(ReaderConstants.ARG_POST_LIST_TYPE)) {
            BundleCompat.getSerializable(
                intent.extras!!,
                ReaderConstants.ARG_POST_LIST_TYPE,
                ReaderPostListType::class.java
            )!!
        } else {
            ReaderTypes.DEFAULT_POST_LIST_TYPE
        }
        // everything shown here comes from the intent, so there's no state to save; AndroidFragment restores the list
        val listArguments = listArguments()
        val title = screenTitle()
        // also the window's title, which TalkBack announces and Recents shows
        setTitle(title)
        setupBackPressCallback()
        setContent {
            AppThemeM3 {
                PostListScreen(title = title, listArguments = listArguments)
            }
        }
    }

    /*
     * the arguments for the list fragment, or null if there's nothing to show
     */
    private fun listArguments(): Bundle? = when (postListType) {
        ReaderPostListType.BLOG_PREVIEW -> {
            val feedId = intent.getLongExtra(ReaderConstants.ARG_FEED_ID, 0)
            if (feedId != 0L) {
                siteId = feedId
                ReaderPostListFragment.argumentsForFeed(feedId)
            } else {
                siteId = intent.getLongExtra(ReaderConstants.ARG_BLOG_ID, 0)
                ReaderPostListFragment.argumentsForBlog(siteId)
            }
        }
        ReaderPostListType.TAG_PREVIEW -> intent.extras
            ?.let { BundleCompat.getSerializable(it, ReaderConstants.ARG_TAG, ReaderTag::class.java) }
            ?.let { ReaderPostListFragment.argumentsForTag(it, postListType) }
        else -> null
    }

    /*
     * a feed shows its name; blogs and tags show their own header in the list, so the title is empty
     */
    private fun screenTitle(): String {
        val feedId = intent.getLongExtra(ReaderConstants.ARG_FEED_ID, 0)
        if (postListType != ReaderPostListType.BLOG_PREVIEW || feedId == 0L) return ""
        return ReaderBlogTable.getFeedName(feedId).ifEmpty { getString(R.string.reader_activity_title_blog_preview) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PostListScreen(title: String, listArguments: Bundle?) {
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            // BaseAppCompatActivity has already applied the window insets
            contentWindowInsets = WindowInsets(0),
            topBar = {
                TopAppBar(
                    title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = ::finish) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    },
                    actions = {
                        if (postListType == ReaderPostListType.BLOG_PREVIEW) {
                            IconButton(onClick = ::shareSite) {
                                Icon(
                                    painterResource(R.drawable.ic_share_white_24dp),
                                    stringResource(R.string.share_action)
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        ) { contentPadding ->
            listArguments?.let {
                AndroidFragment<ReaderPostListFragment>(
                    modifier = Modifier
                        .padding(contentPadding)
                        .fillMaxSize(),
                    arguments = it
                ) { fragment ->
                    listFragment = fragment
                    fragment.enableNestedScrollInterop()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // We register the dispatcher in order to receive the OnPostUploaded event and show the snackbar
        dispatcher.register(this)
    }

    override fun onPause() {
        super.onPause()
        dispatcher.unregister(this)
    }

    private fun setupBackPressCallback() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (listFragment?.onActivityBackPressed() != true) {
                    onBackPressedDispatcher.onBackPressedCompat(this)
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    @Suppress("SwallowedException")
    private fun shareSite() {
        val blog = ReaderBlogTable.getBlogInfo(siteId)

        if (blog?.hasUrl() == true) {
            val intent = Intent(Intent.ACTION_SEND)
            intent.setType("text/plain")
            intent.putExtra(Intent.EXTRA_TEXT, blog.url)
            if (blog.hasName()) {
                intent.putExtra(Intent.EXTRA_SUBJECT, blog.name)
            }

            try {
                readerTracker.trackBlog(
                    AnalyticsTracker.Stat.READER_SITE_SHARED,
                    blog.blogId,
                    blog.feedId,
                    blog.isFollowing,
                    source ?: ""
                )
                startActivity(Intent.createChooser(intent, getString(R.string.share_link)))
            } catch (e: ActivityNotFoundException) {
                ToastUtils.showToast(
                    this@ReaderPostListActivity,
                    R.string.reader_toast_err_share_intent
                )
            }
        } else {
            ToastUtils.showToast(
                this@ReaderPostListActivity,
                R.string.reader_toast_err_share_intent
            )
        }
    }

    /**
     * Returns the view to attach the snackbar to
     */
    private fun getSnackbarAttachView(): View? = findViewById(android.R.id.content)

    override fun onBlogInfoFailed() {
        appLogWrapper.d(AppLog.T.READER, "Blog or feed not found for siteId: $siteId")
        ToastUtils.showToast(this, R.string.reader_toast_err_blog_not_found)
        finish()
    }

    @Deprecated("Deprecated in Java")
    @Suppress("NestedBlockDepth")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            RequestCodes.NO_REBLOG_SITE -> if (resultCode == RESULT_OK) {
                finish() // Finish activity to make My Site page visible
            }

            RequestCodes.EDIT_POST -> if (resultCode == RESULT_OK && data != null && !isFinishing) {
                val localId = data.getIntExtra(EditorConstants.EXTRA_POST_LOCAL_ID, 0)
                val site = BundleCompat.getSerializable(
                    data.extras!!,
                    WordPress.SITE,
                    SiteModel::class.java
                )
                val post = postStore.getPostByLocalPostId(localId)

                if (EditorLauncher.checkToRestart(data)) {
                    ActivityLauncher.editPostOrPageForResult(
                        data,
                        this@ReaderPostListActivity, site,
                        data.getIntExtra(EditorConstants.EXTRA_POST_LOCAL_ID, 0)
                    )
                    // a restart will happen so no need to continue here
                    return
                }

                if (site != null && post != null) {
                    getSnackbarAttachView()?.let { snackbarAttachView ->
                        uploadUtilsWrapper.handleEditPostResultSnackbars(
                            activity = this,
                            snackbarAttachView = snackbarAttachView,
                            data = data,
                            post = post,
                            site = site,
                            uploadAction = uploadActionUseCase.getUploadAction(post),
                            publishPostListener = {
                                UploadUtils.publishPost(
                                    this@ReaderPostListActivity,
                                    post,
                                    site,
                                    dispatcher
                                )
                            })
                    }
                }
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostUploaded(event: OnPostUploaded) {
        val site = siteStore.getSiteByLocalId(selectedSiteRepository.getSelectedSiteLocalId())
        if (site != null && event.post != null) {
            getSnackbarAttachView()?.let { snackbarAttachView ->
                uploadUtilsWrapper.onPostUploadedSnackbarHandler(
                    activity = this,
                    snackbarAttachView = snackbarAttachView,
                    isError = event.isError,
                    isFirstTimePublish = event.isFirstTimePublish,
                    post = event.post,
                    errorMessage = null,
                    site = site
                )
            }
        }
    }
}
