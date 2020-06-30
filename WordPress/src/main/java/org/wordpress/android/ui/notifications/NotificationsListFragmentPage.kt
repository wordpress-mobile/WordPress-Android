package org.wordpress.android.ui.notifications

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import kotlinx.android.synthetic.main.notifications_list_fragment_page.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker.Stat.APP_REVIEWS_EVENT_INCREMENTED_BY_CHECKING_NOTIFICATION
import org.wordpress.android.datasets.NotificationsTable
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.push.GCMMessageHandler
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.PagePostCreationSourcesDetail.POST_FROM_NOTIFS_EMPTY_VIEW
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener
import org.wordpress.android.ui.notifications.NotificationEvents.NoteLikeOrModerationStatusChanged
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsChanged
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsRefreshCompleted
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsRefreshError
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsUnseenStatus
import org.wordpress.android.ui.notifications.adapters.NotesAdapter
import org.wordpress.android.ui.notifications.adapters.NotesAdapter.DataLoadedListener
import org.wordpress.android.ui.notifications.adapters.NotesAdapter.FILTERS
import org.wordpress.android.ui.notifications.adapters.NotesAdapter.FILTERS.FILTER_ALL
import org.wordpress.android.ui.notifications.adapters.NotesAdapter.FILTERS.FILTER_COMMENT
import org.wordpress.android.ui.notifications.adapters.NotesAdapter.FILTERS.FILTER_FOLLOW
import org.wordpress.android.ui.notifications.adapters.NotesAdapter.FILTERS.FILTER_LIKE
import org.wordpress.android.ui.notifications.adapters.NotesAdapter.FILTERS.FILTER_UNREAD
import org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter
import org.wordpress.android.ui.notifications.utils.NotificationsActions
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.widgets.AppRatingDialog.incrementInteractions
import javax.inject.Inject

class NotificationsListFragmentPage : Fragment(), OnScrollToTopListener, DataLoadedListener {
    private var notesAdapter: NotesAdapter? = null
    private var swipeToRefreshHelper: SwipeToRefreshHelper? = null
    private var isAnimatingOutNewNotificationsBar = false
    private var shouldRefreshNotifications = false
    private var tabPosition = 0

    @Inject lateinit var accountStore: AccountStore
    @Inject lateinit var gcmMessageHandler: GCMMessageHandler

