package org.wordpress.android.support.he.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.support.he.util.AttachmentActionsListener

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HEConversationReplyBottomSheet(
    sheetState: androidx.compose.material3.SheetState,
    isSending: Boolean = false,
    messageSendResult: HESupportViewModel.MessageSendResult? = null,
    initialMessageText: String = "",
    initialIncludeAppLogs: Boolean = false,
    onDismiss: (currentMessage: String, currentIncludeAppLogs: Boolean) -> Unit,
    onSend: (String, Boolean) -> Unit,
    onMessageSentSuccessfully: () -> Unit,
    attachments: List<Uri> = emptyList(),
    attachmentActionsListener: AttachmentActionsListener
) {
    var messageText by remember { mutableStateOf(initialMessageText) }
    var includeAppLogs by remember { mutableStateOf(initialIncludeAppLogs) }
    val scrollState = rememberScrollState()

    // Close the sheet when sending completes successfully
    LaunchedEffect(messageSendResult) {
        when (messageSendResult) {
            is HESupportViewModel.MessageSendResult.Success -> {
                // Message sent successfully, close the sheet and clear draft
                onDismiss("", false)
                onMessageSentSuccessfully()
            }
            is HESupportViewModel.MessageSendResult.Failure -> {
                // Message failed to send, draft is saved onDismiss
                // The error will be shown via snackbar from the Activity
                onDismiss("", false)
            }
            null -> {
                // No result yet, do nothing
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { onDismiss(messageText, includeAppLogs) },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onDismiss(messageText, includeAppLogs) },
                    enabled = !isSending
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Text(
                    text = stringResource(R.string.he_support_reply_button),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() }
                )

                TextButton(
                    onClick = { onSend(messageText, includeAppLogs) },
                    enabled = messageText.isNotBlank() && !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.he_support_send_button),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            TicketMainContentView(
                messageText = messageText,
                includeAppLogs = includeAppLogs,
                onMessageChanged = { message -> messageText = message },
                onIncludeAppLogsChanged = { checked -> includeAppLogs = checked },
                enabled = !isSending,
                attachments = attachments,
                attachmentActionsListener = attachmentActionsListener
            )
        }
    }
}
