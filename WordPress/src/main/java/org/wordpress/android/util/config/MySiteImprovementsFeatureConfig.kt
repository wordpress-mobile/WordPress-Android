package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

/**
 * Configuration of the my site infrastructure improvements
 */
class MySiteImprovementsFeatureConfig
@Inject constructor(
    appConfig: AppConfig,
    private val accountStore: AccountStore
) : FeatureConfig(
        appConfig,
        BuildConfig.MY_SITE_IMPROVEMENTS,
        MY_SITE_IMPROVEMENTS_REMOTE_FIELD
) {
    // Temporary solution until the Firebase bug gets fixed
    override fun isEnabled(): Boolean {
        val userId = accountStore.account.userId
        return if (userId != 0L) {
            userId % 2L == 0L
        } else {
            true
        }
    }

    override fun name(): String {
        val userId = accountStore.account.userId
        return if (userId != 0L) {
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
