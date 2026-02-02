package org.wordpress.android.ui.newstats.repository

import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.newstats.StatsCardType
import org.wordpress.android.ui.newstats.StatsCardsConfiguration
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.EnumWithFallbackValueTypeAdapterFactory
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class StatsCardsConfigurationRepository @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
    private val gson = GsonBuilder()
        .registerTypeAdapterFactory(EnumWithFallbackValueTypeAdapterFactory())
        .create()

    // Cache per site to avoid repeated disk reads
    private val configurationCache = mutableMapOf<Long, StatsCardsConfiguration>()

    // StateFlow to notify observers of configuration changes
    private val _configurationFlow = MutableStateFlow<Pair<Long, StatsCardsConfiguration>?>(null)
    val configurationFlow: StateFlow<Pair<Long, StatsCardsConfiguration>?> =
        _configurationFlow.asStateFlow()

    suspend fun getConfiguration(siteId: Long): StatsCardsConfiguration = withContext(ioDispatcher) {
        configurationCache[siteId] ?: loadConfiguration(siteId).also {
            configurationCache[siteId] = it
        }
    }

    suspend fun saveConfiguration(
        siteId: Long,
        configuration: StatsCardsConfiguration
    ): Unit = withContext(ioDispatcher) {
        appPrefsWrapper.setStatsCardsConfigurationJson(siteId, gson.toJson(configuration))
        configurationCache[siteId] = configuration
        _configurationFlow.value = siteId to configuration
    }

    suspend fun removeCard(siteId: Long, cardType: StatsCardType): Unit = withContext(ioDispatcher) {
        val current = getConfiguration(siteId)
        // For non-MOST_VIEWED cards, remove from visibleCards
        val newVisibleCards = current.visibleCards.toMutableList()
        newVisibleCards.remove(cardType)
        saveConfiguration(siteId, current.copy(visibleCards = newVisibleCards))
    }

    suspend fun addCard(siteId: Long, cardType: StatsCardType): Unit = withContext(ioDispatcher) {
        val current = getConfiguration(siteId)
        val newVisibleCards = current.visibleCards + cardType
        saveConfiguration(siteId, current.copy(visibleCards = newVisibleCards))
    }

    @Suppress("TooGenericExceptionCaught")
    private fun loadConfiguration(siteId: Long): StatsCardsConfiguration {
        val json = appPrefsWrapper.getStatsCardsConfigurationJson(siteId)
        if (json == null) {
            return StatsCardsConfiguration()
        }
        return try {
            val config = gson.fromJson(json, StatsCardsConfiguration::class.java)
            if (isValidConfiguration(config)) {
                config
            } else {
                AppLog.w(AppLog.T.STATS, "Stats cards configuration contains invalid card types, resetting to default")
                resetToDefault(siteId)
            }
        } catch (e: Exception) {
            AppLog.e(AppLog.T.STATS, "Failed to parse stats cards configuration, resetting to default", e)
            resetToDefault(siteId)
        }
    }

    /**
     * Validates that the configuration contains only valid card types.
     * Returns false if any card type is null (which happens when EnumWithFallbackValueTypeAdapterFactory
     * encounters an unknown enum value like the old "MOST_VIEWED").
     */
    private fun isValidConfiguration(config: StatsCardsConfiguration): Boolean {
        // Check if visibleCards contains any null values (invalid enum values are deserialized as null)
        @Suppress("USELESS_CAST")
        val hasInvalidCards = config.visibleCards.any { (it as StatsCardType?) == null }
        return !hasInvalidCards
    }

    private fun resetToDefault(siteId: Long): StatsCardsConfiguration {
        val defaultConfig = StatsCardsConfiguration()
        appPrefsWrapper.setStatsCardsConfigurationJson(siteId, gson.toJson(defaultConfig))
        return defaultConfig
    }
}
