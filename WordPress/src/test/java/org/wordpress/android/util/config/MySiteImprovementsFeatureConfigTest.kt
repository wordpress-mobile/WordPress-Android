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
import org.wordpress.android.util.config.MySiteImprovementsFeatureConfig.Companion.MY_SITE_IMPROVEMENTS_NO_ACCOUNT_FIELD
import org.wordpress.android.util.config.MySiteImprovementsFeatureConfig.Companion.MY_SITE_IMPROVEMENTS_REMOTE_FIELD

@RunWith(MockitoJUnitRunner::class)
class MySiteImprovementsFeatureConfigTest {
    @Mock lateinit var appConfig: AppConfig
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var analyticsTracker: AnalyticsTrackerWrapper
    private lateinit var mySiteImprovementsFeatureConfig: MySiteImprovementsFeatureConfig
    private val evenToken = "0"
    private val oddToken = "1"

    @Before
    fun setUp() {
        mySiteImprovementsFeatureConfig = MySiteImprovementsFeatureConfig(appConfig, accountStore, analyticsTracker)
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
    fun `returns feature enabled if token hashcode is even`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.accessToken).thenReturn(evenToken)

        val enabled = mySiteImprovementsFeatureConfig.isEnabled()

        assertThat(enabled).isTrue()
        verify(analyticsTracker).track(
                Stat.FEATURE_FLAG_SET,
                mapOf(MY_SITE_IMPROVEMENTS_REMOTE_FIELD to true, "source" to "TOKEN")
        )
    }

    @Test
    fun `returns feature disabled if token hashcode is odd`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.accessToken).thenReturn(oddToken)

        val enabled = mySiteImprovementsFeatureConfig.isEnabled()

        assertThat(enabled).isFalse()
        verify(analyticsTracker).track(
                Stat.FEATURE_FLAG_SET,
                mapOf(MY_SITE_IMPROVEMENTS_REMOTE_FIELD to false, "source" to "TOKEN")
        )
    }

    @Test
    fun `returns true value when token is missing`() {
        initEmptyToken()

        val enabled = mySiteImprovementsFeatureConfig.isEnabled()

        assertThat(enabled).isTrue()
        verify(analyticsTracker).track(
                Stat.FEATURE_FLAG_SET,
                mapOf(MY_SITE_IMPROVEMENTS_REMOTE_FIELD to true, "source" to "DEFAULT")
        )
    }

    @Test
    fun `returns MY_SITE_IMPROVEMENTS_NO_ACCOUNT_FIELD before last check`() {
        val name = mySiteImprovementsFeatureConfig.name()

        assertThat(name).isEqualTo(MY_SITE_IMPROVEMENTS_NO_ACCOUNT_FIELD)
    }

    @Test
    fun `returns MY_SITE_IMPROVEMENTS_NO_ACCOUNT_FIELD as name when auth token is missing during last check`() {
        initEmptyToken()
        mySiteImprovementsFeatureConfig.isEnabled()

        val name = mySiteImprovementsFeatureConfig.name()

        assertThat(name).isEqualTo(MY_SITE_IMPROVEMENTS_NO_ACCOUNT_FIELD)
    }

    @Test
    fun `returns MY_SITE_IMPROVEMENTS_REMOTE_FIELD as name when auth token set during last check`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.accessToken).thenReturn(oddToken)
        mySiteImprovementsFeatureConfig.isEnabled()

        val name = mySiteImprovementsFeatureConfig.name()

        assertThat(name).isEqualTo(MY_SITE_IMPROVEMENTS_REMOTE_FIELD)
    }

    @Test
    fun `returns feature enabled if init from URI and auth token not yet set`() {
        val uri = mock<UriWrapper>()
        initEmptyToken()
        whenever(uri.getQueryParameter(LoginActivity.TOKEN_PARAMETER)).thenReturn(evenToken)
        mySiteImprovementsFeatureConfig.initFromUri(uri)
        verify(analyticsTracker).track(
                Stat.FEATURE_FLAG_SET,
                mapOf(MY_SITE_IMPROVEMENTS_REMOTE_FIELD to true, "source" to "TOKEN")
        )

        val enabled = mySiteImprovementsFeatureConfig.isEnabled()

        assertThat(enabled).isTrue()
    }

    @Test
    fun `returns feature disabled if init from URI and auth token not yet set`() {
        val uri = mock<UriWrapper>()
        initEmptyToken()
        whenever(uri.getQueryParameter(LoginActivity.TOKEN_PARAMETER)).thenReturn(oddToken)
        mySiteImprovementsFeatureConfig.initFromUri(uri)
        verify(analyticsTracker).track(
                Stat.FEATURE_FLAG_SET,
                mapOf(MY_SITE_IMPROVEMENTS_REMOTE_FIELD to false, "source" to "TOKEN")
        )

        val enabled = mySiteImprovementsFeatureConfig.isEnabled()

        assertThat(enabled).isFalse()
    }

    private fun initEmptyToken() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(accountStore.accessToken).thenReturn("")
    }
}
