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

    private val configWrites = Mutex()

    // StateFlow to notify observers of configuration changes
    private val _configurationFlow = MutableStateFlow<Pair<Long, StatsCardsConfiguration>?>(null)
    val configurationFlow: StateFlow<Pair<Long, StatsCardsConfiguration>?> =
        _configurationFlow.asStateFlow()

    suspend fun getConfiguration(siteId: Long): StatsCardsConfiguration = withContext(ioDispatcher) {
        loadAndMigrate(siteId)
    }

    suspend fun saveConfiguration(
        siteId: Long,
        configuration: StatsCardsConfiguration
    ): Unit = withContext(ioDispatcher) {
        persistConfiguration(siteId, configuration)
    }

    /**
     * Reads [siteId]'s configuration, applies [edit] to it and writes it back, with no other edit
     * interleaving.
     *
     * Every setting a card persists lives in one blob, so changing any of them is a read-modify-write
     * of the whole thing: two running at once and the one that writes first is silently reverted. The
     * lock lives here rather than in a caller because this class is the single writer of that blob and
     * is a singleton, while its callers are not — a lock held by one view model cannot order its
     * writes against a card reorder issued from another.
     *
     * Deliberately reads through [loadAndMigrate] and writes through [persistConfiguration] rather
     * than the suspending accessors above, so a future lock on either cannot deadlock this.
     *
     * An [edit] that changes nothing writes nothing, so the callers whose move is already a no-op
     * (a card at the top asked to move up) stay the no-ops they were and don't re-emit
     * [configurationFlow]. Any migration [loadAndMigrate] applied has already persisted itself.
     */
    suspend fun update(
        siteId: Long,
        edit: (StatsCardsConfiguration) -> StatsCardsConfiguration
    ): Unit = withContext(ioDispatcher) {
        configWrites.withLock {
            val current = loadAndMigrate(siteId)
            val edited = edit(current)
            if (edited != current) {
                persistConfiguration(siteId, edited)
            }
        }
    }

    private fun persistConfiguration(siteId: Long, configuration: StatsCardsConfiguration) {
        appPrefsWrapper.setStatsCardsConfigurationJson(siteId, gson.toJson(configuration))
        _configurationFlow.value = siteId to configuration
    }

    suspend fun removeCard(siteId: Long, cardType: StatsCardType) = update(siteId) { current ->
        // For non-MOST_VIEWED cards, remove from visibleCards
        val newVisibleCards = current.visibleCards.toMutableList()
        newVisibleCards.remove(cardType)
        current.copy(visibleCards = newVisibleCards)
    }

    suspend fun addCard(siteId: Long, cardType: StatsCardType) = update(siteId) { current ->
        current.copy(visibleCards = current.visibleCards + cardType)
    }

    suspend fun moveCardUp(siteId: Long, cardType: StatsCardType) = update(siteId) { current ->
        val index = current.visibleCards.indexOf(cardType)
        if (index > 0) moveCardToIndex(current, cardType, index - 1) else current
    }

    suspend fun moveCardToTop(siteId: Long, cardType: StatsCardType) = update(siteId) { current ->
        val index = current.visibleCards.indexOf(cardType)
        if (index > 0) moveCardToIndex(current, cardType, 0) else current
    }

    suspend fun moveCardDown(siteId: Long, cardType: StatsCardType) = update(siteId) { current ->
        val index = current.visibleCards.indexOf(cardType)
        val isMovable = index >= 0 && index < current.visibleCards.size - 1
        if (isMovable) moveCardToIndex(current, cardType, index + 1) else current
    }

    suspend fun moveCardToBottom(siteId: Long, cardType: StatsCardType) = update(siteId) { current ->
        val index = current.visibleCards.indexOf(cardType)
        val isMovable = index >= 0 && index < current.visibleCards.size - 1
        if (isMovable) moveCardToIndex(current, cardType, current.visibleCards.size - 1) else current
    }

    private fun moveCardToIndex(
        current: StatsCardsConfiguration,
        cardType: StatsCardType,
        newIndex: Int
    ): StatsCardsConfiguration {
        val newVisibleCards = current.visibleCards.toMutableList()
        newVisibleCards.remove(cardType)
        newVisibleCards.add(newIndex, cardType)
        return current.copy(visibleCards = newVisibleCards)
    }

    /**
     * Loads the configuration and brings its card list up to the default set this build ships,
     * persisting it when that changes anything so the migration only runs once per site.
     */
    private fun loadAndMigrate(siteId: Long): StatsCardsConfiguration {
        val config = loadConfiguration(siteId)
        val migrated = addNewDefaultCards(config)
        if (migrated != config) {
            persistConfiguration(siteId, migrated)
        }
        return migrated
    }

    /**
     * Pure function: returns a configuration carrying the cards added to the defaults since it was
     * written, or the configuration itself when it is already up to date.
     *
     * Referrers became a default card in CMM-2583, and sites configured before that - which is any
     * site whose owner has picked a period, metric or chart type, since those are stored alongside
     * the card list - would otherwise keep a list without it and have to discover the card in the
     * add-card list.
     *
     * Each step is gated on its own revision, so that adding a step for a later revision cannot
     * re-run an earlier one. Stamping the version keeps every step a one-off: removing a card
     * once its step has run sticks, and no card other than the one a step adds is ever added,
     * moved or restored. The stored data cannot tell "never had this card" apart from "had it and
     * removed it", so a user who had removed Referrers themselves does get it back - once.
     */
    private fun addNewDefaultCards(config: StatsCardsConfiguration): StatsCardsConfiguration {
        if (config.defaultCardsVersion >= StatsCardsConfiguration.CURRENT_DEFAULT_CARDS_VERSION) {
            return config
        }
        var visibleCards = config.visibleCards
        if (config.defaultCardsVersion < StatsCardsConfiguration.VERSION_REFERRERS) {
            visibleCards = withReferrers(visibleCards)
        }
        return config.copy(
            visibleCards = visibleCards,
            defaultCardsVersion = StatsCardsConfiguration.CURRENT_DEFAULT_CARDS_VERSION
        )
    }

    /** [visibleCards] with Referrers added, or unchanged when the card is already visible. */
    private fun withReferrers(visibleCards: List<StatsCardType>): List<StatsCardType> {
        if (StatsCardType.MOST_VIEWED_REFERRERS in visibleCards) {
            return visibleCards
        }
        val updated = visibleCards.toMutableList()
        // Under Posts & pages, as in the defaults, falling back to the end of the list when the
        // user has removed that card.
        val anchorIndex = updated.indexOf(StatsCardType.MOST_VIEWED_POSTS_AND_PAGES)
        val insertIndex = if (anchorIndex >= 0) anchorIndex + 1 else updated.size
        updated.add(insertIndex, StatsCardType.MOST_VIEWED_REFERRERS)
        return updated
    }

    @Suppress("TooGenericExceptionCaught")
    private fun loadConfiguration(siteId: Long): StatsCardsConfiguration {
        val json = appPrefsWrapper.getStatsCardsConfigurationJson(siteId)
        if (json == null) {
            return defaultConfiguration()
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
        val defaultConfig = defaultConfiguration()
        persistConfiguration(siteId, defaultConfig)
        return defaultConfig
    }

    /** A fresh configuration, already carrying the default cards this build ships. */
    private fun defaultConfiguration() = StatsCardsConfiguration(
        defaultCardsVersion = StatsCardsConfiguration.CURRENT_DEFAULT_CARDS_VERSION
    )
}
