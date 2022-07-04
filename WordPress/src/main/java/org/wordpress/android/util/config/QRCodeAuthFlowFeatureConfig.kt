package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

@Feature(QRCodeAuthFlowFeatureConfig.QRCODE_AUTH_FLOW_REMOTE_FIELD, true)
class QRCodeAuthFlowFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.QRCODE_AUTH_FLOW,
        QRCODE_AUTH_FLOW_REMOTE_FIELD
) {
    companion object {
        const val QRCODE_AUTH_FLOW_REMOTE_FIELD = "qrcode_auth_flow_remote_field"
    }
}
