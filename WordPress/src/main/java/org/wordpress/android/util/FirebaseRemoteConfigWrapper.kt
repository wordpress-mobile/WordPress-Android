package org.wordpress.android.util

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRemoteConfigWrapper @Inject constructor() {
    fun getOpenWebLinksWithJetpackFlowFrequency() =
            FirebaseRemoteConfig.getInstance().getLong(OPEN_WEB_LINKS_WITH_JETPACK_FLOW_FREQUENCY_KEY)

    companion object {
        const val OPEN_WEB_LINKS_WITH_JETPACK_FLOW_FREQUENCY_KEY = "open_web_links_with_jetpack_flow_frequency"
    }
}
