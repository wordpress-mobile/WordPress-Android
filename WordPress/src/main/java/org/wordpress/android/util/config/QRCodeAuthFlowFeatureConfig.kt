package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

@FeatureInDevelopment
class QRCodeAuthFlowFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.QRCODE_AUTH_FLOW
)
