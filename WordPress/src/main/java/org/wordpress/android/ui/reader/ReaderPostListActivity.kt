package org.wordpress.android.ui.reader

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.os.BundleCompat
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.databinding.ReaderActivityPostListBinding
import org.wordpress.android.datasets.ReaderBlogTable
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.posts.EditPostActivity.Companion.checkToRestart
import org.wordpress.android.ui.posts.EditPostActivityConstants
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.uploads.UploadActionUseCase
import org.wordpress.android.ui.uploads.UploadUtils
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.extensions.onBackPressedCompat
import javax.inject.Inject

/*
* serves as the host for ReaderPostListFragment when showing blog preview & tag preview
*/
@AndroidEntryPoint
class ReaderPostListActivity : BaseAppCompatActivity() {
    private var source: String? = null
    private var postListType: ReaderPostListType = ReaderTypes.DEFAULT_POST_LIST_TYPE
    private var siteId: Long = 0
    private lateinit var binding: ReaderActivityPostListBinding

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

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ReaderActivityPostListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        restoreState(savedInstanceState)
        setupBackPressCallback()
        setupToolbar()
    }

    private fun restoreState(savedInstanceState: Bundle?) {
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

        if (postListType == ReaderPostListType.BLOG_PREVIEW) {
            setTitle(R.string.reader_activity_title_blog_preview)
            if (savedInstanceState == null) {
                val blogId = intent.getLongExtra(ReaderConstants.ARG_BLOG_ID, 0)
                val feedId = intent.getLongExtra(ReaderConstants.ARG_FEED_ID, 0)
                if (feedId != 0L) {
                    showListFragmentForFeed(feedId)
                    siteId = feedId
                } else {
                    showListFragmentForBlog(blogId)
                    siteId = blogId
                }
            } else {
                siteId = savedInstanceState.getLong(ReaderConstants.KEY_SITE_ID)
            }
        } else if (postListType == ReaderPostListType.TAG_PREVIEW) {
            setTitle(R.string.reader_activity_title_tag_preview)
            if (intent.hasExtra(ReaderConstants.ARG_TAG)) {
                val tag =
                    BundleCompat.getSerializable(
                        intent.extras!!,
                        ReaderConstants.ARG_TAG,
                        ReaderTag::class.java
                    )
                if (tag != null && savedInstanceState == null) {
                    showListFragmentForTag(tag, postListType)
                }
            }
        }

        // restore the activity title
        if (savedInstanceState?.containsKey(ReaderConstants.KEY_ACTIVITY_TITLE) == true) {
            title = savedInstanceState.getString(ReaderConstants.KEY_ACTIVITY_TITLE)
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        // this particular Activity doesn't show filtering, so we'll disable the FilteredRecyclerView toolbar here
        disableFilteredRecyclerViewToolbar()
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
                val fragment = getListFragment()
                if (fragment == null || !fragment.onActivityBackPressed()) {
                    onBackPressedDispatcher.onBackPressedCompat(this)
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarMain)
        supportActionBar?.let {
            it.setDisplayShowTitleEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }

        if (postListType == ReaderPostListType.TAG_PREVIEW || postListType == ReaderPostListType.BLOG_PREVIEW) {
            binding.toolbarMain.setNavigationOnClickListener {
                finish()
            }
        }
    }

    /*
     * This method hides the FilteredRecyclerView toolbar with spinner so to disable content filtering. These
     * views are not part of this activity.
     */
    private fun disableFilteredRecyclerViewToolbar() {
        // make it invisible - setting height to zero here because setting visibility to View.GONE wouldn't take the
        // occupied space, as otherwise expected
        findViewById<AppBarLayout>(R.id.app_bar_layout)?.let { appBarLayout ->
            val params = appBarLayout.layoutParams as CoordinatorLayout.LayoutParams
            params.height = 0
            appBarLayout.layoutParams = params
        }

        // disabling any CoordinatorLayout behavior for scrolling
        findViewById<Toolbar>(R.id.toolbar_with_spinner)?.let { toolbarWithSpinner ->
            val params = toolbarWithSpinner.layoutParams as AppBarLayout.LayoutParams
            params.scrollFlags = 0
            toolbarWithSpinner.layoutParams = params
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        // store the title for blog/tag preview so we can restore it upon recreation
        if (postListType == ReaderPostListType.BLOG_PREVIEW || postListType == ReaderPostListType.TAG_PREVIEW) {
            outState.putString(ReaderConstants.KEY_ACTIVITY_TITLE, title.toString())
            outState.putLong(ReaderConstants.KEY_SITE_ID, siteId)
        }

        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (postListType == ReaderPostListType.BLOG_PREVIEW) {
            menuInflater.inflate(R.menu.share, menu)
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }

            R.id.menu_share -> {
                shareSite()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
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

    /*
     * show fragment containing list of latest posts for a specific tag
     */
    private fun showListFragmentForTag(tag: ReaderTag, listType: ReaderPostListType?) {
        if (isFinishing) {
            return
        }
        val fragment = ReaderPostListFragment.newInstanceForTag(tag, listType)
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragment_container,
                fragment,
                getString(R.string.fragment_tag_reader_post_list)
            )
            .commit()
        title = ""
    }

    /*
     * show fragment containing list of latest posts in a specific blog
     */
    private fun showListFragmentForBlog(blogId: Long) {
        if (isFinishing) {
            return
        }
        val fragment = ReaderPostListFragment.newInstanceForBlog(blogId)
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragment_container,
                fragment,
                getString(R.string.fragment_tag_reader_post_list)
            )
            .commit()
        title = ""
    }

    private fun showListFragmentForFeed(feedId: Long) {
        if (isFinishing) {
            return
        }
        val fragment = ReaderPostListFragment.newInstanceForFeed(feedId)
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragment_container,
                fragment,
                getString(R.string.fragment_tag_reader_post_list)
            )
            .commit()

        var title = ReaderBlogTable.getFeedName(feedId)
        if (title.isEmpty()) {
            title = getString(R.string.reader_activity_title_blog_preview)
        }
        setTitle(title)
    }

    private fun getListFragment(): ReaderPostListFragment? {
        val fragment =
            supportFragmentManager.findFragmentByTag(getString(R.string.fragment_tag_reader_post_list))
        return (fragment as? ReaderPostListFragment)
    }

    /**
     * Returns the view to attach the snackbar to. Note that this view isn't part of this activity.
     */
    private fun getSnackbarAttachView(): View? = findViewById(R.id.coordinator)

    @Deprecated("Deprecated in Java")
    @Suppress("NestedBlockDepth")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            RequestCodes.NO_REBLOG_SITE -> if (resultCode == RESULT_OK) {
                finish() // Finish activity to make My Site page visible
            }

            RequestCodes.EDIT_POST -> if (resultCode == RESULT_OK && data != null && !isFinishing) {
                val localId = data.getIntExtra(EditPostActivityConstants.EXTRA_POST_LOCAL_ID, 0)
                val site = BundleCompat.getSerializable(
                    data.extras!!,
                    WordPress.SITE,
                    SiteModel::class.java
                )
                val post = postStore.getPostByLocalPostId(localId)

                if (checkToRestart(data)) {
                    ActivityLauncher.editPostOrPageForResult(
                        data,
                        this@ReaderPostListActivity, site,
                        data.getIntExtra(EditPostActivityConstants.EXTRA_POST_LOCAL_ID, 0)
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
