package org.wordpress.android.ui.comments.unified

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.UnifiedCommentsEditFragmentBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityId
import org.wordpress.android.ui.ActivityId.COMMENT_EDITOR
import org.wordpress.android.ui.comments.unified.EditCancelDialogFragment.Companion.EDIT_CANCEL_DIALOG_TAG
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.EditCommentActionEvent.CANCEL_EDIT_CONFIRM
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.EditCommentActionEvent.CLOSE
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.EditCommentActionEvent.DONE
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.FieldType.COMMENT
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.FieldType.USER_EMAIL
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.FieldType.USER_NAME
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.FieldType.WEB_ADDRESS
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarItem.Action
import org.wordpress.android.util.SnackbarItem.Info
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class UnifiedCommentsEditFragment : Fragment(R.layout.unified_comments_edit_fragment) {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var snackbarSequencer: SnackbarSequencer

    private lateinit var viewModel: UnifiedCommentsEditViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory).get(UnifiedCommentsEditViewModel::class.java)

        ActivityId.trackLastActivity(COMMENT_EDITOR)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val site = requireArguments().getSerializable(WordPress.SITE) as SiteModel
        val commentIdentifier = requireNotNull(
            requireArguments().getParcelable<CommentIdentifier>(
                KEY_COMMENT_IDENTIFIER
            )
        )

        UnifiedCommentsEditFragmentBinding.bind(view).apply {
            setupToolbar()
            setupObservers(site, commentIdentifier)
        }
    }

    private fun UnifiedCommentsEditFragmentBinding.setupToolbar() {
        setHasOptionsMenu(true)

        val activity = (requireActivity() as AppCompatActivity)
        activity.setSupportActionBar(toolbarMain)
        activity.supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeAsUpIndicator(R.drawable.ic_cross_white_24dp)
        }
        activity.onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(
                true
            ) {
                override fun handleOnBackPressed() {
                    viewModel.onBackPressed()
                }
            })
    }

    private fun hideKeyboard() {
        if (!isAdded || view == null) return
        ActivityUtils.hideKeyboardForced(view)
    }

    private fun UnifiedCommentsEditFragmentBinding.setupObservers(
        site: SiteModel,
        commentIdentifier: CommentIdentifier
    ) {
        viewModel.uiActionEvent.observeEvent(viewLifecycleOwner, {
            when (it) {
                CLOSE -> {
                    requireActivity().finish()
                }
                DONE -> {
                    requireActivity().apply {
                        setResult(RESULT_OK)
                        finish()
                    }
                }
                CANCEL_EDIT_CONFIRM -> {
                    EditCancelDialogFragment.newInstance().show(childFragmentManager, EDIT_CANCEL_DIALOG_TAG)
                }
            }
        })

        viewModel.onSnackbarMessage.observeEvent(viewLifecycleOwner, { messageHolder ->
            showSnackbar(messageHolder)
        })

        viewModel.uiState.observe(viewLifecycleOwner, { uiState ->
            if (uiState.showProgress) {
                loadingView.visibility = View.VISIBLE
                scrollView.visibility = View.GONE
                uiHelpers.setTextOrHide(progressText, uiState.progressText)

                hideKeyboard()
            } else {
                loadingView.visibility = View.GONE
                scrollView.visibility = View.VISIBLE
            }

            if (uiState.shouldInitComment) {
                uiState.originalComment.let {
                    userName.setText(it.userName)
                    commentEditWebAddress.setText(it.userUrl)
                    commentEditEmailAddress.setText(it.userEmail)
                    commentEditComment.setText(it.commentText)
                }
            }

            if (uiState.shouldInitWatchers) {
                initTextWatchers()
            }

            uiState.editErrorStrings.let { errors ->
                userName.error = errors.userNameError
                commentEditWebAddress.error = errors.userUrlError
                commentEditEmailAddress.error = errors.userEmailError
                commentEditComment.error = errors.commentTextError
            }

            with(uiState.inputSettings) {
                commentEditComment.isEnabled = enableEditComment
                commentEditWebAddress.isEnabled = enableEditUrl
                commentEditEmailAddress.isEnabled = enableEditEmail
                userName.isEnabled = enableEditName
            }
        })

        viewModel.start(site, commentIdentifier)
    }

    private fun UnifiedCommentsEditFragmentBinding.showSnackbar(holder: SnackbarMessageHolder) {
        snackbarSequencer.enqueue(
            SnackbarItem(
                Info(
                    view = coordinator,
                    textRes = holder.message,
                    duration = Snackbar.LENGTH_LONG
                ),
                holder.buttonTitle?.let {
                    Action(
                        textRes = holder.buttonTitle,
                        clickListener = { holder.buttonAction() }
                    )
                },
                dismissCallback = { _, event -> holder.onDismissAction(event) }
            )
        )
    }

    private fun UnifiedCommentsEditFragmentBinding.initTextWatchers() {
        userName.doAfterTextChanged {
            viewModel.onValidateField(it?.let { StringBuffer(it).toString() } ?: "", USER_NAME)
        }

        commentEditWebAddress.doAfterTextChanged {
            viewModel.onValidateField(it?.let { StringBuffer(it).toString() } ?: "", WEB_ADDRESS)
        }

        commentEditEmailAddress.doAfterTextChanged {
            viewModel.onValidateField(it?.let { StringBuffer(it).toString() } ?: "", USER_EMAIL)
        }

        commentEditComment.doAfterTextChanged {
            viewModel.onValidateField(it?.let { StringBuffer(it).toString() } ?: "", COMMENT)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.edit_comment_menu, menu)

        menu.findItem(R.id.action_item)?.let { actionMenu ->
            actionMenu.setOnMenuItemClickListener {
                viewModel.onActionMenuClicked()
                true
            }

            viewModel.uiState.observe(viewLifecycleOwner, { uiState ->
                actionMenu.isEnabled = uiState.canSaveChanges
            })
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                viewModel.onBackPressed()
            }
        }
        return true
    }

    companion object {
        private const val KEY_COMMENT_IDENTIFIER = "key_comment_identifier"

        fun newInstance(site: SiteModel, commentIdentifier: CommentIdentifier): UnifiedCommentsEditFragment {
            val args = Bundle()

            args.putSerializable(WordPress.SITE, site)
            args.putParcelable(KEY_COMMENT_IDENTIFIER, commentIdentifier)

            val fragment = UnifiedCommentsEditFragment()

            fragment.arguments = args

            return fragment
        }
    }
}
