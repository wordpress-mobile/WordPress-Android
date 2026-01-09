package org.wordpress.android.ui.accounts.login.applicationpassword

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityNavigator
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

@AndroidEntryPoint
class ApplicationPasswordRequiredDialogActivity : ComponentActivity() {
    @Inject
    lateinit var activityNavigator: ActivityNavigator

    private val viewModel: ApplicationPasswordAutoAuthDialogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val site: SiteModel? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_SITE, SiteModel::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_SITE)
        }

        if (site == null) {
            finish()
            return
        }

        val featureName = intent.getStringExtra(EXTRA_FEATURE_NAME)

        lifecycleScope.launch {
            viewModel.navigationEvent.collect { event ->
                when (event) {
                    is ApplicationPasswordAutoAuthDialogViewModel.NavigationEvent.Success -> {
                        setResult(RESULT_OK)
                        finish()
                    }
                    is ApplicationPasswordAutoAuthDialogViewModel.NavigationEvent.FallbackToManualLogin -> {
                        activityNavigator.openApplicationPasswordLogin(
                            this@ApplicationPasswordRequiredDialogActivity,
                            event.authUrl
                        )
                        finish()
                    }
                    is ApplicationPasswordAutoAuthDialogViewModel.NavigationEvent.Error -> {
                        ToastUtils.showToast(
                            this@ApplicationPasswordRequiredDialogActivity,
                            R.string.error_generic
                        )
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                }
            }
        }

        setContent {
            AppThemeM3 {
                val isLoading = viewModel.isLoading.collectAsState()
                ApplicationPasswordRequiredDialog(
                    featureName = featureName,
                    isLoading = isLoading.value,
                    onDismiss = { finish() },
                    onConfirm = { viewModel.createApplicationPassword(site) }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_SITE = "extra_site"
        const val EXTRA_FEATURE_NAME = "feature_name_arg"

        fun createIntent(context: Context, site: SiteModel, featureName: String): Intent {
            return Intent(context, ApplicationPasswordRequiredDialogActivity::class.java).apply {
                putExtra(EXTRA_SITE, site)
                putExtra(EXTRA_FEATURE_NAME, featureName)
            }
        }
    }
}

@Composable
fun ApplicationPasswordRequiredDialog(
    featureName: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val description = if (featureName != null) {
        stringResource(R.string.application_password_required_description, featureName) +
            stringResource(R.string.application_password_experimental_feature_note)
    } else {
        stringResource(R.string.application_password_info_description_1) +
            stringResource(R.string.application_password_experimental_feature_note)
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null
            )
        },
        title = { Text(text = stringResource(R.string.application_password_required)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
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
                onClick = onConfirm,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = stringResource(R.string.create))
                }
            }
        }
    )
}
