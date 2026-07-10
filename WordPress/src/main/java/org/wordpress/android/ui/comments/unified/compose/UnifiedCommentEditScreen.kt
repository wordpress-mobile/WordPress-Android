package org.wordpress.android.ui.comments.unified.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.EditCommentUiState
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.FieldType
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.FieldType.COMMENT
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.FieldType.USER_EMAIL
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.FieldType.USER_NAME
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.FieldType.WEB_ADDRESS
import org.wordpress.android.ui.utils.UiString.UiStringRes

/**
 * Compose editor for a comment's author identity and body, replacing the legacy XML
 * unified_comments_edit_fragment. State and validation stay in UnifiedCommentsEditViewModel; this
 * screen is stateless and drives every field from [uiState].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedCommentEditScreen(
    uiState: EditCommentUiState,
    snackbarHostState: SnackbarHostState,
    showDiscardDialog: Boolean,
    onFieldChange: (String, FieldType) -> Unit,
    onSaveClick: () -> Unit,
    onNavigateBack: () -> Unit,
    onDiscardConfirm: () -> Unit,
    onDiscardDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_comment)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cross_white_24dp),
                            contentDescription = stringResource(R.string.close_dialog_button_desc)
                        )
                    }
                },
                actions = {
                    // Disable while saving/loading so a second tap can't fire a duplicate save.
                    TextButton(
                        onClick = onSaveClick,
                        enabled = uiState.canSaveChanges && !uiState.showProgress
                    ) {
                        Text(stringResource(R.string.comment_edit_menu_done))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            if (uiState.showProgress) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    (uiState.progressText as? UiStringRes)?.let {
                        Text(
                            text = stringResource(it.stringRes),
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            } else {
                EditFields(uiState, onFieldChange)
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = onDiscardDismiss,
            title = { Text(stringResource(R.string.comment_edit_cancel_dialog_title)) },
            text = { Text(stringResource(R.string.comment_edit_cancel_dialog_message)) },
            confirmButton = {
                TextButton(onClick = onDiscardConfirm) {
                    Text(stringResource(R.string.button_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = onDiscardDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun EditFields(
    uiState: EditCommentUiState,
    onFieldChange: (String, FieldType) -> Unit
) {
    val edited = uiState.editedComment
    val errors = uiState.editErrorStrings
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EditField(
            value = edited.userName,
            labelRes = R.string.comment_edit_user_name,
            error = errors.userNameError,
            keyboardType = KeyboardType.Text,
            onValueChange = { onFieldChange(it, USER_NAME) }
        )
        EditField(
            value = edited.userUrl,
            labelRes = R.string.comment_edit_web_address,
            error = errors.userUrlError,
            keyboardType = KeyboardType.Uri,
            onValueChange = { onFieldChange(it, WEB_ADDRESS) }
        )
        EditField(
            value = edited.userEmail,
            labelRes = R.string.comment_edit_email_address,
            error = errors.userEmailError,
            keyboardType = KeyboardType.Email,
            onValueChange = { onFieldChange(it, USER_EMAIL) }
        )
        EditField(
            value = edited.commentText,
            labelRes = R.string.comment_edit_comment,
            error = errors.commentTextError,
            keyboardType = KeyboardType.Text,
            singleLine = false,
            // The comment body auto-capitalizes sentences, matching the legacy field and the reply box.
            capitalization = KeyboardCapitalization.Sentences,
            onValueChange = { onFieldChange(it, COMMENT) }
        )
    }
}

@Composable
private fun EditField(
    value: String,
    labelRes: Int,
    error: String?,
    keyboardType: KeyboardType,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        isError = error != null,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else COMMENT_FIELD_MIN_LINES,
        label = { Text(stringResource(labelRes)) },
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization),
        modifier = Modifier.fillMaxWidth()
    )
}

private const val COMMENT_FIELD_MIN_LINES = 5
