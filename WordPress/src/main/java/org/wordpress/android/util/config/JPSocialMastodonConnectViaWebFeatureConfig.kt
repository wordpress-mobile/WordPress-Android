package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val JP_SOCIAL_MASTODON_CONNECT_VIA_WEB = "jp_social_mastodon_connect_via_web"

@Feature(JP_SOCIAL_MASTODON_CONNECT_VIA_WEB, defaultValue = true)
class JPSocialMastodonConnectViaWebFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.JP_SOCIAL_MASTODON_CONNECT_VIA_WEB,
    JP_SOCIAL_MASTODON_CONNECT_VIA_WEB,
)
