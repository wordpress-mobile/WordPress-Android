package org.wordpress.android.ui.notifications.compose

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import org.greenrobot.eventbus.EventBus
import org.wordpress.android.BuildConfig
import org.wordpress.android.analytics.AnalyticsTracker.NOTIFICATIONS_SELECTED_FILTER
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.models.Notification
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.PagePostCreationSourcesDetail.POST_FROM_NOTIFS_EMPTY_VIEW
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsUnseenStatus
import org.wordpress.android.ui.notifications.NotificationsListFragment
import org.wordpress.android.ui.notifications.NotificationsListViewModel
import org.wordpress.android.ui.notifications.NotificationsListViewModel.InlineActionEvent
import org.wordpress.android.ui.notifications.NotificationsPermissionBottomSheetFragment
import org.wordpress.android.ui.notifications.adapters.Filter
import org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter
import org.wordpress.android.ui.reader.ReaderActivityLauncher
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource
import org.wordpress.android.util.PermissionUtils
import org.wordpress.android.util.WPPermissionUtils
import org.wordpress.android.util.WPPermissionUtils.NOTIFICATIONS_PERMISSION_REQUEST_CODE
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.widgets.AppReviewManager
import javax.inject.Inject

/** The Compose notifications list; per-note actions reuse [NotificationsListViewModel]. */
@AndroidEntryPoint
class NotificationsComposeListFragment : Fragment(), OnScrollToTopListener {
    @Inject
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private val listViewModel: NotificationsComposeListViewModel by viewModels()
    private val actionsViewModel: NotificationsListViewModel by viewModels()

