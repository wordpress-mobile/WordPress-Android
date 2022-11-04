package org.wordpress.android.ui.utils

import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.util.config.JetpackMigrationFlowFeatureConfig
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

@Suppress("ForbiddenComment")
// TODO: this will be refactored
class JetpackAppMigrationFlowUtils @Inject constructor(
    private val jetpackMigrationFlowFeatureConfig: JetpackMigrationFlowFeatureConfig,
    private val contextProvider: ContextProvider,
    private val accountStore: AccountStore,
) {
    fun shouldShowMigrationFlow(): Boolean {
        return jetpackMigrationFlowFeatureConfig.isEnabled() && accountStore.hasAccessToken()
    }

    fun startJetpackMigrationFlow() {
        ActivityLauncher.startJetpackMigrationFlow(contextProvider.getContext())
    }
}
