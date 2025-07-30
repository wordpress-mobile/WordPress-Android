package org.wordpress.android.ui.prefs.experimentalfeatures

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun ApplicationPasswordOffConfirmationDialog(
    affectedSites: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onContactSupport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Margin.ExtraLarge.value)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.application_password_disable_feature_title),
                textAlign = TextAlign.Center
            )
                },
        text = {
            Column {
                Text(
                    text = stringResource(
                        R.string.application_password_disable_feature_description,
                        affectedSites
                    )
                )

                val annotatedString = buildAnnotatedString {
                    val supportText = stringResource(R.string.contact_support)
                    val tag = "contact_support_link"
                    pushStringAnnotation(tag = tag, annotation = "contact_support")
                    withStyle(SpanStyle(
                        color = LocalContentColor.current,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Medium
                    )) {
                        append(supportText)
                    }
                    pop()
                }
                Text(
                    text = annotatedString,
                    modifier = Modifier
                        .padding(top = Margin.Medium.value)
                        .clickable {
                            // Only one annotation, so always call
                            onContactSupport()
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Start
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = stringResource(R.string.disable))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ApplicationPasswordInfoDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var showMore by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {},
        icon = {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Margin.ExtraLarge.value)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.application_password_info_title),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = Margin.Small.value)
            ) {
                Text(
                    text = stringResource(R.string.application_password_info_description_1),
                )

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
                    Text(
                        text = stringResource(R.string.application_password_info_description_2),
                    )
                    Spacer(modifier = Modifier.height(Margin.Medium.value))
                    Text(
                        text = stringResource(R.string.application_password_info_description_3),
                    )
                    Spacer(modifier = Modifier.height(Margin.Medium.value))
                    Text(
                        text = stringResource(R.string.application_password_info_description_4),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = stringResource(R.string.enable))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
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
