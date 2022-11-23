package org.wordpress.android.ui.utils

import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.JetpackMigrationFlowFeatureConfig
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class JetpackAppMigrationFlowUtils @Inject constructor(
    private val buildConfigWrapper: BuildConfigWrapper,
    private val jetpackMigrationFlowFeatureConfig: JetpackMigrationFlowFeatureConfig,
    private val contextProvider: ContextProvider,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val accountStore: AccountStore,
) {
    fun shouldShowMigrationFlow() = buildConfigWrapper.isJetpackApp
            && jetpackMigrationFlowFeatureConfig.isEnabled()
            && appPrefsWrapper.getIsFirstTrySharedLoginJetpack()
            && !accountStore.hasAccessToken()

    fun startJetpackMigrationFlow() {
        ActivityLauncher.startJetpackMigrationFlow(contextProvider.getContext())
    }
}
