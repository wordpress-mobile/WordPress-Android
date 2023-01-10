package org.wordpress.android.util.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.persistence.RemoteConfigDao.RemoteConfig
import org.wordpress.android.fluxc.store.mobile.RemoteConfigStore
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.UTILS
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Do not use this class outside of this package. Use [AppConfig] instead
 */

@Singleton
class RemoteFieldConfigRepository
@Inject constructor(
    private val remoteConfigStore: RemoteConfigStore,
    @Named(APPLICATION_SCOPE) private val appScope: CoroutineScope
) {
    var remoteFields: List<RemoteConfig> = arrayListOf()

    fun init() {
        appScope.launch {
            remoteFields = remoteConfigStore.getRemoteConfigs()
            // If the flags are empty, then this means that the
            // that the app is launched for the first time and we need to
            // store the default in the database
            if (remoteFields.isEmpty()) {
                insertRemoteConfigDefaultsInDatabase()
                refresh()
            }
        }
    }

    private fun insertRemoteConfigDefaultsInDatabase() {
        RemoteFieldConfigDefaults.remoteFieldConfigDefaults.mapNotNull { remoteField ->
            remoteField.let {
                remoteConfigStore.insertRemoteConfig(
                    remoteField.key,
                    remoteField.value.toString()
                )
            }
        }
    }

    fun refresh() {
        appScope.launch {
            fetchRemoteFieldConfigs()
            remoteFields = remoteConfigStore.getRemoteConfigs()
        }
    }

    private suspend fun fetchRemoteFieldConfigs() {
        val response = remoteConfigStore.fetchRemoteConfig()
        response.remoteConfig?.let { configValues ->
            AppLog.e(UTILS, "Remote field config values synced")
            AnalyticsTracker.track(
                Stat.REMOTE_FIELD_CONFIG_SYNCED_STATE,
                configValues
            )
        }
        if (response.isError) {
            AppLog.e(UTILS, "Remote field config values sync failed")
        }
    }

    fun getValue(field: String): String {
        return remoteFields.find { it.key == field }?.value ?: ""
    }
}
