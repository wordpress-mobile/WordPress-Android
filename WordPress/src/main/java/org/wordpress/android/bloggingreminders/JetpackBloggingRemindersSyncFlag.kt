package org.wordpress.android.bloggingreminders

import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.JetpackBloggingRemindersSyncFeatureConfig
import javax.inject.Inject

class JetpackBloggingRemindersSyncFlag @Inject constructor(
    private val jetpackBloggingRemindersSyncFeatureConfig: JetpackBloggingRemindersSyncFeatureConfig,
    private val buildConfigWrapper: BuildConfigWrapper
) {
    fun isEnabled() = jetpackBloggingRemindersSyncFeatureConfig.isEnabled() && buildConfigWrapper.isJetpackApp
}
