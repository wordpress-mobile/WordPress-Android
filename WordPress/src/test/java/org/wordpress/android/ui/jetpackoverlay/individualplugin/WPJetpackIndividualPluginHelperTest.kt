package org.wordpress.android.ui.jetpackoverlay.individualplugin

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.persistence.JetpackCPConnectedSiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.config.WPIndividualPluginOverlayFeatureConfig

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class WPJetpackIndividualPluginHelperTest : BaseUnitTest() {
    @Mock
    lateinit var siteStore: SiteStore

    @Mock
    lateinit var wpIndividualPluginOverlayFeatureConfig: WPIndividualPluginOverlayFeatureConfig

    lateinit var helper: WPJetpackIndividualPluginHelper

    @Before
    fun setUp() {
        helper = WPJetpackIndividualPluginHelper(siteStore, wpIndividualPluginOverlayFeatureConfig)
    }

    @Test
    fun `GIVEN config is off WHEN shouldShowJetpackIndividualPluginOverlay THEN return false`() =
        test {
            whenever(wpIndividualPluginOverlayFeatureConfig.isEnabled()).thenReturn(false)

            assertThat(helper.shouldShowJetpackIndividualPluginOverlay()).isFalse
        }

    @Test
    fun `GIVEN config is on and no problem sites WHEN shouldShowJetpackIndividualPluginOverlay THEN return false`() =
        test {
            whenever(wpIndividualPluginOverlayFeatureConfig.isEnabled()).thenReturn(true)
            whenever(siteStore.getJetpackCPConnectedSites()).thenReturn(emptyList())

            assertThat(helper.shouldShowJetpackIndividualPluginOverlay()).isFalse
        }

    @Test
    fun `GIVEN config is on and has problem sites WHEN shouldShowJetpackIndividualPluginOverlay THEN return true`() =
        test {
            whenever(wpIndividualPluginOverlayFeatureConfig.isEnabled()).thenReturn(true)
            val connectedSites = listOf(
                jetpackCPConnectedSiteModel(
                    name = "site1",
                    url = "site1.com",
                    activeJpPlugins = "jetpack-social"
                ),
                jetpackCPConnectedSiteModel(
                    name = "site2",
                    url = "site2.com",
                    activeJpPlugins = "other-plugin"
                )
            )
            whenever(siteStore.getJetpackCPConnectedSites()).thenReturn(connectedSites)

            assertThat(helper.shouldShowJetpackIndividualPluginOverlay()).isTrue
        }

    @Test
    fun `GIVEN no problem sites WHEN getJetpackConnectedSitesWithIndividualPlugins THEN return empty list`() = test {
        whenever(siteStore.getJetpackCPConnectedSites()).thenReturn(emptyList())

        assertThat(helper.getJetpackConnectedSitesWithIndividualPlugins()).isEmpty()
    }

    @Test
    fun `GIVEN has problem sites WHEN getJetpackConnectedSitesWithIndividualPlugins THEN list of sites`() = test {
        val connectedSites = listOf(
            jetpackCPConnectedSiteModel(
                name = "site1",
                url = "site1.com",
                activeJpPlugins = "jetpack-social"
            ),
            jetpackCPConnectedSiteModel(
                name = "site2",
                url = "site2.com",
                activeJpPlugins = "other-plugin"
            )
        )
        whenever(siteStore.getJetpackCPConnectedSites()).thenReturn(connectedSites)

        val sites = helper.getJetpackConnectedSitesWithIndividualPlugins()
        assertThat(sites).hasSize(1)
        assertThat(sites[0].name).isEqualTo("site1")
        assertThat(sites[0].url).isEqualTo("site1.com")
        assertThat(sites[0].individualPluginNames).hasSize(1)
        assertThat(sites[0].individualPluginNames[0]).isEqualTo("Jetpack Social")
    }

    private fun jetpackCPConnectedSiteModel(name: String, url: String, activeJpPlugins: String?) =
        JetpackCPConnectedSiteModel(
            remoteSiteId = null,
            localSiteId = 0,
            url = url,
            name = name,
            description = "description",
            activeJetpackConnectionPlugins = activeJpPlugins?.split(",")?.toList() ?: listOf()
        )
}
