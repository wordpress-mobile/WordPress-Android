package org.wordpress.android.fluxc.model

data class DynamicCardsModel(
    val pinnedItem: DynamicCardType? = null,
    val dynamicCardTypes: List<DynamicCardType> = listOf()
)
