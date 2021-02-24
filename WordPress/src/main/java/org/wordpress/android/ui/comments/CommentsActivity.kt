package org.wordpress.android.ui.comments

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.Callback
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.wordpress.android.R.dimen
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.R.string
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.CommentActionBuilder
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.CommentStore
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload
import org.wordpress.android.models.CommentList
import org.wordpress.android.models.Note
import org.wordpress.android.ui.ActivityId
import org.wordpress.android.ui.ActivityId.COMMENTS
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.comments.CommentsListFragment.CommentStatusCriteria
import org.wordpress.android.ui.comments.CommentsListFragment.CommentStatusCriteria.ALL
import org.wordpress.android.ui.comments.CommentsListFragment.CommentStatusCriteria.APPROVED
import org.wordpress.android.ui.comments.CommentsListFragment.CommentStatusCriteria.SPAM
import org.wordpress.android.ui.comments.CommentsListFragment.CommentStatusCriteria.TRASH
import org.wordpress.android.ui.comments.CommentsListFragment.CommentStatusCriteria.UNAPPROVED
import org.wordpress.android.ui.comments.CommentsListFragment.OnCommentSelectedListener
import org.wordpress.android.ui.notifications.NotificationFragment.OnPostClickListener
import org.wordpress.android.ui.reader.ReaderActivityLauncher
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.widgets.WPSnackbar.Companion.make
import java.lang.ref.WeakReference
import javax.inject.Inject

