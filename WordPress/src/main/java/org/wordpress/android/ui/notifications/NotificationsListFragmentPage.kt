package org.wordpress.android.ui.notifications

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker.Stat.APP_REVIEWS_EVENT_INCREMENTED_BY_CHECKING_NOTIFICATION
import org.wordpress.android.analytics.AnalyticsTracker.Stat.NOTIFICATIONS_INLINE_ACTION_TAPPED
import org.wordpress.android.databinding.NotificationsListFragmentPageBinding
import org.wordpress.android.datasets.NotificationsTable
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.push.GCMMessageHandler
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.PagePostCreationSourcesDetail.POST_FROM_NOTIFS_EMPTY_VIEW
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.ViewPagerFragment
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener
import org.wordpress.android.ui.notifications.NotificationEvents.NoteLikeOrModerationStatusChanged
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsChanged
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsRefreshCompleted
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsRefreshError
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsUnseenStatus
import org.wordpress.android.ui.notifications.NotificationEvents.OnNoteCommentLikeChanged
import org.wordpress.android.ui.notifications.NotificationsListFragment.Companion.TabPosition
import org.wordpress.android.ui.notifications.NotificationsListFragment.Companion.TabPosition.All
import org.wordpress.android.ui.notifications.NotificationsListFragment.Companion.TabPosition.Comment
import org.wordpress.android.ui.notifications.NotificationsListFragment.Companion.TabPosition.Subscribers
import org.wordpress.android.ui.notifications.NotificationsListFragment.Companion.TabPosition.Like
import org.wordpress.android.ui.notifications.NotificationsListFragment.Companion.TabPosition.Unread
import org.wordpress.android.ui.notifications.NotificationsListViewModel.InlineActionEvent
import org.wordpress.android.ui.notifications.NotificationsListViewModel.InlineActionEvent.SharePostButtonTapped
import org.wordpress.android.ui.notifications.adapters.Filter
import org.wordpress.android.ui.notifications.adapters.NotesAdapter
import org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter
import org.wordpress.android.ui.notifications.utils.NotificationsActions
import org.wordpress.android.ui.reader.ReaderActivityLauncher
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.widgets.AppReviewManager.incrementInteractions
import javax.inject.Inject

