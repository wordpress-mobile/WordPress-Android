package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

// TODO: Uncomment the lines 8 and 14 when the remote field is configured
//  and remove line 9 and this to-do
// @Feature(QRCodeAuthFlowFeatureConfig.STATS_REVAMP_V2_REMOTE_FIELD, false)
@FeatureInDevelopment
class QRCodeAuthFlowFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.QRCODE_AUTH_FLOW
//      QRCODE_AUTH_FLOW_REMOTE_FIELD
) {
    companion object {
        const val QRCODE_AUTH_FLOW_REMOTE_FIELD = "qrcode_auth_flow_remote_field"
    }
}
