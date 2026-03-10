package org.wordpress.android.ui.newstats.repository

import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.newstats.InsightsCardType
import org.wordpress.android.ui.newstats.InsightsCardsConfiguration
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.EnumWithFallbackValueTypeAdapterFactory
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class InsightsCardsConfigurationRepository @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
    private val mutex = Mutex()

    private val gson = GsonBuilder()
        .registerTypeAdapterFactory(
            EnumWithFallbackValueTypeAdapterFactory()
        )
        .create()

    private val _configurationFlow =
        MutableStateFlow<Pair<Long, InsightsCardsConfiguration>?>(null)
    val configurationFlow:
        StateFlow<Pair<Long, InsightsCardsConfiguration>?> =
            _configurationFlow.asStateFlow()

    suspend fun getConfiguration(
        siteId: Long
    ): InsightsCardsConfiguration = withContext(ioDispatcher) {
        loadConfiguration(siteId)
    }

    suspend fun saveConfiguration(
        siteId: Long,
        configuration: InsightsCardsConfiguration
    ): Unit = withContext(ioDispatcher) {
        appPrefsWrapper.setStatsInsightsCardsConfigurationJson(
            siteId,
            gson.toJson(configuration)
        )
        _configurationFlow.value = siteId to configuration
    }

    suspend fun removeCard(
        siteId: Long,
        cardType: InsightsCardType
    ): Unit = withContext(ioDispatcher) {
        mutex.withLock {
            val current = getConfiguration(siteId)
            val newVisibleCards =
                current.visibleCards.toMutableList()
            newVisibleCards.remove(cardType)
            val newHiddenCards =
                (current.hiddenCards + cardType).distinct()
            saveConfiguration(
                siteId,
                current.copy(
                    visibleCards = newVisibleCards,
                    hiddenCards = newHiddenCards
                )
            )
        }
    }

    suspend fun addCard(
        siteId: Long,
        cardType: InsightsCardType
    ): Unit = withContext(ioDispatcher) {
        mutex.withLock {
            val current = getConfiguration(siteId)
            if (current.visibleCards.contains(cardType)) {
                return@withLock
            }
            val newVisibleCards =
                current.visibleCards + cardType
            val newHiddenCards =
                current.hiddenCards - cardType
            saveConfiguration(
                siteId,
                current.copy(
                    visibleCards = newVisibleCards,
                    hiddenCards = newHiddenCards
                )
            )
        }
    }

    suspend fun moveCardUp(
        siteId: Long,
        cardType: InsightsCardType
    ): Unit = withContext(ioDispatcher) {
        mutex.withLock {
            val current = getConfiguration(siteId)
            val index = current.visibleCards.indexOf(cardType)
            if (index > 0) {
                moveCardToIndex(
                    siteId, current, cardType, index - 1
                )
            }
        }
    }

    suspend fun moveCardToTop(
        siteId: Long,
        cardType: InsightsCardType
    ): Unit = withContext(ioDispatcher) {
        mutex.withLock {
            val current = getConfiguration(siteId)
            val index = current.visibleCards.indexOf(cardType)
            if (index > 0) {
                moveCardToIndex(siteId, current, cardType, 0)
            }
        }
    }

    suspend fun moveCardDown(
        siteId: Long,
        cardType: InsightsCardType
    ): Unit = withContext(ioDispatcher) {
        mutex.withLock {
            val current = getConfiguration(siteId)
            val index = current.visibleCards.indexOf(cardType)
            if (index >= 0 &&
                index < current.visibleCards.size - 1
            ) {
                moveCardToIndex(
                    siteId, current, cardType, index + 1
                )
            }
        }
    }

    suspend fun moveCardToBottom(
        siteId: Long,
        cardType: InsightsCardType
    ): Unit = withContext(ioDispatcher) {
        mutex.withLock {
            val current = getConfiguration(siteId)
            val index = current.visibleCards.indexOf(cardType)
            if (index >= 0 &&
                index < current.visibleCards.size - 1
            ) {
                moveCardToIndex(
                    siteId,
                    current,
                    cardType,
                    current.visibleCards.size - 1
                )
            }
        }
    }

    private suspend fun moveCardToIndex(
        siteId: Long,
        current: InsightsCardsConfiguration,
        cardType: InsightsCardType,
        newIndex: Int
    ) {
        val newVisibleCards = current.visibleCards.toMutableList()
        newVisibleCards.remove(cardType)
        newVisibleCards.add(newIndex, cardType)
        saveConfiguration(
            siteId,
            current.copy(visibleCards = newVisibleCards)
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private fun loadConfiguration(
        siteId: Long
    ): InsightsCardsConfiguration {
        val json = appPrefsWrapper
            .getStatsInsightsCardsConfigurationJson(siteId)
        if (json == null) {
            return InsightsCardsConfiguration()
        }
        return try {
            val config = gson.fromJson(
                json,
                InsightsCardsConfiguration::class.java
            )
            if (isValidConfiguration(config)) {
                addNewCardTypes(siteId, config)
            } else {
                AppLog.w(
                    AppLog.T.STATS,
                    "Insights cards configuration contains " +
                        "invalid card types, resetting to default"
                )
                resetToDefault(siteId)
            }
        } catch (e: Exception) {
            AppLog.e(
                AppLog.T.STATS,
                "Failed to parse insights cards " +
                    "configuration, resetting to default",
                e
            )
            resetToDefault(siteId)
        }
    }

    private fun addNewCardTypes(
        siteId: Long,
        config: InsightsCardsConfiguration
    ): InsightsCardsConfiguration {
        val allKnown = InsightsCardType.entries
        val knownInConfig = config.visibleCards +
            config.hiddenCards
        val newTypes = allKnown - knownInConfig.toSet()
        if (newTypes.isEmpty()) return config
        val updated = config.copy(
            visibleCards = config.visibleCards + newTypes
        )
        appPrefsWrapper.setStatsInsightsCardsConfigurationJson(
            siteId,
            gson.toJson(updated)
        )
        return updated
    }

    @Suppress("USELESS_CAST")
    private fun isValidConfiguration(
        config: InsightsCardsConfiguration
    ): Boolean {
        return (config.visibleCards as List<Any?>)
            .none { it == null }
    }

    private fun resetToDefault(
        siteId: Long
    ): InsightsCardsConfiguration {
        val defaultConfig = InsightsCardsConfiguration()
        appPrefsWrapper.setStatsInsightsCardsConfigurationJson(
            siteId,
            gson.toJson(defaultConfig)
        )
        return defaultConfig
    }
}
