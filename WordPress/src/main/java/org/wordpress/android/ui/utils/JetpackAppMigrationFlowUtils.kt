package org.wordpress.android.ui.utils

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
) {
    /** TODO: This should perform some additional pre-flight checks. We should:
     *   1. Check that the user is not already logged in.
     *   2. Check that the database is not already populated
     *     * This also covers the case of an already successful migration.
     */
    fun shouldShowMigrationFlow() = buildConfigWrapper.isJetpackApp
            && jetpackMigrationFlowFeatureConfig.isEnabled()
            && appPrefsWrapper.getIsFirstTrySharedLoginJetpack()

    fun startJetpackMigrationFlow() {
        ActivityLauncher.startJetpackMigrationFlow(contextProvider.getContext())
    }
}
