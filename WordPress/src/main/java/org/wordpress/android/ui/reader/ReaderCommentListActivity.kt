package org.wordpress.android.ui.reader

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.comments.unified.CommentIdentifier.ReaderCommentIdentifier
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditActivity
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.utils.showMessage
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.reader.ReaderActivityLauncher.PhotoViewerOption
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.viewmodels.ConversationNotificationsViewModel
import org.wordpress.android.ui.reader.views.compose.comments.ReaderCommentListActions
import org.wordpress.android.ui.reader.views.compose.comments.ReaderCommentListScreen
import org.wordpress.android.ui.suggestion.service.SuggestionEvents.SuggestionNameListUpdated
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.extensions.setContent
import org.wordpress.persistentedittext.PersistentEditTextDatabase
import java.util.EnumSet
import javax.inject.Inject

/**
 * A Reader post's comments, with a sheet for posting a comment or reply
 */
@AndroidEntryPoint
class ReaderCommentListActivity : BaseAppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var readerTracker: ReaderTracker

    @Inject
    lateinit var uiHelpers: UiHelpers

    private val viewModel: ReaderCommentListViewModel by viewModels()

    // shared with CommentNotificationsBottomSheetFragment, which gets it from this activity
    private lateinit var conversationViewModel: ConversationNotificationsViewModel

    private val snackbarHostState = SnackbarHostState()

    // the list picks up an edited comment when the editor saves it
    private val editCommentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) viewModel.onCommentEdited()
    }

    // the reply is kept here rather than in the ViewModel so typing updates the text field synchronously
    private val replyText = mutableStateOf(TextFieldValue(""))
    private val draftDatabase by lazy { PersistentEditTextDatabase(this) }
    private val draftKey get() = "reader_comment_draft_${viewModel.blogId}_${viewModel.postId}"

    private var suggestionServiceConnectionManager: SuggestionServiceConnectionManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // so the bars draw behind the system bars, and the keyboard's insets reach Compose on every API level
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        initConversationViewModel()
        loadDraft()
        observeEvents()

        val actions = ReaderCommentListActions(
            onBack = ::finish,
            onRefresh = {
                conversationViewModel.onRefresh()
                viewModel.onRefresh()
            },
            onLoadMore = viewModel::onLoadMore,
            onScrollHandled = viewModel::onScrollHandled,
            onHeaderClick = viewModel::onHeaderClicked,
            onAuthorClick = viewModel::onAuthorClicked,
            onReplyClick = viewModel::onReplyClicked,
            onLikeClick = viewModel::onLikeClicked,
            onModerateClick = viewModel::onModerateClicked,
            onEditClick = viewModel::onEditClicked,
            onShareClick = viewModel::onShareClicked,
            onImageClick = viewModel::onImageClicked,
            onComposerClick = viewModel::onComposerClicked,
            onReplyTextChange = { replyText.value = it },
            onSendReply = { viewModel.onSendReply(replyText.value.text) },
            onDeleteDraft = { replyText.value = TextFieldValue("") },
            onReplySheetDismissed = { viewModel.onReplySheetDismissed(replyText.value.text.isNotBlank()) }
        )

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val followState by conversationViewModel.updateFollowUiState.observeAsState()
            AppThemeM3 {
                ReaderCommentListScreen(
                    uiState = uiState,
                    followState = followState,
                    replyText = replyText.value,
                    actions = actions,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        saveDraft()
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onDestroy() {
        suggestionServiceConnectionManager?.unbindFromService()
        super.onDestroy()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: SuggestionNameListUpdated) {
        viewModel.onSuggestionsUpdated(event.mRemoteBlogId)
    }

    private fun initConversationViewModel() {
        conversationViewModel =
            ViewModelProvider(this, viewModelFactory)[ConversationNotificationsViewModel::class.java]
        conversationViewModel.snackbarEvents.observe(this) { event ->
            // the bottom sheet shows its own changes
            if (getBottomSheetFragment() != null) return@observe
            event.getContentIfNotHandled()?.let { holder ->
                lifecycleScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showMessage(holder, this@ReaderCommentListActivity, uiHelpers)
                }
            }
        }
        conversationViewModel.showBottomSheetEvent.observe(this) { event ->
            event.applyIfNotHandled {
                val bottomSheet = getBottomSheetFragment()
                if (show && bottomSheet == null) {
                    CommentNotificationsBottomSheetFragment.newInstance(isReceivingNotifications, false)
                        .show(supportFragmentManager, NOTIFICATIONS_BOTTOM_SHEET_TAG)
                } else if (!show) {
                    bottomSheet?.dismiss()
                }
            }
        }
        // the post detail updates its own follow state from this
        conversationViewModel.updateFollowUiState.observe(this) { uiState ->
            setResult(RESULT_OK, Intent().putExtra(FOLLOW_CONVERSATION_UI_STATE_FLAGS_KEY, uiState.flags))
        }
        conversationViewModel.start(
            viewModel.blogId,
            viewModel.postId,
            ThreadedCommentsActionSource.READER_THREADED_COMMENTS
        )
    }

    private fun getBottomSheetFragment(): CommentNotificationsBottomSheetFragment? =
        supportFragmentManager.findFragmentByTag(NOTIFICATIONS_BOTTOM_SHEET_TAG)
            as? CommentNotificationsBottomSheetFragment

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { handleEvent(it) }
            }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun CoroutineScope.handleEvent(event: ReaderCommentListEvent) {
        when (event) {
            is ReaderCommentListEvent.FinishWithError -> {
                ToastUtils.showToast(this@ReaderCommentListActivity, event.messageRes)
                finish()
            }
            is ReaderCommentListEvent.ShowToast -> ToastUtils.showToast(
                this@ReaderCommentListActivity,
                event.messageRes,
                if (event.isLong) ToastUtils.Duration.LONG else ToastUtils.Duration.SHORT
            )
            is ReaderCommentListEvent.ShowModerationUndo -> launch {
                showModerationUndo(event.commentId, getString(event.messageRes))
            }
            ReaderCommentListEvent.ShowSignInPrompt -> showSnackbar(
                message = getString(R.string.reader_snackbar_err_cannot_like_post_logged_out),
                actionLabel = getString(R.string.sign_in),
                duration = SnackbarDuration.Indefinite,
                onAction = viewModel::onSignInClicked
            )
            ReaderCommentListEvent.SignIn -> ActivityLauncher.loginWithoutMagicLink(
                this@ReaderCommentListActivity
            )
            ReaderCommentListEvent.ClearReply -> replyText.value = TextFieldValue("")
            is ReaderCommentListEvent.RestoreReply ->
                replyText.value = TextFieldValue(event.text, TextRange(event.text.length))
            is ReaderCommentListEvent.OpenPostDetail -> ReaderActivityLauncher.showReaderPostDetail(
                this@ReaderCommentListActivity,
                event.blogId,
                event.postId
            )
            is ReaderCommentListEvent.OpenBlogPreview -> ReaderActivityLauncher.showReaderBlogPreview(
                this@ReaderCommentListActivity,
                event.blogId,
                event.isFollowed,
                ReaderTracker.SOURCE_COMMENT,
                readerTracker
            )
            is ReaderCommentListEvent.EditComment -> editCommentLauncher.launch(
                UnifiedCommentsEditActivity.createIntent(
                    this@ReaderCommentListActivity,
                    ReaderCommentIdentifier(event.blogId, event.postId, event.commentId),
                    event.site
                )
            )
            is ReaderCommentListEvent.ShareComment ->
                ActivityLauncher.openShareIntent(this@ReaderCommentListActivity, event.url, null)
            is ReaderCommentListEvent.OpenPhoto -> showPhoto(event.imageUrl, event.isPrivate)
            ReaderCommentListEvent.BindSuggestionService -> if (suggestionServiceConnectionManager == null) {
                suggestionServiceConnectionManager =
                    SuggestionServiceConnectionManager(this@ReaderCommentListActivity, viewModel.blogId)
                        .also { it.bindToService() }
            }
        }
    }

    /*
     * shows the undo snackbar for a moderation, and tells the ViewModel when it's gone - including when it's
     * replaced by another snackbar or the screen stops
     */
    private suspend fun showModerationUndo(commentId: Long, message: String) {
        var isUndone = false
        try {
            snackbarHostState.currentSnackbarData?.dismiss()
            isUndone = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = getString(R.string.undo),
                duration = SnackbarDuration.Long
            ) == SnackbarResult.ActionPerformed
        } finally {
            viewModel.onModerationUndoFinished(commentId, isUndone)
        }
    }

    private fun showSnackbar(
        message: String,
        actionLabel: String?,
        duration: SnackbarDuration = SnackbarDuration.Long,
        onAction: (() -> Unit)? = null
    ) {
        lifecycleScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(message, actionLabel, duration = duration)
            if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
        }
    }

    private fun showPhoto(imageUrl: String, isPrivate: Boolean) {
        val options = EnumSet.noneOf(PhotoViewerOption::class.java)
        if (isPrivate) options.add(PhotoViewerOption.IS_PRIVATE_IMAGE)
        ReaderActivityLauncher.showReaderPhotoViewer(this, imageUrl, null, null, options, 0, 0)
    }

    private fun loadDraft() {
        val draft = draftDatabase.get(draftKey, "")
        if (draft.isNotEmpty()) replyText.value = TextFieldValue(draft, TextRange(draft.length))
    }

    private fun saveDraft() {
        val text = replyText.value.text
        if (text.isBlank()) draftDatabase.remove(draftKey) else draftDatabase.put(draftKey, text)
    }

    companion object {
        private const val NOTIFICATIONS_BOTTOM_SHEET_TAG = "NOTIFICATIONS_BOTTOM_SHEET_TAG"
    }
}
