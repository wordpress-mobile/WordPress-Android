package org.wordpress.android.ui.jpfullplugininstall

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.jpfullplugininstall.onboarding.JetpackFullPluginInstallOnboardingUiStateMapper
import org.wordpress.android.ui.jpfullplugininstall.onboarding.JetpackFullPluginInstallOnboardingViewModel.UiState
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class JetpackFullPluginInstallOnboardingUiStateMapperTest : BaseUnitTest() {
    private val selectedSiteRepository: SelectedSiteRepository = mock()
    private val selectedSiteName = "Site name"
    private val selectedPluginNames = "jetpack-search,jetpack-backup"
    private val selectedSiteModel: SiteModel = SiteModel().apply {
        name = selectedSiteName
        activeJetpackConnectionPlugins = selectedPluginNames
    }
    private val classToTest = JetpackFullPluginInstallOnboardingUiStateMapper(
        selectedSiteRepository = selectedSiteRepository
    )

    @Test
    fun `Should return correct Loaded state when mapLoaded is called`() {
        mockSelectedSite()
        val expected = UiState.Loaded(
            siteName = selectedSiteName,
            pluginNames = listOf("Jetpack Search", "Jetpack VaultPress Backup")
        )
        val actual = classToTest.mapLoaded()
        assertEquals(expected, actual)
    }

    private fun mockSelectedSite() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(selectedSiteModel)
    }
}