class CommentsActivity : LocaleAwareActivity(),
        OnCommentSelectedListener,
        OnPostClickListener {
    private val mTrashedComments = CommentList()
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var site: SiteModel

    private val commentListFilters = listOf(ALL, UNAPPROVED, APPROVED, TRASH, SPAM)

    private var disabledTabsOpacity: Float = 0F

    private lateinit var pagerAdapter: CommentsListPagerAdapter

    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var commentStore: CommentStore

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)

        setContentView(layout.comment_activity)

        site = if (savedInstanceState == null) {
            checkNotNull(intent.getSerializableExtra(WordPress.SITE) as? SiteModel) {
                "SiteModel cannot be null, check the PendingIntent starting CommentsActivity"
            }
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }

        setupActionBar()

        viewPager = findViewById(id.view_pager)
        viewPager.offscreenPageLimit = 1
        tabLayout = findViewById(id.tab_layout)

        pagerAdapter = CommentsListPagerAdapter(commentListFilters, this)
        viewPager.adapter = pagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.setText(commentListFilters[position].mLabelResId)
        }.attach()

        val disabledAlpha = TypedValue()
        resources.getValue(dimen.material_emphasis_disabled, disabledAlpha, true)
        disabledTabsOpacity = disabledAlpha.float
    }

    fun onActionModeStarted() {
        viewPager.isUserInputEnabled = false
        for (i in 0 until tabLayout.tabCount) {
            tabLayout.getTabAt(i)?.view?.isEnabled = false
            tabLayout.getTabAt(i)?.view?.isClickable = false
            tabLayout.getTabAt(i)?.view?.alpha = disabledTabsOpacity
        }
    }

    fun onActionModeStopped() {
        viewPager.isUserInputEnabled = true
        for (i in 0 until tabLayout.tabCount) {
            tabLayout.getTabAt(i)?.view?.isEnabled = true
            tabLayout.getTabAt(i)?.view?.isClickable = true
            tabLayout.getTabAt(i)?.view?.alpha = 1F
        }
    }

    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(id.toolbar_main)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    public override fun onResume() {
        super.onResume()
        ActivityId.trackLastActivity(COMMENTS)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        AppLog.d(T.COMMENTS, "comment activity new intent")
    }

    private fun showReaderPost(remoteBlogId: Long, postId: Long) {
        ReaderActivityLauncher.showReaderPostDetail(this, remoteBlogId, postId)
    }

    /*
     * called from comment list when user taps a comment
     */
    override fun onCommentSelected(commentId: Long, statusFilter: CommentStatus) {
        val detailIntent = Intent(this, CommentsDetailActivity::class.java)
        detailIntent.putExtra(CommentsDetailActivity.COMMENT_ID_EXTRA, commentId)
        detailIntent.putExtra(CommentsDetailActivity.COMMENT_STATUS_FILTER_EXTRA, statusFilter)
        detailIntent.putExtra(WordPress.SITE, site)
        startActivityForResult(detailIntent, 1)
    }

    /*
     * called from comment detail when user taps a link to a post - show the post in a
     * reader detail fragment
     */
    override fun onPostClicked(note: Note, remoteBlogId: Long, postId: Int) {
        showReaderPost(remoteBlogId, postId.toLong())
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(WordPress.SITE, site)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val commentId = data!!.getLongExtra(COMMENT_MODERATE_ID_EXTRA, -1)
            val newStatus = data.getStringExtra(COMMENT_MODERATE_STATUS_EXTRA)
            if (commentId >= 0 && !TextUtils.isEmpty(newStatus)) {
                onModerateComment(
                        commentStore.getCommentBySiteAndRemoteId(site, commentId),
                        CommentStatus.fromString(newStatus)
                )
            }
        }
    }

    @JvmOverloads
    fun onModerateComment(
        comment: CommentModel,
        newStatus: CommentStatus,
        allowUndo: Boolean = true
    ) {
        if (newStatus == CommentStatus.APPROVED || newStatus == CommentStatus.UNAPPROVED) {
            comment.status = newStatus.toString()
            dispatcher.dispatch(CommentActionBuilder.newUpdateCommentAction(comment))
            dispatcher.dispatch(CommentActionBuilder.newPushCommentAction(RemoteCommentPayload(site, comment)))
        } else if (newStatus == CommentStatus.SPAM || newStatus == CommentStatus.TRASH ||
                newStatus == CommentStatus.DELETED) {
            val oldStatus = CommentStatus.fromString(comment.status)

            val targetFragments: ArrayList<CommentsListFragment?> = ArrayList()
            if (oldStatus == CommentStatus.APPROVED || oldStatus == CommentStatus.UNAPPROVED) {
                targetFragments.add(pagerAdapter.getItemAtPosition(commentListFilters.indexOf(ALL)))
                targetFragments.add(pagerAdapter.getItemAtPosition(commentListFilters.indexOf(APPROVED)))
                targetFragments.add(pagerAdapter.getItemAtPosition(commentListFilters.indexOf(UNAPPROVED)))
            } else {
                targetFragments.add(
                        pagerAdapter.getItemAtPosition(
                                commentListFilters.indexOf(
                                        CommentStatusCriteria.fromCommentStatus(
                                                oldStatus
                                        )
                                )
                        )
                )
            }
            targetFragments.forEach { it?.removeComment(comment) }
            val message = when (newStatus) {
                CommentStatus.TRASH -> getString(string.comment_trashed)
                CommentStatus.SPAM -> getString(
                        string.comment_spammed
                )
                else -> getString(string.comment_deleted_permanently)
            }
            if (allowUndo) {
                mTrashedComments.add(comment)
                val undoListener = View.OnClickListener { _: View? ->
                    mTrashedComments.remove(comment)
                    targetFragments.forEach { it?.loadComments() }
                }
                val view = findViewById<View>(id.coordinator_layout)
                if (view != null) {
                    val snackbar = make(view, message, Snackbar.LENGTH_LONG)
                            .setAction(string.undo, undoListener)

                    // do the actual moderation once the undo bar has been hidden
                    snackbar.addCallback(object : Callback() {
                        override fun onDismissed(snackbar: Snackbar, event: Int) {
                            super.onDismissed(snackbar, event)

                            // comment will no longer exist in moderating list if action was undone
                            if (!mTrashedComments.contains(comment)) {
                                return
                            }
                            mTrashedComments.remove(comment)
                            moderateComment(comment, newStatus)
                        }
                    })
                    snackbar.show()
                }
            } else {
                moderateComment(comment, newStatus)
            }
        }
    }

    private fun moderateComment(comment: CommentModel, newStatus: CommentStatus) {
        if (newStatus == CommentStatus.DELETED) {
            // For deletion, we need to dispatch a specific action.
            dispatcher.dispatch(CommentActionBuilder.newDeleteCommentAction(RemoteCommentPayload(site, comment)))
        } else {
            // Actual moderation (push the modified comment).
            comment.status = newStatus.toString()
            dispatcher.dispatch(CommentActionBuilder.newUpdateCommentAction(comment))
            dispatcher.dispatch(CommentActionBuilder.newPushCommentAction(RemoteCommentPayload(site, comment)))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val COMMENT_MODERATE_ID_EXTRA = "commentModerateId"
        const val COMMENT_MODERATE_STATUS_EXTRA = "commentModerateStatus"
    }

    class CommentsListPagerAdapter(
        private val pages: List<CommentStatusCriteria>,
        fragmentActivity: FragmentActivity
    ) : FragmentStateAdapter(fragmentActivity) {
        private val listFragments = mutableMapOf<Int, WeakReference<CommentsListFragment>>()

        fun getItemAtPosition(position: Int): CommentsListFragment? {
            return listFragments[position]?.get()
        }

        override fun getItemCount(): Int = pages.size

        override fun createFragment(position: Int): Fragment {
            val fragment = CommentsListFragment.newInstance(pages[position])
            listFragments[position] = WeakReference(fragment)
            return fragment
        }
    }
}
