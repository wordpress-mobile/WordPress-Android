package org.wordpress.android.ui.jetpackoverlay

import org.wordpress.android.R
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseFour
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseOne
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseThree
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseTwo
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.util.config.JPDeadlineConfig
import javax.inject.Inject

class JetpackFeatureRemovalBrandingUtil @Inject constructor(
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper,
    private val jpDeadlineConfig: JPDeadlineConfig,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper
) {
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

    @Suppress("unused")
    fun getBrandingTextByPhase(screen: JetpackBrandingUtils.Screen): UiString {
        return when (jetpackFeatureRemovalPhaseHelper.getCurrentPhase()) {
            null,
            PhaseOne -> UiStringRes(R.string.wp_jetpack_powered)
            PhaseTwo -> UiStringRes(R.string.wp_jetpack_powered_phase_2)
            PhaseThree -> getBrandingTextForPhaseThreeBasedOnDeadline(screen, jpDeadlineConfig.getValue())
            PhaseFour -> UiStringRes(R.string.wp_jetpack_powered)
            else -> UiStringRes(R.string.wp_jetpack_powered)
        }
    }

    private fun getBrandingTextForPhaseThreeBasedOnDeadline(
        screen: JetpackBrandingUtils.Screen,
        jpDeadlineDate: String?,
    ): UiString {
        val deadlineDate = jpDeadlineDate?.takeIf(String::isNotEmpty)?.let { dateString ->
            dateTimeUtilsWrapper.convertDateFormat(
                dateString,
                JETPACK_OVERLAY_ORIGINAL_DATE_FORMAT,
                JETPACK_OVERLAY_TARGET_DATE_FORMAT
            )
        }

        return when (deadlineDate.isNullOrEmpty()) {
            true -> getPhaseThreeBrandingTextWithoutDeadline(screen)
            else -> getPhaseThreeBrandingTextWithDeadline(screen, deadlineDate)
        }
    }

    private fun getPhaseThreeBrandingTextWithoutDeadline(screen: JetpackBrandingUtils.Screen): UiString {
        return UiString.UiStringResWithParams(
            stringRes = R.string.wp_jetpack_powered_phase_3_without_deadline,
            params = screen.getBrandingTextParams()
        )
    }

    private fun getPhaseThreeBrandingTextWithDeadline(
        screen: JetpackBrandingUtils.Screen,
        deadlineDate: String,
    ): UiString {
        val daysUntilDeadline = countDaysUntilDeadline(deadlineDate)
        return when {
            // Deadline is more than one month away
            daysUntilDeadline > 30 -> {
                UiString.UiStringResWithParams(
                    stringRes = R.string.wp_jetpack_powered_phase_3_with_deadline_months_away,
                    params = screen.getBrandingTextParams()
                )
            }
            // Deadline is more than one week away
            daysUntilDeadline > 7 -> {
                val weeksUntilDeadline = daysUntilDeadline / 7
                UiString.UiStringResWithParams(
                    stringRes = R.string.wp_jetpack_powered_phase_3_with_deadline_weeks_away,
                    params = screen.getBrandingTextParams(weeksUntilDeadline)
                )
            }
            // Deadline is more than one day away
            daysUntilDeadline > 1 -> {
                UiString.UiStringResWithParams(
                    stringRes = R.string.wp_jetpack_powered_phase_3_with_deadline_days_away,
                    params = screen.getBrandingTextParams(daysUntilDeadline)
                )
            }
            // Deadline is one day away
            daysUntilDeadline == 1 -> UiStringRes(R.string.wp_jetpack_powered_phase_3_with_deadline_day_away)
            // Deadline is today or has passed
            else -> UiStringRes(R.string.wp_jetpack_powered)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun countDaysUntilDeadline(deadlineDate: String): Int {
        return dateTimeUtilsWrapper.daysBetween(
            dateTimeUtilsWrapper.dateFromIso8601(deadlineDate),
            dateTimeUtilsWrapper.getTodaysDate()
        )
    }
}