    interface OnNoteClickListener {
        fun onClickNote(noteId: String?)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val adapter = createOrGetNotesAdapter()
        notifications_list.adapter = adapter
        if (savedInstanceState != null) {
            tabPosition = savedInstanceState.getInt(KEY_TAB_POSITION, NotificationsListFragment.TAB_POSITION_ALL)
        }
        when (tabPosition) {
            NotificationsListFragment.TAB_POSITION_ALL -> adapter.setFilter(FILTER_ALL)
            NotificationsListFragment.TAB_POSITION_COMMENT -> adapter.setFilter(FILTER_COMMENT)
            NotificationsListFragment.TAB_POSITION_FOLLOW -> adapter.setFilter(FILTER_FOLLOW)
            NotificationsListFragment.TAB_POSITION_LIKE -> adapter.setFilter(FILTER_LIKE)
            NotificationsListFragment.TAB_POSITION_UNREAD -> adapter.setFilter(FILTER_UNREAD)
            else -> adapter.setFilter(FILTER_ALL)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RequestCodes.NOTE_DETAIL) {
            shouldRefreshNotifications = false
            if (resultCode == Activity.RESULT_OK) {
                val noteId = data?.getStringExtra(NotificationsListFragment.NOTE_MODERATE_ID_EXTRA)
                val newStatus = data?.getStringExtra(NotificationsListFragment.NOTE_MODERATE_STATUS_EXTRA)
                if (!noteId.isNullOrBlank() && !newStatus.isNullOrBlank()) {
                    updateNote(noteId, CommentStatus.fromString(newStatus))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        shouldRefreshNotifications = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.notifications_list_fragment_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            tabPosition = it.getInt(KEY_TAB_POSITION, NotificationsListFragment.TAB_POSITION_ALL)
        }
        notifications_list.layoutManager = LinearLayoutManager(activity)
        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(notifications_refresh) {
            hideNewNotificationsBar()
            fetchNotesFromRemote()
        }
        layout_new_notificatons.visibility = View.GONE
        layout_new_notificatons.setOnClickListener { onScrollToTop() }
    }

    override fun onDestroyView() {
        swipeToRefreshHelper = null
        notifications_list?.adapter = null
        notesAdapter = null
        super.onDestroyView()
    }

    override fun onDataLoaded(itemsCount: Int) {
        if (!isAdded) {
            AppLog.d(T.NOTIFS,
                    "NotificationsListFragmentPage.onDataLoaded occurred when fragment is not attached.")
        }
        if (itemsCount > 0) {
            hideEmptyView()
        } else {
            showEmptyViewForCurrentFilter()
        }
    }

    override fun onPause() {
        super.onPause()
        shouldRefreshNotifications = true
    }

    override fun onResume() {
        super.onResume()
        hideNewNotificationsBar()
        EventBus.getDefault().post(NotificationsUnseenStatus(false))
        if (accountStore.hasAccessToken()) {
            notesAdapter!!.reloadNotesFromDBAsync()
            if (shouldRefreshNotifications) {
                fetchNotesFromRemote()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_TAB_POSITION, tabPosition)
        super.onSaveInstanceState(outState)
    }

    override fun onScrollToTop() {
        if (!isAdded) {
            return
        }
        clearPendingNotificationsItemsOnUI()
        val layoutManager = notifications_list?.layoutManager as LinearLayoutManager
        if (layoutManager.findFirstCompletelyVisibleItemPosition() > 0) {
            layoutManager.smoothScrollToPosition(notifications_list, null, 0)
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    private val mOnNoteClickListener: OnNoteClickListener = object : OnNoteClickListener {
        override fun onClickNote(noteId: String?) {
            if (!isAdded) {
                return
            }
            if (TextUtils.isEmpty(noteId)) {
                return
            }
            incrementInteractions(APP_REVIEWS_EVENT_INCREMENTED_BY_CHECKING_NOTIFICATION)

            // Open the latest version of this note in case it has changed, which can happen if the note was tapped
            // from the list after it was updated by another fragment (such as NotificationsDetailListFragment).
            openNoteForReply(activity, noteId, false, null, notesAdapter!!.currentFilter, false)
        }
    }
    private val mOnScrollListener: OnScrollListener = object : OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            notifications_list?.removeOnScrollListener(this)
            clearPendingNotificationsItemsOnUI()
        }
    }

    private fun clearPendingNotificationsItemsOnUI() {
        hideNewNotificationsBar()
        EventBus.getDefault().post(NotificationsUnseenStatus(false))
        NotificationsActions.updateNotesSeenTimestamp()
        Thread(Runnable { gcmMessageHandler.removeAllNotifications(activity) }).start()
    }

    private fun fetchNotesFromRemote() {
        if (!isAdded || notesAdapter == null) {
            return
        }
        if (!NetworkUtils.isNetworkAvailable(activity)) {
            swipeToRefreshHelper?.isRefreshing = false
            return
        }
        NotificationsUpdateServiceStarter.startService(activity)
    }

    val selectedSite: SiteModel?
        get() = (activity as? WPMainActivity)?.selectedSite

    private fun hideEmptyView() {
        if (isAdded) {
            actionable_empty_view?.visibility = View.GONE
            notifications_list?.visibility = View.VISIBLE
        }
    }

    private fun hideNewNotificationsBar() {
        if (!isAdded || !isNewNotificationsBarShowing || isAnimatingOutNewNotificationsBar) {
            return
        }
        isAnimatingOutNewNotificationsBar = true
        val listener: AnimationListener = object : AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                if (isAdded) {
                    layout_new_notificatons.visibility = View.GONE
                    isAnimatingOutNewNotificationsBar = false
                }
            }

            override fun onAnimationRepeat(animation: Animation) {}
        }
        AniUtils.startAnimation(layout_new_notificatons, R.anim.notifications_bottom_bar_out, listener)
    }

