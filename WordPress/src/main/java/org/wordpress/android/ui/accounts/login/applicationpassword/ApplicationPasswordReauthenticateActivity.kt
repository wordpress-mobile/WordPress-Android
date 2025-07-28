package org.wordpress.android.ui.accounts.login.applicationpassword

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.ui.ActivityNavigator
import org.wordpress.android.ui.compose.theme.AppThemeM3
import javax.inject.Inject

@AndroidEntryPoint
class ApplicationPasswordReauthenticateActivity : ComponentActivity() {
    @Inject
    lateinit var activityNavigator: ActivityNavigator

    private val viewModel: ApplicationPasswordReauthenticateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the authentication URL from intent extras
        val authenticationUrl = intent.getStringExtra(EXTRA_SITE_URL) ?: ""

        // Observe navigation events
        lifecycleScope.launch {
            viewModel.navigationEvent.collect { event ->
                when (event) {
                    is ApplicationPasswordReauthenticateViewModel.NavigationEvent.NavigateToLogin -> {
                        activityNavigator.openApplicationPasswordLogin(
                            this@ApplicationPasswordReauthenticateActivity,
                            event.authenticationUrl
                        )
                        finish()
                    }
                    is ApplicationPasswordReauthenticateViewModel.NavigationEvent.ShowError -> {
                        ToastUtils.showToast(
                            this@ApplicationPasswordReauthenticateActivity,
                            getString(R.string.error_generic)
                        )
                        finish()
                    }
                }
            }
        }

        setContent {
            AppThemeM3 {
                ApplicationPasswordReauthenticateDialog(
                    viewModel = viewModel,
                    onDismiss = {
                        finish()
                    },
                    onConfirm = {
                        viewModel.onDialogConfirmed(authenticationUrl)
                    }
                )
            }
        }
    }

    @Composable
    fun ApplicationPasswordReauthenticateDialog(
        viewModel: ApplicationPasswordReauthenticateViewModel,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit,
    ) {
        val isLoading = viewModel.isLoading.collectAsState()
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null
                )
            },
            title = { Text(text = stringResource(R.string.application_password_invalid)) },
            text = {
                Column(
                    modifier = androidx.compose.ui.Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(text = stringResource(R.string.application_password_invalid_description))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm()
                    },
                    enabled = !isLoading.value
                ) {
                    if (isLoading.value) {
                        CircularProgressIndicator(
                            modifier = androidx.compose.ui.Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = stringResource(R.string.log_in))
                    }
                }
            }
        )
    }

    @Preview
    @Preview(uiMode = UI_MODE_NIGHT_YES)
    @Composable
    fun ApplicationPasswordReauthenticateDialogPreview() {
        AppThemeM3 {
            ApplicationPasswordReauthenticateDialogPreviewContent()
        }
    }
    
    @Composable
    private fun ApplicationPasswordReauthenticateDialogPreviewContent() {
        val isLoading = remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = {},
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null
                )
            },
            title = { Text(text = stringResource(R.string.application_password_invalid)) },
            text = {
                Column(
                    modifier = androidx.compose.ui.Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(text = stringResource(R.string.application_password_invalid_description))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isLoading.value = !isLoading.value
                    },
                    enabled = !isLoading.value
                ) {
                    if (isLoading.value) {
                        CircularProgressIndicator(
                            modifier = androidx.compose.ui.Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = stringResource(R.string.log_in))
                    }
                }
            }
        )
    }

    companion object {
        const val EXTRA_SITE_URL = "site_url_arg"
    }
}

