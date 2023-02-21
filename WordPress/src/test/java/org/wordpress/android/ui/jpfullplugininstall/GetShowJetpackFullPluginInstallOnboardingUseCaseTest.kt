package org.wordpress.android.ui.jpfullplugininstall

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.config.JetpackInstallFullPluginFeatureConfig

class GetShowJetpackFullPluginInstallOnboardingUseCaseTest {
    private val jetpackInstallFullPluginFeatureConfig: JetpackInstallFullPluginFeatureConfig = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val classToTest = GetShowJetpackFullPluginInstallOnboardingUseCase(
        jetpackInstallFullPluginFeatureConfig = jetpackInstallFullPluginFeatureConfig,
        appPrefsWrapper = appPrefsWrapper,
    )

    @Test
    fun `Should return FALSE if site ID is 0`() {
        val siteModel = SiteModel().apply { id = 0 }
        val actual = classToTest.execute(siteModel)
        val expected = false
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should return FALSE if Jetpack install full plugin feature flag is disabled`() {
        whenever(jetpackInstallFullPluginFeatureConfig.isEnabled()).thenReturn(false)
        val actual = classToTest.execute(SiteModel())
        val expected = false
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should return FALSE if Jetpack is connected with full plugin`() {
        val actual = classToTest.execute(SiteModel().apply { activeJetpackConnectionPlugins = "jetpack" })
        val expected = false
        assertThat(actual).isEqualTo(expected)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Should return TRUE if site ID is not 0, feature flag is enabled, should show onboarding and JP is not connected with full plugin`() {
        val siteModel = SiteModel().apply {
            id = 1
            activeJetpackConnectionPlugins = "jetpack-something"
        }
        whenever(jetpackInstallFullPluginFeatureConfig.isEnabled()).thenReturn(true)
        whenever(appPrefsWrapper.getShouldShowJetpackInstallOnboarding(siteModel.id)).thenReturn(true)
        val actual = classToTest.execute(siteModel)
        val expected = true
        assertThat(actual).isEqualTo(expected)
    }
}
