package org.wordpress.android.ui.accounts.login.applicationpassword

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
abstract class ApplicationPasswordDialogActivity : ComponentActivity() {
    @Inject
    lateinit var activityNavigator: ActivityNavigator

    private val viewModel: ApplicationPasswordDialogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the authentication URL from intent extras
        val authenticationUrl = intent.getStringExtra(EXTRA_SITE_URL) ?: ""

        // Observe navigation events
        lifecycleScope.launch {
            viewModel.navigationEvent.collect { event ->
                when (event) {
                    is ApplicationPasswordDialogViewModel.NavigationEvent.NavigateToLogin -> {
                        activityNavigator.openApplicationPasswordLogin(
                            this@ApplicationPasswordDialogActivity,
                            event.authenticationUrl
                        )
                        finish()
                    }
                    is ApplicationPasswordDialogViewModel.NavigationEvent.ShowError -> {
                        ToastUtils.showToast(
                            this@ApplicationPasswordDialogActivity,
                            getString(R.string.error_generic)
                        )
                        finish()
                    }
                }
            }
        }

        setContent {
            AppThemeM3 {
                val isLoading = viewModel.isLoading.collectAsState()
                ApplicationPasswordDialog(
                    title = stringResource(getTitleResource()),
                    description = getDescriptionString(),
                    buttonText = getButtonTextResource(),
                    isLoading = isLoading.value,
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

    protected abstract fun getTitleResource(): Int
    protected abstract fun getDescriptionString(): String
    protected abstract fun getButtonTextResource(): Int

    companion object {
        const val EXTRA_SITE_URL = "site_url_arg"
    }
}

@Composable
fun ApplicationPasswordDialog(
    title: String,
    description: String,
    buttonText: Int,
    isLoading: Boolean,
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
        title = { Text(text = title) },
        text = {
            Column(
                modifier = androidx.compose.ui.Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(text = description)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = androidx.compose.ui.Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = stringResource(buttonText))
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ApplicationPasswordDialogPreview() {
    AppThemeM3 {
        ApplicationPasswordDialog(
            title = "Application Password Required",
            description = "To use this feature, you need to create an application password. " +
                    "This is a secure way to authenticate without using your main password.",
            buttonText = R.string.get_started,
            isLoading = false,
            onDismiss = {},
            onConfirm = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ApplicationPasswordDialogLoadingPreview() {
    AppThemeM3 {
        ApplicationPasswordDialog(
            title = "Application Password Required",
            description = "To use this feature, you need to create an application password. " +
                    "This is a secure way to authenticate without using your main password.",
            buttonText = R.string.get_started,
            isLoading = true,
            onDismiss = {},
            onConfirm = {}
        )
    }
}
