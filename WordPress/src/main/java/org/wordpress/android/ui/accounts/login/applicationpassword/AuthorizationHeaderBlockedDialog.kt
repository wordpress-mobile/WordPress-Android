package org.wordpress.android.ui.accounts.login.applicationpassword

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.wordpress.android.R

/**
 * Explains that the site's server does not pass the Authorization header to WordPress. Shown as a
 * dialog rather than a toast so the user has time to note what to ask their hosting provider.
 */
@Composable
fun AuthorizationHeaderBlockedDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text = stringResource(R.string.application_password_authorization_header_blocked)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.ok))
            }
        },
    )
}
