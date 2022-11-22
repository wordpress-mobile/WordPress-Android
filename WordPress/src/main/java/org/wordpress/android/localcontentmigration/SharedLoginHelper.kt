package org.wordpress.android.localcontentmigration

import org.wordpress.android.localcontentmigration.LocalContentEntity.AccessToken
import org.wordpress.android.localcontentmigration.LocalContentEntityData.AccessTokenData
import org.wordpress.android.localcontentmigration.LocalMigrationError.FeatureDisabled.SharedLoginDisabled
import org.wordpress.android.localcontentmigration.LocalMigrationError.MigrationAlreadyAttempted.SharedLoginAlreadyAttempted
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Failure
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Success
import org.wordpress.android.sharedlogin.JetpackSharedLoginFlag
import org.wordpress.android.sharedlogin.SharedLoginAnalyticsTracker
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import javax.inject.Inject

class SharedLoginHelper @Inject constructor(
    private val jetpackSharedLoginFlag: JetpackSharedLoginFlag,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val sharedLoginAnalyticsTracker: SharedLoginAnalyticsTracker,
    private val localMigrationContentResolver: LocalMigrationContentResolver,
) {
    fun login() = if (!jetpackSharedLoginFlag.isEnabled()) {
        Failure(SharedLoginDisabled)
    } else if (!appPrefsWrapper.getIsFirstTrySharedLoginJetpack()) {
        Failure(SharedLoginAlreadyAttempted)
    } else {
        sharedLoginAnalyticsTracker.trackLoginStart()
        localMigrationContentResolver.getResultForEntityType<AccessTokenData>(AccessToken).thenWith {
            appPrefsWrapper.saveIsFirstTrySharedLoginJetpack(false)
            sharedLoginAnalyticsTracker.trackLoginSuccess()
            Success(it)
        }
    }
}
