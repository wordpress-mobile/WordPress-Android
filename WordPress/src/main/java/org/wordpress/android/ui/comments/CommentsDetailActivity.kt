@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.comments

import org.wordpress.android.util.extensions.onBackPressedCompat
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.comments.CommentActions.OnCommentActionListener
import org.wordpress.android.ui.ScrollableViewInitializedListener
import javax.inject.Inject
import org.wordpress.android.ui.comments.unified.CommentsStoreAdapter
import org.wordpress.android.widgets.WPViewPager
import android.widget.ProgressBar
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import android.os.Bundle
import org.wordpress.android.R
import org.wordpress.android.ui.CollapseFullScreenDialogFragment
import org.wordpress.android.WordPress
import org.wordpress.android.widgets.WPViewPagerTransformer
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.util.analytics.AnalyticsUtils.AnalyticsCommentActionSource
import org.wordpress.android.fluxc.generated.CommentActionBuilder
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsPayload
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.store.CommentStore.OnCommentChanged
import org.wordpress.android.fluxc.action.CommentAction
import org.wordpress.android.ui.comments.LoadCommentsTask.LoadingCallback
import android.os.AsyncTask
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import org.wordpress.android.fluxc.model.CommentModel
import android.content.Intent
import org.wordpress.android.ui.comments.unified.CommentConstants
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.AppBarLayout
import org.greenrobot.eventbus.Subscribe
import org.wordpress.android.models.CommentList
import org.wordpress.android.ui.comments.unified.CommentConstants.COMMENTS_PER_PAGE
import org.wordpress.android.ui.comments.unified.OnLoadMoreListener
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.ToastUtils

@AndroidEntryPoint
@Deprecated(
    """ Comments are being refactored as part of Comments Unification project. If you are adding any
  features or modifying this class, please ping develric or klymyam"""
)
class CommentsDetailActivity : LocaleAwareActivity(), OnLoadMoreListener, OnCommentActionListener,
    ScrollableViewInitializedListener {

    @Suppress("DEPRECATION")
    @JvmField @Inject
    var mCommentsStoreAdapter: CommentsStoreAdapter? = null
    private lateinit var mViewPager: WPViewPager
    private var mAppBarLayout: AppBarLayout? = null
    private var mProgressBar: ProgressBar? = null
    private var mCommentId: Long = 0
    private var mStatusFilter: CommentStatus? = null
    private var mSite: SiteModel? = null

    @Suppress("DEPRECATION")
    private var mAdapter: CommentDetailFragmentAdapter? = null
    private var mOnPageChangeListener: OnPageChangeListener? = null
    private var mIsLoadingComments = false
    private var mIsUpdatingComments = false
    private var mCanLoadMoreComments = true

    companion object {
        const val COMMENT_ID_EXTRA = "commentId"
        const val COMMENT_STATUS_FILTER_EXTRA = "commentStatusFilter"
    }

    @Suppress("DEPRECATION")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mCommentsStoreAdapter!!.register(this)
        AppLog.i(AppLog.T.COMMENTS, "Creating CommentsDetailActivity")
        setContentView(R.layout.comments_detail_activity)

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fragment =
                    supportFragmentManager.findFragmentByTag(CollapseFullScreenDialogFragment.TAG) as CollapseFullScreenDialogFragment?
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

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
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
        mViewPager.setPageTransformer(
            false,
            WPViewPagerTransformer(WPViewPagerTransformer.TransformType.SLIDE_OVER)
        )

        mProgressBar = findViewById(R.id.progress_loading)
        mAppBarLayout = findViewById(R.id.appbar_main)

        // Asynchronously loads comments and build the adapter
        loadDataInViewPager()

        if (savedInstanceState == null) {
            // track initial comment view
            AnalyticsUtils.trackCommentActionWithSiteDetails(
                Stat.COMMENT_VIEWED, AnalyticsCommentActionSource.SITE_COMMENTS, mSite
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(COMMENT_ID_EXTRA, mCommentId)
        outState.putSerializable(WordPress.SITE, mSite)
        outState.putSerializable(COMMENT_STATUS_FILTER_EXTRA, mStatusFilter)
        super.onSaveInstanceState(outState)
    }

    public override fun onDestroy() {
        mCommentsStoreAdapter!!.unregister(this)
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onLoadMore() {
        updateComments()
    }

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
        mCommentsStoreAdapter!!.dispatch(
            CommentActionBuilder.newFetchCommentsAction(
                FetchCommentsPayload(mSite!!, mStatusFilter!!, COMMENTS_PER_PAGE, offset)
            )
        )

        mIsUpdatingComments = true
        setLoadingState(true)
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

    @Suppress("DEPRECATION")
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

    @Suppress("DEPRECATION")
    private fun showCommentList(commentList: CommentList) {
        if (isFinishing) {
            return
        }

        val previousItem = mViewPager.currentItem

        // Only notify adapter when loading new page
        if (mAdapter != null && mAdapter!!.isAddingNewComments(commentList)) {
            mAdapter!!.onNewItems(commentList)
        } else {
            // If current items change, rebuild the adapter
            mAdapter = CommentDetailFragmentAdapter(
                supportFragmentManager, commentList, mSite,
                this@CommentsDetailActivity
            )
            mViewPager.adapter = mAdapter
        }

        val commentIndex = mAdapter!!.commentIndex(mCommentId)
        if (commentIndex < 0) {
            showErrorToastAndFinish()
        }

        if (mOnPageChangeListener != null) {
            mViewPager.removeOnPageChangeListener(mOnPageChangeListener!!)
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
            mViewPager.currentItem = commentIndex
        }

        mViewPager.addOnPageChangeListener(mOnPageChangeListener!!)
    }

    private fun showErrorToastAndFinish() {
        AppLog.e(AppLog.T.COMMENTS, "Comment could not be found.")
        ToastUtils.showToast(this, R.string.error_load_comment)
        finish()
    }

    private fun setLoadingState(visible: Boolean) {
        if (mProgressBar != null) {
            mProgressBar!!.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }

    override fun onModerateComment(site: SiteModel, comment: CommentModel, newStatus: CommentStatus) {
        val resultIntent = Intent()
        resultIntent.putExtra(CommentConstants.COMMENT_MODERATE_ID_EXTRA, comment.remoteCommentId)
        resultIntent.putExtra(CommentConstants.COMMENT_MODERATE_STATUS_EXTRA, newStatus.toString())
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    override fun onScrollableViewInitialized(containerId: Int) {
        mAppBarLayout!!.liftOnScrollTargetViewId = containerId
    }

}
