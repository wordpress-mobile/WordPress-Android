package org.wordpress.android.ui.jetpackplugininstall.fullplugin.onboarding

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.jetpackplugininstall.fullplugin.onboarding.JetpackFullPluginInstallOnboardingViewModel.UiState
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class JetpackFullPluginInstallOnboardingUiStateMapperTest : BaseUnitTest() {
    private val selectedSiteRepository: SelectedSiteRepository = mock()
    private val selectedSiteUrl = "wordpress.com"
    private val selectedPluginNames = "jetpack-search,jetpack-backup"
    private val selectedSiteModel: SiteModel = SiteModel().apply {
        url = selectedSiteUrl
        activeJetpackConnectionPlugins = selectedPluginNames
    }
    private val classToTest = JetpackFullPluginInstallOnboardingUiStateMapper(
        selectedSiteRepository = selectedSiteRepository
    )

    @Test
    fun `Should return correct Loaded state when mapLoaded is called`() {
        mockSelectedSite()
        val expected = UiState.Loaded(
            siteUrl = selectedSiteUrl,
            pluginNames = listOf("Jetpack Search", "Jetpack VaultPress Backup")
        )
        val actual = classToTest.mapLoaded()
        assertEquals(expected, actual)
    }

    private fun mockSelectedSite() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(selectedSiteModel)
    }
}
