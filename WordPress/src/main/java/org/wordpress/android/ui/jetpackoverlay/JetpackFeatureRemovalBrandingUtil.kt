package org.wordpress.android.ui.jetpackoverlay

import org.wordpress.android.R
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalBrandingUtil.Interval.Days
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalBrandingUtil.Interval.Indeterminate
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalBrandingUtil.Interval.Passed
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalBrandingUtil.Interval.Pluralisable
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalBrandingUtil.Interval.Soon
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalBrandingUtil.Interval.Weeks
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseFour
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseOne
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseThree
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseTwo
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringPluralRes
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.config.JPDeadlineConfig
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit.DAYS
import java.time.temporal.ChronoUnit.MONTHS
import java.time.temporal.ChronoUnit.WEEKS
import java.util.Date
import javax.inject.Inject

class JetpackFeatureRemovalBrandingUtil @Inject constructor(
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper,
    private val jpDeadlineConfig: JPDeadlineConfig,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper
) {
    private val jpDeadlineDate: String? by lazy {
        jpDeadlineConfig.getValue()
    }

    fun shouldShowPhaseOneBranding(): Boolean {
        return when (jetpackFeatureRemovalPhaseHelper.getCurrentPhase()) {
            PhaseOne,
            PhaseTwo,
            PhaseThree,
            PhaseFour -> true
            else -> false
        }
    }

    fun shouldShowPhaseTwoBranding(): Boolean {
        return when (jetpackFeatureRemovalPhaseHelper.getCurrentPhase()) {
            PhaseTwo,
            PhaseThree,
            PhaseFour -> true
            else -> false
        }
    }

    fun getBrandingTextByPhase(screen: JetpackPoweredScreen): UiString {
        return when (jetpackFeatureRemovalPhaseHelper.getCurrentPhase()) {
            PhaseTwo -> UiStringRes(R.string.wp_jetpack_powered_phase_2)
            PhaseThree -> when (screen) {
                is JetpackPoweredScreen.WithDynamicText -> getBrandingTextForPhaseThreeBasedOnDeadline(screen)
                else -> UiStringRes(Interval.RES_JP_POWERED)
            }
            else -> UiStringRes(Interval.RES_JP_POWERED)
        }
    }

    private fun getBrandingTextForPhaseThreeBasedOnDeadline(screen: JetpackPoweredScreen.WithDynamicText): UiString {
        val deadline = retrieveDeadline()

        return when (deadline == null) {
            true -> getPhaseThreeMovingSoonBranding(screen)
            false -> getPhaseThreeMovingInBranding(screen, deadline)
        }
    }

    private fun getPhaseThreeMovingInBranding(
        screen: JetpackPoweredScreen.WithDynamicText,
        deadline: LocalDate,
    ): UiString {
        val today = dateTimeUtilsWrapper.getTodaysDate().toLocalDate()
        return when (val interval = Interval.between(today, deadline)) {
            is Indeterminate -> getPhaseThreeMovingSoonBranding(screen)
            is Weeks -> getMovingInText(screen, getQuantityUiStringOf(interval))
            is Days -> getMovingInText(screen, getQuantityUiStringOf(interval))
            is Passed -> UiStringRes(Interval.RES_JP_POWERED)
        }
    }

    private fun retrieveDeadline(): LocalDate? = jpDeadlineDate?.let {
        dateTimeUtilsWrapper.dateFromPattern(it, JETPACK_OVERLAY_ORIGINAL_DATE_FORMAT)?.toLocalDate()
    }

    private fun Date.toLocalDate() = toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

    private fun getPhaseThreeMovingSoonBranding(screen: JetpackPoweredScreen.WithDynamicText) = UiStringResWithParams(
            stringRes = when (screen.isPlural) {
                true -> Soon.RES_ARE_MOVING_SOON
                false -> Soon.RES_IS_MOVING_SOON
            },
            screen.featureName
        )

    private fun getMovingInText(
        screen: JetpackPoweredScreen.WithDynamicText,
        quantityUiString: UiStringPluralRes
    ) = UiStringResWithParams(
        stringRes = when (screen.isPlural) {
            true -> Pluralisable.RES_ARE_MOVING_IN
            false -> Pluralisable.RES_IS_MOVING_IN
        },
        screen.featureName,
        quantityUiString
    )

    private fun getQuantityUiStringOf(interval: Pluralisable) = UiStringPluralRes(
        zeroRes = 0,
        oneRes = interval.oneRes,
        otherRes = interval.otherRes,
        count = interval.number.toInt()
    )

    sealed class Interval {
        sealed interface Indeterminate
        sealed interface Pluralisable {
            val number: Long
            val oneRes: Int
            val otherRes: Int

            companion object {
                const val RES_ARE_MOVING_IN = R.string.wp_jetpack_powered_phase_3_feature_are_moving_in
                const val RES_IS_MOVING_IN = R.string.wp_jetpack_powered_phase_3_feature_is_moving_in
            }
        }

        object Soon : Interval(), Indeterminate {
            const val RES_ARE_MOVING_SOON = R.string.wp_jetpack_powered_phase_3_feature_are_moving_soon
            const val RES_IS_MOVING_SOON = R.string.wp_jetpack_powered_phase_3_feature_is_moving_soon
        }

        data class Weeks(override val number: Long) : Interval(), Pluralisable {
            override val oneRes = R.string.weeks_quantity_one
            override val otherRes = R.string.weeks_quantity_other
        }

        data class Days(override val number: Long) : Interval(), Pluralisable {
            override val oneRes = R.string.days_quantity_one
            override val otherRes = R.string.days_quantity_other
        }

        object Unknown : Interval(), Indeterminate
        object Passed : Interval()

        companion object {
            const val RES_JP_POWERED = R.string.wp_jetpack_powered

            fun between(startDate: LocalDate, endDate: LocalDate): Interval {
                val days = DAYS.between(startDate, endDate)
                return when {
                    days > MONTHS.duration.toDays() -> Soon
                    days >= WEEKS.duration.toDays() -> Weeks(WEEKS.between(startDate, endDate))
                    days >= 1 || startDate == endDate -> Days(days.coerceAtLeast(1))
                    days < 0 -> Passed
                    else -> Unknown
                }
            }
        }
    }
}
