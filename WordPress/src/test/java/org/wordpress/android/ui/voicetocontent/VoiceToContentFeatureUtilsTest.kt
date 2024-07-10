package org.wordpress.android.ui.voicetocontent

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.jetpackai.JetpackAIAssistantFeature
import org.wordpress.android.fluxc.model.jetpackai.Tier
import org.wordpress.android.fluxc.model.jetpackai.UsagePeriod
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.VoiceToContentFeatureConfig

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class VoiceToContentFeatureUtilsTest {
    @Mock
    lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    lateinit var voiceToContentFeatureConfig: VoiceToContentFeatureConfig

    private lateinit var utils: VoiceToContentFeatureUtils

    @Before
    fun setup() {
        utils = VoiceToContentFeatureUtils(buildConfigWrapper, voiceToContentFeatureConfig)
    }

    @Test
    fun `when buildConfigWrapper and featureConfig are enabled then returns true`() {
        // Arrange
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)
        whenever(voiceToContentFeatureConfig.isEnabled()).thenReturn(true)
        whenever(buildConfigWrapper.isDebug()).thenReturn(true)

        // Act
        val result = utils.isVoiceToContentEnabled()

        // Assert
        assertEquals(true, result)
    }

    @Test
    fun `when buildConfigWrapper is disabled then returns false `() {
        // Arrange
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(false)

        // Act
        val result = utils.isVoiceToContentEnabled()

        // Assert
        assertEquals(false, result)
    }

    @Test
    fun `when voiceToContentFeatureConfig is disabled then returns false `() {
        // Arrange
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)
        whenever(voiceToContentFeatureConfig.isEnabled()).thenReturn(false)

        // Act
        val result = utils.isVoiceToContentEnabled()

        // Assert
        assertEquals(false, result)
    }

    @Test
    fun `when site requires an upgrade, then is not eligible for voiceToContent`() {
        val feature = JetpackAIAssistantFeature(
            hasFeature = false,
            isOverLimit = false,
            requestsCount = 0,
            requestsLimit = 0,
            usagePeriod = null,
            siteRequireUpgrade = true,
            upgradeUrl = null,
            upgradeType = "",
            currentTier = null,
            nextTier = null,
            tierPlans = emptyList(),
            tierPlansEnabled = false,
            costs = null
        )
        assertFalse(utils.isEligibleForVoiceToContent(feature))
    }

    @Test
    fun `when site does not require an upgrade, then is eligible for voiceToContent`() {
        val feature = JetpackAIAssistantFeature(
            hasFeature = false,
            isOverLimit = false,
            requestsCount = 0,
            requestsLimit = 0,
            usagePeriod = null,
            siteRequireUpgrade = false,
            upgradeType = "",
            upgradeUrl = null,
            currentTier = null,
            nextTier = null,
            tierPlans = emptyList(),
            tierPlansEnabled = false,
            costs = null
        )
        assertTrue(utils.isEligibleForVoiceToContent(feature))
    }

    @Test
    fun `when is free plan, then request limit is calculate for free plan`() {
        val freePlanFeature = JetpackAIAssistantFeature(
            hasFeature = false,
            isOverLimit = false,
            requestsCount = 50,
            requestsLimit = 100,
            usagePeriod = null,
            siteRequireUpgrade = false,
            upgradeType = "",
            upgradeUrl = null,
            currentTier = Tier(JETPACK_AI_FREE, 0, 0, null),
            nextTier = null,
            tierPlans = emptyList(),
            tierPlansEnabled = false,
            costs = null
        )
        assertEquals(50, utils.getRequestLimit(freePlanFeature))

        val freePlanFeatureExceed = freePlanFeature.copy(requestsCount = 150)
        assertEquals(0, utils.getRequestLimit(freePlanFeatureExceed))
    }

    @Test
    fun `when unlimited plan, then request limit is calculated for unlimited plan`() {
        val unlimitedPlanFeature = JetpackAIAssistantFeature(
            hasFeature = false,
            isOverLimit = false,
            requestsCount = 0,
            requestsLimit = 0,
            usagePeriod = null,
            siteRequireUpgrade = false,
            upgradeType = "",
            upgradeUrl = null,
            currentTier = Tier("", 0, 1, null),
            nextTier = null,
            tierPlans = emptyList(),
            tierPlansEnabled = false,
            costs = null
        )
        assertEquals(Int.MAX_VALUE, utils.getRequestLimit(unlimitedPlanFeature))
    }

    @Test
    fun `when limited plan, then request limit is calculated for limited plan`() {
        val limitedPlanFeature = JetpackAIAssistantFeature(
            hasFeature = false,
            isOverLimit = false,
            requestsCount = 0,
            requestsLimit = 0,
            usagePeriod = UsagePeriod("2024-01-01", "2024-02-01", 100),
            siteRequireUpgrade = false,
            upgradeType = "",
            upgradeUrl = null,
            currentTier = Tier("", 200, 0, null),
            nextTier = null,
            tierPlans = emptyList(),
            tierPlansEnabled = false,
            costs = null
        )
        assertEquals(100, utils.getRequestLimit(limitedPlanFeature))

        val limitedPlanFeatureExceed = limitedPlanFeature.copy(
            usagePeriod = UsagePeriod("2024-01-01", "2024-02-01", 250)
        )
        assertEquals(0, utils.getRequestLimit(limitedPlanFeatureExceed))
    }

    companion object {
        private const val JETPACK_AI_FREE = "jetpack_ai_free"
    }
}
