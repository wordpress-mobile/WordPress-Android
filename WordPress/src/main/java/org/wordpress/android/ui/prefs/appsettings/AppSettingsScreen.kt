package org.wordpress.android.ui.prefs.appsettings

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.SingleChoiceAlertDialog
import org.wordpress.android.ui.compose.theme.AppThemeM3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    uiState: AppSettingsUiState,
    languageSummary: String,
    onNavigateBack: () -> Unit,
    onLanguageClick: () -> Unit,
    onThemeSelected: (String) -> Unit,
    onOpenLinksInJetpackChanged: (Boolean) -> Unit,
    onDeviceSettingsClick: () -> Unit,
    onPrivacySettingsClick: () -> Unit,
    onPrivacySettingsClosed: () -> Unit,
    onImageOptimizeChanged: (Boolean) -> Unit,
    onStripImageLocationChanged: (Boolean) -> Unit,
    onVideoOptimizeChanged: (Boolean) -> Unit,
    onMediaChoiceSelected: (MediaChoice, Int) -> Unit,
    onWhatsNewClick: () -> Unit,
    onExperimentalFeaturesClick: () -> Unit,
    onDebugSettingsClick: () -> Unit,
    onSendUsageStatsChanged: (Boolean) -> Unit,
    onSendCrashReportsChanged: (Boolean) -> Unit,
    onLearnMoreClick: (String) -> Unit,
) {
    val isPrivacy = uiState.isPrivacySettingsShown
    BackHandler(enabled = isPrivacy, onBack = onPrivacySettingsClosed)
    // Separate scroll states so returning from Privacy settings restores the main list's position
    val mainScrollState = rememberScrollState()
    val privacyScrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isPrivacy) R.string.preference_privacy_settings else R.string.me_btn_app_settings
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = if (isPrivacy) onPrivacySettingsClosed else onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(if (isPrivacy) privacyScrollState else mainScrollState)
                .padding(vertical = 8.dp)
        ) {
            if (isPrivacy) {
                PrivacySettingsContent(
                    isSendUsageStats = uiState.isSendUsageStats,
                    isSendCrashReports = uiState.isSendCrashReports,
                    onSendUsageStatsChanged = onSendUsageStatsChanged,
                    onSendCrashReportsChanged = onSendCrashReportsChanged,
                    onLearnMoreClick = onLearnMoreClick,
                )
            } else {
                var showThemeDialog by rememberSaveable { mutableStateOf(false) }
                var openMediaChoice by rememberSaveable { mutableStateOf<MediaChoice?>(null) }

                val themeEntries = stringArrayResource(R.array.app_theme_entries).toList()
                val themeValues = stringArrayResource(R.array.app_theme_entry_values).toList()

                SettingsCard(
                    rows = buildList<@Composable () -> Unit> {
                        add {
                            SettingsRow(
                                title = stringResource(R.string.interface_language),
                                summary = languageSummary,
                                onClick = onLanguageClick
                            )
                        }
                        add {
                            SettingsRow(
                                title = stringResource(R.string.app_theme),
                                summary = labelFor(uiState.themeValue, themeEntries, themeValues),
                                onClick = { showThemeDialog = true }
                            )
                        }
                        if (uiState.showOpenLinksInJetpack) {
                            add {
                                SettingsSwitchRow(
                                    title = stringResource(R.string.preference_open_links_in_jetpack),
                                    checked = uiState.isOpenLinksInJetpack,
                                    onCheckedChange = onOpenLinksInJetpackChanged
                                )
                            }
                        }
                        add {
                            SettingsRow(
                                title = stringResource(R.string.preference_open_device_settings),
                                onClick = onDeviceSettingsClick
                            )
                        }
                    }
                )

                SettingsCard(
                    rows = listOf<@Composable () -> Unit> {
                        SettingsRow(
                            title = stringResource(R.string.preference_privacy_settings),
                            onClick = onPrivacySettingsClick
                        )
                    }
                )

                SettingsCard(
                    title = stringResource(R.string.media),
                    rows = listOf<@Composable () -> Unit>(
                        {
                            SettingsSwitchRow(
                                title = stringResource(R.string.site_settings_optimize_images),
                                summary = stringResource(R.string.site_settings_optimize_images_summary),
                                checked = uiState.isImageOptimize,
                                onCheckedChange = onImageOptimizeChanged
                            )
                        },
                        {
                            MediaChoiceRow(MediaChoice.IMAGE_MAX_SIZE, uiState) { openMediaChoice = it }
                        },
                        {
                            MediaChoiceRow(MediaChoice.IMAGE_QUALITY, uiState) { openMediaChoice = it }
                        },
                        {
                            SettingsSwitchRow(
                                title = stringResource(R.string.preference_strip_image_location),
                                checked = uiState.isStripImageLocation,
                                onCheckedChange = onStripImageLocationChanged
                            )
                        },
                        {
                            SettingsSwitchRow(
                                title = stringResource(R.string.site_settings_optimize_videos),
                                summary = stringResource(R.string.site_settings_optimize_video_summary),
                                checked = uiState.isVideoOptimize,
                                onCheckedChange = onVideoOptimizeChanged
                            )
                        },
                        {
                            MediaChoiceRow(MediaChoice.VIDEO_WIDTH, uiState) { openMediaChoice = it }
                        },
                        {
                            MediaChoiceRow(MediaChoice.VIDEO_QUALITY, uiState) { openMediaChoice = it }
                        },
                    )
                )

                SettingsCard(
                    rows = buildList<@Composable () -> Unit> {
                        uiState.whatsNewVersion?.let { version ->
                            add {
                                SettingsRow(
                                    title = stringResource(R.string.whats_new_in_version_title),
                                    summary = stringResource(R.string.version_with_name_param, version),
                                    onClick = onWhatsNewClick
                                )
                            }
                        }
                        add {
                            SettingsRow(
                                title = stringResource(R.string.experimental_features_screen_title),
                                onClick = onExperimentalFeaturesClick
                            )
                        }
                        if (uiState.showDebugSettings) {
                            add {
                                SettingsRow(
                                    title = stringResource(R.string.preference_open_debug_settings),
                                    onClick = onDebugSettingsClick
                                )
                            }
                        }
                    }
                )

                if (showThemeDialog) {
                    ChoiceDialog(
                        title = stringResource(R.string.app_theme),
                        entries = themeEntries,
                        values = themeValues,
                        currentValue = uiState.themeValue,
                        onSelected = onThemeSelected,
                        onDismiss = { showThemeDialog = false }
                    )
                }

                openMediaChoice?.let { choice ->
                    ChoiceDialog(
                        title = stringResource(choice.titleRes),
                        entries = stringArrayResource(choice.entriesRes).toList(),
                        values = stringArrayResource(choice.valuesRes).toList(),
                        currentValue = uiState.mediaValues[choice].toString(),
                        onSelected = { onMediaChoiceSelected(choice, it.toInt()) },
                        onDismiss = { openMediaChoice = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaChoiceRow(
    choice: MediaChoice,
    uiState: AppSettingsUiState,
    onClick: (MediaChoice) -> Unit,
) {
    // Each size and quality row is only enabled while its optimize switch is on
    val enabled = when (choice) {
        MediaChoice.IMAGE_MAX_SIZE, MediaChoice.IMAGE_QUALITY -> uiState.isImageOptimize
        MediaChoice.VIDEO_WIDTH, MediaChoice.VIDEO_QUALITY -> uiState.isVideoOptimize
    }
    SettingsRow(
        title = stringResource(choice.titleRes),
        summary = labelFor(
            uiState.mediaValues[choice].toString(),
            stringArrayResource(choice.entriesRes).toList(),
            stringArrayResource(choice.valuesRes).toList()
        ),
        enabled = enabled,
        onClick = { onClick(choice) }
    )
}

@Composable
private fun ChoiceDialog(
    title: String,
    entries: List<String>,
    values: List<String>,
    currentValue: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(values.indexOf(currentValue).coerceAtLeast(0)) }
    SingleChoiceAlertDialog(
        title = title,
        options = entries,
        selectedIndex = selectedIndex,
        onOptionSelected = { selectedIndex = it },
        onConfirm = {
            onDismiss()
            onSelected(values[selectedIndex])
        },
        onDismiss = onDismiss,
        confirmButtonText = stringResource(R.string.ok)
    )
}

/**
 * Returns the label for [value], falling back to the first entry for unknown values as the old settings screen did
 */
private fun labelFor(value: String, entries: List<String>, values: List<String>): String =
    entries.getOrNull(values.indexOf(value)) ?: entries.first()

@Composable
private fun PrivacySettingsContent(
    isSendUsageStats: Boolean,
    isSendCrashReports: Boolean,
    onSendUsageStatsChanged: (Boolean) -> Unit,
    onSendCrashReportsChanged: (Boolean) -> Unit,
    onLearnMoreClick: (String) -> Unit,
) {
    SettingsCard(
        rows = listOf<@Composable () -> Unit>(
            {
                SettingsSwitchRow(
                    title = stringResource(R.string.preference_collect_information),
                    checked = isSendUsageStats,
                    onCheckedChange = onSendUsageStatsChanged
                )
            },
            {
                SettingsLearnMoreRow(
                    caption = stringResource(R.string.cookie_policy_learn_more_caption),
                    buttonText = stringResource(R.string.learn_more),
                    onClick = { onLearnMoreClick(COOKIE_POLICY_URL) }
                )
            },
            {
                SettingsLearnMoreRow(
                    caption = stringResource(R.string.privacy_policy_learn_more_caption),
                    buttonText = stringResource(R.string.privacy_policy_read),
                    onClick = { onLearnMoreClick(PRIVACY_POLICY_URL) }
                )
            },
            {
                SettingsLearnMoreRow(
                    caption = stringResource(R.string.ccpa_privacy_notice_caption),
                    buttonText = stringResource(R.string.ccpa_privacy_notice_read),
                    onClick = { onLearnMoreClick(CCPA_NOTICE_URL) }
                )
            },
            {
                SettingsLearnMoreRow(
                    caption = stringResource(R.string.third_party_policy_learn_more_caption),
                    buttonText = stringResource(R.string.learn_more),
                    onClick = { onLearnMoreClick(COOKIE_POLICY_URL) }
                )
            },
        )
    )

    SettingsCard(
        rows = listOf<@Composable () -> Unit> {
            SettingsSwitchRow(
                title = stringResource(R.string.preference_crash_reports),
                summary = stringResource(R.string.preference_crash_reports_summary),
                checked = isSendCrashReports,
                onCheckedChange = onSendCrashReportsChanged
            )
        }
    )
}

private const val COOKIE_POLICY_URL = "https://www.automattic.com/cookies"
private const val PRIVACY_POLICY_URL = "https://www.automattic.com/privacy"
private const val CCPA_NOTICE_URL = "https://automattic.com/privacy/#california-consumer-privacy-act-ccpa"

private val previewState = AppSettingsUiState(
    themeValue = "default",
    showOpenLinksInJetpack = true,
    isOpenLinksInJetpack = false,
    isImageOptimize = true,
    isStripImageLocation = false,
    isVideoOptimize = false,
    mediaValues = mapOf(
        MediaChoice.IMAGE_MAX_SIZE to 2000,
        MediaChoice.IMAGE_QUALITY to 85,
        MediaChoice.VIDEO_WIDTH to 1280,
        MediaChoice.VIDEO_QUALITY to 0,
    ),
    whatsNewVersion = "27.2",
    showDebugSettings = true,
    isSendUsageStats = true,
    isSendCrashReports = true,
    isPrivacySettingsShown = false,
)

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AppSettingsScreenPreview() {
    AppThemeM3 {
        AppSettingsScreen(
            uiState = previewState,
            languageSummary = "English (United States)",
            onNavigateBack = {},
            onLanguageClick = {},
            onThemeSelected = {},
            onOpenLinksInJetpackChanged = {},
            onDeviceSettingsClick = {},
            onPrivacySettingsClick = {},
            onPrivacySettingsClosed = {},
            onImageOptimizeChanged = {},
            onStripImageLocationChanged = {},
            onVideoOptimizeChanged = {},
            onMediaChoiceSelected = { _, _ -> },
            onWhatsNewClick = {},
            onExperimentalFeaturesClick = {},
            onDebugSettingsClick = {},
            onSendUsageStatsChanged = {},
            onSendCrashReportsChanged = {},
            onLearnMoreClick = {},
        )
    }
}
