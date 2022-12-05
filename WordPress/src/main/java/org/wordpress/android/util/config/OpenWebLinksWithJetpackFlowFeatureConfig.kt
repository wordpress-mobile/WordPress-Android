package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

@Feature(OpenWebLinksWithJetpackFlowFeatureConfig.OPEN_WEB_LINKS_WITH_JETPACK_FLOW, false)
class OpenWebLinksWithJetpackFlowFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.OPEN_WEB_LINKS_WITH_JETPACK_FLOW,
        OPEN_WEB_LINKS_WITH_JETPACK_FLOW
) {
    override fun isEnabled(): Boolean {
        return BuildConfig.ENABLE_OPEN_WEB_LINKS_WITH_JP_FLOW && super.isEnabled()
    }
    companion object {
        const val OPEN_WEB_LINKS_WITH_JETPACK_FLOW = "open_web_links_with_jetpack_flow"
    }
}
