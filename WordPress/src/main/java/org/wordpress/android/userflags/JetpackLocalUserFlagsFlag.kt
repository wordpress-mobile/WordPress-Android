package org.wordpress.android.userflags

import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.JetpackLocalUserFlagsFeatureConfig
import javax.inject.Inject

class JetpackLocalUserFlagsFlag @Inject constructor(
    private val jetpackLocalUserFlagsFeatureConfig: JetpackLocalUserFlagsFeatureConfig,
    private val buildConfigWrapper: BuildConfigWrapper
) {
    fun isEnabled() = jetpackLocalUserFlagsFeatureConfig.isEnabled() && buildConfigWrapper.isJetpackApp
}