@AndroidEntryPoint
class NotificationsListFragmentPage : ViewPagerFragment(R.layout.notifications_list_fragment_page),
    OnScrollToTopListener {
    private lateinit var notesAdapter: NotesAdapter
    private var swipeToRefreshHelper: SwipeToRefreshHelper? = null
    private var isAnimatingOutNewNotificationsBar = false
    private var tabPosition = 0
    private val viewModel: NotificationsListViewModel by viewModels()

    @Inject
    lateinit var accountStore: AccountStore

    @Inject
    lateinit var gcmMessageHandler: GCMMessageHandler

    @Inject
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private val showNewUnseenNotificationsRunnable = Runnable {
        if (isAdded) {
            binding?.notificationsList?.addOnScrollListener(mOnScrollListener)
        }
    }

    private var binding: NotificationsListFragmentPageBinding? = null

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RequestCodes.NOTE_DETAIL) {
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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            tabPosition = it.getInt(KEY_TAB_POSITION, All.ordinal)
        }
        notesAdapter = NotesAdapter(requireActivity(), inlineActionEvents = viewModel.inlineActionEvents).apply {
            onNoteClicked = { noteId -> handleNoteClick(noteId) }
            onNotesLoaded = {
                itemCount -> updateEmptyLayouts(itemCount)
                swipeToRefreshHelper?.isRefreshing = false
            }
            viewModel.inlineActionEvents.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .onEach(::handleInlineActionEvent)
                .launchIn(viewLifecycleOwner.lifecycleScope)
        }
        binding = NotificationsListFragmentPageBinding.bind(view).apply {
            notificationsList.layoutManager = LinearLayoutManagerWrapper(view.context)
            notificationsList.adapter = notesAdapter
            swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(notificationsRefresh) {
                hideNewNotificationsBar()
                fetchRemoteNotes()
            }
            layoutNewNotificatons.visibility = View.GONE
            layoutNewNotificatons.setOnClickListener { onScrollToTop() }
            (TabPosition.entries.getOrNull(tabPosition) ?: All).let { notesAdapter.setFilter(it.filter) }
        }
        viewModel.updatedNote.observe(viewLifecycleOwner) {
            notesAdapter.updateNote(it)
        }

        swipeToRefreshHelper?.isRefreshing = true
        notesAdapter.reloadLocalNotes()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        notesAdapter.cancelReloadLocalNotes()
        swipeToRefreshHelper = null
        binding?.notificationsList?.adapter = null
        binding?.notificationsList?.removeCallbacks(showNewUnseenNotificationsRunnable)
        binding = null
    }

    private fun updateEmptyLayouts(itemsCount: Int) {
        if (!isAdded) {
            AppLog.d(
                T.NOTIFS,
                "NotificationsListFragmentPage.onDataLoaded occurred when fragment is not attached."
            )
        }
        binding?.apply {
            if (itemsCount > 0) {
                hideEmptyView()
            } else {
                showEmptyViewForCurrentFilter()
            }
        }
    }

    override fun getScrollableViewForUniqueIdProvision(): View? {
        return binding?.notificationsList
    }

    override fun onResume() {
        super.onResume()
        binding?.hideNewNotificationsBar()
        EventBus.getDefault().post(NotificationsUnseenStatus(false))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_TAB_POSITION, tabPosition)
        super.onSaveInstanceState(outState)
    }

    override fun onScrollToTop() {
        if (!isAdded) {
            return
        }
        binding?.apply {
            clearPendingNotificationsItemsOnUI()
            val layoutManager = notificationsList.layoutManager as LinearLayoutManager
            if (layoutManager.findFirstCompletelyVisibleItemPosition() > 0) {
                layoutManager.smoothScrollToPosition(notificationsList, null, 0)
            }
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

    private fun handleNoteClick(noteId: String) {
        if (!isAdded || noteId.isEmpty()) {
            return
        }
        incrementInteractions(APP_REVIEWS_EVENT_INCREMENTED_BY_CHECKING_NOTIFICATION)

        viewModel.openNote(
            noteId,
            { siteId, postId, commentId ->
                activity?.let {
                    ReaderActivityLauncher.showReaderComments(
                        it,
                        siteId,
                        postId,
                        commentId,
                        ThreadedCommentsActionSource.COMMENT_NOTIFICATION.sourceDescription
                    )
                }
            },
            {
                // Open the latest version of this note in case it has changed, which can happen if the note was
                // tapped from the list after it was updated by another fragment (such as the
                // NotificationsDetailListFragment).
                openNoteForReply(activity, noteId, filter = notesAdapter.currentFilter)
            }
        )
    }

    private val mOnScrollListener: OnScrollListener = object : OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            binding?.notificationsList?.removeOnScrollListener(this)
            binding?.clearPendingNotificationsItemsOnUI()
        }
    }

    private fun NotificationsListFragmentPageBinding.clearPendingNotificationsItemsOnUI() {
        hideNewNotificationsBar()
        EventBus.getDefault().post(NotificationsUnseenStatus(false))
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                NotificationsActions.updateNotesSeenTimestamp()
                gcmMessageHandler.removeAllNotifications(activity)
            }
        }
    }

    private fun fetchRemoteNotes() {
        if (!isAdded) {
            return
        }
        if (!NetworkUtils.isNetworkAvailable(activity)) {
            swipeToRefreshHelper?.isRefreshing = false
            return
        }
        swipeToRefreshHelper?.isRefreshing = true
        NotificationsUpdateServiceStarter.startService(activity)
    }

    val selectedSite: SiteModel?
        get() = (activity as? WPMainActivity)?.selectedSite

    private fun NotificationsListFragmentPageBinding.hideEmptyView() {
        if (isAdded) {
            actionableEmptyView.visibility = View.GONE
            notificationsList.visibility = View.VISIBLE
        }
    }

    private fun NotificationsListFragmentPageBinding.hideNewNotificationsBar() {
        if (!isAdded || !isNewNotificationsBarShowing || isAnimatingOutNewNotificationsBar) {
            return
        }
        isAnimatingOutNewNotificationsBar = true
        val listener: AnimationListener = object : AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                if (isAdded) {
                    layoutNewNotificatons.visibility = View.GONE
                    isAnimatingOutNewNotificationsBar = false
                }
            }

            override fun onAnimationRepeat(animation: Animation) {}
        }
        AniUtils.startAnimation(layoutNewNotificatons, R.anim.notifications_bottom_bar_out, listener)
    }

    private val isNewNotificationsBarShowing: Boolean
        get() = binding?.layoutNewNotificatons?.visibility == View.VISIBLE

    private fun performActionForActiveFilter() {
        if (!isAdded) {
            return
        }
        if (!accountStore.hasAccessToken()) {
            ActivityLauncher.showSignInForResult(activity)
            return
        }
        if (tabPosition == Unread.ordinal) {
            ActivityLauncher.addNewPostForResult(activity, selectedSite, false, POST_FROM_NOTIFS_EMPTY_VIEW, -1, null)
        } else if (activity is WPMainActivity) {
            (requireActivity() as WPMainActivity).setReaderPageActive()
        }
    }

    private fun NotificationsListFragmentPageBinding.showEmptyView(
        @StringRes titleResId: Int,
        @StringRes descriptionResId: Int = 0,
        @StringRes buttonResId: Int = 0
    ) {
        if (isAdded) {
            actionableEmptyView.visibility = View.VISIBLE
            notificationsList.visibility = View.GONE
            actionableEmptyView.title.setText(titleResId)
            if (descriptionResId != 0) {
                actionableEmptyView.subtitle.setText(descriptionResId)
                actionableEmptyView.subtitle.visibility = View.VISIBLE
            } else {
                actionableEmptyView.subtitle.visibility = View.GONE
            }
            if (buttonResId != 0) {
                actionableEmptyView.button.setText(buttonResId)
                actionableEmptyView.button.visibility = View.VISIBLE
            } else {
                actionableEmptyView.button.visibility = View.GONE
            }
            actionableEmptyView.button.setOnClickListener { performActionForActiveFilter() }
        }
    }

    // Show different empty view message and action button based on selected tab.
    private fun NotificationsListFragmentPageBinding.showEmptyViewForCurrentFilter() {
        if (!accountStore.hasAccessToken()) {
            return
        }
        val titleResId: Int
        var descriptionResId = 0
        var buttonResId = 0
        when (tabPosition) {
            All.ordinal -> {
                titleResId = R.string.notifications_empty_all
                descriptionResId = R.string.notifications_empty_action_all
                buttonResId = R.string.notifications_empty_view_reader
            }
            Comment.ordinal -> {
                titleResId = R.string.notifications_empty_comments
                descriptionResId = R.string.notifications_empty_action_comments
                buttonResId = R.string.notifications_empty_view_reader
            }
            Subscribers.ordinal -> {
                titleResId = R.string.notifications_empty_subscribers
                descriptionResId = R.string.notifications_empty_action_followers_likes
                buttonResId = R.string.notifications_empty_view_reader
            }
            Like.ordinal -> {
                titleResId = R.string.notifications_empty_likes
                descriptionResId = R.string.notifications_empty_action_followers_likes
                buttonResId = R.string.notifications_empty_view_reader
            }
            Unread.ordinal -> {
                if (selectedSite == null) {
                    titleResId = R.string.notifications_empty_unread
                } else {
                    titleResId = R.string.notifications_empty_unread
                    descriptionResId = R.string.notifications_empty_action_unread
                    buttonResId = R.string.posts_empty_list_button
                }
            }
            else -> titleResId = R.string.notifications_empty_list
        }
        if (BuildConfig.ENABLE_READER) {
            showEmptyView(titleResId, descriptionResId, buttonResId)
        } else {
            showEmptyView(titleResId)
        }
        actionableEmptyView.image.visibility = if (DisplayUtils.isLandscape(context)) View.GONE else View.VISIBLE
    }

    private fun NotificationsListFragmentPageBinding.showNewNotificationsBar() {
        if (!isAdded || isNewNotificationsBarShowing) {
            return
        }
        AniUtils.startAnimation(layoutNewNotificatons, R.anim.notifications_bottom_bar_in)
        layoutNewNotificatons.visibility = View.VISIBLE
    }

    private fun NotificationsListFragmentPageBinding.showNewUnseenNotificationsUI() {
        if (!isAdded || notificationsList.layoutManager == null) {
            return
        }
        notificationsList.clearOnScrollListeners()
        notificationsList.removeCallbacks(showNewUnseenNotificationsRunnable)
        notificationsList.postDelayed(showNewUnseenNotificationsRunnable, 1000L)
        val first = notificationsList.layoutManager!!.getChildAt(0)
        // Show new notifications bar if first item is not visible on the screen.
        if (first != null && notificationsList.layoutManager!!.getPosition(first) > 0) {
            showNewNotificationsBar()
        }
    }

    private fun updateNote(noteId: String, status: CommentStatus) = lifecycleScope.launch {
        withContext(Dispatchers.IO) {
            val note = NotificationsTable.getNoteById(noteId)
            if (note != null) {
                note.localStatus = status.toString()
                NotificationsTable.saveNote(note)
                EventBus.getDefault().post(NotificationsChanged())
            }
        }
    }

    private fun handleInlineActionEvent(actionEvent: InlineActionEvent) {
        analyticsTrackerWrapper.track(NOTIFICATIONS_INLINE_ACTION_TAPPED, mapOf(
            InlineActionEvent.KEY_INLINE_ACTION to actionEvent::class.simpleName
        ))
        when (actionEvent) {
            is SharePostButtonTapped -> actionEvent.notification.let { postNotification ->
                context?.let {
                    ActivityLauncher.openShareIntent(it, postNotification.url, postNotification.title)
                }
            }
            is InlineActionEvent.LikeCommentButtonTapped -> viewModel.likeComment(actionEvent.note, actionEvent.liked)
            is InlineActionEvent.LikePostButtonTapped -> viewModel.likePost(actionEvent.note, actionEvent.liked)
        }
    }


    /**
     * Mark notifications as read in CURRENT tab, use filteredNotes instead of notes
     */
    fun markAllNotesAsRead() {
        viewModel.markNoteAsRead(requireContext(), notesAdapter.filteredNotes)
    }

    @Subscribe(sticky = true, threadMode = MAIN)
    fun onEventMainThread(event: NoteLikeOrModerationStatusChanged) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                NotificationsActions.downloadNoteAndUpdateDB(
                    event.noteId,
                    {
                        EventBus.getDefault()
                            .removeStickyEvent(
                                NoteLikeOrModerationStatusChanged::class.java
                            )
                    }
                ) {
                    EventBus.getDefault().removeStickyEvent(
                        NoteLikeOrModerationStatusChanged::class.java
                    )
                }
            }
        }
    }

    @Subscribe(sticky = true, threadMode = MAIN)
    fun onEventMainThread(event: NotificationsChanged) {
        if (!isAdded) {
            return
        }
        notesAdapter.reloadLocalNotes()
        if (event.hasUnseenNotes) {
            binding?.showNewUnseenNotificationsUI()
        }
    }

    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: NotificationsRefreshCompleted) {
        if (!isAdded) {
            return
        }
        swipeToRefreshHelper?.isRefreshing = false
        notesAdapter.addAll(event.notes)
    }

    @Suppress("unused", "UNUSED_PARAMETER")
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
        binding?.apply {
            if (event.hasUnseenNotes) {
                showNewUnseenNotificationsUI()
            } else {
                hideNewNotificationsBar()
            }
        }
    }

    @Subscribe(sticky = true, threadMode = MAIN)
    fun onEventMainThread(event: OnNoteCommentLikeChanged) {
        if (!isAdded) {
            return
        }
        notesAdapter.updateNote(event.note)
    }

    @Subscribe(sticky = true, threadMode = MAIN)
    fun onEventMainThread(event: NotificationEvents.OnNotePostLikeChanged) {
        if (!isAdded) {
            return
        }
        notesAdapter.updateNote(event.note.apply { setLikedPost(event.liked) })
    }

    companion object {
        const val KEY_TAB_POSITION = "tabPosition"
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

        @Suppress("LongParameterList")
        fun openNoteForReply(
            activity: Activity?,
            noteId: String?,
            shouldShowKeyboard: Boolean = false,
            replyText: String? = null,
            filter: Filter? = null,
            isTappedFromPushNotification: Boolean = false,
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
            activity.startActivityForResult(detailIntent, RequestCodes.NOTE_DETAIL)
        }
    }

    /**
     * LinearLayoutManagerWrapper is a workaround for a bug in RecyclerView that blocks the UI thread
     * when we perform the first click on the inline actions in the notifications list.
     */
    internal class LinearLayoutManagerWrapper : LinearLayoutManager {
        constructor(context: Context) : super(context)
        constructor(context: Context, orientation: Int, reverseLayout: Boolean) : super(
            context,
            orientation,
            reverseLayout
        )

        constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
            context,
            attrs,
            defStyleAttr,
            defStyleRes
        )

        override fun supportsPredictiveItemAnimations(): Boolean = false
    }
}
