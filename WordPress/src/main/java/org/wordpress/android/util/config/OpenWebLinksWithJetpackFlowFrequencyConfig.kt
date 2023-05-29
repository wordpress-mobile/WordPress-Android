package org.wordpress.android.util.config

import org.wordpress.android.annotation.RemoteFieldDefaultGenerater
import javax.inject.Inject

const val OPEN_WEB_LINKS_WITH_JETPACK_FLOW_FREQUENCY_REMOTE_FIELD = "open_web_links_with_jetpack_flow_frequency"
const val OPEN_WEB_LINKS_WITH_JETPACK_FLOW_FREQUENCY_DEFAULT = "0"

@RemoteFieldDefaultGenerater(
    remoteField = OPEN_WEB_LINKS_WITH_JETPACK_FLOW_FREQUENCY_REMOTE_FIELD,
    defaultValue = OPEN_WEB_LINKS_WITH_JETPACK_FLOW_FREQUENCY_DEFAULT
)

class OpenWebLinksWithJetpackFlowFrequencyConfig @Inject constructor(appConfig: AppConfig) :
    RemoteConfigField<Long>(
        appConfig,
        OPEN_WEB_LINKS_WITH_JETPACK_FLOW_FREQUENCY_REMOTE_FIELD
    )
