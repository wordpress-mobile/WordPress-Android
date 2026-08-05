package org.wordpress.android.ui.compose.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.wordpress.android.R

/**
 * Asks the user whether they'd like to share feedback after turning a feature off.
 *
 * @param message Feature-specific prompt, e.g. "Are you willing to share feedback on the new stats?"
 * @param onDismiss Called when the user declines or dismisses the dialog
 * @param onSendFeedback Called when the user opts to send feedback
 */
@Composable
fun FeedbackDialog(
    message: String,
    onDismiss: () -> Unit,
    onSendFeedback: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.experimental_features_feedback_dialog_title)) },
        text = { Text(text = message) },
        confirmButton = {
            Button(onClick = onSendFeedback) {
                Text(text = stringResource(R.string.send_feedback))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.experimental_features_feedback_dialog_decline))
            }
        }
    )
}
