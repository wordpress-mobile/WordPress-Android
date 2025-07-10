package org.wordpress.android.ui.media

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
            Button(onClick = onConfirm) {
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

