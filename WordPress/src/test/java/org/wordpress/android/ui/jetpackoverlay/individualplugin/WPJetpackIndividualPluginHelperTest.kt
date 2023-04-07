package org.wordpress.android.ui.jetpackoverlay.individualplugin

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.persistence.JetpackCPConnectedSiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.config.AppConfig
import org.wordpress.android.util.config.WPIndividualPluginOverlayFeatureConfig
import org.wordpress.android.util.config.WPIndividualPluginOverlayMaxShownConfig
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class WPJetpackIndividualPluginHelperTest : BaseUnitTest() {
    @Mock
    lateinit var siteStore: SiteStore

    @Mock
    lateinit var appPrefs: AppPrefsWrapper

    @Mock
    lateinit var wpIndividualPluginOverlayFeatureConfig: WPIndividualPluginOverlayFeatureConfig

    @Mock
    lateinit var appConfig: AppConfig

    private lateinit var wpIndividualPluginOverlayMaxShownConfig: WPIndividualPluginOverlayMaxShownConfig

    private lateinit var helper: WPJetpackIndividualPluginHelper

    @Before
    fun setUp() {
        wpIndividualPluginOverlayMaxShownConfig = WPIndividualPluginOverlayMaxShownConfig(appConfig)
        helper = WPJetpackIndividualPluginHelper(
            siteStore,
            appPrefs,
            wpIndividualPluginOverlayFeatureConfig,
            wpIndividualPluginOverlayMaxShownConfig,
        )

        whenever(appConfig.getRemoteFieldConfigValue(any())).thenReturn("3")
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
    fun `GIVEN first time WHEN shouldShowJetpackIndividualPluginOverlay THEN returns true`() =
        test {
            mockBaseConditionsForShowingOverlay()

            // 1st time: should return true
            whenever(appPrefs.wpJetpackIndividualPluginOverlayShownCount).thenReturn(0)
            whenever(appPrefs.wpJetpackIndividualPluginOverlayLastShownTimestamp).thenReturn(0)

            assertThat(helper.shouldShowJetpackIndividualPluginOverlay()).isTrue
        }

    @Test
    fun `GIVEN second time, after less than a day WHEN shouldShowJetpackIndividualPluginOverlay THEN returns false`() =
        test {
            mockBaseConditionsForShowingOverlay()

            // 2nd time, after less than a day: should return false
            whenever(appPrefs.wpJetpackIndividualPluginOverlayShownCount).thenReturn(1)
            whenever(appPrefs.wpJetpackIndividualPluginOverlayLastShownTimestamp).thenReturn(timeFor(hoursAgo = 20))

            assertThat(helper.shouldShowJetpackIndividualPluginOverlay()).isFalse
        }

    @Test
    fun `GIVEN second time, after more than a day WHEN shouldShowJetpackIndividualPluginOverlay THEN returns true`() =
        test {
            mockBaseConditionsForShowingOverlay()

            // 2nd time, after more than a day: should return true
            whenever(appPrefs.wpJetpackIndividualPluginOverlayShownCount).thenReturn(1)
            whenever(appPrefs.wpJetpackIndividualPluginOverlayLastShownTimestamp).thenReturn(timeFor(daysAgo = 1))

            assertThat(helper.shouldShowJetpackIndividualPluginOverlay()).isTrue
        }

    @Test
    fun `GIVEN third time, after less than 3 days WHEN shouldShowJetpackIndividualPluginOverlay THEN returns false`() =
        test {
            mockBaseConditionsForShowingOverlay()

            // 3rd time, after less than 3 days: should return false
            whenever(appPrefs.wpJetpackIndividualPluginOverlayShownCount).thenReturn(2)
            whenever(appPrefs.wpJetpackIndividualPluginOverlayLastShownTimestamp).thenReturn(timeFor(daysAgo = 2))

            assertThat(helper.shouldShowJetpackIndividualPluginOverlay()).isFalse
        }

    @Test
    fun `GIVEN third time, after more than 3 days WHEN shouldShowJetpackIndividualPluginOverlay THEN returns true`() =
        test {
            mockBaseConditionsForShowingOverlay()

            // 3rd time, after more than 3 days: should return true
            whenever(appPrefs.wpJetpackIndividualPluginOverlayShownCount).thenReturn(2)
            whenever(appPrefs.wpJetpackIndividualPluginOverlayLastShownTimestamp).thenReturn(timeFor(daysAgo = 4))

            assertThat(helper.shouldShowJetpackIndividualPluginOverlay()).isTrue
        }

    @Test
    fun `GIVEN fourth time WHEN shouldShowJetpackIndividualPluginOverlay THEN returns false`() =
        test {
            mockBaseConditionsForShowingOverlay()

            // 4th time, after many days: should return false
            whenever(appPrefs.wpJetpackIndividualPluginOverlayShownCount).thenReturn(3)

            assertThat(helper.shouldShowJetpackIndividualPluginOverlay()).isFalse
        }

    @Test
    fun `GIVEN no problem sites WHEN getJetpackConnectedSitesWithIndividualPlugins THEN return empty list`() = test {
        whenever(siteStore.getJetpackCPConnectedSites()).thenReturn(emptyList())

        assertThat(helper.getJetpackConnectedSitesWithIndividualPlugins()).isEmpty()
    }

    @Test
    fun `GIVEN has problem sites WHEN getJetpackConnectedSitesWithIndividualPlugins THEN return list of sites`() =
        test {
            val connectedSites = listOf(
                jetpackCPConnectedSiteModel(
                    name = "site1",
                    url = "https://site1.com",
                    activeJpPlugins = "jetpack-social"
                ),
                jetpackCPConnectedSiteModel(
                    name = "site2",
                    url = "https://site2.com",
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

    @Test
    fun `WHEN onJetpackIndividualPluginOverlayShown THEN app prefs is called to increment count`() {
        helper.onJetpackIndividualPluginOverlayShown()

        verify(appPrefs).incrementWPJetpackIndividualPluginOverlayShownCount()
    }

    @Test
    fun `WHEN onJetpackIndividualPluginOverlayShown THEN app prefs is called to update timestamp`() {
        helper.onJetpackIndividualPluginOverlayShown()

        verify(appPrefs).wpJetpackIndividualPluginOverlayLastShownTimestamp = any()
    }

    private suspend fun mockBaseConditionsForShowingOverlay() {
        whenever(wpIndividualPluginOverlayFeatureConfig.isEnabled()).thenReturn(true)
        val connectedSites = listOf(
            jetpackCPConnectedSiteModel(
                name = "site1",
                url = "https://site1.com",
                activeJpPlugins = "jetpack-social"
            ),
            jetpackCPConnectedSiteModel(
                name = "site2",
                url = "https://site2.com",
                activeJpPlugins = "other-plugin"
            )
        )
        whenever(siteStore.getJetpackCPConnectedSites()).thenReturn(connectedSites)
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

    private fun timeFor(daysAgo: Int = 0, hoursAgo: Int = 0): Long {
        val calendar = Calendar.getInstance()
        // subtract a minute to make sure the time is a bit longer ago than the current time
        calendar.add(Calendar.MINUTE, -1)
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        calendar.add(Calendar.HOUR_OF_DAY, -hoursAgo)
        return calendar.timeInMillis
    }
}
