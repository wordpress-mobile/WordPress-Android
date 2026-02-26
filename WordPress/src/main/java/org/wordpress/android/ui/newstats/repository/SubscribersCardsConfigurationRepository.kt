package org.wordpress.android.ui.newstats.repository

import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.newstats.subscribers.SubscribersCardType
import org.wordpress.android.ui.newstats.subscribers.SubscribersCardsConfiguration
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.EnumWithFallbackValueTypeAdapterFactory
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SubscribersCardsConfigurationRepository @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    @Named(IO_THREAD)
    private val ioDispatcher: CoroutineDispatcher
) {
    private val gson = GsonBuilder()
        .registerTypeAdapterFactory(
            EnumWithFallbackValueTypeAdapterFactory()
        )
        .create()

    private val _configurationFlow = MutableStateFlow<
        Pair<Long, SubscribersCardsConfiguration>?>(null)
    val configurationFlow: StateFlow<
        Pair<Long, SubscribersCardsConfiguration>?> =
        _configurationFlow.asStateFlow()

    suspend fun getConfiguration(
        siteId: Long
    ): SubscribersCardsConfiguration =
        withContext(ioDispatcher) {
            loadConfiguration(siteId)
        }

    suspend fun saveConfiguration(
        siteId: Long,
        configuration: SubscribersCardsConfiguration
    ): Unit = withContext(ioDispatcher) {
        appPrefsWrapper
            .setSubscribersCardsConfigurationJson(
                siteId, gson.toJson(configuration)
            )
        _configurationFlow.value =
            siteId to configuration
    }

    suspend fun removeCard(
        siteId: Long,
        cardType: SubscribersCardType
    ): Unit = withContext(ioDispatcher) {
        val current = getConfiguration(siteId)
        val newVisibleCards =
            current.visibleCards.toMutableList()
        newVisibleCards.remove(cardType)
        saveConfiguration(
            siteId,
            current.copy(visibleCards = newVisibleCards)
        )
    }

    suspend fun addCard(
        siteId: Long,
        cardType: SubscribersCardType
    ): Unit = withContext(ioDispatcher) {
        val current = getConfiguration(siteId)
        val newVisibleCards =
            current.visibleCards + cardType
        saveConfiguration(
            siteId,
            current.copy(visibleCards = newVisibleCards)
        )
    }

    suspend fun moveCardUp(
        siteId: Long,
        cardType: SubscribersCardType
    ): Unit = withContext(ioDispatcher) {
        val current = getConfiguration(siteId)
        val index =
            current.visibleCards.indexOf(cardType)
        if (index > 0) {
            moveCardToIndex(
                siteId, current, cardType, index - 1
            )
        }
    }

    suspend fun moveCardToTop(
        siteId: Long,
        cardType: SubscribersCardType
    ): Unit = withContext(ioDispatcher) {
        val current = getConfiguration(siteId)
        val index =
            current.visibleCards.indexOf(cardType)
        if (index > 0) {
            moveCardToIndex(
                siteId, current, cardType, 0
            )
        }
    }

    suspend fun moveCardDown(
        siteId: Long,
        cardType: SubscribersCardType
    ): Unit = withContext(ioDispatcher) {
        val current = getConfiguration(siteId)
        val index =
            current.visibleCards.indexOf(cardType)
        if (index >= 0 &&
            index < current.visibleCards.size - 1
        ) {
            moveCardToIndex(
                siteId, current, cardType, index + 1
            )
        }
    }

    suspend fun moveCardToBottom(
        siteId: Long,
        cardType: SubscribersCardType
    ): Unit = withContext(ioDispatcher) {
        val current = getConfiguration(siteId)
        val index =
            current.visibleCards.indexOf(cardType)
        if (index >= 0 &&
            index < current.visibleCards.size - 1
        ) {
            moveCardToIndex(
                siteId, current, cardType,
                current.visibleCards.size - 1
            )
        }
    }

    private suspend fun moveCardToIndex(
        siteId: Long,
        current: SubscribersCardsConfiguration,
        cardType: SubscribersCardType,
        newIndex: Int
    ) {
        val newVisibleCards =
            current.visibleCards.toMutableList()
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
    ): SubscribersCardsConfiguration {
        val json = appPrefsWrapper
            .getSubscribersCardsConfigurationJson(siteId)
        if (json == null) {
            return SubscribersCardsConfiguration()
        }
        return try {
            val config = gson.fromJson(
                json,
                SubscribersCardsConfiguration::class.java
            )
            if (isValidConfiguration(config)) {
                config
            } else {
                AppLog.w(
                    AppLog.T.STATS,
                    "Subscribers cards config contains " +
                        "invalid card types, " +
                        "resetting to default"
                )
                resetToDefault(siteId)
            }
        } catch (e: Exception) {
            AppLog.e(
                AppLog.T.STATS,
                "Failed to parse subscribers cards " +
                    "config, resetting to default",
                e
            )
            resetToDefault(siteId)
        }
    }

    private fun isValidConfiguration(
        config: SubscribersCardsConfiguration
    ): Boolean {
        val validCards = config.visibleCards
            .filterIsInstance<SubscribersCardType>()
        return validCards.size ==
            config.visibleCards.size
    }

    private fun resetToDefault(
        siteId: Long
    ): SubscribersCardsConfiguration {
        val defaultConfig = SubscribersCardsConfiguration()
        appPrefsWrapper
            .setSubscribersCardsConfigurationJson(
                siteId, gson.toJson(defaultConfig)
            )
        return defaultConfig
    }
}
