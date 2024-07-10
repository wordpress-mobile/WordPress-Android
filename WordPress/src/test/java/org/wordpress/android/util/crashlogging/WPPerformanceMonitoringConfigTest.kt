package org.wordpress.android.util.crashlogging

import com.automattic.android.tracks.crashlogging.PerformanceMonitoringConfig
import org.junit.Test
import org.mockito.kotlin.whenever
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.mock
import org.wordpress.android.util.config.RemoteConfigWrapper
import org.wordpress.android.util.crashlogging.WPPerformanceMonitoringConfig.Companion.RELATIVE_PROFILES_SAMPLE_RATE

private const val VALID_SAMPLE_RATE = 0.01
private const val INVALID_SAMPLE_RATE = 0.0

class WPPerformanceMonitoringConfigTest {
    private val remoteConfig: RemoteConfigWrapper = mock()
    private val analytics: AnalyticsTrackerWrapper = mock()
    private val buildConfig: BuildConfigWrapper = mock()

    private val sut = WPPerformanceMonitoringConfig(remoteConfig, analytics, buildConfig)

    @Test
    fun `given the user disabled tracking, disable performance monitoring`() {
        whenever(buildConfig.isDebug()).thenReturn(false)
        whenever(analytics.hasUserOptedOut).thenReturn(true)
        whenever(remoteConfig.getPerformanceMonitoringSampleRate()).thenReturn(VALID_SAMPLE_RATE)

        val result = sut.invoke()

        assertThat(result).isEqualTo(PerformanceMonitoringConfig.Disabled)
    }

    @Test
    fun `given the debug app, disable performance monitoring`() {
        whenever(buildConfig.isDebug()).thenReturn(true)
        whenever(analytics.hasUserOptedOut).thenReturn(false)
        whenever(remoteConfig.getPerformanceMonitoringSampleRate()).thenReturn(VALID_SAMPLE_RATE)

        val result = sut.invoke()

        assertThat(result).isEqualTo(PerformanceMonitoringConfig.Disabled)
    }

    @Test
    fun `given the app is not debug and user did not disabled tracking, enable performance monitoring`() {
        whenever(buildConfig.isDebug()).thenReturn(false)
        whenever(analytics.hasUserOptedOut).thenReturn(false)
        whenever(remoteConfig.getPerformanceMonitoringSampleRate()).thenReturn(VALID_SAMPLE_RATE)

        val result = sut.invoke()

        assertThat(result).isEqualTo(
            PerformanceMonitoringConfig.Enabled(
                VALID_SAMPLE_RATE,
                RELATIVE_PROFILES_SAMPLE_RATE
            )
        )
    }

    @Test
    fun `given invalid sample rate, disable performance monitoring`() {
        whenever(buildConfig.isDebug()).thenReturn(false)
        whenever(analytics.hasUserOptedOut).thenReturn(false)
        whenever(remoteConfig.getPerformanceMonitoringSampleRate()).thenReturn(INVALID_SAMPLE_RATE)

        val result = sut.invoke()

        assertThat(result).isEqualTo(PerformanceMonitoringConfig.Disabled)
    }
}
