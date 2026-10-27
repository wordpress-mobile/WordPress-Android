package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.WpComWpRsFeatureConfig.Companion.WP_COM_WP_RS_REMOTE_FIELD
import javax.inject.Inject

/**
 * Configuration for serving the wordpress-rs screens (posts, pages, comments) to sites reached over
 * the WP.com REST API. Those sites authenticate with an OAuth bearer token rather than an
 * application password, so this rolls them out gradually rather than moving the whole WP.com
 * audience off the legacy screens at once.
 */
@Feature(WP_COM_WP_RS_REMOTE_FIELD, false)
class WpComWpRsFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.WP_COM_WP_RS,
    WP_COM_WP_RS_REMOTE_FIELD,
) {
    companion object {
        const val WP_COM_WP_RS_REMOTE_FIELD = "android_wp_rs_wpcom"
    }
}
