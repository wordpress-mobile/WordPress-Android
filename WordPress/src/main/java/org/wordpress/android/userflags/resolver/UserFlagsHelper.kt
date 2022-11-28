package org.wordpress.android.userflags.resolver

import org.wordpress.android.localcontentmigration.LocalContentEntity.UserFlags
import org.wordpress.android.localcontentmigration.LocalContentEntityData.UserFlagsData
import org.wordpress.android.localcontentmigration.LocalMigrationContentResolver
import org.wordpress.android.localcontentmigration.LocalMigrationError.FeatureDisabled.UserFlagsDisabled
import org.wordpress.android.localcontentmigration.LocalMigrationError.MigrationAlreadyAttempted.UserFlagsAlreadyAttempted
import org.wordpress.android.localcontentmigration.LocalMigrationError.NoUserFlagsFoundError
import org.wordpress.android.localcontentmigration.LocalMigrationError.PersistenceError.FailedToSaveUserFlags
import org.wordpress.android.localcontentmigration.LocalMigrationError.PersistenceError.FailedToSaveUserFlagsWithException
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Failure
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Success
import org.wordpress.android.localcontentmigration.thenWith
import org.wordpress.android.resolver.ResolverUtility
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.userflags.JetpackLocalUserFlagsFlag
import org.wordpress.android.userflags.UserFlagsAnalyticsTracker
import org.wordpress.android.userflags.UserFlagsAnalyticsTracker.ErrorType
import javax.inject.Inject

class UserFlagsHelper @Inject constructor(
    private val jetpackLocalUserFlagsFlag: JetpackLocalUserFlagsFlag,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val userFlagsAnalyticsTracker: UserFlagsAnalyticsTracker,
    private val localMigrationContentResolver: LocalMigrationContentResolver,
    private val resolverUtility: ResolverUtility,
) {
    fun migrateUserFlags() = if (!jetpackLocalUserFlagsFlag.isEnabled()) {
        Failure(UserFlagsDisabled)
    } else if (!appPrefsWrapper.getIsFirstTryUserFlagsJetpack()) {
        Failure(UserFlagsAlreadyAttempted)
    } else {
        userFlagsAnalyticsTracker.trackStart()
        localMigrationContentResolver.getResultForEntityType<UserFlagsData>(UserFlags)
    }
            .thenWith(::checkIfEmpty)
            .thenWith(::updateUserFlagsData)
            .thenWith(::success)

    private fun checkIfEmpty(userFlagsData: UserFlagsData) = if (userFlagsData.flags.isEmpty()) {
        userFlagsAnalyticsTracker.trackFailed(ErrorType.NoUserFlagsFoundError)
        Failure(NoUserFlagsFoundError)
    } else {
        Success(userFlagsData)
    }

    private fun updateUserFlagsData(userFlagsData: UserFlagsData) = runCatching {
        val userFlags = userFlagsData.flags
        val qsStatusList = userFlagsData.quickStartStatusList
        val qsTaskList = userFlagsData.quickStartTaskList

        for ((key, value) in userFlags) {
            val userFlagPrefKey = UserFlagsPrefKey(key)
            when (value) {
                is String -> appPrefsWrapper.setString(userFlagPrefKey, value)
                is Long -> appPrefsWrapper.setLong(userFlagPrefKey, value)
                is Int -> appPrefsWrapper.setInt(userFlagPrefKey, value)
                is Boolean -> appPrefsWrapper.setBoolean(userFlagPrefKey, value)
                is Collection<*> -> {
                    val stringSet = value.filterIsInstance<String>().toSet()
                    appPrefsWrapper.setStringSet(userFlagPrefKey, stringSet)
                }
            }
        }
        if (!resolverUtility.copyQsDataWithIndexes(qsStatusList, qsTaskList)) {
            userFlagsAnalyticsTracker.trackFailed(ErrorType.UpdateUserFlagsError)
            Failure(FailedToSaveUserFlags)
        } else {
            Success(userFlagsData)
        }
    }.getOrElse { Failure(FailedToSaveUserFlagsWithException(it)) }

    private fun success(userFlagsData: UserFlagsData) = run {
        appPrefsWrapper.saveIsFirstTryUserFlagsJetpack(false)
        userFlagsAnalyticsTracker.trackSuccess()
        Success(userFlagsData)
    }
}
