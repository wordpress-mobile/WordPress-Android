package org.wordpress.android.ui.mysite.cards.dynamiccard

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.DynamicCardsModel.DynamicCardRowModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.DynamicCardsModel.DynamicCardModel

/* DYNAMIC CARDS */
private const val DYNAMIC_CARD_ID = "year_in_review_2023"
private const val DYNAMIC_CARD_TITLE = "News"
private const val DYNAMIC_CARD_REMOTE_FEATURE_FLAG = "dynamic_dashboard_cards"
private const val DYNAMIC_CARD_FEATURED_IMAGE = "https://path/to/image"
private const val DYNAMIC_CARD_URL = "https://wordpress.com"
private const val DYNAMIC_CARD_ACTION = "Call to action"
private const val DYNAMIC_CARD_ORDER = "top"
private const val DYNAMIC_CARD_ROW_ICON = "https://path/to/image"
private const val DYNAMIC_CARD_ROW_TITLE = "Row title"
private const val DYNAMIC_CARD_ROW_DESCRIPTION = "Row description"

private val DYNAMIC_CARD_ROW_MODEL = DynamicCardRowModel(
    icon = DYNAMIC_CARD_ROW_ICON,
    title = DYNAMIC_CARD_ROW_TITLE,
    description = DYNAMIC_CARD_ROW_DESCRIPTION
)

private val DYNAMIC_CARD_MODEL = DynamicCardModel(
    id = DYNAMIC_CARD_ID,
    title = DYNAMIC_CARD_TITLE,
    remoteFeatureFlag = DYNAMIC_CARD_REMOTE_FEATURE_FLAG,
    featuredImage = DYNAMIC_CARD_FEATURED_IMAGE,
    url = DYNAMIC_CARD_URL,
    action = DYNAMIC_CARD_ACTION,
    order = CardModel.DynamicCardsModel.CardOrder.fromString(DYNAMIC_CARD_ORDER),
    rows = listOf(DYNAMIC_CARD_ROW_MODEL)
)

private val DYNAMIC_CARDS_MODEL = CardModel.DynamicCardsModel(
    pages = listOf(DYNAMIC_CARD_MODEL)
)

@OptIn(ExperimentalCoroutinesApi::class)
class DynamicCardsViewModelSliceTest : BaseUnitTest() {
    lateinit var dynamicCardsViewModelSlice: DynamicCardsViewModelSlice

    @Before
    fun setUp() {
        dynamicCardsViewModelSlice = DynamicCardsViewModelSlice()
    }

    @Test
    fun testGetBuilderParams() {
        val builderParams = dynamicCardsViewModelSlice.getBuilderParams(DYNAMIC_CARDS_MODEL)
        assertThat(builderParams.dynamicCards).isEqualTo(DYNAMIC_CARDS_MODEL)
    }
}
