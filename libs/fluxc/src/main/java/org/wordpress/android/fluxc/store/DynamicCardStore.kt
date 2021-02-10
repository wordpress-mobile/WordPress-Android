package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.fluxc.model.DynamicCardsModel
import org.wordpress.android.fluxc.persistence.DynamicCardSqlUtils
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicCardStore
@Inject constructor(
    private val coroutineEngine: CoroutineEngine,
    private val dynamicCardSqlUtils: DynamicCardSqlUtils
) {
    // Cards should be hidden until the app is restarted, that's why they're not in the DB
    private var hiddenCards = mutableMapOf<Int, Set<DynamicCardType>>()
    suspend fun pinCard(siteId: Int, dynamicCardType: DynamicCardType) =
            coroutineEngine.withDefaultContext(T.MAIN, this, "Pin dynamic card") {
                dynamicCardSqlUtils.pin(siteId, dynamicCardType)
            }

    suspend fun unpinCard(siteId: Int) =
            coroutineEngine.withDefaultContext(T.MAIN, this, "Unpin dynamic card") {
                dynamicCardSqlUtils.unpin(siteId)
            }

    suspend fun removeCard(siteId: Int, dynamicCardType: DynamicCardType) =
            coroutineEngine.withDefaultContext(T.MAIN, this, "Remove dynamic card") {
                dynamicCardSqlUtils.remove(siteId, dynamicCardType)
            }

    suspend fun hideCard(siteId: Int, dynamicCardType: DynamicCardType)  =
            coroutineEngine.withDefaultContext(T.MAIN, this, "Hide dynamic card") {
                val currentHiddenCards = hiddenCards[siteId]?.toMutableSet() ?: mutableSetOf()
                currentHiddenCards.add(dynamicCardType)
                hiddenCards[siteId] = currentHiddenCards
            }

    suspend fun getCards(siteId: Int): DynamicCardsModel =
            coroutineEngine.withDefaultContext(T.MAIN, this, "Get dynamic card") {
                val pinnedCard = dynamicCardSqlUtils.selectPinned(siteId)
                val removedCards = dynamicCardSqlUtils.selectRemoved(siteId).toMutableSet()
                removedCards.addAll(hiddenCards[siteId] ?: setOf())
                val filteredCards = DynamicCardType.values().filter { !removedCards.contains(it) }
                val pinnedCardIndex = if (pinnedCard != null) {
                    filteredCards.indexOf(pinnedCard)
                } else {
                    -1
                }
                if (pinnedCardIndex > -1 && pinnedCard != null) {
                    val mutableFilteredCards = filteredCards.toMutableList()
                    mutableFilteredCards.removeAt(pinnedCardIndex)
                    mutableFilteredCards.add(0, pinnedCard)
                    DynamicCardsModel(pinnedItem = pinnedCard, dynamicCardTypes = mutableFilteredCards)
                } else {
                    DynamicCardsModel(dynamicCardTypes = filteredCards)
                }
            }
}
