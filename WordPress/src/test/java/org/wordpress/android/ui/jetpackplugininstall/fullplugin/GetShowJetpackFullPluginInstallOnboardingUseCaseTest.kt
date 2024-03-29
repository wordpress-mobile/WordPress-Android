package org.wordpress.android.ui.jetpackplugininstall.fullplugin

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.prefs.AppPrefsWrapper

class GetShowJetpackFullPluginInstallOnboardingUseCaseTest {
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val classToTest = GetShowJetpackFullPluginInstallOnboardingUseCase(
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
    fun `Should return FALSE if Jetpack is connected with full plugin`() {
        val actual = classToTest.execute(SiteModel().apply { activeJetpackConnectionPlugins = "jetpack" })
        val expected = false
        assertThat(actual).isEqualTo(expected)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Should return TRUE if site ID is not 0, should show onboarding and JP is not connected with full plugin`() {
        val siteModel = SiteModel().apply {
            id = 1
            activeJetpackConnectionPlugins = "jetpack-something"
        }
        whenever(appPrefsWrapper.getShouldShowJetpackInstallOnboarding(siteModel.id)).thenReturn(true)
        val actual = classToTest.execute(siteModel)
        val expected = true
        assertThat(actual).isEqualTo(expected)
    }
}
