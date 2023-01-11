package org.wordpress.android.ui.mysite.cards.quickstart

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickStartCardBuilderParams
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository.QuickStartCategory
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText

@ExperimentalCoroutinesApi
class QuickStartCardBuilderTest : BaseUnitTest() {
    private lateinit var builder: QuickStartCardBuilder

    private val completedTasks: List<QuickStartTaskDetails> = listOf(QuickStartTaskDetails.UPDATE_SITE_TITLE)
    private val uncompletedTasks: List<QuickStartTaskDetails> = listOf(QuickStartTaskDetails.VIEW_SITE_TUTORIAL)
    private val onItemClick: (QuickStartTaskType) -> Unit = {}
    private val onRemoveMenuItemClick: () -> Unit = {}

    @Before
    fun setUp() {
        builder = QuickStartCardBuilder()
    }

    /* TITLE */

    @Test
    fun `when card is built, then title exists`() {
        val quickStartCard = buildQuickStartCard()

        assertThat(quickStartCard.title).isEqualTo(UiStringRes(R.string.quick_start_sites))
    }

    /* TASK TYPE ITEM */

    @Test
    fun `when card is built, then customise quick start task type item exists`() {
        val quickStartCard = buildQuickStartCard()

        assertThat(quickStartCard.taskTypeItems.map { it.quickStartTaskType }).contains(QuickStartTaskType.CUSTOMIZE)
    }

    @Test
    fun `when card is built, then grow quick start task type item exists`() {
        val quickStartCard = buildQuickStartCard()

        assertThat(quickStartCard.taskTypeItems.map { it.quickStartTaskType }).contains(QuickStartTaskType.GROW)
    }

    /* TASK TYPE ITEM TITLE */

    @Test
    fun `given uncompleted tasks exist, when card is built, then title is enabled`() {
        val quickStartCard = buildQuickStartCard()

        assertThat(getQuickStartTaskTypeItem(quickStartCard).titleEnabled).isTrue
    }

    @Test
    fun `given uncompleted tasks do not exist, when card is built, then title is disabled`() {
        val quickStartCard = buildQuickStartCard(uncompletedTasks = emptyList())

        assertThat(getQuickStartTaskTypeItem(quickStartCard).titleEnabled).isFalse
    }

    @Test
    fun `when customize task type item is built, then customize title exists`() {
        val quickStartCard = buildQuickStartCard()

        assertThat(getQuickStartTaskTypeItem(quickStartCard, QuickStartTaskType.CUSTOMIZE).title)
            .isEqualTo(UiStringRes(R.string.quick_start_sites_type_customize))
    }

    @Test
    fun `when grow task type item is built, then grow title exists`() {
        val quickStartCard = buildQuickStartCard()

        assertThat(getQuickStartTaskTypeItem(quickStartCard, QuickStartTaskType.GROW).title)
            .isEqualTo(UiStringRes(R.string.quick_start_sites_type_grow))
    }

    @Test
    fun `given uncompleted tasks exist, when card is built, then title is not struck through`() {
        val quickStartCard = buildQuickStartCard()

        assertThat(getQuickStartTaskTypeItem(quickStartCard).strikeThroughTitle).isFalse
    }

    @Test
    fun `given uncompleted tasks do not exist, when card is built, then title is struck through`() {
        val quickStartCard = buildQuickStartCard(uncompletedTasks = emptyList())

        assertThat(getQuickStartTaskTypeItem(quickStartCard).strikeThroughTitle).isTrue
    }

    /* TASK TYPE ITEM SUBTITLE */

    @Test
    fun `when card is built, then task type item subtitle contains completed amd uncompleted count`() {
        val quickStartCard = buildQuickStartCard()

        assertThat(getQuickStartTaskTypeItem(quickStartCard).subtitle)
            .isEqualTo(
                UiStringResWithParams(
                    R.string.quick_start_sites_type_tasks_completed,
                    listOf(
                        UiStringText("${completedTasks.size}"),
                        UiStringText("${completedTasks.size + uncompletedTasks.size}")
                    )
                )
            )
    }

    /* TASK TYPE ITEM PROGRESS BAR */

    @Test
    fun `given non zero completed tasks, when card is built, then completed tasks progress is non zero`() {
        val quickStartCard = buildQuickStartCard()

        val percentCompleted = 50
        assertThat(getQuickStartTaskTypeItem(quickStartCard).progress).isEqualTo(percentCompleted)
    }

    @Test
    fun `given zero completed tasks, when card is built, then completed tasks progress is zero`() {
        val quickStartCard = buildQuickStartCard(emptyList())

        assertThat(getQuickStartTaskTypeItem(quickStartCard).progress).isEqualTo(0)
    }

    @Test
    fun `when card is built, then progress color equals primary color`() {
        val quickStartCard = buildQuickStartCard(emptyList())

        assertThat(getQuickStartTaskTypeItem(quickStartCard).progressColor).isEqualTo(R.color.colorPrimary)
    }

    /* ITEM CLICK */

    @Test
    fun `when card is built, then on click action is set on the task type item`() {
        val quickStartCard = buildQuickStartCard()

        val taskTypeItem = getQuickStartTaskTypeItem(quickStartCard)
        assertThat(taskTypeItem.onClick)
            .isEqualTo(ListItemInteraction.create(taskTypeItem.quickStartTaskType, onItemClick))
    }

    /* MORE MENU */

    @Test
    fun `when card is built, then more menu is visible`() {
        val quickStartCard = buildQuickStartCard()

        assertThat(quickStartCard.moreMenuVisible).isTrue
    }

    /* REMOVE MENU ITEM */

    @Test
    fun `when card is built, then remove menu item click is set on the card`() {
        val quickStartCard = buildQuickStartCard()

        assertThat(quickStartCard.onRemoveMenuItemClick).isNotNull
    }

    private fun buildQuickStartCard(
        completedTasks: List<QuickStartTaskDetails>? = null,
        uncompletedTasks: List<QuickStartTaskDetails>? = null
    ): QuickStartCard {
        val customizeCategory = buildQuickStartCategory(QuickStartTaskType.CUSTOMIZE, completedTasks, uncompletedTasks)
        val growCategory = buildQuickStartCategory(QuickStartTaskType.GROW, completedTasks, uncompletedTasks)
        return builder.build(
            QuickStartCardBuilderParams(
                listOf(customizeCategory, growCategory),
                onRemoveMenuItemClick,
                onItemClick
            )
        )
    }

    private fun buildQuickStartCategory(
        taskType: QuickStartTaskType,
        completedTasks: List<QuickStartTaskDetails>?,
        uncompletedTasks: List<QuickStartTaskDetails>?
    ): QuickStartCategory {
        return QuickStartCategory(
            taskType = taskType,
            completedTasks = completedTasks ?: this.completedTasks,
            uncompletedTasks = uncompletedTasks ?: this.uncompletedTasks
        )
    }

    private fun getQuickStartTaskTypeItem(
        card: QuickStartCard,
        type: QuickStartTaskType = QuickStartTaskType.CUSTOMIZE
    ) = card.taskTypeItems.first { it.quickStartTaskType == type }
}
