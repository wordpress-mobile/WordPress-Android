package org.wordpress.android.ui.jetpackoverlay

import android.util.Log
import org.wordpress.android.ui.jetpackoverlay.JETPACKFEATUREOVERLAYPHASE.PHASE_ONE
import org.wordpress.android.ui.jetpackoverlay.JETPACKFEATUREOVERLAYPHASE.PHASE_THREE
import org.wordpress.android.ui.jetpackoverlay.JETPACKFEATUREOVERLAYPHASE.PHASE_TWO
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseFour
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseNewUsers
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseOne
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseThree
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseTwo
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date
import javax.inject.Inject

class JetpackFeatureRemovalOverlayUtil @Inject constructor(
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper,
    private val jetpackFeatureOverlayShownTracker: JetpackFeatureOverlayShownTracker,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val siteUtilsWrapper: SiteUtilsWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper
) {
    fun shouldShowFeatureSpecificJetpackOverlay(feature: JetpackOverlayConnectedFeature): Boolean {
        return !buildConfigWrapper.isJetpackApp && isWpComSite() &&
                isInFeatureSpecificRemovalPhase(feature)
    }

    private fun isInFeatureSpecificRemovalPhase(feature: JetpackOverlayConnectedFeature): Boolean {
        return jetpackFeatureRemovalPhaseHelper.getCurrentPhase() != null &&
                when (jetpackFeatureRemovalPhaseHelper.getCurrentPhase()) {
                    null -> return false
                    PhaseOne, PhaseTwo, PhaseThree -> hasExceededOverlayFrequency(
                            feature,
                            getCurrentPhasePreference()!!
                    )
                    PhaseFour -> return false
                    PhaseNewUsers -> return false
                }
    }

    private fun hasExceededOverlayFrequency(
        feature: JetpackOverlayConnectedFeature,
        currentPhasePreference: JETPACKFEATUREOVERLAYPHASE
    ): Boolean {
        return (hasExceededFeatureSpecificOverlayFrequency(feature, currentPhasePreference) ||
                hasExceededGlobalOverlayFrequency(currentPhasePreference))
    }

    fun clearSharedPreferences() {
        jetpackFeatureOverlayShownTracker.clear()
    }

    fun setOverlayShown(feature: JetpackOverlayConnectedFeature, timestamp: Long) {
        jetpackFeatureOverlayShownTracker.setFeatureOverlayShownTimeStamp(feature, PHASE_ONE, timestamp)
    }

    fun getOverlayShown(feature: JetpackOverlayConnectedFeature): Long? {
        return jetpackFeatureOverlayShownTracker.getFeatureOverlayShownTimeStamp(feature, PHASE_ONE)
    }

    fun getEarliestOverlayShownTime() {
        val overlayShownDate = jetpackFeatureOverlayShownTracker.getEarliestOverlayShownTime(PHASE_ONE)
                ?.let { Date(it) }
        Log.e("overlay shown date", overlayShownDate.toString())
    }

    private fun hasExceededFeatureSpecificOverlayFrequency(
        feature: JetpackOverlayConnectedFeature,
        phase: JETPACKFEATUREOVERLAYPHASE
    ): Boolean {
        // Feature Overlay is never shown
        val overlayShownDate = jetpackFeatureOverlayShownTracker.getFeatureOverlayShownTimeStamp(feature, phase)
                ?.let { Date(it) } ?: return true
        val daysPastOverlayShown = dateTimeUtilsWrapper.daysBetween(overlayShownDate, Date(System.currentTimeMillis()))
        if (daysPastOverlayShown >= PhaseOne.featureSpecificOverlayFrequency)
            return true
        return false
    }

    private fun hasExceededGlobalOverlayFrequency(phase: JETPACKFEATUREOVERLAYPHASE): Boolean {
        // Overlay is never shown
        val overlayShownDate = jetpackFeatureOverlayShownTracker.getEarliestOverlayShownTime(phase)
                ?.let { Date(it) } ?: return true
        val daysPastOverlayShown = dateTimeUtilsWrapper.daysBetween(overlayShownDate, Date(System.currentTimeMillis()))
        if (daysPastOverlayShown >= PhaseOne.globalOverlayFrequency)
            return true
        return false
    }

    private fun isWpComSite(): Boolean {
        val selectedSite = selectedSiteRepository.getSelectedSite()
        return selectedSite != null && siteUtilsWrapper.isAccessedViaWPComRest(selectedSite)
    }

    private fun getCurrentPhasePreference(): JETPACKFEATUREOVERLAYPHASE? {
        return when (jetpackFeatureRemovalPhaseHelper.getCurrentPhase()) {
            PhaseOne -> PHASE_ONE
            PhaseTwo -> PHASE_TWO
            PhaseThree -> PHASE_THREE
            else -> null
        }
    }
}
