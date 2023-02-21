package org.wordpress.android.ui.jpfullplugininstall.onboarding

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.ui.jpfullplugininstall.onboarding.JetpackFullPluginInstallOnboardingViewModel.ActionEvent
import org.wordpress.android.ui.jpfullplugininstall.onboarding.JetpackFullPluginInstallOnboardingViewModel.UiState
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.prefs.AppPrefsWrapper

@ExperimentalCoroutinesApi
class JetpackFullPluginInstallOnboardingViewModelTest : BaseUnitTest() {
    private val selectedSiteRepository: SelectedSiteRepository = mock()
    private val uiStateMapper: JetpackFullPluginInstallOnboardingUiStateMapper = mock()
    private val analyticsTracker: JetpackFullPluginInstallOnboardingAnalyticsTracker = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()

    private val siteName = "Site Name"
    private val pluginNames = listOf("jetpack-search", "jetpack-backup")
    private val selectedSite = SiteModel().apply {
        id = 1
        name = siteName
        activeJetpackConnectionPlugins = pluginNames.joinToString(",")
    }
    private val loadedUiState = UiState.Loaded(
        siteName = siteName,
        pluginNames = pluginNames,
    )
    private val classToTest = JetpackFullPluginInstallOnboardingViewModel(
        uiStateMapper = uiStateMapper,
        selectedSiteRepository = selectedSiteRepository,
        analyticsTracker = analyticsTracker,
        appPrefsWrapper = appPrefsWrapper,
        bgDispatcher = testDispatcher(),
    )

    @Test
    fun `Should have UI state initial value as None`() {
        assertThat(classToTest.uiState.value).isEqualTo(UiState.None)
    }

    @Test
    fun `Should post Loaded UI state when onScreenShown is called`() {
        mockUiStateMapper()
        classToTest.onScreenShown()
        val loadedUiState = classToTest.uiState.value as UiState.Loaded
        assertThat(loadedUiState.siteName).isEqualTo(siteName)
        assertThat(loadedUiState.pluginNames).isEqualTo(pluginNames)
    }

    @Test
    fun `Should update show onboarding flag when onScreenShown is called`() {
        mockSelectedSite()
        classToTest.onScreenShown()
        verify(appPrefsWrapper).setShouldShowJetpackInstallOnboarding(selectedSite.id, false)
    }

    @Test
    fun `Should call JetpackFullPluginInstallOnboardingUiStateMapper#mapLoaded when onScreenShown is called`() {
        classToTest.onScreenShown()
        verify(uiStateMapper).mapLoaded()
    }

    @Test
    fun `Should post action OpenTermsAndConditions when onTermsAndConditionsClick is called`() = test {
        val result = ArrayList<ActionEvent>()
        val job = launch {
            classToTest.actionEvents.collectLatest {
                result.add(it)
            }
        }
        classToTest.onTermsAndConditionsClick()
        assertThat(result.first()).isEqualTo(ActionEvent.OpenTermsAndConditions)
        job.cancel()
    }

    @Test
    fun `Should post action ContactSupport when onContactSupportClick is called`() = test {
        mockSelectedSite()
        val result = ArrayList<ActionEvent>()
        val job = launch {
            classToTest.actionEvents.collectLatest {
                result.add(it)
            }
        }
        classToTest.onContactSupportClick()
        assertThat(result.first()).isEqualTo(
            ActionEvent.ContactSupport(
                origin = HelpActivity.Origin.JETPACK_INSTALL_FULL_PLUGIN_ONBOARDING,
                selectedSite = selectedSite,
            )
        )
        job.cancel()
    }

    @Test
    fun `Should post action Dismiss when onDismissScreen is called`() = test {
        mockSelectedSite()
        val result = ArrayList<ActionEvent>()
        val job = launch {
            classToTest.actionEvents.collectLatest {
                result.add(it)
            }
        }
        classToTest.onDismissScreenClick()
        assertThat(result.first()).isEqualTo(ActionEvent.Dismiss)
        job.cancel()
    }

    @Test
    fun `Should track install button click when onInstallFullPluginClick is called`() {
        classToTest.onInstallFullPluginClick()
        verify(analyticsTracker).trackInstallButtonClick()
    }

    @Test
    fun `Should track dismiss screen click when onDismissScreenClick is called`() {
        classToTest.onDismissScreenClick()
        verify(analyticsTracker).trackScreenDismissed()
    }

    @Test
    fun `Should track screen shown when onScreenShown is called`() {
        classToTest.onScreenShown()
        verify(analyticsTracker).trackScreenShown()
    }

    private fun mockSelectedSite() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(selectedSite)
    }

    private fun mockUiStateMapper() {
        whenever(uiStateMapper.mapLoaded()).thenReturn(loadedUiState)
    }
}
