package org.wordpress.android.ui.accounts.login.applicationpassword

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityNavigator
import org.wordpress.android.ui.compose.theme.AppThemeM3
import javax.inject.Inject

@AndroidEntryPoint
class ApplicationPasswordReauthenticateActivity : ComponentActivity() {
    @Inject
    lateinit var activityNavigator: ActivityNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the authentication URL from intent extras
        val authenticationUrl = intent.getStringExtra(EXTRA_AUTHENTICATION_URL) ?: ""

        setContent {
            AppThemeM3 {
                ApplicationPasswordOffReauthenticateScreen(
                    onDismiss = { finish() },
                    onConfirm = {
                        if (authenticationUrl.isNotEmpty()) {
                            activityNavigator.openApplicationPasswordLogin(this, authenticationUrl)
                        }
                        finish()
                    }
                )
            }
        }
    }

    @Composable
    private fun ApplicationPasswordOffReauthenticateScreen(
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
    ) {
        val showDialog = remember { mutableStateOf(true) }

        if (showDialog.value) {
            ApplicationPasswordOffReauthenticateDialog(
                onDismiss = {
                    showDialog.value = false
                    onDismiss()
                },
                onConfirm = {
                    showDialog.value = false
                    onConfirm()
                }
            )
        }
    }

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

    companion object {
        const val EXTRA_AUTHENTICATION_URL = "authentication_url"
    }
}
