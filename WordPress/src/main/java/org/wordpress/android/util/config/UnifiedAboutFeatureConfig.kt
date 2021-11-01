package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

/**
 * Feature configuration for Unified About Screen
 */
// TODO: Uncomment the lines @Feature and UNIFIED_ABOUT_REMOTE_FIELD when remote field is configured and
//  remove line @FeatureInDevelopment and this to-do lines
// @Feature(DomainPurchaseFeatureConfig.DOMAIN_PURCHASE_REMOTE_FIELD, false)
@FeatureInDevelopment
class UnifiedAboutFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
        appConfig,
        BuildConfig.UNIFIED_ABOUT
//        UNIFIED_ABOUT_REMOTE_FIELD
) {
    companion object {
        const val UNIFIED_ABOUT_REMOTE_FIELD = "unified_about_remote_field"
    }
}
