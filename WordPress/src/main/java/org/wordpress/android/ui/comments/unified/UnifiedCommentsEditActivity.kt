package org.wordpress.android.ui.comments.unified

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityId
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.EditCommentActionEvent.CANCEL_EDIT_CONFIRM
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.EditCommentActionEvent.CLOSE
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.EditCommentActionEvent.DONE
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.EditCommentUiState
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.EditErrorStrings
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.InputSettings
import org.wordpress.android.ui.comments.unified.compose.UnifiedCommentsEditScreen
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.extensions.getParcelableExtraCompat
import org.wordpress.android.util.extensions.getSerializableExtraCompat
import org.wordpress.android.util.extensions.setContent
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class UnifiedCommentsEditActivity : BaseAppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var uiHelpers: UiHelpers

    private lateinit var viewModel: UnifiedCommentsEditViewModel

    private val showDiscardDialog = mutableStateOf(false)
    private val snackbarHostState = SnackbarHostState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory)[UnifiedCommentsEditViewModel::class.java]
        ActivityId.trackLastActivity(ActivityId.COMMENT_EDITOR)

        val site = requireNotNull(intent.getSerializableExtraCompat<SiteModel>(WordPress.SITE))
        val commentIdentifier = requireNotNull(
            intent.getParcelableExtraCompat<CommentIdentifier>(KEY_COMMENT_IDENTIFIER)
        )

        onBackPressedDispatcher.addCallback(this) { viewModel.onBackPressed() }
        setupObservers()

        setContent {
            AppThemeM3 {
                val uiState by viewModel.uiState.observeAsState(initialUiState())
                val isDiscardDialogVisible by showDiscardDialog
                UnifiedCommentsEditScreen(
                    uiState = uiState,
                    showDiscardDialog = isDiscardDialogVisible,
                    snackbarHostState = snackbarHostState,
                    onFieldChanged = viewModel::onValidateField,
                    onDoneClick = viewModel::onActionMenuClicked,
                    onBackClick = viewModel::onBackPressed,
                    onConfirmDiscard = viewModel::onConfirmEditingDiscard,
                    onDismissDiscard = { showDiscardDialog.value = false }
                )
            }
        }

        viewModel.start(site, commentIdentifier)
    }

    private fun setupObservers() {
        viewModel.uiActionEvent.observeEvent(this) { event ->
            when (event) {
                CLOSE -> finish()
                DONE -> {
                    setResult(RESULT_OK)
                    finish()
                }
                CANCEL_EDIT_CONFIRM -> showDiscardDialog.value = true
            }
        }
        viewModel.onSnackbarMessage.observeEvent(this) { showSnackbar(it) }
    }

    private fun showSnackbar(holder: SnackbarMessageHolder) {
        val message = uiHelpers.getTextOfUiString(this, holder.message).toString()
        val actionLabel = holder.buttonTitle?.let { uiHelpers.getTextOfUiString(this, it).toString() }
        lifecycleScope.launch {
            val result = snackbarHostState.showSnackbar(message, actionLabel)
            if (result == SnackbarResult.ActionPerformed) {
                holder.buttonAction()
            }
            // Always report the dismissal: the load-error snackbar closes the screen from it
            holder.onDismissAction(0)
        }
    }

    /** The ui state shown for the first frame, before [UnifiedCommentsEditViewModel.start] emits. */
    private fun initialUiState() = EditCommentUiState(
        canSaveChanges = false,
        shouldInitComment = false,
        shouldInitWatchers = false,
        showProgress = true,
        progressText = UiStringRes(R.string.loading),
        originalComment = CommentEssentials(),
        editedComment = CommentEssentials(),
        editErrorStrings = EditErrorStrings(),
        inputSettings = InputSettings(
            enableEditName = true,
            enableEditUrl = true,
            enableEditEmail = true,
            enableEditComment = true
        )
    )

    companion object {
        @JvmStatic
        fun createIntent(
            context: Context,
            commentIdentifier: CommentIdentifier,
            siteModel: SiteModel
        ): Intent =
            Intent(context, UnifiedCommentsEditActivity::class.java).apply {
                putExtra(KEY_COMMENT_IDENTIFIER, commentIdentifier)
                putExtra(WordPress.SITE, siteModel)
            }

        private const val KEY_COMMENT_IDENTIFIER = "key_comment_identifier"
    }
}
