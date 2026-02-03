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

    // StateFlow to notify observers of configuration changes
    private val _configurationFlow = MutableStateFlow<Pair<Long, StatsCardsConfiguration>?>(null)
    val configurationFlow: StateFlow<Pair<Long, StatsCardsConfiguration>?> =
        _configurationFlow.asStateFlow()

    suspend fun getConfiguration(siteId: Long): StatsCardsConfiguration = withContext(ioDispatcher) {
        loadConfiguration(siteId)
    }

    suspend fun saveConfiguration(
        siteId: Long,
        configuration: StatsCardsConfiguration
    ): Unit = withContext(ioDispatcher) {
        appPrefsWrapper.setStatsCardsConfigurationJson(siteId, gson.toJson(configuration))
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

    suspend fun moveCardUp(siteId: Long, cardType: StatsCardType): Unit = withContext(ioDispatcher) {
        val current = getConfiguration(siteId)
        val index = current.visibleCards.indexOf(cardType)
        if (index > 0) {
            moveCardToIndex(siteId, current, cardType, index - 1)
        }
    }

    suspend fun moveCardToTop(siteId: Long, cardType: StatsCardType): Unit = withContext(ioDispatcher) {
        val current = getConfiguration(siteId)
        val index = current.visibleCards.indexOf(cardType)
        if (index > 0) {
            moveCardToIndex(siteId, current, cardType, 0)
        }
    }

    suspend fun moveCardDown(siteId: Long, cardType: StatsCardType): Unit = withContext(ioDispatcher) {
        val current = getConfiguration(siteId)
        val index = current.visibleCards.indexOf(cardType)
        if (index >= 0 && index < current.visibleCards.size - 1) {
            moveCardToIndex(siteId, current, cardType, index + 1)
        }
    }

    suspend fun moveCardToBottom(siteId: Long, cardType: StatsCardType): Unit = withContext(ioDispatcher) {
        val current = getConfiguration(siteId)
        val index = current.visibleCards.indexOf(cardType)
        if (index >= 0 && index < current.visibleCards.size - 1) {
            moveCardToIndex(siteId, current, cardType, current.visibleCards.size - 1)
        }
    }

    private suspend fun moveCardToIndex(
        siteId: Long,
        current: StatsCardsConfiguration,
        cardType: StatsCardType,
        newIndex: Int
    ) {
        val newVisibleCards = current.visibleCards.toMutableList()
        newVisibleCards.remove(cardType)
        newVisibleCards.add(newIndex, cardType)
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
     * Returns false if any card type is null (which happens when Gson's default enum deserializer
     * encounters an unknown enum value like the old "MOST_VIEWED").
     *
     * Note: Since StatsCardType doesn't have a @FallbackValue annotation, unknown enum values
     * are deserialized as null by Gson and can sneak into the List<StatsCardType> at runtime,
     * bypassing Kotlin's null-safety. We use filterIsInstance to safely count valid entries.
     */
    private fun isValidConfiguration(config: StatsCardsConfiguration): Boolean {
        // filterIsInstance safely handles nulls that may have snuck into the list from Gson
        val validCards = config.visibleCards.filterIsInstance<StatsCardType>()
        return validCards.size == config.visibleCards.size
    }

    private fun resetToDefault(siteId: Long): StatsCardsConfiguration {
        val defaultConfig = StatsCardsConfiguration()
        appPrefsWrapper.setStatsCardsConfigurationJson(siteId, gson.toJson(defaultConfig))
        return defaultConfig
    }
}
