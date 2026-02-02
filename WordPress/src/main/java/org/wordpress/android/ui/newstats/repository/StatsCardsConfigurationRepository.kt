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

    fun getConfigurationSync(siteId: Long): StatsCardsConfiguration {
        return configurationCache[siteId] ?: loadConfiguration(siteId).also {
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

    /**
     * Removes a Most Viewed card at the given index.
     */
    suspend fun removeMostViewedCard(siteId: Long, index: Int): Unit = withContext(ioDispatcher) {
        val current = getConfiguration(siteId)
        // Remove one MOST_VIEWED from visibleCards
        val newVisibleCards = current.visibleCards.toMutableList()
        val mostViewedIndex = newVisibleCards.indexOfFirst { it == StatsCardType.MOST_VIEWED }
        if (mostViewedIndex >= 0) {
            // Find the nth MOST_VIEWED occurrence
            var count = 0
            val indexToRemove = newVisibleCards.indexOfFirst {
                if (it == StatsCardType.MOST_VIEWED) {
                    if (count == index) true else { count++; false }
                } else false
            }
            if (indexToRemove >= 0) {
                newVisibleCards.removeAt(indexToRemove)
            }
        }
        // Remove the corresponding data source
        val newDataSources = current.mostViewedDataSources.toMutableList()
        if (index in newDataSources.indices) {
            newDataSources.removeAt(index)
        }
        saveConfiguration(siteId, current.copy(
            visibleCards = newVisibleCards,
            mostViewedDataSources = newDataSources
        ))
    }

    /**
     * Adds a Most Viewed card with the given data source.
     */
    suspend fun addMostViewedCard(siteId: Long, dataSourceName: String): Unit = withContext(ioDispatcher) {
        val current = getConfiguration(siteId)
        // Add MOST_VIEWED to visibleCards (after the last MOST_VIEWED or at default position)
        val newVisibleCards = current.visibleCards.toMutableList()
        val lastMostViewedIndex = newVisibleCards.indexOfLast { it == StatsCardType.MOST_VIEWED }
        if (lastMostViewedIndex >= 0) {
            newVisibleCards.add(lastMostViewedIndex + 1, StatsCardType.MOST_VIEWED)
        } else {
            // Add at default position (after VIEWS_STATS)
            val viewsIndex = newVisibleCards.indexOfFirst { it == StatsCardType.VIEWS_STATS }
            if (viewsIndex >= 0) {
                newVisibleCards.add(viewsIndex + 1, StatsCardType.MOST_VIEWED)
            } else {
                newVisibleCards.add(StatsCardType.MOST_VIEWED)
            }
        }
        // Add the data source
        val newDataSources = current.mostViewedDataSources + dataSourceName
        saveConfiguration(siteId, current.copy(
            visibleCards = newVisibleCards,
            mostViewedDataSources = newDataSources
        ))
    }

    /**
     * Updates the data source for a Most Viewed card at the given index.
     */
    suspend fun updateMostViewedDataSource(
        siteId: Long,
        index: Int,
        dataSourceName: String
    ): Unit = withContext(ioDispatcher) {
        val current = getConfiguration(siteId)
        val newDataSources = current.mostViewedDataSources.toMutableList()
        if (index in newDataSources.indices) {
            newDataSources[index] = dataSourceName
        }
        saveConfiguration(siteId, current.copy(mostViewedDataSources = newDataSources))
    }

    private fun loadConfiguration(siteId: Long): StatsCardsConfiguration {
        val json = appPrefsWrapper.getStatsCardsConfigurationJson(siteId)
        if (json == null) {
            return StatsCardsConfiguration()
        }
        return try {
            gson.fromJson(json, StatsCardsConfiguration::class.java)
        } catch (e: Exception) {
            // Wipe corrupted data and save default configuration
            val defaultConfig = StatsCardsConfiguration()
            appPrefsWrapper.setStatsCardsConfigurationJson(siteId, gson.toJson(defaultConfig))
            defaultConfig
        }
    }
}
