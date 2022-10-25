package org.wordpress.android.ui.main.utils

import org.wordpress.android.util.config.JetpackSwitchWelcomeScreenFeatureConfig
import javax.inject.Inject

class JetpackAppSwitchUtils @Inject constructor(
    private val jetpackSwitchWelcomeScreenFeatureConfig: JetpackSwitchWelcomeScreenFeatureConfig,
) {
    fun shouldShowWelcomeScreenInJetpackApp(): Boolean {
        // TODO Add logic to show the welcome screen that follows a switch to the Jetpack app
        return jetpackSwitchWelcomeScreenFeatureConfig.isEnabled()
    }
}
