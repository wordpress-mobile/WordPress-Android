package org.wordpress.android.ui.prefs.experimentalfeatures

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.extensions.setContent
import org.wordpress.android.support.SupportWebViewActivity
import org.wordpress.android.ui.accounts.HelpActivity

@AndroidEntryPoint
class ExperimentalFeaturesActivity : BaseAppCompatActivity() {
    private val viewModel: ExperimentalFeaturesViewModel by viewModels()

    @Suppress("LongMethod")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppThemeM3 {
                val features by viewModel.switchStates.collectAsStateWithLifecycle()
                val applicationPasswordDialogState by
                viewModel.applicationPasswordDialogState.collectAsStateWithLifecycle()
                val showNetworkDebuggingError by
                viewModel.showNetworkDebuggingError.collectAsStateWithLifecycle()
                val showDialog = remember { mutableStateOf(false) }

                if (showNetworkDebuggingError) {
                    NetworkDebuggingErrorDialog(
                        onDismiss = { viewModel.dismissNetworkDebuggingError() }
                    )
                }

                when (applicationPasswordDialogState) {
                    is ExperimentalFeaturesViewModel.ApplicationPasswordDialogState.Disable -> {
                        ApplicationPasswordOffConfirmationDialog(
                            affectedSites = (applicationPasswordDialogState as
                                    ExperimentalFeaturesViewModel.ApplicationPasswordDialogState.Disable).affectedSites,
                            onDismiss = { viewModel.dismissDisableApplicationPassword() },
                            onConfirm = { viewModel.confirmDisableApplicationPassword() },
                            onContactSupport = {
                                val intent = SupportWebViewActivity.createIntent(
                                    context = this,
                                    origin = HelpActivity.Origin.UNKNOWN,
                                    selectedSite = null,
                                    extraSupportTags = null
                                )
                                this.startActivity(intent)
                            }
                        )
                    }
                    ExperimentalFeaturesViewModel.ApplicationPasswordDialogState.Info -> {
                        ApplicationPasswordInfoDialog(
                            onDismiss = { viewModel.dismissApplicationPasswordInfo() },
                            onConfirm = { viewModel.confirmApplicationPasswordInfo() }
                        )
                    }
                    ExperimentalFeaturesViewModel.ApplicationPasswordDialogState.None -> {
                        // Stub
                    }
                }

                // Only show other dialogs if we are not showing the Application Password one
                if (applicationPasswordDialogState is ExperimentalFeaturesViewModel.ApplicationPasswordDialogState.None
                    && showDialog.value) {
                    FeedbackDialog(
                        onDismiss = { showDialog.value = false },
                        onSendFeedback = {
                            showDialog.value = false
                            ActivityLauncher.viewFeedbackForm(this, "Editor")
                        }
                    )
                }

                ExperimentalFeaturesScreen(
                    features = features,
                    onFeatureToggled = { feature, enabled ->
                        if (feature == ExperimentalFeatures.Feature.EXPERIMENTAL_BLOCK_EDITOR && !enabled) {
                            showDialog.value = true
                        }
                        viewModel.onFeatureToggled(feature, enabled)
                    },
                    onNavigateBack = onBackPressedDispatcher::onBackPressed
                )
            }
        }
    }
}
