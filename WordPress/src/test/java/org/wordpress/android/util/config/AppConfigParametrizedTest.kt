package org.wordpress.android.util.config

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.AppConfig.FeatureState
import org.wordpress.android.util.config.AppConfig.FeatureState.BuildConfigValue
import org.wordpress.android.util.config.AppConfig.FeatureState.DefaultValue
import org.wordpress.android.util.config.AppConfig.FeatureState.ManuallyOverriden
import org.wordpress.android.util.config.AppConfig.FeatureState.RemoteValue
import org.wordpress.android.util.config.manual.ManualFeatureConfig

@RunWith(Parameterized::class)
class AppConfigParametrizedTest(
    private val params: Params
) {
    private val remoteConfig: RemoteConfig = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val featureConfig: FeatureConfig = mock()
    private val manualFeatureConfig: ManualFeatureConfig = mock()
    private lateinit var appConfig: AppConfig

    @Before
    fun setUp() {
        appConfig = AppConfig(remoteConfig, analyticsTracker, manualFeatureConfig)
    }

    @Test
    fun `shows correct value of isEnabled based on params and tracks the decision`() {
        setupFeatureConfig()

        assertThat(appConfig.isEnabled(featureConfig)).isEqualTo(params.result.isEnabled)

        verify(analyticsTracker).track(Stat.FEATURE_FLAG_SET, featureConfig)
    }

    @Test
    fun `shows correct value of feature set based on params and tracks the decision`() {
        setupFeatureConfig()

        assertThat(appConfig.featureState(featureConfig)).isEqualTo(params.result)

        verify(analyticsTracker).track(Stat.FEATURE_FLAG_SET, featureConfig)
    }

    private fun setupFeatureConfig() {
        whenever(manualFeatureConfig.hasManualSetup(featureConfig)).thenReturn(params.hasManualSetup)
        whenever(manualFeatureConfig.isManuallyEnabled(featureConfig)).thenReturn(params.isManuallyEnabled)
        whenever(featureConfig.buildConfigValue).thenReturn(params.buildConfigValue)
        if (params.hasRemoteField) {
            whenever(featureConfig.remoteField).thenReturn(remoteField)
        } else {
            whenever(featureConfig.remoteField).thenReturn(null)
        }
        whenever(remoteConfig.isEnabled(remoteField)).thenReturn(params.remoteConfigValue)
        whenever(remoteConfig.isInitialized()).thenReturn(params.isInitialized)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun parameters() = listOf(
                // Manual override shows the flag as enabled
                arrayOf(
                        Params(
                                hasManualSetup = true,
                                isManuallyEnabled = true,
                                result = ManuallyOverriden(true)
                        )
                ),
                // Manual override shows the flag as disabled
                arrayOf(
                        Params(
                                hasManualSetup = true,
                                isManuallyEnabled = false,
                                result = ManuallyOverriden(false)
                        )
                ),
                // Returns build config value true when remote field is missing
                arrayOf(
                        Params(
                                hasRemoteField = false,
                                buildConfigValue = true,
                                result = BuildConfigValue(true)
                        )
                ),
                // Returns build config value false when remote field is missing
                arrayOf(
                        Params(
                                hasRemoteField = false,
                                buildConfigValue = false,
                                result = BuildConfigValue(false)
                        )
                ),
                // Returns build config value true if remote field set
                arrayOf(
                        Params(
                                hasRemoteField = true,
                                buildConfigValue = true,
                                result = BuildConfigValue(true)
                        )
                ),
                // Returns default value == true from remote field when not initialized
                arrayOf(
                        Params(
                                isInitialized = false,
                                hasRemoteField = true,
                                remoteConfigValue = true,
                                result = DefaultValue(true)
                        )
                ),
                // Returns default value == false from remote field when not initialized
                arrayOf(
                        Params(
                                isInitialized = false,
                                hasRemoteField = true,
                                remoteConfigValue = false,
                                result = DefaultValue(false)
                        )
                ),
                // Returns remote value == true from remote field when initialized
                arrayOf(
                        Params(
                                isInitialized = true,
                                hasRemoteField = true,
                                remoteConfigValue = true,
                                result = RemoteValue(true)
                        )
                ),
                // Returns remote value == false from remote field when initialized
                arrayOf(
                        Params(
                                isInitialized = true,
                                hasRemoteField = true,
                                remoteConfigValue = false,
                                result = RemoteValue(false)
                        )
                )
        )

        private const val remoteField = "remote_field"

        data class Params(
            val hasManualSetup: Boolean = false,
            val isManuallyEnabled: Boolean = false,
            val hasRemoteField: Boolean = false,
            val buildConfigValue: Boolean = false,
            val isInitialized: Boolean = false,
            val remoteConfigValue: Boolean = false,
            val result: FeatureState
        )
    }
}
