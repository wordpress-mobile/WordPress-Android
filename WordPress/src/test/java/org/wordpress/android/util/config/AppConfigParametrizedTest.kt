package org.wordpress.android.util.config

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.AppConfig.FeatureState
import org.wordpress.android.util.config.AppConfig.FeatureState.BuildConfigValue
import org.wordpress.android.util.config.AppConfig.FeatureState.DefaultValue
import org.wordpress.android.util.config.AppConfig.FeatureState.ManuallyOverriden
import org.wordpress.android.util.config.AppConfig.FeatureState.RemoteValue
import org.wordpress.android.util.config.AppConfig.FeatureState.StaticValue

@RunWith(Parameterized::class)
class AppConfigParametrizedTest(
    private val params: Params
) {
    private val remoteConfig: FeatureFlagConfig = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val featureConfig: FeatureConfig = mock()
    private val manualFeatureConfig: ManualFeatureConfig = mock()
    private lateinit var appConfig: AppConfig

    @Before
    fun setUp() {
        appConfig = AppConfig(remoteConfig, analyticsTracker, manualFeatureConfig)
    }

    @Test
    fun `shows correct value of isEnabled based on params`() {
        setupFeatureConfig()

        assertThat(appConfig.isEnabled(featureConfig)).isEqualTo(params.result.isEnabled)
    }

    @Test
    fun `shows correct value of feature set based on params`() {
        setupFeatureConfig()

        assertThat(appConfig.featureState(featureConfig)).isEqualTo(params.result)
    }

    private fun setupFeatureConfig() {
        whenever(manualFeatureConfig.hasManualSetup(featureConfig)).thenReturn(params.hasManualSetup)
        whenever(manualFeatureConfig.isManuallyEnabled(featureConfig)).thenReturn(params.isManuallyEnabled)
        whenever(featureConfig.buildConfigValue).thenReturn(params.buildConfigValue)
        whenever(featureConfig.remoteField).thenReturn(params.remoteField)
        whenever(remoteConfig.isEnabled(REMOTE_FIELD)).thenReturn(params.remoteConfigValue)
        whenever(
                remoteConfig.getFeatureState(
                        REMOTE_FIELD,
                        featureConfig.buildConfigValue
                )
        ).thenReturn(params.remoteFeatureState)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        @Suppress("LongMethod")
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
                                remoteField = null,
                                buildConfigValue = true,
                                result = BuildConfigValue(true)
                        )
                ),
                // Returns build config value false when remote field is missing
                arrayOf(
                        Params(
                                remoteField = null,
                                buildConfigValue = false,
                                result = BuildConfigValue(false)
                        )
                ),
                // Returns build config value true if remote field set
                arrayOf(
                        Params(
                                remoteField = REMOTE_FIELD,
                                buildConfigValue = true,
                                result = BuildConfigValue(true)
                        )
                ),
                // Returns remote value == true and source STATIC from remote field
                arrayOf(
                        Params(
                                remoteFeatureState = StaticValue(true),
                                remoteField = REMOTE_FIELD,
                                remoteConfigValue = true,
                                result = StaticValue(true)
                        )
                ),
                // Returns remote value == false and source STATIC from remote field
                arrayOf(
                        Params(
                                remoteFeatureState = StaticValue(false),
                                remoteField = REMOTE_FIELD,
                                remoteConfigValue = false,
                                result = StaticValue(false)
                        )
                ),
                // Returns remote value == true from remote field and source REMOTE
                arrayOf(
                        Params(
                                remoteFeatureState = RemoteValue(true),
                                remoteField = REMOTE_FIELD,
                                remoteConfigValue = true,
                                result = RemoteValue(true)
                        )
                ),
                // Returns default remote value == true from remote field and source Default
                arrayOf(
                        Params(
                                remoteFeatureState = DefaultValue(true),
                                remoteField = REMOTE_FIELD,
                                remoteConfigValue = true,
                                result = DefaultValue(true)
                        )
                )
        )

        private const val REMOTE_FIELD = "remote_field"

        data class Params(
            val hasManualSetup: Boolean = false,
            val isManuallyEnabled: Boolean = false,
            val remoteField: String? = null,
            val buildConfigValue: Boolean = false,
            val remoteFeatureState: FeatureState? = null,
            val remoteConfigValue: Boolean = false,
            val result: FeatureState
        )
    }
}