    private val isNewNotificationsBarShowing: Boolean
        get() = layout_new_notificatons != null && layout_new_notificatons.visibility == View.VISIBLE

    private fun performActionForActiveFilter() {
        if (!isAdded) {
            return
        }
        if (!accountStore.hasAccessToken()) {
            ActivityLauncher.showSignInForResult(activity)
            return
        }
        if (tabPosition == NotificationsListFragment.TAB_POSITION_UNREAD) {
            ActivityLauncher.addNewPostForResult(activity, selectedSite, false, POST_FROM_NOTIFS_EMPTY_VIEW)
        } else if (activity is WPMainActivity) {
            (requireActivity() as WPMainActivity).setReaderPageActive()
        }
    }

    private fun showEmptyView(
        @StringRes titleResId: Int,
        @StringRes descriptionResId: Int = 0,
        @StringRes buttonResId: Int = 0
    ) {
        if (isAdded) {
            actionable_empty_view?.visibility = View.VISIBLE
            notifications_list?.visibility = View.GONE
            actionable_empty_view?.title?.setText(titleResId)
            if (descriptionResId != 0) {
                actionable_empty_view?.subtitle?.setText(descriptionResId)
                actionable_empty_view?.subtitle?.visibility = View.VISIBLE
            } else {
                actionable_empty_view?.subtitle?.visibility = View.GONE
            }
            if (buttonResId != 0) {
                actionable_empty_view?.button?.setText(buttonResId)
                actionable_empty_view?.button?.visibility = View.VISIBLE
            } else {
                actionable_empty_view?.button?.visibility = View.GONE
            }
            actionable_empty_view?.button?.setOnClickListener { performActionForActiveFilter() }
        }
    }

    // Show different empty view message and action button based on selected tab.
    private fun showEmptyViewForCurrentFilter() {
        if (!accountStore.hasAccessToken()) {
            return
        }
        when (tabPosition) {
            NotificationsListFragment.TAB_POSITION_ALL -> showEmptyView(
                    R.string.notifications_empty_all,
                    R.string.notifications_empty_action_all,
                    R.string.notifications_empty_view_reader
            )
            NotificationsListFragment.TAB_POSITION_COMMENT -> showEmptyView(
                    R.string.notifications_empty_comments,
                    R.string.notifications_empty_action_comments,
                    R.string.notifications_empty_view_reader
            )
            NotificationsListFragment.TAB_POSITION_FOLLOW -> showEmptyView(
                    R.string.notifications_empty_followers,
                    R.string.notifications_empty_action_followers_likes,
                    R.string.notifications_empty_view_reader
            )
            NotificationsListFragment.TAB_POSITION_LIKE -> showEmptyView(
                    R.string.notifications_empty_likes,
                    R.string.notifications_empty_action_followers_likes,
                    R.string.notifications_empty_view_reader
            )
            NotificationsListFragment.TAB_POSITION_UNREAD -> if (selectedSite == null) {
                showEmptyView(R.string.notifications_empty_unread)
            } else {
                showEmptyView(
                        R.string.notifications_empty_unread,
                        R.string.notifications_empty_action_unread,
                        R.string.posts_empty_list_button
                )
            }
            else -> showEmptyView(R.string.notifications_empty_list)
        }
        actionable_empty_view?.image?.visibility = if (DisplayUtils.isLandscape(context)) View.GONE else View.VISIBLE
    }

    private fun showNewNotificationsBar() {
        if (!isAdded || isNewNotificationsBarShowing) {
            return
        }
        AniUtils.startAnimation(layout_new_notificatons, R.anim.notifications_bottom_bar_in)
        layout_new_notificatons?.visibility = View.VISIBLE
    }

    private fun showNewUnseenNotificationsUI() {
        if (!isAdded || notifications_list?.layoutManager == null) {
            return
        }
        notifications_list?.clearOnScrollListeners()
        notifications_list?.postDelayed({
            if (isAdded) {
                notifications_list?.addOnScrollListener(mOnScrollListener)
            }
        }, 1000L)
        val first = notifications_list?.layoutManager!!.getChildAt(0)
        // Show new notifications bar if first item is not visible on the screen.
        if (first != null && notifications_list?.layoutManager!!.getPosition(first) > 0) {
            showNewNotificationsBar()
        }
    }

