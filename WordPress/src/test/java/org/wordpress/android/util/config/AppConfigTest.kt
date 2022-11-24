package org.wordpress.android.util.config

import kotlinx.coroutines.CoroutineScope
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.ExperimentConfig.Variant

@RunWith(MockitoJUnitRunner::class)
class AppConfigTest {
    @Mock lateinit var featureFlagConfig: FeatureFlagConfig
    @Mock lateinit var analyticsTracker: AnalyticsTrackerWrapper
    @Mock lateinit var experimentConfig: ExperimentConfig
    @Mock lateinit var manualFeatureConfig: ManualFeatureConfig
    @Mock lateinit var appScope: CoroutineScope
    private lateinit var appConfig: AppConfig
    private val remoteField = "remote_field"
    private val experimentVariantA = "variantA"
    private val experimentVariantB = "variantB"

    @Before
    fun setUp() {
        appConfig = AppConfig(featureFlagConfig, analyticsTracker, manualFeatureConfig)
    }

    @Test
    fun `refresh passes the call to remote config`() {
        appConfig.refresh(appScope)

        verify(featureFlagConfig).refresh(appScope)
    }

    @Test
    fun `returns experiment variant from remote config when experiment contains variant and tracks the event`() {
        val variant = Variant(experimentVariantA)
        setupExperimentConfig(experimentVariantA, listOf(variant))

        assertThat(appConfig.getCurrentVariant(experimentConfig)).isEqualTo(variant)
        verify(analyticsTracker).track(
                Stat.EXPERIMENT_VARIANT_SET,
                mapOf(experimentConfig.remoteField to experimentVariantA)
        )
    }

    @Test
    fun `returns null variant when the experiment variant doesn't match remote config`() {
        val variant = Variant("different variant")
        setupExperimentConfig(experimentVariantA, listOf(variant))

        assertThatIllegalArgumentException().isThrownBy { appConfig.getCurrentVariant(experimentConfig) }
        verify(analyticsTracker).track(
                Stat.EXPERIMENT_VARIANT_SET,
                mapOf(remoteField to experimentVariantA)
        )
    }

    @Test
    fun `caches value and tracks only once when the remote value changes`() {
        val variantA = Variant(experimentVariantA)
        val variantB = Variant(experimentVariantB)
        setupExperimentConfig(experimentVariantA, listOf(variantA, variantB))

        assertThat(appConfig.getCurrentVariant(experimentConfig)).isEqualTo(variantA)

        setupExperimentConfig(experimentVariantB, listOf(variantA, variantB))

        assertThat(appConfig.getCurrentVariant(experimentConfig)).isEqualTo(variantA)

        verify(analyticsTracker).track(
                Stat.EXPERIMENT_VARIANT_SET,
                mapOf(remoteField to experimentVariantA)
        )
    }

    private fun setupExperimentConfig(remoteConfigValue: String, experimentVariants: List<Variant>) {
        whenever(experimentConfig.remoteField).thenReturn(remoteField)
        whenever(experimentConfig.variants).thenReturn(experimentVariants)
        whenever(featureFlagConfig.getString(remoteField)).thenReturn(remoteConfigValue)
    }
}
