package org.wordpress.android.util.config

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.MySiteImprovementsFeatureConfig.Companion.MY_SITE_IMPROVEMENTS_NO_ACCOUNT_FIELD
import org.wordpress.android.util.config.MySiteImprovementsFeatureConfig.Companion.MY_SITE_IMPROVEMENTS_REMOTE_FIELD

@RunWith(MockitoJUnitRunner::class)
class MySiteImprovementsFeatureConfigTest {
    @Mock lateinit var appConfig: AppConfig
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var accountModel: AccountModel
    @Mock lateinit var analyticsTracker: AnalyticsTrackerWrapper
    private lateinit var mySiteImprovementsFeatureConfig: MySiteImprovementsFeatureConfig

    @Before
    fun setUp() {
        mySiteImprovementsFeatureConfig = MySiteImprovementsFeatureConfig(appConfig, accountStore, analyticsTracker)
        whenever(accountStore.account).thenReturn(accountModel)
    }

    @Test
    fun `returns feature enabled if user ID is even`() {
        whenever(accountModel.userId).thenReturn(2)

        val enabled = mySiteImprovementsFeatureConfig.isEnabled()

        assertThat(enabled).isTrue()
        verify(analyticsTracker).track(
                Stat.FEATURE_FLAG_SET,
                mapOf(MY_SITE_IMPROVEMENTS_REMOTE_FIELD to true, "user_id_set" to true)
        )
    }

    @Test
    fun `returns feature disabled if user ID is odd`() {
        whenever(accountModel.userId).thenReturn(3)

        val enabled = mySiteImprovementsFeatureConfig.isEnabled()

        assertThat(enabled).isFalse()
        verify(analyticsTracker).track(
                Stat.FEATURE_FLAG_SET,
                mapOf(MY_SITE_IMPROVEMENTS_REMOTE_FIELD to false, "user_id_set" to true)
        )
    }

    @Test
    fun `returns true value when user ID is 0`() {
        whenever(accountModel.userId).thenReturn(0)

        val enabled = mySiteImprovementsFeatureConfig.isEnabled()

        assertThat(enabled).isTrue()
        verify(analyticsTracker).track(
                Stat.FEATURE_FLAG_SET,
                mapOf(MY_SITE_IMPROVEMENTS_REMOTE_FIELD to true, "user_id_set" to false)
        )
    }

    @Test
    fun `returns MY_SITE_IMPROVEMENTS_NO_ACCOUNT_FIELD before last check`() {
        val name = mySiteImprovementsFeatureConfig.name()

        assertThat(name).isEqualTo(MY_SITE_IMPROVEMENTS_NO_ACCOUNT_FIELD)
    }

    @Test
    fun `returns MY_SITE_IMPROVEMENTS_NO_ACCOUNT_FIELD as name when user ID is 0 during last check`() {
        whenever(accountModel.userId).thenReturn(0)
        mySiteImprovementsFeatureConfig.isEnabled()

        val name = mySiteImprovementsFeatureConfig.name()

        assertThat(name).isEqualTo(MY_SITE_IMPROVEMENTS_NO_ACCOUNT_FIELD)
    }

    @Test
    fun `returns MY_SITE_IMPROVEMENTS_REMOTE_FIELD as name when user ID is not 0 during last check`() {
        whenever(accountModel.userId).thenReturn(1)
        mySiteImprovementsFeatureConfig.isEnabled()

        val name = mySiteImprovementsFeatureConfig.name()

        assertThat(name).isEqualTo(MY_SITE_IMPROVEMENTS_REMOTE_FIELD)
    }
}
