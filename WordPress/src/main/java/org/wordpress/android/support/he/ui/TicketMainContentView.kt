package org.wordpress.android.support.he.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import org.wordpress.android.R
import org.wordpress.android.support.he.model.AttachmentState
import org.wordpress.android.support.he.util.AttachmentActionsListener
import org.wordpress.android.ui.compose.theme.AppThemeM3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketMainContentView(
    messageText: String,
    includeAppLogs: Boolean,
    onMessageChanged: (String) -> Unit,
    onIncludeAppLogsChanged: (Boolean) -> Unit,
    enabled: Boolean = true,
    attachmentState: AttachmentState = AttachmentState(),
    attachmentActionsListener: AttachmentActionsListener
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        val messageLabel = stringResource(R.string.he_support_message_label)
        Text(
            text = messageLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .semantics { heading() }
        )

        OutlinedTextField(
            value = messageText,
            onValueChange = { message -> onMessageChanged(message) },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .semantics { contentDescription = messageLabel },
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            enabled = enabled,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.he_support_screenshots_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(bottom = 4.dp)
                .semantics { heading() }
        )

        Text(
            text = stringResource(R.string.he_support_screenshots_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (attachmentState.acceptedUris.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                attachmentState.acceptedUris.forEach { imageUri ->
                    ImagePreviewItem(
                        imageUri = imageUri,
                        onRemove = { attachmentActionsListener.onRemoveImage(imageUri) },
                        enabled = enabled
                    )
                }
            }
        }

        val addScreenshotsLabel = stringResource(R.string.he_support_add_screenshots_button)
        OutlinedButton(
            onClick = attachmentActionsListener::onAddImageClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .semantics { contentDescription = addScreenshotsLabel },
            shape = RoundedCornerShape(12.dp),
            enabled = enabled,
            border = BorderStroke(
                width = 1.dp,
                color = if (enabled) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                }
            )
        ) {
            Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = addScreenshotsLabel,
                style = MaterialTheme.typography.labelLarge
            )
        }

        // Show rejected attachments with thumbnails
        if (attachmentState.rejectedUris.isNotEmpty() && attachmentState.rejectionReason != null) {
            Spacer(modifier = Modifier.height(16.dp))
            val reason = when (attachmentState.rejectionReason) {
                AttachmentState.RejectionReason.TotalSizeExceeded ->
                    stringResource(R.string.he_support_total_size_exceeded)
                AttachmentState.RejectionReason.FileTooLarge ->
                    stringResource(R.string.he_support_file_too_large)
            }
            RejectedAttachmentsSection(
                skippedUris = attachmentState.rejectedUris,
                reason = reason
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.he_support_app_logs_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .semantics { heading() }
        )

        val includeLogsLabel = stringResource(R.string.he_support_include_logs_title)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = includeLogsLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Text(
                        text = stringResource(R.string.he_support_include_logs_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.size(16.dp))

                Switch(
                    checked = includeAppLogs,
                    onCheckedChange = { checked -> onIncludeAppLogsChanged(checked) },
                    enabled = enabled,
                    modifier = Modifier.semantics {
                        contentDescription = includeLogsLabel
                    }
                )
            }
        }
    }
}

@Composable
private fun ImagePreviewItem(
    imageUri: Uri,
    onRemove: () -> Unit,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .size(100.dp)
    ) {
        Card(
            modifier = Modifier.size(100.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            AsyncImage(
                model = imageUri,
                contentDescription = stringResource(R.string.he_support_screenshot_preview),
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }

        if (enabled) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.he_support_remove_screenshot),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RejectedAttachmentsSection(
    skippedUris: List<Uri>,
    reason: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section header with error message
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.he_support_skipped_files_header),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Thumbnails of rejected files
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            skippedUris.forEach { uri ->
                RejectedImagePreviewItem(imageUri = uri)
            }
        }
    }
}

@Composable
private fun RejectedImagePreviewItem(
    imageUri: Uri
) {
    Box(
        modifier = Modifier.size(100.dp)
    ) {
        Card(
            modifier = Modifier.size(100.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Box {
                AsyncImage(
                    model = imageUri,
                    contentDescription = stringResource(R.string.he_support_screenshot_preview),
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                // Semi-transparent red overlay to indicate rejection
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                )
            }
        }

        // Error icon in the center
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .size(32.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.error,
            shadowElevation = 4.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


@Preview(showBackground = true, name = "HE main ticket content")
@Suppress("EmptyFunctionBlock")
@Composable
private fun TicketMainContentViewPreview() {
    AppThemeM3(isDarkTheme = false) {
        TicketMainContentView(
            messageText = "",
            includeAppLogs = false,
            onMessageChanged = { },
            onIncludeAppLogsChanged = { },
            attachmentActionsListener = object : AttachmentActionsListener {
                override fun onAddImageClick() { }
                override fun onRemoveImage(uri: Uri) { }
            }
        )
    }
}

@Preview(showBackground = true, name = "HE main ticket content - Dark", uiMode = UI_MODE_NIGHT_YES)
@Suppress("EmptyFunctionBlock")
@Composable
private fun TicketMainContentViewPreviewDark() {
    AppThemeM3(isDarkTheme = true) {
        TicketMainContentView(
            messageText = "",
            includeAppLogs = false,
            onMessageChanged = { },
            onIncludeAppLogsChanged = { },
            attachmentActionsListener = object : AttachmentActionsListener {
                override fun onAddImageClick() { }
                override fun onRemoveImage(uri: Uri) { }
            }
        )
    }
}
