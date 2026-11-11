package org.wordpress.android.ui.prefs.experimentalfeatures

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.compose.components.FeedbackDialog
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.extensions.setContent

@AndroidEntryPoint
class ExperimentalFeaturesActivity : BaseAppCompatActivity() {
    private val viewModel: ExperimentalFeaturesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppThemeM3 {
                val features by viewModel.switchStates.collectAsStateWithLifecycle()
                val showNetworkDebuggingError by
                viewModel.showNetworkDebuggingError.collectAsStateWithLifecycle()
                val showDialog = rememberSaveable { mutableStateOf(false) }

                if (showNetworkDebuggingError) {
                    NetworkDebuggingErrorDialog(
                        onDismiss = { viewModel.dismissNetworkDebuggingError() }
                    )
                }

                if (showDialog.value) {
                    FeedbackDialog(
                        message = stringResource(R.string.experimental_features_feedback_dialog_message),
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
