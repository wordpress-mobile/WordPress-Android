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


private const val ONE_DAY_TIME_IN_MILLIS = 1000L * 60L * 60L * 24L

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
                dateTimeUtilsWrapper.getTodaysDate())
        return daysPastOverlayShown >= PhaseOne.featureSpecificOverlayFrequency
    }

    private fun hasExceededGlobalOverlayFrequency(phase: JetpackFeatureRemovalOverlayPhase): Boolean {
        // Overlay is never shown
        val overlayShownDate = jetpackFeatureOverlayShownTracker.getEarliestOverlayShownTime(phase)
                ?.let { Date(it) } ?: return true
        val daysPastOverlayShown = dateTimeUtilsWrapper.daysBetween(
                overlayShownDate,
                dateTimeUtilsWrapper.getTodaysDate())
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

    @Suppress("unused", "UNUSED_PARAMETER")
    private fun onFeatureSpecificOverlayShown(feature: JetpackOverlayConnectedFeature) {
    // Commented out for testing purposes only
//        if (isInFeatureSpecificRemovalPhase())
//            jetpackFeatureOverlayShownTracker.setFeatureOverlayShownTimeStamp(feature, getCurrentPhasePreference()!!)
    }

    fun initializeFeatureShownOn() {
        // This function sets the time when the overlay was show
        // This is only for testing purposes

        // The first parameter sets the no of days when the overlay was last shown
        // For example, value 2 means, the overlay shown time stamp will be set 2 days before
        // The Second parameter sets the feature for which the overlay was shown

        setFeatureAccessedOn(3, JetpackOverlayConnectedFeature.STATS)
        setFeatureAccessedOn(2, JetpackOverlayConnectedFeature.NOTIFICATIONS)
        setFeatureAccessedOn(4, JetpackOverlayConnectedFeature.READER)


        // Inorder to clear the values and reset when the overlay was shown, un comment the below code
        // jetpackFeatureOverlayShownTracker.clear()
    }


    private fun setFeatureAccessedOn(noOfDaysPastFeatureAccessed:Int, jetpackConnectedFeature: JetpackOverlayConnectedFeature) {
        val featureAccessedMockedTimeinMillis = (System.currentTimeMillis() -
                (noOfDaysPastFeatureAccessed * ONE_DAY_TIME_IN_MILLIS))

        jetpackFeatureOverlayShownTracker.setFeatureOverlayShownTimeStamp(jetpackConnectedFeature
                ,PHASE_ONE, featureAccessedMockedTimeinMillis)
    }

    fun onOverlayShown(overlayScreenType: JetpackFeatureOverlayScreenType?) {
        overlayScreenType?.let {
            when (it) {
                STATS -> onFeatureSpecificOverlayShown(JetpackOverlayConnectedFeature.STATS)
                NOTIFICATIONS -> onFeatureSpecificOverlayShown(JetpackOverlayConnectedFeature.NOTIFICATIONS)
                READER -> onFeatureSpecificOverlayShown(JetpackOverlayConnectedFeature.READER)
                SITE_CREATION -> TODO()
            }
        }
    }

    enum class JetpackFeatureOverlayScreenType {
        STATS,
        NOTIFICATIONS,
        READER,
        SITE_CREATION
    }
}
