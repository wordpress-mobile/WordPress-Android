package org.wordpress.android.ui.comments.unified

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityId
import org.wordpress.android.ui.ActivityId.COMMENT_EDITOR
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.EditCommentActionEvent.CLOSE
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.EditCommentActionEvent.DONE
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.EditCommentUiState
import org.wordpress.android.ui.comments.unified.compose.UnifiedCommentEditScreen
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.utils.showMessage
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.getParcelableCompat
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class UnifiedCommentsEditFragment : Fragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var uiHelpers: UiHelpers

    private lateinit var viewModel: UnifiedCommentsEditViewModel
    private lateinit var site: SiteModel
    private lateinit var commentIdentifier: CommentIdentifier

    private val snackbarHostState = SnackbarHostState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory)[UnifiedCommentsEditViewModel::class.java]

        site = requireNotNull(arguments?.getSerializableCompat(WordPress.SITE))
        commentIdentifier = requireNotNull(requireArguments().getParcelableCompat(KEY_COMMENT_IDENTIFIER))

        ActivityId.trackLastActivity(COMMENT_EDITOR)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            // Stable id so the fragment restores the ComposeView's saved state across rotation.
            id = R.id.comment_edit_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppThemeM3 {
                    val uiState by viewModel.uiState.observeAsState(EditCommentUiState(showProgress = true))
                    UnifiedCommentEditScreen(
                        uiState = uiState,
                        snackbarHostState = snackbarHostState,
                        onFieldChange = { value, fieldType -> viewModel.onValidateField(value, fieldType) },
                        onSaveClick = { viewModel.onActionMenuClicked() },
                        onNavigateBack = { viewModel.onBackPressed() },
                        onDiscardConfirm = { viewModel.onConfirmEditingDiscard() },
                        onDiscardDismiss = { viewModel.onDiscardDialogDismissed() }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        viewModel.start(site, commentIdentifier)
    }

    private fun setupObservers() {
        viewModel.uiActionEvent.observeEvent(viewLifecycleOwner) { event ->
            when (event) {
                CLOSE -> requireActivity().finish()
                DONE -> requireActivity().apply {
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
        viewModel.onSnackbarMessage.observeEvent(viewLifecycleOwner) { showSnackbar(it) }
    }

    private fun showSnackbar(holder: SnackbarMessageHolder) {
        val context = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            snackbarHostState.showMessage(holder, context, uiHelpers)
        }
    }

    companion object {
        private const val KEY_COMMENT_IDENTIFIER = "key_comment_identifier"

        fun newInstance(site: SiteModel, commentIdentifier: CommentIdentifier): UnifiedCommentsEditFragment {
            return UnifiedCommentsEditFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(WordPress.SITE, site)
                    putParcelable(KEY_COMMENT_IDENTIFIER, commentIdentifier)
                }
            }
        }
    }
}
