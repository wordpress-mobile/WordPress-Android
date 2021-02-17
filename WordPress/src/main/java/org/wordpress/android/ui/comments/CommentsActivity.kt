package org.wordpress.android.ui.comments

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.Callback
import com.google.android.material.tabs.TabLayout
import org.wordpress.android.R
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
import org.wordpress.android.ui.comments.CommentsListFragment.OnCommentSelectedListener
import org.wordpress.android.ui.notifications.NotificationFragment.OnPostClickListener
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.ui.comments.CommentsListFragment.CommentStatusCriteria.ALL
import org.wordpress.android.ui.comments.CommentsListFragment.CommentStatusCriteria.UNAPPROVED
import org.wordpress.android.ui.comments.CommentsListFragment.CommentStatusCriteria.APPROVED
import org.wordpress.android.ui.comments.CommentsListFragment.CommentStatusCriteria.TRASH
import org.wordpress.android.ui.comments.CommentsListFragment.CommentStatusCriteria.SPAM
import org.wordpress.android.ui.reader.ReaderActivityLauncher
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.widgets.WPSnackbar.Companion.make
import java.lang.ref.WeakReference
import javax.inject.Inject

class CommentsActivity : LocaleAwareActivity(),
        OnCommentSelectedListener,
        OnPostClickListener,
        BasicDialogPositiveClickInterface {
    private val mTrashedComments = CommentList()
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager
    private lateinit var site: SiteModel

    val COMMENT_LIST_FILTERS = listOf(ALL, UNAPPROVED, APPROVED, TRASH, SPAM)
//    val COMMENT_LIST_FILTERS = listOf(ALL)

    private lateinit var pagerAdapter: CommentsListPagerAdapter


    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var  commentStore: CommentStore

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

        viewPager = findViewById(R.id.view_pager)
        tabLayout = findViewById(id.tab_layout)
        tabLayout.setupWithViewPager(viewPager)

        val currentCommentStatusType: CommentStatus = if (intent != null && intent.hasExtra(SAVED_COMMENTS_STATUS_TYPE)) {
            intent.getSerializableExtra(SAVED_COMMENTS_STATUS_TYPE) as CommentStatus
        } else {
            // Read the value from app preferences here. Default to 0 - All
            AppPrefs.getCommentsStatusFilter().toCommentStatus()
        }
        if (savedInstanceState == null) {
//            val commentsListFragment = CommentsListFragment()
//            // initialize comment status filter first time
//            commentsListFragment.setCommentStatusFilter(currentCommentStatusType)
//            supportFragmentManager.beginTransaction()
//                    .add(
//                            id.layout_fragment_container, commentsListFragment,
//                            getString(string.fragment_tag_comment_list)
//                    )
//                    .commitAllowingStateLoss()
        } else {
            intent.putExtra(KEY_AUTO_REFRESHED, savedInstanceState.getBoolean(KEY_AUTO_REFRESHED))
            intent.putExtra(KEY_EMPTY_VIEW_MESSAGE, savedInstanceState.getString(KEY_EMPTY_VIEW_MESSAGE))
        }

        pagerAdapter = CommentsListPagerAdapter(COMMENT_LIST_FILTERS, site, supportFragmentManager)
        viewPager.adapter = pagerAdapter


    }

    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_main)
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

    private val listFragment: CommentsListFragment?
        get() {
            val fragment = supportFragmentManager.findFragmentByTag(
                    getString(
                            string.fragment_tag_comment_list
                    )
            ) ?: return null
            return fragment as CommentsListFragment
        }

    private fun hasListFragment(): Boolean {
        return listFragment != null
    }

    private fun showReaderFragment(remoteBlogId: Long, postId: Long) {
        ReaderActivityLauncher.showReaderPostDetail(this, remoteBlogId, postId)
//        val fm = supportFragmentManager
//        fm.executePendingTransactions()
//        val fragment: Fragment = newInstance(remoteBlogId, postId)
//        val ft = fm.beginTransaction()
//        val tagForFragment = getString(string.fragment_tag_reader_post_detail)
//        ft.add(id.layout_fragment_container, fragment, tagForFragment)
//                .addToBackStack(tagForFragment)
//                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
//        ft.commit()
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
        showReaderFragment(remoteBlogId, postId.toLong())
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(WordPress.SITE, site)

        // retain the id of the highlighted comments
        if (hasListFragment()) {
            outState.putBoolean(KEY_AUTO_REFRESHED, listFragment!!.mHasAutoRefreshedComments)
            outState.putString(KEY_EMPTY_VIEW_MESSAGE, listFragment!!.emptyViewMessage)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onCreateDialog(id: Int): Dialog {
        val dialog = CommentDialogs.createCommentDialog(this, id)
        return dialog ?: super.onCreateDialog(id)
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

    fun onModerateComment(
        comment: CommentModel,
        newStatus: CommentStatus
    ) {
        if (newStatus == CommentStatus.APPROVED || newStatus == CommentStatus.UNAPPROVED) {
            listFragment!!.updateEmptyView()
            comment.status = newStatus.toString()
            dispatcher.dispatch(CommentActionBuilder.newUpdateCommentAction(comment))
            dispatcher.dispatch(CommentActionBuilder.newPushCommentAction(RemoteCommentPayload(site, comment)))
        } else if (newStatus == CommentStatus.SPAM || newStatus == CommentStatus.TRASH || newStatus == CommentStatus.DELETED) {
            mTrashedComments.add(comment)
            listFragment!!.removeComment(comment)
            val message = if (newStatus == CommentStatus.TRASH) getString(string.comment_trashed) else if (newStatus == CommentStatus.SPAM) getString(
                    string.comment_spammed
            ) else getString(string.comment_deleted_permanently)
            val undoListener = View.OnClickListener { v: View? ->
                mTrashedComments.remove(comment)
                listFragment!!.loadComments()
            }
            val view = listFragment!!.view
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
        }
    }

    private fun moderateComment(comment: CommentModel, newStatus: CommentStatus) {
        if (newStatus == CommentStatus.DELETED) {
            // For deletion, we need to dispatch a specific action.
            dispatcher.dispatch(CommentActionBuilder.newDeleteCommentAction(RemoteCommentPayload(site!!, comment)))
        } else {
            // Actual moderation (push the modified comment).
            comment.status = newStatus.toString()
            dispatcher.dispatch(CommentActionBuilder.newUpdateCommentAction(comment))
            dispatcher.dispatch(CommentActionBuilder.newPushCommentAction(RemoteCommentPayload(site!!, comment)))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPositiveClicked(instanceTag: String) {
//        val fragmentById = supportFragmentManager.findFragmentById(id.layout_fragment_container)
//        if (fragmentById is BasicDialogPositiveClickInterface) {
//            (fragmentById as BasicDialogPositiveClickInterface).onPositiveClicked(instanceTag)
//        }
    }

    companion object {
        const val KEY_AUTO_REFRESHED = "has_auto_refreshed"
        const val KEY_EMPTY_VIEW_MESSAGE = "empty_view_message"
        private const val SAVED_COMMENTS_STATUS_TYPE = "saved_comments_status_type"
        const val COMMENT_MODERATE_ID_EXTRA = "commentModerateId"
        const val COMMENT_MODERATE_STATUS_EXTRA = "commentModerateStatus"
    }

    class CommentsListPagerAdapter(
        private val pages: List<CommentStatusCriteria>,
        private val site: SiteModel,
        val fm: FragmentManager
    ) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        private val listFragments = mutableMapOf<Int, WeakReference<CommentsListFragment>>()

        override fun getCount(): Int = pages.size

        override fun getItem(position: Int): CommentsListFragment =
                CommentsListFragment.newInstance(pages[position])

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val fragment = super.instantiateItem(container, position) as CommentsListFragment
            listFragments[position] = WeakReference(fragment)
            return fragment
        }

        override fun getPageTitle(position: Int): CharSequence =
                WordPress.getContext().getString(pages[position].mLabelResId)

        fun getItemAtPosition(position: Int): CommentsListFragment? {
            return listFragments[position]?.get()
        }
    }
}
