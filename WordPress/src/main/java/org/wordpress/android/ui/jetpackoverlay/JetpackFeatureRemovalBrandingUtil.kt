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
import org.wordpress.android.util.DateTimeUtilsWrapper
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

    fun getBrandingTextByPhase(screen: JetpackPoweredScreen): UiString {
        return when (jetpackFeatureRemovalPhaseHelper.getCurrentPhase()) {
            null,
            PhaseOne -> UiStringRes(R.string.wp_jetpack_powered)
            PhaseTwo -> UiStringRes(R.string.wp_jetpack_powered_phase_2)
            PhaseThree -> when (screen) {
                is JetpackPoweredScreen.WithDynamicText -> {
                    getBrandingTextForPhaseThreeBasedOnDeadline(screen, jpDeadlineConfig.getValue())
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
        return UiStringResWithParams(
            stringRes = R.string.wp_jetpack_powered_phase_3_moving_soon,
            params = screen.getBrandingTextParams()
        )
    }

    @Suppress("MagicNumber", "ForbiddenComment")
    private fun getPhaseThreeBrandingTextWithDeadline(
        screen: JetpackPoweredScreen.WithDynamicText,
        daysUntilDeadline: Int,
    ): UiString {
        return when {
            daysUntilDeadline > 30 -> UiStringResWithParams(
                stringRes = R.string.wp_jetpack_powered_phase_3_moving_soon,
                params = screen.getBrandingTextParams()
            )
            daysUntilDeadline > 7 -> {
                return when (val weeksUntilDeadline = daysUntilDeadline / 7) {
                    1 -> UiStringResWithParams(
                        stringRes = R.string.wp_jetpack_powered_phase_3_with_deadline_one_week_away,
                        params = screen.getBrandingTextParams()
                    )
                    else -> UiStringResWithParams(
                        stringRes = R.string.wp_jetpack_powered_phase_3_with_deadline_n_weeks_away,
                        params = screen.getBrandingTextParams(weeksUntilDeadline)
                    )
                }
            }
            daysUntilDeadline > 1 -> UiStringResWithParams(
                stringRes = R.string.wp_jetpack_powered_phase_3_with_deadline_n_days_away,
                params = screen.getBrandingTextParams(daysUntilDeadline)
            )
            daysUntilDeadline == 1 -> UiStringResWithParams(
                stringRes = R.string.wp_jetpack_powered_phase_3_with_deadline_one_day_away,
                params = screen.getBrandingTextParams()
            )
            else -> UiStringRes(
                stringRes = R.string.wp_jetpack_powered
            )
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

    private fun JetpackPoweredScreen.WithDynamicText.getBrandingTextParams(timeUntilDeadline: Int? = null) =
        listOfNotNull(
            featureName,
            featureVerb,
            timeUntilDeadline?.let { UiString.UiStringText("$it") },
        )

    private val JetpackPoweredScreen.WithDynamicText.featureVerb
        get() = UiStringRes(
            when (isFeatureNameSingular) {
                true -> R.string.wp_jetpack_powered_phase_3_feature_verb_singular_is
                else -> R.string.wp_jetpack_powered_phase_3_feature_verb_plural_are
            }
        )

}
