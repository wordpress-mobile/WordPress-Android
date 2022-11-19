package org.wordpress.android.ui.jetpackoverlay

import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayPhase.PHASE_ONE
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayPhase.PHASE_THREE
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayPhase.PHASE_TWO
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureOverlayScreenType.NOTIFICATIONS
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureOverlayScreenType.READER
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureOverlayScreenType.SITE_CREATION
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureOverlayScreenType.STATS
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseFour
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseNewUsers
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseOne
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseThree
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseTwo
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.SiteUtilsWrapper
import java.util.Date
import javax.inject.Inject

class JetpackFeatureRemovalOverlayUtil @Inject constructor(
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper,
    private val jetpackFeatureOverlayShownTracker: JetpackFeatureOverlayShownTracker,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val siteUtilsWrapper: SiteUtilsWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
) {
    fun shouldShowFeatureSpecificJetpackOverlay(feature: JetpackOverlayConnectedFeature): Boolean {
        return !buildConfigWrapper.isJetpackApp && isWpComSite() &&
                isInFeatureSpecificRemovalPhase() && hasExceededOverlayFrequency(
                feature,
                getCurrentPhasePreference()!!
        )
    }

    fun shouldShowSiteCreationOverlay(): Boolean {
        return !buildConfigWrapper.isJetpackApp && isInSiteCreationPhase()
    }

    fun shouldDisableSiteCreation(): Boolean {
        return shouldShowSiteCreationOverlay() &&
                jetpackFeatureRemovalPhaseHelper.getSiteCreationPhase() ==
                JetpackFeatureRemovalSiteCreationPhase.PHASE_TWO
    }

    private fun isInSiteCreationPhase(): Boolean {
        return when (jetpackFeatureRemovalPhaseHelper.getSiteCreationPhase()) {
            null -> false
            JetpackFeatureRemovalSiteCreationPhase.PHASE_ONE,
            JetpackFeatureRemovalSiteCreationPhase.PHASE_TWO -> true
        }
    }

    private fun isInFeatureSpecificRemovalPhase(): Boolean {
        return jetpackFeatureRemovalPhaseHelper.getCurrentPhase() != null &&
                when (jetpackFeatureRemovalPhaseHelper.getCurrentPhase()) {
                    null -> false
                    PhaseOne -> true
                    PhaseTwo, PhaseThree, PhaseFour, PhaseNewUsers -> false
                }
    }

    private fun hasExceededOverlayFrequency(
        feature: JetpackOverlayConnectedFeature,
        currentPhasePreference: JetpackFeatureRemovalOverlayPhase
    ): Boolean {
        return (hasExceededFeatureSpecificOverlayFrequency(feature, currentPhasePreference) ||
                hasExceededGlobalOverlayFrequency(currentPhasePreference))
    }

    private fun hasExceededFeatureSpecificOverlayFrequency(
        feature: JetpackOverlayConnectedFeature,
        phase: JetpackFeatureRemovalOverlayPhase
    ): Boolean {
        // Feature Overlay is never shown
        val overlayShownDate = jetpackFeatureOverlayShownTracker.getFeatureOverlayShownTimeStamp(
                feature,
                phase
        )?.let { Date(it) } ?: return true
        val daysPastOverlayShown = dateTimeUtilsWrapper.daysBetween(
                overlayShownDate,
                dateTimeUtilsWrapper.getTodaysDate()
        )
        return daysPastOverlayShown >= PhaseOne.featureSpecificOverlayFrequency
    }

    private fun hasExceededGlobalOverlayFrequency(phase: JetpackFeatureRemovalOverlayPhase): Boolean {
        // Overlay is never shown
        val overlayShownDate = jetpackFeatureOverlayShownTracker.getEarliestOverlayShownTime(phase)
                ?.let { Date(it) } ?: return true
        val daysPastOverlayShown = dateTimeUtilsWrapper.daysBetween(
                overlayShownDate,
                dateTimeUtilsWrapper.getTodaysDate()
        )
        return daysPastOverlayShown >= PhaseOne.globalOverlayFrequency
    }

    private fun isWpComSite(): Boolean {
        val selectedSite = selectedSiteRepository.getSelectedSite()
        return selectedSite != null && siteUtilsWrapper.isAccessedViaWPComRest(selectedSite)
    }

    private fun getCurrentPhasePreference(): JetpackFeatureRemovalOverlayPhase? {
        return when (jetpackFeatureRemovalPhaseHelper.getCurrentPhase()) {
            PhaseOne -> PHASE_ONE
            PhaseTwo -> PHASE_TWO
            PhaseThree -> PHASE_THREE
            else -> null
        }
    }

    private fun onFeatureSpecificOverlayShown(feature: JetpackOverlayConnectedFeature) {
        if (isInFeatureSpecificRemovalPhase())
            jetpackFeatureOverlayShownTracker.setFeatureOverlayShownTimeStamp(
                    feature,
                    getCurrentPhasePreference()!!,
                    System.currentTimeMillis()
            )
    }

    fun onOverlayShown(overlayScreenType: JetpackFeatureOverlayScreenType?) {
        overlayScreenType?.let {
            when (it) {
                STATS -> onFeatureSpecificOverlayShown(JetpackOverlayConnectedFeature.STATS)
                NOTIFICATIONS -> onFeatureSpecificOverlayShown(JetpackOverlayConnectedFeature.NOTIFICATIONS)
                READER -> onFeatureSpecificOverlayShown(JetpackOverlayConnectedFeature.READER)
                SITE_CREATION -> trackSiteCreationOverlayShown()
            }
        }
    }

    private fun trackSiteCreationOverlayShown() {
        // add tracking logic
    }

    enum class JetpackFeatureOverlayScreenType {
        STATS,
        NOTIFICATIONS,
        READER,
        SITE_CREATION
    }
}
