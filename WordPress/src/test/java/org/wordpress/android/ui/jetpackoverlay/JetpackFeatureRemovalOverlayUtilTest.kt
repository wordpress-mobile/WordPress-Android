package org.wordpress.android.ui.jetpackoverlay

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayPhase.PHASE_ONE
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseOne
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseTwo
import org.wordpress.android.ui.jetpackoverlay.JetpackOverlayConnectedFeature.STATS
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.SiteUtilsWrapper
import java.util.Date

private const val ONE_DAY_TIME_IN_MILLIS = 1000L * 60L * 60L * 24L

@RunWith(MockitoJUnitRunner::class)
class JetpackFeatureRemovalOverlayUtilTest {
    @Mock private lateinit var jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper
    @Mock private lateinit var jetpackFeatureOverlayShownTracker: JetpackFeatureOverlayShownTracker
    @Mock private lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock private lateinit var siteUtilsWrapper: SiteUtilsWrapper
    @Mock private lateinit var buildConfigWrapper: BuildConfigWrapper
    @Mock private lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper

    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

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
                dateTimeUtilsWrapper
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

    // This test is for phase 2 and above right now, when the changes for phase 2 is implemented,
    // the jetpackFeatureRemovalOverlayUtil.shouldShowFeatureSpecificJetpackOverlay(STATS)
    // would return true for phase 2 and 3
    @Test
    @Suppress("MaxLineLength")
    fun `given feature removal in phase two, when shouldShowFeatureSpecificJetpackOverlay invoked, then return false`() {
        setupMockForWpComSite()
        whenever(jetpackFeatureRemovalPhaseHelper.getCurrentPhase()).thenReturn(PhaseTwo)

        val shouldShowOverlay = jetpackFeatureRemovalOverlayUtil
                .shouldShowFeatureSpecificJetpackOverlay(STATS)

        assertFalse(shouldShowOverlay)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `given feature is never accessed, when shouldShowFeatureSpecificJetpackOverlay invoked, then return true`() {
        setupMockForWpComSite()
        whenever(jetpackFeatureRemovalPhaseHelper.getCurrentPhase()).thenReturn(PhaseOne)
        whenever(
                jetpackFeatureOverlayShownTracker.getFeatureOverlayShownTimeStamp(
                        STATS,
                        PHASE_ONE
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
        setupMockForFeatureAccessed(8)

        val shouldShowOverlay = jetpackFeatureRemovalOverlayUtil
                .shouldShowFeatureSpecificJetpackOverlay(STATS)

        assertTrue(shouldShowOverlay)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `given feature is accessed after globalOverlayFrequency, when shouldShowFeatureSpecificJetpackOverlay invoked, then return true`() {
        setupMockForWpComSite()
        // The feature was accessed 3 days ago and the globalOverlayFrequency for phase one is 2
        // The passed number should not exceed feature specific globalOverlayFrequency
        // but should be less than global overlay frequency
        setUpMockForEarliestAccessedFeature(3L)

        val shouldShowOverlay = jetpackFeatureRemovalOverlayUtil
                .shouldShowFeatureSpecificJetpackOverlay(STATS)

        assertTrue(shouldShowOverlay)
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
                        Date(featureAccessedMockedTimeinMillis),
                        currentMockedDate
                )
        ).thenReturn(noOfDaysPastFeatureAccessed.toInt())
    }

    private fun setUpMockForEarliestAccessedFeature(noOfDaysPastFeatureAccessed:Long) {
        val featureAccessedMockedTimeinMillis = (System.currentTimeMillis() -
                (noOfDaysPastFeatureAccessed * ONE_DAY_TIME_IN_MILLIS))

        whenever(jetpackFeatureOverlayShownTracker.getEarliestOverlayShownTime(PHASE_ONE))
                .thenReturn(featureAccessedMockedTimeinMillis)

        setupMockForFeatureAccessed(noOfDaysPastFeatureAccessed)
    }
}
