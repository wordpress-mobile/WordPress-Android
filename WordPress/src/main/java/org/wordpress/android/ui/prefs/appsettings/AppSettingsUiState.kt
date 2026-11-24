package org.wordpress.android.ui.prefs.appsettings

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import org.wordpress.android.R

data class AppSettingsUiState(
    val themeValue: String,
    val showOpenLinksInJetpack: Boolean,
    val isOpenLinksInJetpack: Boolean,
    val isImageOptimize: Boolean,
    val isStripImageLocation: Boolean,
    val isVideoOptimize: Boolean,
    val mediaValues: Map<MediaChoice, Int>,
    val whatsNewVersion: String? = null,
    val showDebugSettings: Boolean,
    val isSendUsageStats: Boolean,
    val isSendCrashReports: Boolean,
    val isPrivacySettingsShown: Boolean,
)

/**
 * The media rows that pick one value from a list. Entries are the labels shown to the user, values are the
 * integers stored in AppPrefs.
 */
enum class MediaChoice(
    @StringRes val titleRes: Int,
    @ArrayRes val entriesRes: Int,
    @ArrayRes val valuesRes: Int,
) {
    IMAGE_MAX_SIZE(
        R.string.site_settings_default_image_width_title,
        R.array.site_settings_image_max_size_entries,
        R.array.site_settings_image_max_size_values
    ),
    IMAGE_QUALITY(
        R.string.site_settings_default_image_quality_title,
        R.array.site_settings_image_quality_entries,
        R.array.site_settings_image_quality_values
    ),
    VIDEO_WIDTH(
        R.string.site_settings_default_video_width_title,
        R.array.site_settings_video_width_entries,
        R.array.site_settings_video_width_values
    ),
    VIDEO_QUALITY(
        R.string.site_settings_default_video_quality_title,
        R.array.site_settings_video_bitrate_entries,
        R.array.site_settings_video_bitrate_values
    ),
}

sealed interface AppSettingsEvent {
    data object OpenDeviceSettings : AppSettingsEvent
    data object ShowWhatsNew : AppSettingsEvent
    data class OpenUrl(val url: String) : AppSettingsEvent
    data class ShowToast(@StringRes val messageRes: Int) : AppSettingsEvent
    data class ApplyTheme(val themeValue: String) : AppSettingsEvent
}
