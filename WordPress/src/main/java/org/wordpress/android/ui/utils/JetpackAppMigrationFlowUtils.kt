package org.wordpress.android.ui.utils

import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.util.config.JetpackMigrationFlowFeatureConfig
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

// TODO: this will be refactored
class JetpackAppMigrationFlowUtils @Inject constructor(
    private val jetpackMigrationFlowFeatureConfig: JetpackMigrationFlowFeatureConfig,
    private val contextProvider: ContextProvider,
) {
    fun isFlagEnabled(): Boolean {
        return jetpackMigrationFlowFeatureConfig.isEnabled()
    }

    fun startJetpackMigrationFlow() {
        ActivityLauncher.startJetpackMigrationFlow(contextProvider.getContext())
    }
}
