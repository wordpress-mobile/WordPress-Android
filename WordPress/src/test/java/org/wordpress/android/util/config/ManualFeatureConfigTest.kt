package org.wordpress.android.util.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper

@RunWith(MockitoJUnitRunner::class)
class ManualFeatureConfigTest {
    @Mock lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock lateinit var appConfig: AppConfig
    @Mock lateinit var buildConfigWrapper: BuildConfigWrapper
    private lateinit var manualFeatureConfig: ManualFeatureConfig
    private lateinit var localFeatureConfig: FeatureConfig
    private lateinit var remoteFeatureConfig: FeatureConfig
    private val remoteField = "remote_feature_enabled"
    private val featureKey = "feature_key"

    @Before
    fun setUp() {
        manualFeatureConfig = ManualFeatureConfig(appPrefsWrapper, buildConfigWrapper)
        localFeatureConfig = LocalFeatureConfig(appConfig)
        remoteFeatureConfig = object : FeatureConfig(appConfig, true, remoteField) {}
        whenever(buildConfigWrapper.isDebugSettingsEnabled()).thenReturn(true)
    }

    @Test
    fun `sets manual config value from feature key`() {
        manualFeatureConfig.setManuallyEnabled(featureKey, true)

        verify(appPrefsWrapper).setManualFeatureConfig(true, featureKey)
    }

    @Test
    fun `does not set manual config value from feature key when flag disabled`() {
        whenever(buildConfigWrapper.isDebugSettingsEnabled()).thenReturn(false)

        manualFeatureConfig.setManuallyEnabled(featureKey, true)

        verifyNoInteractions(appPrefsWrapper)
    }

    @Test
    fun `hasManualSetup is true for local feature config`() {
        whenever(appPrefsWrapper.hasManualFeatureConfig("LocalFeatureConfig")).thenReturn(true)

        val hasManualSetup = manualFeatureConfig.hasManualSetup(localFeatureConfig)

        assertThat(hasManualSetup).isTrue()
    }

    @Test
    fun `hasManualSetup is true for remote feature config`() {
        whenever(appPrefsWrapper.hasManualFeatureConfig(remoteField)).thenReturn(true)

        val hasManualSetup = manualFeatureConfig.hasManualSetup(remoteFeatureConfig)

        assertThat(hasManualSetup).isTrue()
    }

    @Test
    fun `hasManualSetup is true for feature key`() {
        whenever(appPrefsWrapper.hasManualFeatureConfig(featureKey)).thenReturn(true)

        val hasManualSetup = manualFeatureConfig.hasManualSetup(featureKey)

        assertThat(hasManualSetup).isTrue()
    }

    @Test
    fun `hasManualSetup does not call appPrefsWrapper when flag is turned off`() {
        whenever(buildConfigWrapper.isDebugSettingsEnabled()).thenReturn(false)

        val hasManualSetup = manualFeatureConfig.hasManualSetup(featureKey)

        verifyNoInteractions(appPrefsWrapper)
        assertThat(hasManualSetup).isFalse()
    }

    @Test
    fun `isManuallyEnabled is true for local feature config`() {
        whenever(appPrefsWrapper.getManualFeatureConfig("LocalFeatureConfig")).thenReturn(true)

        val isManuallyEnabled = manualFeatureConfig.isManuallyEnabled(localFeatureConfig)

        assertThat(isManuallyEnabled).isTrue()
    }

    @Test
    fun `isManuallyEnabled is true for remote feature config`() {
        whenever(appPrefsWrapper.getManualFeatureConfig(remoteField)).thenReturn(true)

        val isManuallyEnabled = manualFeatureConfig.isManuallyEnabled(remoteFeatureConfig)

        assertThat(isManuallyEnabled).isTrue()
    }

    @Test
    fun `isManuallyEnabled is true for feature key`() {
        whenever(appPrefsWrapper.getManualFeatureConfig(featureKey)).thenReturn(true)

        val isManuallyEnabled = manualFeatureConfig.isManuallyEnabled(featureKey)

        assertThat(isManuallyEnabled).isTrue()
    }

    @Test
    fun `isManuallyEnabled does not call appPrefsWrapper when flag is turned off`() {
        whenever(buildConfigWrapper.isDebugSettingsEnabled()).thenReturn(false)

        val isManuallyEnabled = manualFeatureConfig.isManuallyEnabled(featureKey)

        verifyNoInteractions(appPrefsWrapper)
        assertThat(isManuallyEnabled).isFalse()
    }
}

class LocalFeatureConfig(appConfig: AppConfig) : FeatureConfig(appConfig, true, null)
