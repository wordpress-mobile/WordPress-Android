package org.wordpress.android.ui.jetpackoverlay

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayPhase.PHASE_ONE
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayPhase.PHASE_THREE
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseFour
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseOne
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseThree
import org.wordpress.android.ui.jetpackoverlay.JetpackOverlayConnectedFeature.STATS
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date

private const val ONE_DAY_TIME_IN_MILLIS = 1000L * 60L * 60L * 24L

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class JetpackFeatureRemovalOverlayUtilTest : BaseUnitTest() {
    @Mock
    private lateinit var jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper

    @Mock
    private lateinit var jetpackFeatureOverlayShownTracker: JetpackFeatureOverlayShownTracker

    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var siteUtilsWrapper: SiteUtilsWrapper

    @Mock
    private lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    private lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper

    @Mock
    private lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private lateinit var jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil

    private val currentMockedDate = Date(System.currentTimeMillis())

    @Before
    fun setup() {
        jetpackFeatureRemovalOverlayUtil = JetpackFeatureRemovalOverlayUtil(
            jetpackFeatureRemovalPhaseHelper,
            jetpackFeatureOverlayShownTracker,
            selectedSiteRepository,
            siteUtilsWrapper,
            buildConfigWrapper,
            dateTimeUtilsWrapper,
            analyticsTrackerWrapper
        )
    }

    // general phase tests
    @Test
    fun `given jetpack app, shouldShowFeatureSpecificJetpackOverlay invoked, then return false`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)

        val shouldShowOverlay = jetpackFeatureRemovalOverlayUtil
            .shouldShowFeatureSpecificJetpackOverlay(STATS)

        assertFalse(shouldShowOverlay)
    }

    @Test
    fun `given non wpcomSite, shouldShowFeatureSpecificJetpackOverlay invoked, then return false`() {
        val fakeSiteModel = SiteModel()
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(fakeSiteModel)
        whenever(siteUtilsWrapper.isAccessedViaWPComRest(fakeSiteModel)).thenReturn(false)

        val shouldShowOverlay = jetpackFeatureRemovalOverlayUtil
            .shouldShowFeatureSpecificJetpackOverlay(STATS)

        assertFalse(shouldShowOverlay)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `given feature removal not started, when shouldShowFeatureSpecificJetpackOverlay invoked, then return false`() {
        setupMockForWpComSite()
        whenever(jetpackFeatureRemovalPhaseHelper.getCurrentPhase()).thenReturn(null)

        val shouldShowOverlay = jetpackFeatureRemovalOverlayUtil
            .shouldShowFeatureSpecificJetpackOverlay(STATS)

        assertFalse(shouldShowOverlay)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `given feature removal in phase four, when shouldShowFeatureSpecificJetpackOverlay invoked, then return false`() {
        setupMockForWpComSite()
        whenever(jetpackFeatureRemovalPhaseHelper.getCurrentPhase()).thenReturn(PhaseFour)

        val shouldShowOverlay = jetpackFeatureRemovalOverlayUtil
            .shouldShowFeatureSpecificJetpackOverlay(STATS)

        assertFalse(shouldShowOverlay)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `given feature is never accessed, when shouldShowFeatureSpecificJetpackOverlay invoked, then return true`() {
        setupMockForWpComSite()
        whenever(jetpackFeatureRemovalPhaseHelper.getCurrentPhase()).thenReturn(PhaseThree)
        whenever(
            jetpackFeatureOverlayShownTracker.getFeatureOverlayShownTimeStamp(
                STATS,
                PHASE_THREE
            )
        ).thenReturn(null)
        whenever(
                jetpackFeatureOverlayShownTracker.getTheLastShownOverlayTimeStamp(
                        PHASE_THREE
                )
        ).thenReturn(null)

        val shouldShowOverlay = jetpackFeatureRemovalOverlayUtil
            .shouldShowFeatureSpecificJetpackOverlay(STATS)

        assertTrue(shouldShowOverlay)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `given feature is accessed after feature specific frequency, when shouldShowFeatureSpecificJetpackOverlay invoked, then return true`() {
        setupMockForWpComSite()
        // The passed number should exceed the feature specific overlay frequency
        setUpMockForEarliestAccessedFeature(8)

        val shouldShowOverlay = jetpackFeatureRemovalOverlayUtil
            .shouldShowFeatureSpecificJetpackOverlay(STATS)

        assertTrue(shouldShowOverlay)
    }

    @Test
    fun `shouldShowSiteCreationOverlay always returns false`() {
        val shouldShowOverlay = jetpackFeatureRemovalOverlayUtil
            .shouldShowSiteCreationOverlay()

        assertFalse(shouldShowOverlay)
    }

    private fun setupMockForWpComSite() {
        val fakeSiteModel = SiteModel()
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(fakeSiteModel)
        whenever(siteUtilsWrapper.isAccessedViaWPComRest(fakeSiteModel)).thenReturn(true)
    }

    private fun setupMockForFeatureAccessed(
        noOfDaysPastFeatureAccessed: Long,
    ) {
        val featureAccessedMockedTimeinMillis = (System.currentTimeMillis() -
                (noOfDaysPastFeatureAccessed * ONE_DAY_TIME_IN_MILLIS))
        whenever(jetpackFeatureRemovalPhaseHelper.getCurrentPhase()).thenReturn(PhaseOne)
        whenever(
            jetpackFeatureOverlayShownTracker.getFeatureOverlayShownTimeStamp(
                STATS,
                PHASE_ONE
            )
        ).thenReturn(featureAccessedMockedTimeinMillis)
        whenever(dateTimeUtilsWrapper.getTodaysDate()).thenReturn(currentMockedDate)
        whenever(
                dateTimeUtilsWrapper.daysBetween(
                        any(), any()
                )
        ).thenReturn(noOfDaysPastFeatureAccessed.toInt())
    }

    private fun setUpMockForEarliestAccessedFeature(noOfDaysPastFeatureAccessed: Long) {
        val featureAccessedMockedTimeinMillis = (System.currentTimeMillis() -
                (noOfDaysPastFeatureAccessed * ONE_DAY_TIME_IN_MILLIS))

        whenever(jetpackFeatureOverlayShownTracker.getTheLastShownOverlayTimeStamp(PHASE_ONE))
                .thenReturn(featureAccessedMockedTimeinMillis)

        setupMockForFeatureAccessed(noOfDaysPastFeatureAccessed)
    }
}
