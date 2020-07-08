package org.wordpress.android.util.config

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.ExperimentConfig.Variant

@RunWith(MockitoJUnitRunner::class)
class AppConfigTest {
    @Mock lateinit var remoteConfig: RemoteConfig
    @Mock lateinit var analyticsTracker: AnalyticsTrackerWrapper
    @Mock lateinit var featureConfig: FeatureConfig
    @Mock lateinit var experimentConfig: ExperimentConfig
    private lateinit var appConfig: AppConfig
    private val remoteField = "remote_field"
    private val experimentVariantA = "variantA"
    private val experimentVariantB = "variantB"

    @Before
    fun setUp() {
        appConfig = AppConfig(remoteConfig, analyticsTracker)
    }

    @Test
    fun `refresh passes the call to remote config`() {
        appConfig.refresh()

        verify(remoteConfig).refresh()
    }

    @Test
    fun `returns feature as enabled when the build config value is enabled and tracks the event`() {
        setupFeatureConfig(buildConfigValue = true, remoteConfigValue = false)

        assertThat(appConfig.isEnabled(featureConfig)).isTrue()
        verify(analyticsTracker).track(Stat.FEATURE_FLAG_SET, mapOf(remoteField to true))
    }

    @Test
    fun `returns feature as enabled when the remote config value is enabled and tracks the event`() {
        setupFeatureConfig(buildConfigValue = false, remoteConfigValue = true)

        assertThat(appConfig.isEnabled(featureConfig)).isTrue()
        verify(analyticsTracker).track(Stat.FEATURE_FLAG_SET, mapOf(remoteField to true))
    }

    @Test
    fun `returns feature as disabled when the remote config value is disabled and tracks the event`() {
        setupFeatureConfig(buildConfigValue = false, remoteConfigValue = false)

        assertThat(appConfig.isEnabled(featureConfig)).isFalse()
        verify(analyticsTracker).track(Stat.FEATURE_FLAG_SET, mapOf(remoteField to false))
    }

    @Test
    fun `returns cached true value and tracks only once`() {
        setupFeatureConfig(buildConfigValue = false, remoteConfigValue = true)

        assertThat(appConfig.isEnabled(featureConfig)).isTrue()

        setupFeatureConfig(buildConfigValue = false, remoteConfigValue = false)

        assertThat(appConfig.isEnabled(featureConfig)).isTrue()

        verify(analyticsTracker).track(Stat.FEATURE_FLAG_SET, mapOf(remoteField to true))
    }

    @Test
    fun `returns cached false value and tracks only once`() {
        setupFeatureConfig(buildConfigValue = false, remoteConfigValue = false)

        assertThat(appConfig.isEnabled(featureConfig)).isFalse()

        setupFeatureConfig(buildConfigValue = true, remoteConfigValue = true)

        assertThat(appConfig.isEnabled(featureConfig)).isFalse()

        verify(analyticsTracker).track(Stat.FEATURE_FLAG_SET, mapOf(remoteField to false))
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

    private fun setupFeatureConfig(buildConfigValue: Boolean, remoteConfigValue: Boolean) {
        whenever(featureConfig.buildConfigValue).thenReturn(buildConfigValue)
        whenever(featureConfig.remoteField).thenReturn(remoteField)
        whenever(remoteConfig.isEnabled(remoteField)).thenReturn(remoteConfigValue)
    }

    private fun setupExperimentConfig(remoteConfigValue: String, experimentVariants: List<Variant>) {
        whenever(experimentConfig.remoteField).thenReturn(remoteField)
        whenever(experimentConfig.variants).thenReturn(experimentVariants)
        whenever(remoteConfig.getString(remoteField)).thenReturn(remoteConfigValue)
    }
}
