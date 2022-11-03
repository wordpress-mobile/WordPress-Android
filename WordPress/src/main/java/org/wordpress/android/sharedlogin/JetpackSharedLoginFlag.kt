package org.wordpress.android.sharedlogin

import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.JetpackSharedLoginFeatureConfig
import javax.inject.Inject

class JetpackSharedLoginFlag @Inject constructor(
    private val jetpackSharedLoginFeatureConfig: JetpackSharedLoginFeatureConfig,
    private val buildConfigWrapper: BuildConfigWrapper
) {
    fun isEnabled() = jetpackSharedLoginFeatureConfig.isEnabled() && buildConfigWrapper.isJetpackApp
}
