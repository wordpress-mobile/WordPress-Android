package org.wordpress.android.ui.prefs

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.extensions.setContent

enum class ExperimentalFeature(val prefKey: String, val labelResId: Int) {
    EXPERIMENTAL_BLOCK_EDITOR("experimental_block_editor", R.string.experimental_block_editor),
    EXPERIMENTAL_BLOCK_EDITOR_THEME_STYLES(
        "experimental_block_editor_theme_styles",
        R.string.experimental_block_editor_theme_styles
    );

    fun isEnabled() : Boolean {
        return AppPrefs.getExperimentalFeatureConfig(prefKey)
    }

    fun setEnabled(isEnabled: Boolean) {
        AppPrefs.setExperimentalFeatureConfig(isEnabled, prefKey)
    }
}

class FeatureViewModel : ViewModel() {
    private val _switchStates = MutableStateFlow<Map<ExperimentalFeature, Boolean>>(emptyMap())
    val switchStates: StateFlow<Map<ExperimentalFeature, Boolean>> = _switchStates.asStateFlow()

    init {
        val initialStates = ExperimentalFeature.entries.associate { feature ->
            feature to feature.isEnabled()
        }
        _switchStates.value = initialStates
    }

    fun onFeatureToggled(feature: ExperimentalFeature, enabled: Boolean) {
        _switchStates.update { currentStates ->
            currentStates.toMutableMap().apply {
                this[feature] = enabled
                feature.setEnabled(enabled)
            }
        }
    }
}

@AndroidEntryPoint
class ExperimentalFeaturesActivity : BaseAppCompatActivity() {
    private val viewModel: FeatureViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppThemeM3 {
                val features by viewModel.switchStates.collectAsStateWithLifecycle()
                val showDialog = remember { mutableStateOf(false) }

                if (showDialog.value) {
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
                        if (feature == ExperimentalFeature.EXPERIMENTAL_BLOCK_EDITOR && !enabled) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentalFeaturesScreen(
    features: Map<ExperimentalFeature, Boolean>,
    onFeatureToggled: (feature: ExperimentalFeature, enabled: Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.experimental_features_screen_title))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.back)
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            Column {
                features.forEach { (feature, enabled) ->
                    FeatureToggle(
                        feature = feature,
                        enabled = enabled,
                        onChange = onFeatureToggled,
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureToggle(
    feature: ExperimentalFeature,
    enabled: Boolean,
    onChange: (ExperimentalFeature, Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onChange(feature, !enabled) }
            .padding(horizontal = Margin.ExtraLarge.value, vertical = Margin.MediumLarge.value)
    ) {
        Text(
            text = stringResource(feature.labelResId),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Margin.Medium.value)
        )
        Switch(
            checked = enabled,
            onCheckedChange = { newValue ->
                onChange(feature, newValue)
            },
        )
    }
}

@Composable
fun FeedbackDialog(onDismiss: () -> Unit, onSendFeedback: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.experimental_features_feedback_dialog_title)) },
        text = { Text(text = stringResource(R.string.experimental_features_feedback_dialog_message)) },
        confirmButton = {
            Button(onClick = onSendFeedback) {
                Text(text = stringResource(R.string.send_feedback))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.experimental_features_feedback_dialog_decline))
            }
        }
    )
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ExperimentalFeaturesScreenPreview() {
    AppThemeM3 {
        val featuresStatusAlternated = remember {
            ExperimentalFeature.entries.toTypedArray().mapIndexed { index, feature ->
                feature to (index % 2 == 0)
            }.toMap()
        }

        ExperimentalFeaturesScreen(
            features = featuresStatusAlternated,
            onFeatureToggled = { _, _ -> },
            onNavigateBack = {}
        )
    }
}
