package org.wordpress.android.ui.jetpackoverlay

import org.wordpress.android.R
import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed class JetpackBrandingUiState {
    sealed interface Indeterminate
    sealed interface Pluralisable {
        val number: Long
        val oneRes: Int
        val otherRes: Int

        companion object {
            var RES_ARE_MOVING_IN = R.string.wp_jetpack_powered_phase_3_feature_are_moving_in
            var RES_IS_MOVING_IN = R.string.wp_jetpack_powered_phase_3_feature_is_moving_in
        }
    }

    object Soon : JetpackBrandingUiState(), Indeterminate {
        var RES_ARE_MOVING_SOON = R.string.wp_jetpack_powered_phase_3_feature_are_moving_soon
        var RES_IS_MOVING_SOON = R.string.wp_jetpack_powered_phase_3_feature_is_moving_soon
    }

    data class Weeks(override val number: Long) : JetpackBrandingUiState(), Pluralisable {
        override var oneRes = R.string.weeks_quantity_one
        override var otherRes = R.string.weeks_quantity_other
    }

    data class Days(override val number: Long) : JetpackBrandingUiState(), Pluralisable {
        override var oneRes = R.string.days_quantity_one
        override var otherRes = R.string.days_quantity_other
    }

    object Unknown : JetpackBrandingUiState(), Indeterminate
    data object Passed : JetpackBrandingUiState()

    companion object {
        var RES_JP_POWERED = R.string.wp_jetpack_powered

        fun between(startDate: LocalDate, endDate: LocalDate): JetpackBrandingUiState {
            val days = ChronoUnit.DAYS.between(startDate, endDate)
            return when {
                days > ChronoUnit.MONTHS.duration.toDays() -> Soon
                days >= ChronoUnit.WEEKS.duration.toDays() -> Weeks(ChronoUnit.WEEKS.between(startDate, endDate))
                days >= 1 || startDate == endDate -> Days(days.coerceAtLeast(1))
                days < 0 -> Passed
                else -> Unknown
            }
        }
    }
}
