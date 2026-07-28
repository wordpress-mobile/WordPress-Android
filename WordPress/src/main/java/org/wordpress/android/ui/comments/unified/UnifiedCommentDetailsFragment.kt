package org.wordpress.android.ui.comments.unified

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.datasets.UserSuggestionTable
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.Close
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.LaunchEditComment
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.OpenPostInReader
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.ReplySent
import org.wordpress.android.ui.comments.unified.UnifiedCommentDetailsViewModel.CommentDetailsUiState
import org.wordpress.android.ui.comments.unified.compose.CommentDetailsActions
import org.wordpress.android.ui.comments.unified.compose.UnifiedCommentDetailsScreen
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.utils.showMessage
import org.wordpress.android.ui.notifications.NotificationsListFragment
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.ReaderActivityLauncher
import org.wordpress.android.ui.suggestion.Suggestion
import org.wordpress.android.ui.suggestion.service.SuggestionEvents.SuggestionNameListUpdated
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.viewmodel.observeEvent
import org.wordpress.persistentedittext.PersistentEditTextDatabase
import javax.inject.Inject

class UnifiedCommentDetailsFragment : Fragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var uiHelpers: UiHelpers

    private lateinit var viewModel: UnifiedCommentDetailsViewModel

    private lateinit var site: SiteModel
    private var remoteCommentId: Long = 0
    private var noteId: String? = null
    private var suggestionServiceConnectionManager: SuggestionServiceConnectionManager? = null

    // The result reported to the launching screen, accumulated across observers: moderation adds
    // the note extras the notifications list uses to update the moderated note's row.
    private val resultIntent = Intent()

    // Compose state owned by the fragment so the ViewModel observers can drive it: the reply
    // draft (cleared on send), the @-mention suggestions and the snackbar queue.
    private val replyText = mutableStateOf(TextFieldValue(""))
    private val suggestions = mutableStateOf<List<Suggestion>>(emptyList())
    private val snackbarHostState = SnackbarHostState()

    // Persists the reply draft across process death, replacing the legacy reply box's
    // PersistentEditTextHelper (same library, keyed explicitly instead of by view path).
    private val draftDatabase by lazy { PersistentEditTextDatabase(requireContext()) }

    // Key drafts by the local site id: siteId (the WP.com blog id) is 0 for all self-hosted
    // application-password sites, which would collide drafts across sites.
    private val draftKey get() = "unified_comment_details_${site.id}-$remoteCommentId"

    private val editCommentLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                viewModel.onCommentEdited()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory)[UnifiedCommentDetailsViewModel::class.java]
        site = requireNotNull(arguments?.getSerializableCompat<SiteModel>(WordPress.SITE))
        remoteCommentId = requireArguments().getLong(KEY_REMOTE_COMMENT_ID)
        noteId = arguments?.getString(KEY_NOTE_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Opened via the notification's "reply" action: focus the reply field right away, like the
        // legacy detail. Only on the first creation — not again after a rotation.
        val focusReplyField =
            savedInstanceState == null && arguments?.getBoolean(KEY_FOCUS_REPLY_FIELD) == true
        return ComposeView(requireContext()).apply {
            // A stable id (not View.generateViewId()) lets the fragment restore the ComposeView's
            // saved state across rotation, so rememberSaveable dialog flags survive config changes.
            id = R.id.comment_detail_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppThemeM3 {
                    val uiState by viewModel.uiState.observeAsState(CommentDetailsUiState())
                    val replyTextValue by replyText
                    val suggestionList by suggestions
                    UnifiedCommentDetailsScreen(
                        uiState = uiState,
                        replyText = replyTextValue,
                        onReplyTextChange = { replyText.value = it },
                        suggestions = suggestionList,
                        // Liking stays on FluxC, which only supports WP.com-accessed sites — hide
                        // the button on self-hosted application-password sites like the legacy
                        // comment detail does.
                        showLikeButton = SiteUtils.isAccessedViaWPComRest(site),
                        focusReplyFieldOnLaunch = focusReplyField,
                        snackbarHostState = snackbarHostState,
                        actions = actions
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSuggestions()
        loadReplyDraft(isFreshView = savedInstanceState == null)
        setupObservers()
        // Lets the notifications host lift its app bar with the comment's scroll position;
        // the rs comments list host doesn't implement the listener, so this is a no-op there.
        (activity as? ScrollableViewInitializedListener)?.onScrollableViewInitialized(view.id)
        viewModel.start(site, remoteCommentId, noteId)
    }

    // A single instance for the fragment's lifetime, so recompositions see a stable parameter
    private val actions by lazy {
        CommentDetailsActions(
            onModerateClick = { viewModel.onApproveClicked() },
            onSpamClick = { viewModel.onSpamClicked() },
            onLikeClick = { viewModel.onLikeClicked() },
            onEditClick = { viewModel.onEditClicked() },
            onTrashClick = { viewModel.onTrashClicked() },
            onDeletePermanentlyClick = { viewModel.onDeletePermanentlyClicked() },
            onCopyLinkClick = { copyLink(viewModel.uiState.value?.commentUrl.orEmpty()) },
            onShareLinkClick = { shareLink(viewModel.uiState.value?.commentUrl.orEmpty()) },
            onPostTitleClick = { viewModel.onPostTitleClicked() },
            onSendReply = { viewModel.onReplyClicked(it) }
        )
    }

    private fun setupObservers() {
        viewModel.uiActionEvent.observeEvent(viewLifecycleOwner) { event ->
            when (event) {
                is Close -> requireActivity().finish()
                is ReplySent -> clearReplyInput()
                is LaunchEditComment -> editCommentLauncher.launch(
                    UnifiedCommentsEditActivity.createIntent(
                        requireContext(),
                        event.commentIdentifier,
                        event.site
                    )
                )
                is OpenPostInReader -> ReaderActivityLauncher.showReaderPostDetail(
                    requireContext(),
                    event.blogId,
                    event.postId
                )
            }
        }

        viewModel.onSnackbarMessage.observeEvent(viewLifecycleOwner) { showSnackbar(it) }

        // Report the change to the launching screen (the rs comments list only refreshes when
        // the result says something actually changed).
        viewModel.commentChanged.observeEvent(viewLifecycleOwner) {
            requireActivity().setResult(AppCompatActivity.RESULT_OK, resultIntent)
        }

        // Note mode: attach the extras the notifications list uses to update the moderated
        // note's row without waiting for its next refresh.
        viewModel.commentModerated.observeEvent(viewLifecycleOwner) { status ->
            noteId?.let { id ->
                resultIntent.putExtra(NotificationsListFragment.NOTE_MODERATE_ID_EXTRA, id)
                resultIntent.putExtra(NotificationsListFragment.NOTE_MODERATE_STATUS_EXTRA, status.toString())
                requireActivity().setResult(AppCompatActivity.RESULT_OK, resultIntent)
            }
        }
    }

    /**
     * Binds the user-suggestion service so `@`-mention suggestions are downloaded, and seeds the
     * suggestion state from the local table — WP.com-accessed sites only, like the legacy detail.
     * [onSuggestionsUpdated] refreshes the state when the service reports new data.
     */
    private fun setupSuggestions() {
        if (!SiteUtils.isAccessedViaWPComRest(site)) return
        val connectionManager = suggestionServiceConnectionManager
            ?: SuggestionServiceConnectionManager(requireActivity(), site.siteId).also {
                suggestionServiceConnectionManager = it
            }
        connectionManager.bindToService()
        loadSuggestions()
    }

    private fun loadSuggestions() {
        suggestions.value = Suggestion.fromUserSuggestions(
            UserSuggestionTable.getSuggestionsForSite(site.siteId) ?: emptyList()
        )
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSuggestionsUpdated(event: SuggestionNameListUpdated) {
        if (event.mRemoteBlogId != 0L && event.mRemoteBlogId == site.siteId) {
            loadSuggestions()
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

    override fun onPause() {
        super.onPause()
        saveReplyDraft()
    }

    /**
     * Restores the reply draft (saved in [onPause]) into the reply field. Prefill from the
     * notification's inline-reply text applies only on a fresh view and never clobbers a draft.
     */
    private fun loadReplyDraft(isFreshView: Boolean) {
        val draft = draftDatabase.get(draftKey, "")
        val prefill = arguments?.getString(KEY_PREFILL_REPLY_TEXT)
        val initial = when {
            draft.isNotEmpty() -> draft
            isFreshView && !prefill.isNullOrEmpty() -> prefill
            else -> return
        }
        replyText.value = TextFieldValue(initial, selection = TextRange(initial.length))
    }

    private fun saveReplyDraft() {
        val text = replyText.value.text
        if (text.isBlank()) {
            draftDatabase.remove(draftKey)
        } else {
            draftDatabase.put(draftKey, text)
        }
    }

    private fun clearReplyInput() {
        replyText.value = TextFieldValue("")
        draftDatabase.remove(draftKey)
        view?.let { ActivityUtils.hideKeyboardForced(it) }
    }

    private fun showSnackbar(holder: SnackbarMessageHolder) {
        val context = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            snackbarHostState.showMessage(holder, context, uiHelpers)
        }
    }

    private fun copyLink(url: String) {
        if (url.isEmpty()) return
        val clipboard = requireContext().getSystemService(ClipboardManager::class.java)
        val message = if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("CommentLinkAddress", url))
            R.string.comment_q_action_copied_url
        } else {
            R.string.error_copy_to_clipboard
        }
        showSnackbar(SnackbarMessageHolder(UiStringRes(message)))
    }

    private fun shareLink(url: String) {
        if (url.isEmpty()) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.comment_share_link_via)))
        } catch (e: ActivityNotFoundException) {
            AppLog.e(AppLog.T.COMMENTS, "No activity found to share the comment link", e)
            ToastUtils.showToast(requireContext(), R.string.comment_toast_err_share_intent)
        }
    }

    override fun onDestroy() {
        suggestionServiceConnectionManager?.unbindFromService()
        super.onDestroy()
    }

    companion object {
        private const val KEY_REMOTE_COMMENT_ID = "key_remote_comment_id"
        private const val KEY_NOTE_ID = "key_note_id"
        private const val KEY_PREFILL_REPLY_TEXT = "key_prefill_reply_text"
        private const val KEY_FOCUS_REPLY_FIELD = "key_focus_reply_field"

        @JvmStatic
        fun newInstance(site: SiteModel, remoteCommentId: Long): UnifiedCommentDetailsFragment {
            return UnifiedCommentDetailsFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(WordPress.SITE, site)
                    putLong(KEY_REMOTE_COMMENT_ID, remoteCommentId)
                }
            }
        }

        /**
         * Factory for the notifications host: [noteId] puts the screen in note mode (see
         * [UnifiedCommentDetailsViewModel]), [prefillReplyText] carries the notification's
         * inline-reply text and [focusReplyField] opens the keyboard on the reply field.
         */
        @JvmStatic
        fun newInstance(
            site: SiteModel,
            remoteCommentId: Long,
            noteId: String,
            prefillReplyText: String?,
            focusReplyField: Boolean
        ): UnifiedCommentDetailsFragment {
            return newInstance(site, remoteCommentId).apply {
                requireArguments().apply {
                    putString(KEY_NOTE_ID, noteId)
                    putString(KEY_PREFILL_REPLY_TEXT, prefillReplyText)
                    putBoolean(KEY_FOCUS_REPLY_FIELD, focusReplyField)
                }
            }
        }
    }
}