    private val scrollToTopRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var showPermissionWarning by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(RequestPermission()) { isGranted ->
        val result = if (isGranted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        WPPermissionUtils.setPermissionListAsked(
            requireActivity(),
            NOTIFICATIONS_PERMISSION_REQUEST_CODE,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            intArrayOf(result),
            false
        )
        actionsViewModel.resetNotificationsPermissionWarningDismissState()
        updatePermissionWarning()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppThemeM3 {
                    val uiState by listViewModel.uiState.collectAsStateWithLifecycle()
                    NotificationsListScreen(
                        uiState = uiState,
                        showPermissionWarning = showPermissionWarning,
                        isReaderEnabled = BuildConfig.ENABLE_READER,
                        hasSelectedSite = selectedSite != null,
                        scrollToTopRequests = scrollToTopRequests,
                        onPermissionWarningClick = ::onPermissionWarningClick,
                        onPermissionWarningDismiss = {
                            showPermissionWarning = false
                            actionsViewModel.onNotificationsPermissionWarningDismissed()
                        },
                        onMenuOpened = { analyticsTrackerWrapper.track(Stat.NOTIFICATION_MENU_TAPPED) },
                        onMarkAllRead = ::markAllAsRead,
                        onSettingsClick = { ActivityLauncher.viewNotificationsSettings(activity) },
                        onTabChanged = { filter ->
                            analyticsTrackerWrapper.track(
                                Stat.NOTIFICATION_TAPPED_SEGMENTED_CONTROL,
                                mapOf(NOTIFICATIONS_SELECTED_FILTER to filter.toString())
                            )
                        },
                        onRefresh = ::refresh,
                        onNoteClick = ::openNote,
                        onInlineAction = ::onInlineAction,
                        onEmptyAction = ::onEmptyAction,
                        onListScrolled = listViewModel::onListScrolled,
                        onNewNotificationsTapped = listViewModel::onNewNotificationsBarTapped
                    )
                }
            }
        }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().post(NotificationsUnseenStatus(false))
        refresh()
        updatePermissionWarning()
    }

    /** [WPMainActivity] forwards the detail screen's result here, as it did for the legacy list. */
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != RequestCodes.NOTE_DETAIL || resultCode != Activity.RESULT_OK) return
        val noteId = data?.getStringExtra(NotificationsListFragment.NOTE_MODERATE_ID_EXTRA)
        val newStatus = data?.getStringExtra(NotificationsListFragment.NOTE_MODERATE_STATUS_EXTRA)
        if (!noteId.isNullOrBlank() && !newStatus.isNullOrBlank()) {
            listViewModel.onNoteModerated(noteId, CommentStatus.fromString(newStatus))
        }
    }

    override fun onScrollToTop() {
        scrollToTopRequests.tryEmit(Unit)
    }

    private val selectedSite
        get() = (activity as? WPMainActivity)?.selectedSite

    private fun refresh() {
        if (listViewModel.onRefreshRequested()) {
            NotificationsUpdateServiceStarter.startService(activity)
        }
    }

    private fun markAllAsRead(filter: Filter) {
        analyticsTrackerWrapper.track(Stat.NOTIFICATIONS_MARK_ALL_READ_TAPPED)
        actionsViewModel.markNoteAsRead(requireContext(), listViewModel.notesFor(filter))
    }

    private fun openNote(noteId: String, filter: Filter) {
        if (noteId.isEmpty()) return
        AppReviewManager.incrementInteractions(Stat.APP_REVIEWS_EVENT_INCREMENTED_BY_CHECKING_NOTIFICATION)
        actionsViewModel.openNote(
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
            { NotificationsListFragment.openNoteForReply(activity, noteId, false, null, filter, false) }
        )
    }

    private fun onInlineAction(noteId: String, action: NotificationInlineAction) {
        val note = listViewModel.noteById(noteId) ?: return
        val event = when (action) {
            is NotificationInlineAction.LikeComment -> InlineActionEvent.LikeCommentButtonTapped(note, !action.isLiked)
            is NotificationInlineAction.LikePost -> InlineActionEvent.LikePostButtonTapped(note, !action.isLiked)
            NotificationInlineAction.Share ->
                InlineActionEvent.SharePostButtonTapped(Notification.PostLike(url = note.url, title = note.title))
        }
        analyticsTrackerWrapper.track(
            Stat.NOTIFICATIONS_INLINE_ACTION_TAPPED,
            mapOf(InlineActionEvent.KEY_INLINE_ACTION to event::class.simpleName)
        )
        when (event) {
            is InlineActionEvent.SharePostButtonTapped -> context?.let {
                ActivityLauncher.openShareIntent(it, event.notification.url, event.notification.title)
            }
            is InlineActionEvent.LikeCommentButtonTapped -> actionsViewModel.likeComment(event.note, event.liked)
            is InlineActionEvent.LikePostButtonTapped -> actionsViewModel.likePost(event.note, event.liked)
        }
    }

    private fun onEmptyAction(filter: Filter) {
        if (filter == Filter.UNREAD) {
            selectedSite?.let {
                ActivityLauncher.addNewPostForResult(activity, it, false, POST_FROM_NOTIFS_EMPTY_VIEW, -1, null)
            }
        } else {
            (activity as? WPMainActivity)?.setReaderPageActive()
        }
    }

    private fun updatePermissionWarning() {
        val hasPermission = PermissionUtils.checkNotificationsPermission(activity)
        if (hasPermission) {
            // Reset so the warning can show again if the permission is revoked later.
            actionsViewModel.resetNotificationsPermissionWarningDismissState()
        }
        showPermissionWarning = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !hasPermission &&
                !actionsViewModel.isNotificationsPermissionsWarningDismissed
    }

    private fun onPermissionWarningClick() {
        val isAlwaysDenied = WPPermissionUtils.isPermissionAlwaysDenied(
            requireActivity(),
            Manifest.permission.POST_NOTIFICATIONS
        )
        if (isAlwaysDenied) {
            NotificationsPermissionBottomSheetFragment().show(
                parentFragmentManager,
                NotificationsPermissionBottomSheetFragment.TAG
            )
        } else {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        fun newInstance() = NotificationsComposeListFragment()
    }
}
