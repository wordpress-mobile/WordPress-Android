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
            if (!remoteFields.containsAllFields()) {
                insertMissingRemoteConfigDefaultsInDatabase(remoteFields.map { it.key })
                refresh()
            }
        }
    }

    private fun List<RemoteConfig>.containsAllFields(): Boolean {
        val defaults = RemoteFieldConfigDefaults.remoteFieldConfigDefaults
        return defaults.all { default ->
            this.any { it.key == default.key }
        }
    }

    private fun insertMissingRemoteConfigDefaultsInDatabase(existingKeys: List<String>) {
        RemoteFieldConfigDefaults.remoteFieldConfigDefaults.forEach { remoteField ->
            if (remoteField.key !in existingKeys) {
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

            // re-insert the defaults in case they were removed from the remote config and also to make sure latest
            // version defaults overwrite the old ones that might already be in the database
            insertMissingRemoteConfigDefaultsInDatabase(configValues.map { it.key })
        }
        if (response.isError) {
            AppLog.e(UTILS, "Remote field config values sync failed")
        }
    }

    fun getValue(field: String): String {
        // search the remote fields (from local database) then in-memory defaults, and return "" as fallback
        return remoteFields.find { it.key == field }?.value
            ?: RemoteFieldConfigDefaults.remoteFieldConfigDefaults[field]?.toString()
            ?: ""
    }
}
