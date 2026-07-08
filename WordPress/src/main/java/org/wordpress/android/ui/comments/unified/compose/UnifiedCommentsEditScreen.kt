package org.wordpress.android.ui.comments.unified.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.comments.unified.CommentEssentials
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.EditCommentUiState
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.EditErrorStrings
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.FieldType
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.InputSettings
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.utils.uiStringText

/**
 * The unified comment edit screen: a four-field form (author name, web address, email address,
 * comment text) with per-keystroke validation and a Done action enabled only when the comment
 * has valid, unsaved changes. Replaces the XML unified_comments_edit_fragment layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
fun UnifiedCommentsEditScreen(
    uiState: EditCommentUiState,
    showDiscardDialog: Boolean,
    snackbarHostState: SnackbarHostState,
    onFieldChanged: (String, FieldType) -> Unit,
    onDoneClick: () -> Unit,
    onBackClick: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onDismissDiscard: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_comment)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cross_white_24dp),
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onDoneClick, enabled = uiState.canSaveChanges) {
                        Text(stringResource(R.string.comment_edit_menu_done))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            if (uiState.showProgress) {
                EditCommentLoading(
                    progressText = uiState.progressText?.let { uiStringText(it) },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                EditCommentFields(
                    uiState = uiState,
                    onFieldChanged = onFieldChanged,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // The legacy screen force-hid the keyboard whenever the progress state took over
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(uiState.showProgress) {
        if (uiState.showProgress) {
            keyboardController?.hide()
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = onDismissDiscard,
            title = { Text(stringResource(R.string.comment_edit_cancel_dialog_title)) },
            text = { Text(stringResource(R.string.comment_edit_cancel_dialog_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmDiscard) { Text(stringResource(R.string.button_discard)) }
            },
            dismissButton = {
                TextButton(onClick = onDismissDiscard) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun EditCommentLoading(progressText: String?, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        CircularProgressIndicator()
        if (!progressText.isNullOrEmpty()) {
            Text(text = progressText, modifier = Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
private fun EditCommentFields(
    uiState: EditCommentUiState,
    onFieldChanged: (String, FieldType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        EditCommentField(
            value = uiState.editedComment.userName,
            labelRes = R.string.comment_edit_user_name,
            error = uiState.editErrorStrings.userNameError,
            enabled = uiState.inputSettings.enableEditName,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            singleLine = true,
            onValueChange = { onFieldChanged(it, FieldType.USER_NAME) }
        )
        EditCommentField(
            value = uiState.editedComment.userUrl,
            labelRes = R.string.comment_edit_web_address,
            error = uiState.editErrorStrings.userUrlError,
            enabled = uiState.inputSettings.enableEditUrl,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
            onValueChange = { onFieldChanged(it, FieldType.WEB_ADDRESS) }
        )
        EditCommentField(
            value = uiState.editedComment.userEmail,
            labelRes = R.string.comment_edit_email_address,
            error = uiState.editErrorStrings.userEmailError,
            enabled = uiState.inputSettings.enableEditEmail,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            onValueChange = { onFieldChanged(it, FieldType.USER_EMAIL) }
        )
        EditCommentField(
            value = uiState.editedComment.commentText,
            labelRes = R.string.comment_edit_comment,
            error = uiState.editErrorStrings.commentTextError,
            enabled = uiState.inputSettings.enableEditComment,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrectEnabled = true,
                keyboardType = KeyboardType.Text
            ),
            singleLine = false,
            onValueChange = { onFieldChanged(it, FieldType.COMMENT) }
        )
    }
}

@Composable
@Suppress("LongParameterList")
private fun EditCommentField(
    value: String,
    labelRes: Int,
    error: String?,
    enabled: Boolean,
    keyboardOptions: KeyboardOptions,
    singleLine: Boolean,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        enabled = enabled,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else COMMENT_FIELD_MIN_LINES,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    )
}

private const val COMMENT_FIELD_MIN_LINES = 5

@Preview(showBackground = true)
@Composable
private fun UnifiedCommentsEditScreenPreview() {
    AppThemeM3 {
        UnifiedCommentsEditScreen(
            uiState = EditCommentUiState(
                canSaveChanges = true,
                shouldInitComment = false,
                shouldInitWatchers = false,
                showProgress = false,
                originalComment = CommentEssentials(),
                editedComment = CommentEssentials(
                    userName = "Jane Doe",
                    commentText = "This is a great post!",
                    userUrl = "https://example.com",
                    userEmail = "jane@example.com"
                ),
                editErrorStrings = EditErrorStrings(),
                inputSettings = InputSettings(
                    enableEditName = true,
                    enableEditUrl = true,
                    enableEditEmail = true,
                    enableEditComment = true
                )
            ),
            showDiscardDialog = false,
            snackbarHostState = SnackbarHostState(),
            onFieldChanged = { _, _ -> },
            onDoneClick = {},
            onBackClick = {},
            onConfirmDiscard = {},
            onDismissDiscard = {}
        )
    }
}
