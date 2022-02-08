package org.wordpress.android.util.config

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.accounts.LoginActivity
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.MySiteDashboardStatsCardFeatureConfig.Companion.MY_SITE_DASHBOARD_STATS_CARD_NO_ACCOUNT_FIELD
import org.wordpress.android.util.config.MySiteDashboardStatsCardFeatureConfig.Companion.MY_SITE_DASHBOARD_STATS_CARD_REMOTE_FIELD

@RunWith(MockitoJUnitRunner::class)
class MySiteDashboardStatsCardFeatureConfigTest {
    @Mock lateinit var appConfig: AppConfig
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var analyticsTracker: AnalyticsTrackerWrapper
    private lateinit var mySiteDashboardStatsCardFeatureConfig: MySiteDashboardStatsCardFeatureConfig
    private val evenToken = "0"
    private val oddToken = "1"

    @Before
    fun setUp() {
        mySiteDashboardStatsCardFeatureConfig = MySiteDashboardStatsCardFeatureConfig(
                appConfig,
                accountStore,
                analyticsTracker
        )
    }

    @Test
    fun `evenToken hashcode is even`() {
        assertThat(evenToken.hashCode() % 2).isEqualTo(0)
    }

    @Test
    fun `oddToken hashcode is odd`() {
        assertThat(oddToken.hashCode() % 2).isNotEqualTo(0)
    }

    @Test
    fun `given even token hashcode, when feature enabled status checked, then feature is enabled`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.accessToken).thenReturn(evenToken)

        val enabled = mySiteDashboardStatsCardFeatureConfig.isEnabled()

        assertThat(enabled).isTrue
        verify(analyticsTracker).track(
                Stat.FEATURE_FLAG_VALUE,
                mapOf(MY_SITE_DASHBOARD_STATS_CARD_REMOTE_FIELD to true, "source" to "TOKEN")
        )
    }

    @Test
    fun `given odd token hashcode, when feature enabled status checked, then feature is disabled`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.accessToken).thenReturn(oddToken)

        val enabled = mySiteDashboardStatsCardFeatureConfig.isEnabled()

        assertThat(enabled).isFalse
        verify(analyticsTracker).track(
                Stat.FEATURE_FLAG_VALUE,
                mapOf(MY_SITE_DASHBOARD_STATS_CARD_REMOTE_FIELD to false, "source" to "TOKEN")
        )
    }

    @Test
    fun `given missing token, when feature enabled status checked, then feature is enabled`() {
        initEmptyToken()

        val enabled = mySiteDashboardStatsCardFeatureConfig.isEnabled()

        assertThat(enabled).isTrue
        verify(analyticsTracker).track(
                Stat.FEATURE_FLAG_VALUE,
                mapOf(MY_SITE_DASHBOARD_STATS_CARD_REMOTE_FIELD to true, "source" to "DEFAULT")
        )
    }

    @Test
    fun `when name checked before last check, then returns no account name`() {
        val name = mySiteDashboardStatsCardFeatureConfig.name()

        assertThat(name).isEqualTo(MY_SITE_DASHBOARD_STATS_CARD_NO_ACCOUNT_FIELD)
    }

    @Test
    fun `given missing auth token during last check, when name checked, then returns no account name`() {
        initEmptyToken()
        mySiteDashboardStatsCardFeatureConfig.isEnabled()

        val name = mySiteDashboardStatsCardFeatureConfig.name()

        assertThat(name).isEqualTo(MY_SITE_DASHBOARD_STATS_CARD_NO_ACCOUNT_FIELD)
    }

    @Test
    fun `given auth token set during last check, when name checked, then returns remote field name`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.accessToken).thenReturn(oddToken)
        mySiteDashboardStatsCardFeatureConfig.isEnabled()

        val name = mySiteDashboardStatsCardFeatureConfig.name()

        assertThat(name).isEqualTo(MY_SITE_DASHBOARD_STATS_CARD_REMOTE_FIELD)
    }

    @Test
    fun `given even token hashcode, when init from URI, then returns feature enabled`() {
        val uri = mock<UriWrapper>()
        initEmptyToken()
        whenever(uri.getQueryParameter(LoginActivity.TOKEN_PARAMETER)).thenReturn(evenToken)
        mySiteDashboardStatsCardFeatureConfig.initFromUri(uri)
        verify(analyticsTracker).track(
                Stat.FEATURE_FLAG_VALUE,
                mapOf(MY_SITE_DASHBOARD_STATS_CARD_REMOTE_FIELD to true, "source" to "TOKEN")
        )

        val enabled = mySiteDashboardStatsCardFeatureConfig.isEnabled()

        assertThat(enabled).isTrue
    }

    @Test
    fun `given odd token hashcode, when init from URI, then returns feature disabled`() {
        val uri = mock<UriWrapper>()
        initEmptyToken()
        whenever(uri.getQueryParameter(LoginActivity.TOKEN_PARAMETER)).thenReturn(oddToken)
        mySiteDashboardStatsCardFeatureConfig.initFromUri(uri)
        verify(analyticsTracker).track(
                Stat.FEATURE_FLAG_VALUE,
                mapOf(MY_SITE_DASHBOARD_STATS_CARD_REMOTE_FIELD to false, "source" to "TOKEN")
        )

        val enabled = mySiteDashboardStatsCardFeatureConfig.isEnabled()

        assertThat(enabled).isFalse
    }

    private fun initEmptyToken() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(accountStore.accessToken).thenReturn("")
    }
}
