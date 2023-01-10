package org.wordpress.android.ui.mysite.dynamiccards

import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.mysite.MySiteCardAndItem.DynamicCard
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository.QuickStartCategory
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuFragment.DynamicCardMenuModel
import org.wordpress.android.ui.mysite.dynamiccards.quickstart.QuickStartItemBuilder
import org.wordpress.android.util.config.QuickStartDynamicCardsFeatureConfig
import javax.inject.Inject

class DynamicCardsBuilder @Inject constructor(
    private val quickStartDynamicCardsFeatureConfig: QuickStartDynamicCardsFeatureConfig,
    private val quickStartItemBuilder: QuickStartItemBuilder
) {
    fun build(
        quickStartCategories: List<QuickStartCategory>,
        pinnedDynamicCard: DynamicCardType?,
        visibleDynamicCards: List<DynamicCardType>,
        onDynamicCardMoreClick: (DynamicCardMenuModel) -> Unit,
        onQuickStartTaskCardClick: (QuickStartTask) -> Unit
    ): List<DynamicCard> {
        val dynamicCards = mutableListOf<DynamicCard>().also { list ->
            // Add all possible future dynamic cards here. If we ever have a remote source of dynamic cards, we'd
            // need to implement a smarter solution where we'd build the sources based on the dynamic cards.
            // This means that the stream of dynamic cards would emit a new stream for each of the cards. The
            // current solution is good enough for a few sources.
            if (quickStartDynamicCardsFeatureConfig.isEnabled()) {
                list.addAll(quickStartCategories
                    .filter { (it.completedTasks + it.uncompletedTasks).isNotEmpty() }.map { category ->
                        quickStartItemBuilder.build(
                            category,
                            pinnedDynamicCard,
                            onDynamicCardMoreClick,
                            onQuickStartTaskCardClick
                        )
                    })
            }
        }.associateBy { it.dynamicCardType }
        return visibleDynamicCards.mapNotNull { dynamicCardType -> dynamicCards[dynamicCardType] }
    }
}
