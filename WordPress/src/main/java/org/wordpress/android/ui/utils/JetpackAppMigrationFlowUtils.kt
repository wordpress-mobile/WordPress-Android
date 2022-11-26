package org.wordpress.android.ui.utils

import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.localcontentmigration.LocalMigrationState.SingleStep
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.JetpackMigrationFlowFeatureConfig
import org.wordpress.android.util.helpers.Version
import org.wordpress.android.util.publicdata.AppStatus
import org.wordpress.android.util.publicdata.WordPressPublicData
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class JetpackAppMigrationFlowUtils @Inject constructor(
    private val buildConfigWrapper: BuildConfigWrapper,
    private val jetpackMigrationFlowFeatureConfig: JetpackMigrationFlowFeatureConfig,
    private val contextProvider: ContextProvider,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val accountStore: AccountStore,
    private val appStatus: AppStatus,
    private val wordPressPublicData: WordPressPublicData,
) {
    private val minimumSupportedVersion = "21.2" // non semantic minimum supported version

    fun shouldShowMigrationFlow() = buildConfigWrapper.isJetpackApp
            && jetpackMigrationFlowFeatureConfig.isEnabled()
            && appPrefsWrapper.getIsFirstTrySharedLoginJetpack()
            && !accountStore.hasAccessToken()
            && isWordPressInstalled()
            && !dismissedWordPressUpdate()

    fun isWordPressCompatible(): Boolean {
        val wordPressVersion = wordPressPublicData.nonSemanticPackageVersion()
        return wordPressVersion != null && Version(wordPressVersion) >= Version(minimumSupportedVersion)
    }

    fun startJetpackMigrationFlow() {
        ActivityLauncher.startJetpackMigrationFlow(contextProvider.getContext())
    }

    fun startWordPressUpdateFlow(singleStep: SingleStep) {
        ActivityLauncher.startJetpackMigrationFlowWithSingleStep(contextProvider.getContext(), singleStep)
    }

    private fun isWordPressInstalled() = appStatus.isAppInstalled(wordPressPublicData.currentPackageId())

    private fun dismissedWordPressUpdate() = appPrefsWrapper.getDismissedWordPressUpdateJetpackMigration()
}
