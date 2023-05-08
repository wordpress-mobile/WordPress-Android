package org.wordpress.android.ui.mysite.cards.dashboard.activity

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.config.DashboardCardActivityLogConfig

@ExperimentalCoroutinesApi
class DashboardActivityLogCardFeatureUtilsTest : BaseUnitTest() {
    @Mock
    private lateinit var dashboardCardActivityLogConfig: DashboardCardActivityLogConfig

    @Mock
    private lateinit var siteUtilsWrapper: SiteUtilsWrapper

    @Mock
    private lateinit var siteModel: SiteModel

    private lateinit var dashboardActivityLogCardFeatureUtils: DashboardActivityLogCardFeatureUtils

    @Before
    fun setUp() {
        dashboardActivityLogCardFeatureUtils = DashboardActivityLogCardFeatureUtils(
            dashboardCardActivityLogConfig,
            siteUtilsWrapper
        )
        setUpMocks()
    }

    private fun setUpMocks() {
        whenever(dashboardCardActivityLogConfig.isEnabled()).thenReturn(true)
        whenever(siteUtilsWrapper.isAccessedViaWPComRest(siteModel)).thenReturn(true)
        whenever(siteModel.hasCapabilityManageOptions).thenReturn(true)
        whenever(siteModel.isJetpackConnected).thenReturn(true)
        whenever(siteModel.isWpForTeamsSite).thenReturn(false)
    }

    @Test
    fun `given feature flag is disabled, when isEnaled is called, then false is returned`() {
        whenever(dashboardCardActivityLogConfig.isEnabled()).thenReturn(false)

        val result = dashboardActivityLogCardFeatureUtils.shouldRequestActivityCard(siteModel)

        Assertions.assertThat(result).isFalse
    }


    @Test
    fun `given site accessed is not via wpComOrJetpack, when build is called, then null is returned`() {
        whenever(siteUtilsWrapper.isAccessedViaWPComRest(siteModel)).thenReturn(false)
        whenever(siteModel.isJetpackConnected).thenReturn(false)

        val result = dashboardActivityLogCardFeatureUtils.shouldRequestActivityCard(siteModel)

        Assertions.assertThat(result).isFalse
    }

    @Test
    fun `given does not hasCapabilityManageOptions for site, when build is called, then null is returned`() {
        whenever(siteModel.hasCapabilityManageOptions).thenReturn(false)

        val result = dashboardActivityLogCardFeatureUtils.shouldRequestActivityCard(siteModel)

        Assertions.assertThat(result).isFalse
    }

    @Test
    fun `given is wp for teams site, when build is called, then null is returned`() {
        whenever(siteModel.isWpForTeamsSite).thenReturn(true)

        val result = dashboardActivityLogCardFeatureUtils.shouldRequestActivityCard(siteModel)

        Assertions.assertThat(result).isFalse
    }
}
