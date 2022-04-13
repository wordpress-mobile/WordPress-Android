package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

// TODO: Uncomment the lines 9 and 13 when remote field is configured and remove line 10 and this to-do
// @Feature(SiteDomainsFeatureConfig.SITE_DOMAINS_REMOTE_FIELD, false)
@FeatureInDevelopment
class SiteDomainsFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.SITE_DOMAINS
//        SITE_DOMAINS_REMOTE_FIELD
) {
    companion object {
        const val SITE_DOMAINS_REMOTE_FIELD = "site_domains_remote_field"
    }
}
