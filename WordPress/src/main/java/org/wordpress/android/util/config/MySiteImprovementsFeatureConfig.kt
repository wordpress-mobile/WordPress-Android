package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Configuration of the my site infrastructure improvements
 */
@Singleton
class MySiteImprovementsFeatureConfig
@Inject constructor(
    appConfig: AppConfig,
    private val accountStore: AccountStore,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : FeatureConfig(
        appConfig,
        BuildConfig.MY_SITE_IMPROVEMENTS,
        MY_SITE_IMPROVEMENTS_REMOTE_FIELD
) {
    private var cachedEnabledValue: Boolean? = null
    private var cachedHasUserId: Boolean = false

    // Temporary solution until the Firebase bug gets fixed
    override fun isEnabled(): Boolean {
        val userId = accountStore.account.userId
        val hasUserId = userId != 0L
        val isEnabled = if (hasUserId) {
            userId % 2L == 0L
        } else {
            true
        }
        if (cachedEnabledValue != isEnabled || cachedHasUserId != hasUserId) {
            analyticsTracker.track(
                    Stat.FEATURE_FLAG_SET,
                    mapOf(MY_SITE_IMPROVEMENTS_REMOTE_FIELD to isEnabled, "user_id_set" to hasUserId)
            )
            cachedEnabledValue = isEnabled
            cachedHasUserId = hasUserId
        }
        return isEnabled
    }

    override fun name(): String {
        return if (cachedHasUserId) {
            MY_SITE_IMPROVEMENTS_REMOTE_FIELD
        } else {
            MY_SITE_IMPROVEMENTS_NO_ACCOUNT_FIELD
        }
    }

    companion object {
        const val MY_SITE_IMPROVEMENTS_REMOTE_FIELD = "my_site_improvements_random_enabled"
        const val MY_SITE_IMPROVEMENTS_NO_ACCOUNT_FIELD = "my_site_improvements_no_account_enabled"
    }
}
