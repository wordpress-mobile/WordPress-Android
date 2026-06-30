package org.wordpress.android.ui.comments.unified

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.wordpress.android.R
import com.google.android.material.R as MaterialR
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.ReaderIncludeCommentBoxBinding
import org.wordpress.android.databinding.UnifiedCommentDetailsFragmentBinding
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.SPAM
import org.wordpress.android.fluxc.model.CommentStatus.TRASH
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.CollapseFullScreenDialogFragment
import org.wordpress.android.ui.CommentFullScreenDialogFragment
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.Close
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.LaunchEditComment
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.OpenPostInReader
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.ReplySent
import org.wordpress.android.ui.comments.unified.UnifiedCommentDetailsViewModel.CommentDetailsUiState
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.ReaderActivityLauncher
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager
import org.wordpress.android.ui.suggestion.util.SuggestionUtils
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarItem.Info
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.ColorUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.extensions.getColorResIdFromAttribute
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class UnifiedCommentDetailsFragment :
    Fragment(R.layout.unified_comment_details_fragment),
    CollapseFullScreenDialogFragment.OnConfirmListener,
    CollapseFullScreenDialogFragment.OnCollapseListener {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var snackbarSequencer: SnackbarSequencer

    private lateinit var viewModel: UnifiedCommentDetailsViewModel
    private var binding: UnifiedCommentDetailsFragmentBinding? = null
    private var currentState: CommentDetailsUiState? = null

    private lateinit var site: SiteModel
    private var remoteCommentId: Long = 0
    private var suggestionServiceConnectionManager: SuggestionServiceConnectionManager? = null

    private val mediumOpacity by lazy {
        ResourcesCompat.getFloat(resources, MaterialR.dimen.material_emphasis_medium)
    }

    private val editCommentLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                viewModel.refreshComment()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory)[UnifiedCommentDetailsViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        site = requireNotNull(arguments?.getSerializableCompat<SiteModel>(WordPress.SITE))
        remoteCommentId = requireArguments().getLong(KEY_REMOTE_COMMENT_ID)

        UnifiedCommentDetailsFragmentBinding.bind(view).apply {
            binding = this
            setupClickListeners()
            layoutCommentBox.setupReplyBox()
            layoutCommentBox.setupSuggestions()
            setupObservers()
        }

        viewModel.start(site, remoteCommentId)
    }

    private fun UnifiedCommentDetailsFragmentBinding.setupClickListeners() {
        layoutButtons.btnModerate.setOnClickListener { viewModel.onApproveClicked() }
        layoutButtons.btnSpam.setOnClickListener { viewModel.onSpamClicked() }
        layoutButtons.btnLike.setOnClickListener { viewModel.onLikeClicked() }
        layoutButtons.btnMore.setOnClickListener { showMoreMenu(it) }
        textPostTitle.setOnClickListener { viewModel.onPostTitleClicked() }
        // Spam is hidden by default in the shared comment_action_footer layout; the unified detail
        // always offers it, so show it up front rather than only once the comment has loaded.
        layoutButtons.btnSpam.visibility = View.VISIBLE
    }

    private fun ReaderIncludeCommentBoxBinding.setupReplyBox() {
        layoutContainer.visibility = View.VISIBLE
        editComment.initializeWithPrefix('@')
        editComment.doAfterTextChanged { btnSubmitReply.isEnabled = !it.isNullOrBlank() }
        btnSubmitReply.setOnClickListener { viewModel.onReplyClicked(editComment.text.toString()) }
        buttonExpand.setOnClickListener { showFullScreenReply() }
        editComment.autoSaveTextHelper.uniqueId = "${site.siteId}-$remoteCommentId"
        editComment.autoSaveTextHelper.loadString(editComment)
    }

    private fun ReaderIncludeCommentBoxBinding.setupSuggestions() {
        if (!SiteUtils.isAccessedViaWPComRest(site)) return
        val connectionManager = SuggestionServiceConnectionManager(requireActivity(), site.siteId)
        suggestionServiceConnectionManager = connectionManager
        editComment.setAdapter(SuggestionUtils.setupUserSuggestions(site, requireActivity(), connectionManager))
    }

    private fun showFullScreenReply() {
        val box = binding?.layoutCommentBox ?: return
        val bundle = CommentFullScreenDialogFragment.newBundle(
            box.editComment.text.toString(),
            box.editComment.selectionStart,
            box.editComment.selectionEnd,
            site.siteId
        )
        CollapseFullScreenDialogFragment.Builder(requireContext())
            .setTitle(R.string.comment)
            .setOnCollapseListener(this)
            .setOnConfirmListener(this)
            .setContent(CommentFullScreenDialogFragment::class.java, bundle)
            .setAction(R.string.send)
            .setHideActivityBar(true)
            .build()
            .show(requireActivity().supportFragmentManager, fullScreenDialogTag())
    }

    override fun onConfirm(result: Bundle?) {
        val box = binding?.layoutCommentBox ?: return
        if (result != null) {
            box.editComment.setText(result.getString(CommentFullScreenDialogFragment.RESULT_REPLY))
            viewModel.onReplyClicked(box.editComment.text.toString())
        }
    }

    override fun onCollapse(result: Bundle?) {
        val box = binding?.layoutCommentBox ?: return
        if (result != null) {
            box.editComment.setText(result.getString(CommentFullScreenDialogFragment.RESULT_REPLY))
            box.editComment.setSelection(
                result.getInt(CommentFullScreenDialogFragment.RESULT_SELECTION_START),
                result.getInt(CommentFullScreenDialogFragment.RESULT_SELECTION_END)
            )
            box.editComment.requestFocus()
        }
    }

    override fun onResume() {
        super.onResume()
        // Reattach listeners to a collapsible reply dialog that may have survived recreation
        val fragment = requireActivity().supportFragmentManager
            .findFragmentByTag(fullScreenDialogTag()) as? CollapseFullScreenDialogFragment
        if (fragment != null && fragment.isAdded) {
            fragment.setOnCollapseListener(this)
            fragment.setOnConfirmListener(this)
        }
    }

    private fun UnifiedCommentDetailsFragmentBinding.setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { renderUiState(it) }

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
    }

    private fun clearReplyInput() {
        binding?.layoutCommentBox?.editComment?.let { edit ->
            edit.setText("")
            edit.autoSaveTextHelper.clearSavedText(edit)
        }
        view?.let { ActivityUtils.hideKeyboardForced(it) }
    }

    private fun UnifiedCommentDetailsFragmentBinding.renderUiState(uiState: CommentDetailsUiState) {
        progressBar.visibility = if (uiState.showProgress) View.VISIBLE else View.GONE
        // INVISIBLE (not GONE) so the scroll view keeps its weighted space while the comment loads,
        // which keeps the action buttons pinned to the bottom instead of floating to the top.
        scrollView.visibility = if (uiState.contentVisible) View.VISIBLE else View.INVISIBLE
        if (!uiState.contentVisible) return
        currentState = uiState

        textAuthorName.text = uiState.authorName
        textDate.text = uiState.datePublished
        textCommentContent.text = HtmlCompat.fromHtml(uiState.commentText, HtmlCompat.FROM_HTML_MODE_COMPACT)
        uiHelpers.setTextOrHide(textPostTitle, uiState.postTitle)

        if (uiState.authorAvatarUrl.isNotEmpty()) {
            imageManager.loadIntoCircle(imageAvatar, ImageType.AVATAR_WITHOUT_BACKGROUND, uiState.authorAvatarUrl)
        }

        textStatus.setText(
            when (uiState.status) {
                APPROVED -> R.string.comment_status_approved
                UNAPPROVED -> R.string.comment_status_unapproved
                SPAM -> R.string.comment_status_spam
                TRASH -> R.string.comment_status_trash
                else -> R.string.comment_status_all
            }
        )

        renderActionButtons(uiState)
        renderReplyBox(uiState)
    }

    private fun UnifiedCommentDetailsFragmentBinding.renderActionButtons(uiState: CommentDetailsUiState) {
        with(layoutButtons) {
            when (uiState.status) {
                APPROVED -> styleActionButton(
                    btnModerateIcon, btnModerateText,
                    R.drawable.ic_checkmark_white_24dp, R.string.comment_status_approved, isOn = true
                )
                TRASH -> styleActionButton(
                    btnModerateIcon, btnModerateText,
                    R.drawable.ic_undo_white_24dp, R.string.mnu_comment_untrash, isOn = false
                )
                else -> styleActionButton(
                    btnModerateIcon, btnModerateText,
                    R.drawable.ic_checkmark_white_24dp, R.string.mnu_comment_approve, isOn = false
                )
            }

            btnSpamText.setText(
                if (uiState.status == SPAM) R.string.mnu_comment_unspam else R.string.mnu_comment_spam
            )

            styleActionButton(
                btnLikeIcon, btnLikeText,
                if (uiState.isLiked) R.drawable.ic_star_white_24dp else R.drawable.ic_star_outline_white_24dp,
                if (uiState.isLiked) R.string.mnu_comment_liked else R.string.like,
                isOn = uiState.isLiked
            )
        }
    }

    /**
     * Styles a footer action button (icon + label) for its on/off state, matching the legacy
     * comment detail: accent colour at full opacity when on, [MaterialR.attr.colorOnSurface] at
     * medium opacity when off.
     */
    private fun styleActionButton(
        icon: ImageView,
        text: TextView,
        @DrawableRes iconRes: Int,
        @StringRes textRes: Int,
        isOn: Boolean
    ) {
        val colorRes = requireContext().getColorResIdFromAttribute(
            if (isOn) MaterialR.attr.colorSecondary else MaterialR.attr.colorOnSurface
        )
        ColorUtils.setImageResourceWithTint(icon, iconRes, colorRes)
        text.setText(textRes)
        text.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
        val alpha = if (isOn) 1f else mediumOpacity
        icon.alpha = alpha
        text.alpha = alpha
    }

    private fun UnifiedCommentDetailsFragmentBinding.renderReplyBox(uiState: CommentDetailsUiState) {
        with(layoutCommentBox) {
            editComment.hint = if (uiState.authorName.isNotBlank()) {
                getString(R.string.comment_reply_to_user, uiState.authorName)
            } else {
                getString(R.string.reader_hint_comment_on_post)
            }
            editComment.isEnabled = !uiState.isReplyInProgress
            progressSubmitComment.visibility = if (uiState.isReplyInProgress) View.VISIBLE else View.GONE
            btnSubmitReply.visibility = if (uiState.isReplyInProgress) View.GONE else View.VISIBLE
        }
    }

    private fun UnifiedCommentDetailsFragmentBinding.showSnackbar(holder: SnackbarMessageHolder) {
        snackbarSequencer.enqueue(
            SnackbarItem(
                Info(
                    view = coordinator,
                    textRes = holder.message,
                    duration = Snackbar.LENGTH_LONG
                ),
                dismissCallback = { _, event -> holder.onDismissAction(event) }
            )
        )
    }

    private fun showMoreMenu(anchor: View) {
        val state = currentState ?: return
        PopupMenu(requireContext(), anchor).apply {
            menuInflater.inflate(R.menu.unified_comment_details_more, menu)
            menu.findItem(R.id.menu_trash).setTitle(
                if (state.status == TRASH) R.string.mnu_comment_untrash else R.string.mnu_comment_trash
            )
            menu.findItem(R.id.menu_copy_link).isVisible = state.commentUrl.isNotEmpty()
            menu.findItem(R.id.menu_share_link).isVisible = state.commentUrl.isNotEmpty()
            menu.findItem(R.id.menu_delete_permanently).isVisible =
                state.status == TRASH || state.status == SPAM
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_edit -> viewModel.onEditClicked()
                    R.id.menu_trash -> viewModel.onTrashClicked()
                    R.id.menu_copy_link -> copyLink(state.commentUrl)
                    R.id.menu_share_link -> shareLink(state.commentUrl)
                    R.id.menu_delete_permanently -> confirmDeletePermanently()
                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
            show()
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
        binding?.showSnackbar(SnackbarMessageHolder(UiStringRes(message)))
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
            ToastUtils.showToast(requireContext(), R.string.comment_toast_err_share_intent)
        }
    }

    private fun confirmDeletePermanently() {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.dlg_sure_to_delete_comment)
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.onDeletePermanentlyClicked() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun fullScreenDialogTag() = "${CollapseFullScreenDialogFragment.TAG}_${site.siteId}_$remoteCommentId"

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        suggestionServiceConnectionManager?.unbindFromService()
        super.onDestroy()
    }

    companion object {
        private const val KEY_REMOTE_COMMENT_ID = "key_remote_comment_id"

        fun newInstance(site: SiteModel, remoteCommentId: Long): UnifiedCommentDetailsFragment {
            return UnifiedCommentDetailsFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(WordPress.SITE, site)
                    putLong(KEY_REMOTE_COMMENT_ID, remoteCommentId)
                }
            }
        }
    }
}
