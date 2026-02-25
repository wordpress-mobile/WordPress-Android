package org.wordpress.android.ui.newstats.devices

import androidx.annotation.StringRes
import org.wordpress.android.R

/**
 * UI State for the Devices stats card.
 */
sealed class DevicesCardUiState {
    data object Loading : DevicesCardUiState()

    data class Loaded(
        val items: List<DeviceItem>,
        val maxValueForBar: Double
    ) : DevicesCardUiState()

    data class Error(
        @StringRes val messageResId: Int,
        val isAuthError: Boolean = false
    ) : DevicesCardUiState()
}

/**
 * Represents the type of device data being displayed.
 */
enum class DeviceType(@StringRes val labelResId: Int) {
    SCREENSIZE(R.string.stats_devices_screensize),
    BROWSER(R.string.stats_devices_browser),
    PLATFORM(R.string.stats_devices_platform)
}

/**
 * A single device item for display.
 */
data class DeviceItem(
    val name: String,
    val value: Double
)
