package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.analytics.AnalyticsTracker.Stat.FEATURE_FLAG_VALUE
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.accounts.LoginActivity
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.MySiteImprovementsFeatureConfig.Source.DEFAULT
import org.wordpress.android.util.config.MySiteImprovementsFeatureConfig.Source.TOKEN
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
    private var cachedSource: Source = DEFAULT

    // Temporary solution until the Firebase bug gets fixed
    override fun isEnabled(): Boolean {
        return initEnabledValue(accountStore.accessToken.orEmpty().hashCode(), accountStore.hasAccessToken())
    }

    private fun initEnabledValue(
        id: Int,
        hasToken: Boolean
    ): Boolean {
        val (source, isEnabled) = when {
            hasToken -> {
                TOKEN to (id % 2L == 0L)
            }
            cachedEnabledValue != null -> {
                cachedSource to (cachedEnabledValue == true)
            }
            else -> {
                DEFAULT to true
            }
        }
        if (cachedEnabledValue != isEnabled || cachedSource != source) {
            analyticsTracker.track(
                    FEATURE_FLAG_VALUE,
                    mapOf(MY_SITE_IMPROVEMENTS_REMOTE_FIELD to isEnabled, "source" to source.name)
            )
            cachedEnabledValue = isEnabled
            cachedSource = source
        }
        return isEnabled
    }

    fun initFromUri(uri: UriWrapper) {
        if (!accountStore.hasAccessToken()) {
            val queryParameter = uri.getQueryParameter(LoginActivity.TOKEN_PARAMETER)
            if (!queryParameter.isNullOrEmpty()) {
                initEnabledValue(queryParameter.hashCode(), true)
            }
        }
    }

    override fun name(): String {
        return if (cachedSource != DEFAULT) {
            MY_SITE_IMPROVEMENTS_REMOTE_FIELD
        } else {
            MY_SITE_IMPROVEMENTS_NO_ACCOUNT_FIELD
        }
    }

    companion object {
        const val MY_SITE_IMPROVEMENTS_REMOTE_FIELD = "my_site_improvements_random_enabled"
        const val MY_SITE_IMPROVEMENTS_NO_ACCOUNT_FIELD = "my_site_improvements_no_account_enabled"
    }

    enum class Source {
        TOKEN, DEFAULT
    }
}
