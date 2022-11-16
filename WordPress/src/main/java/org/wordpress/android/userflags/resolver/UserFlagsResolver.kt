package org.wordpress.android.userflags.resolver

import org.wordpress.android.localcontentmigration.LocalContentEntity.UserFlags
import org.wordpress.android.localcontentmigration.LocalContentEntityData.UserFlagsData
import org.wordpress.android.localcontentmigration.LocalMigrationContentResolver
import org.wordpress.android.resolver.ResolverUtility
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.userflags.JetpackLocalUserFlagsFlag
import org.wordpress.android.userflags.UserFlagsAnalyticsTracker
import org.wordpress.android.userflags.UserFlagsAnalyticsTracker.ErrorType
import javax.inject.Inject

class UserFlagsResolver @Inject constructor(
    private val jetpackLocalUserFlagsFlag: JetpackLocalUserFlagsFlag,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val userFlagsAnalyticsTracker: UserFlagsAnalyticsTracker,
    private val localMigrationContentResolver: LocalMigrationContentResolver,
    private val resolverUtility: ResolverUtility,
) {
    fun tryGetUserFlags(onSuccess: () -> Unit, onFailure: () -> Unit) {
        val isFeatureFlagEnabled = jetpackLocalUserFlagsFlag.isEnabled()
        if (!isFeatureFlagEnabled) {
            onFailure()
            return
        }
        val isFirstTry = appPrefsWrapper.getIsFirstTryUserFlagsJetpack()
        if (!isFirstTry) {
            onFailure()
            return
        }
        userFlagsAnalyticsTracker.trackStart()
        appPrefsWrapper.saveIsFirstTryUserFlagsJetpack(false)
        val userFlags: UserFlagsData = localMigrationContentResolver.getDataForEntityType(UserFlags)
        if (userFlags.flags.isNotEmpty()) {
            val success = updateUserFlagsData(userFlags)
            if (success) {
                userFlagsAnalyticsTracker.trackSuccess()
                onSuccess()
            } else {
                userFlagsAnalyticsTracker.trackFailed(ErrorType.UpdateUserFlagsError)
                onFailure()
            }
        } else {
                userFlagsAnalyticsTracker.trackFailed(ErrorType.NoUserFlagsFoundError)
                onFailure()
            }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun updateUserFlagsData(
        userFlagsData: UserFlagsData
    ): Boolean {
        try {
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
            return resolverUtility.copyQsDataWithIndexes(qsStatusList, qsTaskList)
        } catch (exception: Exception) {
            return false
        }
    }
}
