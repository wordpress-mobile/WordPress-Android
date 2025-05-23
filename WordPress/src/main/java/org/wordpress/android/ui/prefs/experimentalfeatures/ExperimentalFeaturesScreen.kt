package org.wordpress.android.ui.prefs.experimentalfeatures

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentalFeaturesScreen(
    features: Map<Feature, Boolean>,
    onFeatureToggled: (feature: Feature, enabled: Boolean) -> Unit,
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

                Column(
                    modifier = Modifier.padding(
                        start = Margin.ExtraLarge.value,
                        end = Margin.ExtraLarge.value,
                        top = Margin.Large.value,
                        bottom = Margin.Large.value
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Margin.ExtraLarge.value)
                    )
                    Text(
                        text = stringResource(R.string.experimental_block_editor_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Margin.Small.value)
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureToggle(
    feature: Feature,
    enabled: Boolean,
    onChange: (Feature, Boolean) -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = stringResource(feature.labelResId),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        supportingContent = {
            Text(
                text = stringResource(feature.descriptionResId),
                style = MaterialTheme.typography.bodySmall,
            )
        },
        trailingContent = {
            Switch(
                checked = enabled,
                onCheckedChange = { newValue ->
                    onChange(feature, newValue)
                },
            )
        },
        modifier = Modifier.clickable { onChange(feature, !enabled) }
    )
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
            ExperimentalFeatures.Feature.entries.toTypedArray().mapIndexed { index, feature ->
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