    private fun updateNote(noteId: String, status: CommentStatus) {
        val note = NotificationsTable.getNoteById(noteId)
        if (note != null) {
            note.localStatus = status.toString()
            NotificationsTable.saveNote(note)
            EventBus.getDefault().post(NotificationsChanged())
        }
    }

    private fun createOrGetNotesAdapter(): NotesAdapter {
        return notesAdapter ?: NotesAdapter(requireActivity(), this, null).apply {
            notesAdapter = this
            this.setOnNoteClickListener(mOnNoteClickListener)
        }
    }

    @Subscribe(sticky = true, threadMode = MAIN)
    fun onEventMainThread(event: NoteLikeOrModerationStatusChanged) {
        NotificationsActions.downloadNoteAndUpdateDB(
                event.noteId,
                {
                    EventBus.getDefault()
                            .removeStickyEvent(
                                    NoteLikeOrModerationStatusChanged::class.java
                            )
                    val note = NotificationsTable.getNoteById(event.noteId)
                    if (note != null) {
                        notesAdapter?.replaceNote(note)
                    }
                }
        ) {
            EventBus.getDefault().removeStickyEvent(
                    NoteLikeOrModerationStatusChanged::class.java
            )
        }
    }

    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: NotificationsChanged) {
        if (!isAdded) {
            return
        }
        notesAdapter!!.reloadNotesFromDBAsync()
        if (event.hasUnseenNotes) {
            showNewUnseenNotificationsUI()
        }
    }

    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: NotificationsRefreshCompleted) {
        if (!isAdded) {
            return
        }
        swipeToRefreshHelper?.isRefreshing = false
        notesAdapter!!.addAll(event.notes, true)
    }

    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(error: NotificationsRefreshError?) {
        if (isAdded) {
            swipeToRefreshHelper?.isRefreshing = false
        }
    }

    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: NotificationsUnseenStatus) {
        if (!isAdded) {
            return
        }
        if (event.hasUnseenNotes) {
            showNewUnseenNotificationsUI()
        } else {
            hideNewNotificationsBar()
        }
    }

    companion object {
        private const val KEY_TAB_POSITION = "tabPosition"
        fun newInstance(position: Int): Fragment {
            val fragment = NotificationsListFragmentPage()
            val bundle = Bundle()
            bundle.putInt(KEY_TAB_POSITION, position)
            fragment.arguments = bundle
            return fragment
        }

        private fun getOpenNoteIntent(activity: Activity, noteId: String): Intent {
            val detailIntent = Intent(activity, NotificationsDetailActivity::class.java)
            detailIntent.putExtra(NotificationsListFragment.NOTE_ID_EXTRA, noteId)
            return detailIntent
        }

        fun openNoteForReply(
            activity: Activity?,
            noteId: String?,
            shouldShowKeyboard: Boolean,
            replyText: String?,
            filter: FILTERS?,
            isTappedFromPushNotification: Boolean
        ) {
            if (noteId == null || activity == null || activity.isFinishing) {
                return
            }
            val detailIntent = getOpenNoteIntent(activity, noteId)
            detailIntent.putExtra(NotificationsListFragment.NOTE_INSTANT_REPLY_EXTRA, shouldShowKeyboard)
            if (!TextUtils.isEmpty(replyText)) {
                detailIntent.putExtra(NotificationsListFragment.NOTE_PREFILLED_REPLY_EXTRA, replyText)
            }
            detailIntent.putExtra(NotificationsListFragment.NOTE_CURRENT_LIST_FILTER_EXTRA, filter)
            detailIntent.putExtra(
                    NotificationsUpdateServiceStarter.IS_TAPPED_ON_NOTIFICATION,
                    isTappedFromPushNotification
            )
            openNoteForReplyWithParams(detailIntent, activity)
        }

        private fun openNoteForReplyWithParams(detailIntent: Intent, activity: Activity) {
            activity.startActivityForResult(detailIntent, RequestCodes.NOTE_DETAIL)
        }
    }
}
