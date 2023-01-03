package org.wordpress.android.ui.mysite.dynamiccards

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.mysite.MySiteCardAndItem.DynamicCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.DynamicCard.QuickStartDynamicCard
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository.QuickStartCategory
import org.wordpress.android.ui.mysite.dynamiccards.quickstart.QuickStartItemBuilder
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.config.QuickStartDynamicCardsFeatureConfig

@ExperimentalCoroutinesApi
class DynamicCardsBuilderTest : BaseUnitTest() {
    @Mock
    lateinit var quickStartDynamicCardsFeatureConfig: QuickStartDynamicCardsFeatureConfig

    @Mock
    lateinit var quickStartItemBuilder: QuickStartItemBuilder

    private lateinit var dynamicCardsBuilder: DynamicCardsBuilder
    private val quickStartCategory: QuickStartCategory
        get() = QuickStartCategory(
            taskType = QuickStartTaskType.CUSTOMIZE,
            uncompletedTasks = listOf(QuickStartTaskDetails.UPDATE_SITE_TITLE),
            completedTasks = emptyList()
        )

    @Before
    fun setUp() {
        setUpDynamicCardsBuilder()
        setUpQuickStartDynamicCardBuilder()
        whenever(quickStartDynamicCardsFeatureConfig.isEnabled()).thenReturn(true)
    }

    /* QUICK START DYNAMIC CARD */

    @Test
    fun `given quick start is not in progress, when site is selected, then QS dynamic card not built`() {
        val dynamicCards = buildDynamicCards(isQuickStartDynamicCardEnabled = false, isQuickStartInProgress = false)

        assertThat(dynamicCards.findQuickStartDynamicCard()).isNull()
    }

    @Test
    fun `given dynamic card disabled + QS in progress, when site is selected, then QS dynamic card not built`() {
        val dynamicCards = buildDynamicCards(isQuickStartDynamicCardEnabled = false, isQuickStartInProgress = true)

        assertThat(dynamicCards.findQuickStartDynamicCard()).isNull()
    }

    @Test
    fun `given dynamic card enabled + quick start in progress, when site is selected, then QS dynamic card built`() {
        val dynamicCards = buildDynamicCards(isQuickStartDynamicCardEnabled = true, isQuickStartInProgress = true)

        assertThat(dynamicCards.findQuickStartDynamicCard()).isNotNull
    }

    private fun List<DynamicCard>.findQuickStartDynamicCard() = this.find { it is QuickStartDynamicCard }

    private fun buildDynamicCards(
        isQuickStartDynamicCardEnabled: Boolean,
        isQuickStartInProgress: Boolean
    ): List<DynamicCard> {
        whenever(quickStartDynamicCardsFeatureConfig.isEnabled()).thenReturn(isQuickStartDynamicCardEnabled)
        return dynamicCardsBuilder.build(
            quickStartCategories = if (isQuickStartInProgress) listOf(quickStartCategory) else emptyList(),
            pinnedDynamicCard = mock(),
            visibleDynamicCards = listOf(
                DynamicCardType.CUSTOMIZE_QUICK_START,
                DynamicCardType.GROW_QUICK_START
            ),
            onDynamicCardMoreClick = mock(),
            onQuickStartTaskCardClick = mock()
        )
    }

    private fun setUpDynamicCardsBuilder() {
        dynamicCardsBuilder = DynamicCardsBuilder(quickStartDynamicCardsFeatureConfig, quickStartItemBuilder)
    }

    private fun setUpQuickStartDynamicCardBuilder() {
        doAnswer {
            initQuickStartDynamicCard()
        }.whenever(quickStartItemBuilder).build(any(), anyOrNull(), any(), any())
    }

    private fun initQuickStartDynamicCard(): QuickStartDynamicCard {
        return QuickStartDynamicCard(
            id = DynamicCardType.CUSTOMIZE_QUICK_START,
            title = UiStringRes(0),
            taskCards = mock(),
            accentColor = 0,
            progress = 0,
            onMoreClick = mock()
        )
    }
}
