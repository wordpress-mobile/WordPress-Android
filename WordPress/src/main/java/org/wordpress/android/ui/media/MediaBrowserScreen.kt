package org.wordpress.android.ui.media

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3

@Composable
fun ApplicationPasswordOffReauthenticateDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null
            )
        },
        title = { Text(text = stringResource(R.string.application_password_invalid)) },
        text = { Text(text = stringResource(R.string.application_password_invalid_description)) },
        confirmButton = {
            Button(onClick = {
                android.util.Log.d("MediaBrowserScreen", "Button clicked - calling onConfirm")
                onConfirm()
            }) {
                Text(text = stringResource(R.string.log_in))
            }
        }
    )
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ApplicationPasswordOffReauthenticateDialogPreview() {
    AppThemeM3 {
        ApplicationPasswordOffReauthenticateDialog(
            onDismiss = {},
            onConfirm = {},
        )
    }
}

/**
 * Helper function to show ApplicationPasswordOffReauthenticateDialog from Java code
 */
@JvmOverloads
fun showApplicationPasswordOffReauthenticateDialog(
    composeView: androidx.compose.ui.platform.ComposeView,
    onDismiss: Runnable? = null,
    onConfirm: Runnable? = null
) {
    composeView.setContent {
        val showDialog = remember { mutableStateOf(true) }

        AppThemeM3 {
            if (showDialog.value) {
                ApplicationPasswordOffReauthenticateDialog(
                    onDismiss = {
                        android.util.Log.d("MediaBrowserScreen", "Helper - onDismiss called")
                        showDialog.value = false
                        onDismiss?.run()
                    },
                    onConfirm = {
                        android.util.Log.d("MediaBrowserScreen", "Helper - onConfirm called")
                        showDialog.value = false
                        onConfirm?.run()
                    }
                )
            }
        }
    }
}

