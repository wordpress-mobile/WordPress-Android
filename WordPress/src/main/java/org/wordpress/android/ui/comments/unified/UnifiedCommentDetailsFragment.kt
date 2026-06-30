package org.wordpress.android.ui.comments.unified

import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.UnifiedCommentDetailsFragmentBinding
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.SPAM
import org.wordpress.android.fluxc.model.CommentStatus.TRASH
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.Close
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.LaunchEditComment
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.ReplySent
import org.wordpress.android.ui.comments.unified.UnifiedCommentDetailsViewModel.CommentDetailsUiState
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarItem.Info
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class UnifiedCommentDetailsFragment : Fragment(R.layout.unified_comment_details_fragment) {
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

    private val editCommentLauncher: ActivityResultLauncher<android.content.Intent> =
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

        val site = requireNotNull(arguments?.getSerializableCompat<SiteModel>(WordPress.SITE))
        val remoteCommentId = requireArguments().getLong(KEY_REMOTE_COMMENT_ID)

        UnifiedCommentDetailsFragmentBinding.bind(view).apply {
            binding = this
            setupClickListeners()
            setupObservers()
        }

        viewModel.start(site, remoteCommentId)
    }

    private fun UnifiedCommentDetailsFragmentBinding.setupClickListeners() {
        buttonApprove.setOnClickListener { viewModel.onApproveClicked() }
        buttonSpam.setOnClickListener { viewModel.onSpamClicked() }
        buttonTrash.setOnClickListener { viewModel.onTrashClicked() }
        buttonLike.setOnClickListener { viewModel.onLikeClicked() }
        buttonEdit.setOnClickListener { viewModel.onEditClicked() }
        buttonSendReply.setOnClickListener { viewModel.onReplyClicked(replyEditText.text.toString()) }
    }

    private fun UnifiedCommentDetailsFragmentBinding.setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { renderUiState(it) }

        viewModel.uiActionEvent.observeEvent(viewLifecycleOwner) { event ->
            when (event) {
                is Close -> requireActivity().finish()
                is ReplySent -> {
                    replyEditText.text?.clear()
                    view?.let { ActivityUtils.hideKeyboardForced(it) }
                }
                is LaunchEditComment -> editCommentLauncher.launch(
                    UnifiedCommentsEditActivity.createIntent(
                        requireContext(),
                        event.commentIdentifier,
                        event.site
                    )
                )
            }
        }

        viewModel.onSnackbarMessage.observeEvent(viewLifecycleOwner) { showSnackbar(it) }
    }

    private fun UnifiedCommentDetailsFragmentBinding.renderUiState(uiState: CommentDetailsUiState) {
        progressBar.visibility = if (uiState.showProgress) View.VISIBLE else View.GONE
        scrollView.visibility = if (uiState.contentVisible) View.VISIBLE else View.GONE
        if (!uiState.contentVisible) return

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

        buttonApprove.setText(
            if (uiState.status == APPROVED) R.string.mnu_comment_unapprove else R.string.mnu_comment_approve
        )
        buttonSpam.setText(
            if (uiState.status == SPAM) R.string.mnu_comment_unspam else R.string.mnu_comment_spam
        )
        buttonTrash.setText(
            if (uiState.status == TRASH) R.string.mnu_comment_untrash else R.string.mnu_comment_trash
        )
        buttonLike.setText(if (uiState.isLiked) R.string.mnu_comment_liked else R.string.like)

        replyEditText.hint = if (uiState.authorName.isNotBlank()) {
            getString(R.string.comment_reply_to_user, uiState.authorName)
        } else {
            getString(R.string.reply)
        }
        buttonSendReply.isEnabled = !uiState.isReplyInProgress
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

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
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
