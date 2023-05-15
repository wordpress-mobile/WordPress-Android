package org.wordpress.android.ui.comments

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.action.CommentAction
import org.wordpress.android.fluxc.generated.CommentActionBuilder
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsPayload
import org.wordpress.android.fluxc.store.CommentStore.OnCommentChanged
import org.wordpress.android.models.CommentList
import org.wordpress.android.ui.CollapseFullScreenDialogFragment
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.comments.CommentActions.OnCommentActionListener
import org.wordpress.android.ui.comments.LoadCommentsTask.LoadingCallback
import org.wordpress.android.ui.comments.unified.CommentConstants
import org.wordpress.android.ui.comments.unified.CommentConstants.COMMENTS_PER_PAGE
import org.wordpress.android.ui.comments.unified.CommentsStoreAdapter
import org.wordpress.android.ui.comments.unified.OnLoadMoreListener
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.util.analytics.AnalyticsUtils.AnalyticsCommentActionSource
import org.wordpress.android.util.extensions.onBackPressedCompat
import org.wordpress.android.widgets.WPViewPager
import org.wordpress.android.widgets.WPViewPagerTransformer
import javax.inject.Inject

@AndroidEntryPoint
class CommentsDetailActivity : LocaleAwareActivity(), OnLoadMoreListener,
    OnCommentActionListener, ScrollableViewInitializedListener {

    companion object {
        const val COMMENT_ID_EXTRA = "commentId"
        const val COMMENT_STATUS_FILTER_EXTRA = "commentStatusFilter"
    }

    @Inject lateinit var mCommentsStoreAdapter: CommentsStoreAdapter

    private var mViewPager: WPViewPager? = null
    private var mAppBarLayout: AppBarLayout? = null
    private var mProgressBar: ProgressBar? = null

    private var mCommentId: Long = 0
    private var mStatusFilter: CommentStatus? = null
    private var mSite: SiteModel? = null
    private var mAdapter: CommentDetailFragmentAdapter? = null
    private var mOnPageChangeListener: OnPageChangeListener? = null

    private var mIsLoadingComments = false
    private var mIsUpdatingComments = false
    private var mCanLoadMoreComments = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mCommentsStoreAdapter.register(this)
        AppLog.i(AppLog.T.COMMENTS, "Creating ${CommentsDetailActivity::class.simpleName}")

        setContentView(R.layout.comments_detail_activity)

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fragment = supportFragmentManager
                    .findFragmentByTag(CollapseFullScreenDialogFragment.TAG) as CollapseFullScreenDialogFragment?

                if (fragment != null) {
                    fragment.collapse()
                } else {
                    onBackPressedDispatcher.onBackPressedCompat(this)
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_main)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        if (savedInstanceState == null) {
            mCommentId = intent.getLongExtra(COMMENT_ID_EXTRA, -1)
            mSite = intent.getSerializableExtra(WordPress.SITE) as SiteModel?
            mStatusFilter =
                intent.getSerializableExtra(COMMENT_STATUS_FILTER_EXTRA) as CommentStatus?
        } else {
            mCommentId = savedInstanceState.getLong(COMMENT_ID_EXTRA)
            mSite = savedInstanceState.getSerializable(WordPress.SITE) as SiteModel?
            mStatusFilter =
                savedInstanceState.getSerializable(COMMENT_STATUS_FILTER_EXTRA) as CommentStatus?
        }

        // set up the viewpager and adapter for lateral navigation
        mViewPager = findViewById(R.id.viewpager)
        mViewPager!!.setPageTransformer(false,
            WPViewPagerTransformer(WPViewPagerTransformer.TransformType.SLIDE_OVER))

        mProgressBar = findViewById(R.id.progress_loading)
        mAppBarLayout = findViewById(R.id.appbar_main)

        // Asynchronously loads comments and build the adapter
        loadDataInViewPager()

        if (savedInstanceState == null) {
            // track initial comment view
            AnalyticsUtils.trackCommentActionWithSiteDetails(Stat.COMMENT_VIEWED,
                AnalyticsCommentActionSource.SITE_COMMENTS, mSite)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            putLong(COMMENT_ID_EXTRA, mCommentId)
            putSerializable(WordPress.SITE, mSite)
            putSerializable(COMMENT_STATUS_FILTER_EXTRA, mStatusFilter)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        mCommentsStoreAdapter.unregister(this)
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCommentChanged(event: OnCommentChanged) {
        mIsUpdatingComments = false
        setLoadingState(false)

        // Don't refresh the list on push, we already updated comments
        if (event.causeOfChange != CommentAction.PUSH_COMMENT) {
            if (event.changedCommentsLocalIds.size > 0) {
                loadDataInViewPager()
            } else if (!event.isError) {
                // There are no more comments to load
                mCanLoadMoreComments = false
            }
        }
        if (event.isError) {
            if (!TextUtils.isEmpty(event.error.message)) {
                ToastUtils.showToast(this, event.error.message)
            }
        }
    }

    private fun loadDataInViewPager() {
        if (mIsLoadingComments) {
            AppLog.w(AppLog.T.COMMENTS, "load comments task already active")
        } else {
            LoadCommentsTask(mCommentsStoreAdapter, mStatusFilter, mSite, object : LoadingCallback {
                override fun isLoading(loading: Boolean) {
                    setLoadingState(loading)
                    mIsLoadingComments = loading
                }

                override fun loadingFinished(commentList: CommentList) {
                    if (!commentList.isEmpty()) {
                        showCommentList(commentList)
                    }
                }
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    private fun setLoadingState(visible: Boolean) {
        if (mProgressBar != null) {
            mProgressBar!!.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }

    private fun showCommentList(commentList: CommentList) {
        if (isFinishing) {
            return
        }
        val previousItem = mViewPager!!.currentItem

        // Only notify adapter when loading new page
        if (mAdapter != null && mAdapter!!.isAddingNewComments(commentList)) {
            mAdapter!!.onNewItems(commentList)
        } else {
            // If current items change, rebuild the adapter
            mAdapter = CommentDetailFragmentAdapter(supportFragmentManager, commentList,
                mSite, this@CommentsDetailActivity
            )
            mViewPager!!.adapter = mAdapter
        }
        val commentIndex = mAdapter!!.commentIndex(mCommentId)
        if (commentIndex < 0) {
            showErrorToastAndFinish()
        }
        if (mOnPageChangeListener != null) {
            mViewPager!!.removeOnPageChangeListener(mOnPageChangeListener!!)
        } else {
            mOnPageChangeListener = object : SimpleOnPageChangeListener() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    val comment = mAdapter!!.getCommentAtPosition(position)
                    if (comment != null) {
                        mCommentId = comment.remoteCommentId
                        // track subsequent comment views
                        AnalyticsUtils.trackCommentActionWithSiteDetails(
                            Stat.COMMENT_VIEWED, AnalyticsCommentActionSource.SITE_COMMENTS, mSite
                        )
                    }
                }
            }
        }
        if (commentIndex != previousItem) {
            mViewPager!!.currentItem = commentIndex
        }
        mViewPager!!.addOnPageChangeListener(mOnPageChangeListener!!)
    }

    private fun showErrorToastAndFinish() {
        AppLog.e(AppLog.T.COMMENTS, "Comment could not be found.")
        ToastUtils.showToast(this, R.string.error_load_comment)
        finish()
    }

    override fun onModerateComment(site: SiteModel?, comment: CommentModel, newStatus: CommentStatus) {
        val resultIntent = Intent()
        resultIntent.putExtra(CommentConstants.COMMENT_MODERATE_ID_EXTRA, comment.remoteCommentId)
        resultIntent.putExtra(CommentConstants.COMMENT_MODERATE_STATUS_EXTRA, newStatus.toString())
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    override fun onScrollableViewInitialized(containerId: Int) {
        mAppBarLayout!!.liftOnScrollTargetViewId = containerId
    }

    override fun onLoadMore() {
        updateComments()
    }

    @Suppress("ReturnCount")
    private fun updateComments() {
        if (mIsUpdatingComments) {
            AppLog.w(AppLog.T.COMMENTS, "update comments task already running")
            return
        } else if (!NetworkUtils.isNetworkAvailable(this)) {
            ToastUtils.showToast(this, getString(R.string.error_refresh_comments_showing_older))
            return
        } else if (!mCanLoadMoreComments) {
            AppLog.w(AppLog.T.COMMENTS, "no more comments to be loaded")
            return
        }
        val offset = mAdapter!!.count
        mCommentsStoreAdapter.dispatch(
            CommentActionBuilder.newFetchCommentsAction(
                FetchCommentsPayload(mSite!!, mStatusFilter!!, COMMENTS_PER_PAGE, offset)
            )
        )
        mIsUpdatingComments = true
        setLoadingState(true)
    }
}
