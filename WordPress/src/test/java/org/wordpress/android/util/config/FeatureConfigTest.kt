package org.wordpress.android.util.config

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class FeatureConfigTest {
    @Mock lateinit var appConfig: AppConfig
    private lateinit var featureConfig: FeatureConfig

    @Before
    fun setUp() {
        featureConfig = TestFeatureConfig(appConfig)
    }

    @Test
    fun `returns isEnabled == true from app config`() {
        whenever(appConfig.isEnabled(featureConfig)).thenReturn(true)

        assertThat(featureConfig.isEnabled()).isTrue()
    }

    @Test
    fun `returns isEnabled == false from app config`() {
        whenever(appConfig.isEnabled(featureConfig)).thenReturn(false)

        assertThat(featureConfig.isEnabled()).isFalse()
    }

    private class TestFeatureConfig(appConfig: AppConfig) : FeatureConfig(appConfig, true, "remote_field")
}
