package org.wordpress.android.ui.jetpackoverlay

import org.wordpress.android.R
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.ui.jetpackoverlay.JetpackBrandingUiState.Days
import org.wordpress.android.ui.jetpackoverlay.JetpackBrandingUiState.Indeterminate
import org.wordpress.android.ui.jetpackoverlay.JetpackBrandingUiState.Passed
import org.wordpress.android.ui.jetpackoverlay.JetpackBrandingUiState.Pluralisable
import org.wordpress.android.ui.jetpackoverlay.JetpackBrandingUiState.Soon
import org.wordpress.android.ui.jetpackoverlay.JetpackBrandingUiState.Weeks
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
            PhaseThree -> (screen as? JetpackPoweredScreen.WithDynamicText)?.let { screenWithDynamicText ->
                getDynamicBrandingForScreen(screenWithDynamicText)
            } ?: UiStringRes(JetpackBrandingUiState.RES_JP_POWERED)
            else -> UiStringRes(JetpackBrandingUiState.RES_JP_POWERED)
        }
    }

    private fun getDynamicBrandingForScreen(screen: JetpackPoweredScreen.WithDynamicText): UiString {
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
        return when (val interval = JetpackBrandingUiState.between(today, deadline)) {
            is Indeterminate -> getPhaseThreeMovingSoonBranding(screen)
            is Weeks -> getMovingInUiString(screen, getQuantityUiString(interval))
            is Days -> getMovingInUiString(screen, getQuantityUiString(interval))
            is Passed -> UiStringRes(JetpackBrandingUiState.RES_JP_POWERED)
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

    private fun getMovingInUiString(
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

    private fun getQuantityUiString(interval: Pluralisable): UiStringPluralRes {
        return when (interval) {
            is Weeks -> (interval as? Weeks)?.run {
                UiStringPluralRes(
                    zeroRes = otherRes,
                    oneRes = oneRes,
                    otherRes = otherRes,
                    count = number.toInt(),
                )
            } ?: error("Pluralisable interval should be of type Weeks")
            is Days -> (interval as? Days)?.run {
                UiStringPluralRes(
                    zeroRes = otherRes,
                    oneRes = oneRes,
                    otherRes = otherRes,
                    count = number.toInt(),
                )
            } ?: error("Pluralisable interval should be of type Days")
        }
    }

}
