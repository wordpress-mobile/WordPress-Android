package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

@Feature(SiteDomainsFeatureConfig.SITE_DOMAINS_REMOTE_FIELD, true)
class SiteDomainsFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
    appConfig,
    BuildConfig.SITE_DOMAINS,
    SITE_DOMAINS_REMOTE_FIELD
) {
    companion object {
        const val SITE_DOMAINS_REMOTE_FIELD = "site_domains_remote_field"
    }
}
