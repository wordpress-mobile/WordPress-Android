package org.wordpress.android.ui.mysite.cards

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.JetpackInstallFullPluginCard
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.jpfullplugininstall.JetpackInstallFullPluginCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.jpfullplugininstall.JetpackInstallFullPluginShownTracker
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class JetpackInstallFullPluginCardViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Mock
    lateinit var jetpackInstallFullPluginShownTracker: JetpackInstallFullPluginShownTracker


    private lateinit var uiModels: MutableList<JetpackInstallFullPluginCard?>

    private lateinit var viewModel: JetpackInstallFullPluginCardViewModelSlice

    private val individualPluginsInput =
        "jetpack-search,jetpack-backup,jetpack-protect,jetpack-videopress,jetpack-social,jetpack-boost"

    private val individualPluginsOutput = listOf(
        "Jetpack Search",
        "Jetpack VaultPress Backup",
        "Jetpack Protect",
        "Jetpack VideoPress",
        "Jetpack Social",
        "Jetpack Boost")

    private val fullPlugin = "jetpack"

    @Before
    fun setUp() {
        viewModel = JetpackInstallFullPluginCardViewModelSlice(
            appPrefsWrapper,
            selectedSiteRepository,
            analyticsTrackerWrapper,
            jetpackInstallFullPluginShownTracker
        )
        uiModels = mutableListOf()
        viewModel.uiModel.observeForever {  event ->
            uiModels.add(event)
        }
    }

    @Test
    fun `given individual plugin conditions are met, when build card is invoked, then card is built`() {
        val site = mock<SiteModel> {
            on { id } doReturn 1
            on { name } doReturn "Test Site"
            on { activeJetpackConnectionPlugins } doReturn individualPluginsInput
        }

        whenever(appPrefsWrapper.getShouldHideJetpackInstallFullPluginCard(any())).thenReturn(false)

        viewModel.buildCard(site)

        val uiModel = uiModels.last() as JetpackInstallFullPluginCard
        assertThat(uiModel.siteName).isEqualTo("Test Site")
        assertTrue(
            "All values in pluginNames must be in expected list",
            uiModel.pluginNames.all { individualPluginsOutput.contains(it) })
    }

    @Test
    fun `given full plugin conditions are met, when build card is invoked, then card is not built`() {
        val site = mock<SiteModel> {
            on { id } doReturn 1
            on { activeJetpackConnectionPlugins } doReturn fullPlugin
        }

        whenever(appPrefsWrapper.getShouldHideJetpackInstallFullPluginCard(any())).thenReturn(false)

        viewModel.buildCard(site)

        assertThat(uiModels.last()).isNull()
    }

    @Test
    fun `given site id is 0, when build card is invoked, then card is not built`() {
        val site = mock<SiteModel> {
            on { id } doReturn 0
        }

        viewModel.buildCard(site)
    }

    @Test
    fun `given card is hidden, when build card is invoked, then card is not built`() {
        val site = mock<SiteModel> {
            on { id } doReturn 1
        }

        whenever(appPrefsWrapper.getShouldHideJetpackInstallFullPluginCard(any())).thenReturn(true)

        viewModel.buildCard(site)

        assertThat(uiModels.last()).isNull()
    }
}
