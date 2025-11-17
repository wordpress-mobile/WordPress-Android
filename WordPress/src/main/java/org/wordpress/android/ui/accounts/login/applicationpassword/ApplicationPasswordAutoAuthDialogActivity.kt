package org.wordpress.android.ui.accounts.login.applicationpassword

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.unit.Margin

@AndroidEntryPoint
class ApplicationPasswordAutoAuthDialogActivity : ComponentActivity() {
    private val viewModel: ApplicationPasswordAutoAuthDialogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the site from intent extras
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

        // Observe navigation events
        lifecycleScope.launch {
            viewModel.navigationEvent.collect { event ->
                when (event) {
                    is ApplicationPasswordAutoAuthDialogViewModel.NavigationEvent.Success -> {
                        setResult(RESULT_SUCCESS)
                        finish()
                    }
                    is ApplicationPasswordAutoAuthDialogViewModel.NavigationEvent.Error -> {
                        setResult(RESULT_ERROR)
                        finish()
                    }
                }
            }
        }

        setContent {
            AppThemeM3 {
                val isLoading = viewModel.isLoading.collectAsState()
                ApplicationPasswordAutoAuthDialog(
                    isLoading = isLoading.value,
                    onDismiss = {
                        setResult(RESULT_DISMISSED)
                        finish()
                                },
                    onConfirm = { viewModel.createApplicationPassword(site) }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_SITE = "extra_site"
        const val RESULT_SUCCESS = -1
        const val RESULT_ERROR = -0
        const val RESULT_DISMISSED = 1

        fun createIntent(context: Context, site: SiteModel): Intent {
            return Intent(context, ApplicationPasswordAutoAuthDialogActivity::class.java).apply {
                putExtra(EXTRA_SITE, site)
            }
        }
    }
}

@Composable
fun ApplicationPasswordAutoAuthDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var showMore by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Margin.ExtraLarge.value)
            )
        },
        title = { Text(text = stringResource(R.string.application_password_info_title)) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = Margin.Small.value)
            ) {
                Text(text = stringResource(R.string.application_password_info_description_1))

                if (!showMore) {
                    Spacer(modifier = Modifier.height(Margin.Medium.value))
                    Text(
                        text = stringResource(R.string.learn_more),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable { showMore = true }
                            .padding(vertical = Margin.Small.value)
                    )
                } else {
                    Spacer(modifier = Modifier.height(Margin.Medium.value))
                    Text(text = stringResource(R.string.application_password_info_description_2))
                    Spacer(modifier = Modifier.height(Margin.Medium.value))
                    Text(text = stringResource(R.string.application_password_info_description_3))
                    Spacer(modifier = Modifier.height(Margin.Medium.value))
                    Text(text = stringResource(R.string.application_password_info_description_4))
                }
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
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}
