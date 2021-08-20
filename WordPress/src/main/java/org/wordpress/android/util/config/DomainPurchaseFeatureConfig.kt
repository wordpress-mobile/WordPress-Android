package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

// TODO: Uncomment the lines 9 and 13 when remote field is configured and remove line 10 and this to-do
// @Feature(DomainPurchaseFeatureConfig.DOMAIN_PURCHASE_REMOTE_FIELD, false)
@FeatureInDevelopment
class DomainPurchaseFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.DOMAIN_PURCHASE
//        DOMAINS_PURCHASE_REMOTE_FIELD
) {
    companion object {
        const val DOMAIN_PURCHASE_REMOTE_FIELD = "domain_purchase_remote_field"
    }
}
