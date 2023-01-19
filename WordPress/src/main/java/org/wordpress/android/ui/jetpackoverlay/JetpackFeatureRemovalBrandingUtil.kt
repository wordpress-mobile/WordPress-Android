package org.wordpress.android.ui.jetpackoverlay

import org.wordpress.android.R
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseFour
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseOne
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseThree
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseTwo
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.config.JPDeadlineConfig
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
            null,
            PhaseOne -> UiStringRes(R.string.wp_jetpack_powered)
            PhaseTwo -> UiStringRes(R.string.wp_jetpack_powered_phase_2)
            PhaseThree -> when (screen) {
                is JetpackPoweredScreen.WithDynamicText -> {
                    getBrandingTextForPhaseThreeBasedOnDeadline(screen, jpDeadlineDate)
                }
                else -> UiStringRes(R.string.wp_jetpack_powered)
            }
            PhaseFour -> UiStringRes(R.string.wp_jetpack_powered)
            else -> UiStringRes(R.string.wp_jetpack_powered)
        }
    }

    private fun getBrandingTextForPhaseThreeBasedOnDeadline(
        screen: JetpackPoweredScreen.WithDynamicText,
        jpDeadlineDate: String?,
    ): UiString {
        val daysUntilDeadline = jpDeadlineDate?.let(::countDaysUntilDeadlineOrNull)
        return when (daysUntilDeadline == null) {
            true -> getPhaseThreeBrandingTextWithoutDeadline(screen)
            else -> getPhaseThreeBrandingTextWithDeadline(screen, daysUntilDeadline)
        }
    }

    private fun getPhaseThreeBrandingTextWithoutDeadline(screen: JetpackPoweredScreen.WithDynamicText): UiString {
        return UiStringResWithParams(screen.getMovingSoonText(), screen.featureName)
    }

    private fun getPhaseThreeBrandingTextWithDeadline(
        screen: JetpackPoweredScreen.WithDynamicText,
        daysUntilDeadline: Int,
    ): UiString {
        return when {
            daysUntilDeadline > 30 -> UiStringResWithParams(screen.getMovingSoonText(), screen.featureName)
            daysUntilDeadline > 7 -> (daysUntilDeadline / 7).let { weeksUntilDeadline ->
                return when {
                    weeksUntilDeadline > 1 -> UiStringResWithParams(
                        screen.getWeeksAwayText(),
                        screen.featureName,
                        UiStringText("$weeksUntilDeadline"),
                    )
                    else -> UiStringResWithParams(screen.getWeekAwayText(), screen.featureName)
                }
            }
            daysUntilDeadline > 1 -> UiStringResWithParams(
                screen.getDaysAwayText(),
                screen.featureName,
                UiStringText("$daysUntilDeadline"),
            )
            daysUntilDeadline == 1 -> UiStringResWithParams(
                stringRes = screen.getDayAwayText(),
                screen.featureName
            )
            else -> UiStringRes(R.string.wp_jetpack_powered)
        }
    }

    private fun countDaysUntilDeadlineOrNull(jpDeadlineDate: String): Int? {
        return with(dateTimeUtilsWrapper) {
            getTodaysDate().let { startDate ->
                dateFromPattern(jpDeadlineDate, JETPACK_OVERLAY_ORIGINAL_DATE_FORMAT)?.let { endDate ->
                    daysBetween(startDate, endDate)
                }
            }
        }
    }

    private fun JetpackPoweredScreen.WithDynamicText.getMovingSoonText() = when (isFeatureNameSingular) {
        true -> R.string.wp_jetpack_powered_phase_3_is_moving_soon
        else -> R.string.wp_jetpack_powered_phase_3_are_moving_soon
    }

    private fun JetpackPoweredScreen.WithDynamicText.getWeekAwayText() = when (isFeatureNameSingular) {
        true -> R.string.wp_jetpack_powered_phase_3_with_deadline_is_one_week_away
        else -> R.string.wp_jetpack_powered_phase_3_with_deadline_are_one_week_away
    }

    private fun JetpackPoweredScreen.WithDynamicText.getWeeksAwayText() = when (isFeatureNameSingular) {
        true -> R.string.wp_jetpack_powered_phase_3_with_deadline_is_n_weeks_away
        else -> R.string.wp_jetpack_powered_phase_3_with_deadline_are_n_weeks_away
    }

    private fun JetpackPoweredScreen.WithDynamicText.getDayAwayText() = when (isFeatureNameSingular) {
        true -> R.string.wp_jetpack_powered_phase_3_with_deadline_is_one_day_away
        else -> R.string.wp_jetpack_powered_phase_3_with_deadline_are_one_day_away
    }

    private fun JetpackPoweredScreen.WithDynamicText.getDaysAwayText() = when (isFeatureNameSingular) {
        true -> R.string.wp_jetpack_powered_phase_3_with_deadline_is_n_days_away
        else -> R.string.wp_jetpack_powered_phase_3_with_deadline_are_n_days_away
    }
}
