package org.wordpress.android.ui.main.feedbackform

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MediaUriPager
import org.wordpress.android.ui.compose.components.ProgressDialog
import org.wordpress.android.ui.compose.components.ProgressDialogState
import org.wordpress.android.ui.compose.theme.AppThemeM3

@Composable
fun FeedbackFormScreen(
    messageText: State<String>?,
    attachments: State<List<FeedbackFormAttachment>>,
    progressDialogState: State<ProgressDialogState?>?,
    onMessageChanged: (String) -> Unit,
    onSubmitClick: (context: Context) -> Unit,
    onCloseClick: (context: Context) -> Unit,
    onChooseMediaClick: () -> Unit,
    onRemoveMediaClick: (uri: Uri) -> Unit,
) {
    val context = LocalContext.current
    val message = messageText?.value ?: ""
    val content: @Composable () -> Unit = @Composable {
        MessageSection(
            messageText = messageText?.value,
            onMessageChanged = {
                onMessageChanged(it)
            },
        )
        MediaUriPager(
            mediaUris = attachments.value.map { it.uri },
            onButtonClick =  { uri -> onRemoveMediaClick(uri) },
            modifier = Modifier
                .padding(
                    vertical = V_PADDING.dp,
                    horizontal = H_PADDING.dp
                )
        )
        AttachmentButton(
            onChooseMediaClick = onChooseMediaClick
        )
        SubmitButton(
            isEnabled = message.isNotEmpty(),
            onClick = {
                onSubmitClick(context)
            }
        )
        progressDialogState?.value?.let {
            ProgressDialog(it)
        }
    }
    Screen(
        content = content,
        onCloseClick = { onCloseClick(context) }
    )
}

@Composable
private fun MessageSection(
    messageText: String?,
    onMessageChanged: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .padding(
                vertical = V_PADDING.dp,
                horizontal = H_PADDING.dp
            )
    ) {
        OutlinedTextField(
            value = messageText ?: "",
            placeholder = {
                Text(stringResource(id = R.string.feedback_form_message_hint))
            },
            onValueChange = {
                onMessageChanged(it.take(MAX_CHARS))
            },
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions.Default.copy(
                capitalization = KeyboardCapitalization.Sentences
            ),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 180.dp)
                .focusRequester(focusRequester),
        )
    }
}

@Composable
private fun SubmitButton(
    onClick: () -> Unit,
    isEnabled: Boolean,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(
                vertical = V_PADDING.dp,
                horizontal = H_PADDING.dp
            ),
    ) {
        Button(
            enabled = isEnabled,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.submit).uppercase(),
            )
        }
    }
}

@Composable
private fun AttachmentButton(
    onChooseMediaClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onChooseMediaClick()
            }
            .padding(
                vertical = V_PADDING.dp,
                horizontal = H_PADDING.dp
            ),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_attachment_link),
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = null // decorative element
        )
        Text(
            text = stringResource(R.string.feedback_form_add_attachments),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 10.dp
                ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen(
    content: @Composable () -> Unit,
    onCloseClick: () -> Unit
) {
    AppThemeM3 {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.feedback_form_title)) },
                    navigationIcon = {
                        IconButton(onClick = onCloseClick) {
                            Icon(Icons.Filled.Close, stringResource(R.string.close))
                        }
                    },
                )
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                content()
            }
        }
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true
)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun FeedbackFormScreenPreview() {
    val attachment1 = FeedbackFormAttachment(
        uri = Uri.parse("https://via.placeholder.com/150"),
        attachmentType = FeedbackFormAttachmentType.IMAGE,
        size = 123456789,
        mimeType = "image/jpeg",
    )
    val attachment2 = FeedbackFormAttachment(
        uri = Uri.parse("https://via.placeholder.com/150"),
        attachmentType = FeedbackFormAttachmentType.VIDEO,
        size = 123456789,
        mimeType = "video/mp4",
    )
    val attachments = MutableStateFlow(listOf(attachment1, attachment2))
    val messageText = MutableStateFlow("I love this app!")
    val progressDialogState = MutableStateFlow<ProgressDialogState?>(
        ProgressDialogState(
            message = R.string.uploading,
            showCancel = false,
            progress = 50f / 100f,
            dismissible = false,
        )
    )

    FeedbackFormScreen(
        messageText = messageText.collectAsState(),
        progressDialogState = progressDialogState.collectAsState(),
        attachments = attachments.collectAsState(),
        onMessageChanged = {},
        onSubmitClick = {},
        onCloseClick = {},
        onChooseMediaClick = {},
        onRemoveMediaClick = {}
    )
}

private const val H_PADDING = 18
private const val V_PADDING = 12
private const val MAX_CHARS = 500 // matches iOS limit
