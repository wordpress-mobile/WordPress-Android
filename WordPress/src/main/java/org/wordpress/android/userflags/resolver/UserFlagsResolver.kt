package org.wordpress.android.userflags.resolver

import android.database.Cursor
import org.wordpress.android.provider.query.QueryResult
import org.wordpress.android.resolver.ContentResolverWrapper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.userflags.JetpackLocalUserFlagsFlag
import org.wordpress.android.userflags.UserFlagsAnalyticsTracker
import org.wordpress.android.userflags.UserFlagsAnalyticsTracker.ErrorType
import org.wordpress.android.userflags.provider.UserFlagsProvider
import org.wordpress.android.util.publicdata.WordPressPublicData
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class UserFlagsResolver @Inject constructor(
    private val jetpackLocalUserFlagsFlag: JetpackLocalUserFlagsFlag,
    private val contextProvider: ContextProvider,
    private val wordPressPublicData: WordPressPublicData,
    private val queryResult: QueryResult,
    private val contentResolverWrapper: ContentResolverWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val userFlagsAnalyticsTracker: UserFlagsAnalyticsTracker
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
        val userFlagsResultCursor = getUserFlagsResultCursor()
        if (userFlagsResultCursor != null) {
            val userFlags = queryResult.getValue<Map<String, Any?>>(userFlagsResultCursor) ?: emptyMap()
            if (userFlags.isNotEmpty()) {
                val success = updateUserFlags(userFlags)
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
        } else {
            userFlagsAnalyticsTracker.trackFailed(ErrorType.QueryUserFlagsError)
            onFailure()
        }
    }

    private fun getUserFlagsResultCursor(): Cursor? {
        val wordpressUserFlagsUriValue =
                "content://${wordPressPublicData.currentPackageId()}.${UserFlagsProvider::class.simpleName}"
        return contentResolverWrapper.queryUri(
                contextProvider.getContext().contentResolver,
                wordpressUserFlagsUriValue
        )
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun updateUserFlags(
        userFlags: Map<String, Any?>
    ): Boolean {
        try {
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
            return true
        } catch (exception: Exception) {
            return false
        }
    }
}
