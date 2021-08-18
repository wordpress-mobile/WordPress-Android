package org.wordpress.android.ui.mysite.quickstart

import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.mysite.MySiteItem.QuickStartBlock
import org.wordpress.android.ui.mysite.QuickStartRepository.QuickStartCategory
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText

@InternalCoroutinesApi
class QuickStartBlockBuilderTest : BaseUnitTest() {
    private lateinit var builder: QuickStartBlockBuilder

    private val completedTasks: List<QuickStartTaskDetails> = listOf(QuickStartTaskDetails.UPDATE_SITE_TITLE)
    private val uncompletedTasks: List<QuickStartTaskDetails> = listOf(QuickStartTaskDetails.VIEW_SITE_TUTORIAL)
    private val onItemClick: (QuickStartTaskType) -> Unit = {}
    private val onRemoveMenuItemClick: () -> Unit = {}

    @Before
    fun setUp() {
        builder = QuickStartBlockBuilder()
    }

    /* ICON */

    @Test
    fun `when block is built, then icon exists`() {
        val quickStartBlock = buildQuickStartBlock()

        assertThat(quickStartBlock.icon).isEqualTo(R.drawable.ic_list_checkmark_white_24dp)
    }

    /* TITLE */

    @Test
    fun `when block is built, then title exists`() {
        val quickStartBlock = buildQuickStartBlock()

        assertThat(quickStartBlock.title).isEqualTo(UiStringRes(R.string.quick_start_sites))
    }

    /* TASK TYPE ITEM */

    @Test
    fun `when block is built, then customise quick start task type item exists`() {
        val quickStartBlock = buildQuickStartBlock()

        assertThat(quickStartBlock.taskTypeItems.map { it.quickStartTaskType }).contains(QuickStartTaskType.CUSTOMIZE)
    }

    @Test
    fun `when block is built, then grow quick start task type item exists`() {
        val quickStartBlock = buildQuickStartBlock()

        assertThat(quickStartBlock.taskTypeItems.map { it.quickStartTaskType }).contains(QuickStartTaskType.GROW)
    }

    /* TASK TYPE ITEM TITLE */

    @Test
    fun `given uncompleted tasks exist, when block is built, then title is enabled`() {
        val quickStartBlock = buildQuickStartBlock()

        assertThat(getQuickStartTaskTypeItem(quickStartBlock).titleEnabled).isTrue
    }

    @Test
    fun `given uncompleted tasks do not exist, when block is built, then title is disabled`() {
        val quickStartBlock = buildQuickStartBlock(uncompletedTasks = emptyList())

        assertThat(getQuickStartTaskTypeItem(quickStartBlock).titleEnabled).isFalse
    }

    @Test
    fun `when customize task type item is built, then customize title exists`() {
        val quickStartBlock = buildQuickStartBlock()

        assertThat(getQuickStartTaskTypeItem(quickStartBlock, QuickStartTaskType.CUSTOMIZE).title)
                .isEqualTo(UiStringRes(R.string.quick_start_sites_type_customize))
    }

    @Test
    fun `when grow task type item is built, then grow title exists`() {
        val quickStartBlock = buildQuickStartBlock()

        assertThat(getQuickStartTaskTypeItem(quickStartBlock, QuickStartTaskType.GROW).title)
                .isEqualTo(UiStringRes(R.string.quick_start_sites_type_grow))
    }

    @Test
    fun `given uncompleted tasks exist, when block is built, then title is not struck through`() {
        val quickStartBlock = buildQuickStartBlock()

        assertThat(getQuickStartTaskTypeItem(quickStartBlock).strikeThroughTitle).isFalse
    }

    @Test
    fun `given uncompleted tasks do not exist, when block is built, then title is struck through`() {
        val quickStartBlock = buildQuickStartBlock(uncompletedTasks = emptyList())

        assertThat(getQuickStartTaskTypeItem(quickStartBlock).strikeThroughTitle).isTrue
    }

    /* TASK TYPE ITEM SUBTITLE */

    @Test
    fun `when block is built, then task type item subtitle contains completed amd uncompleted count`() {
        val quickStartBlock = buildQuickStartBlock()

        assertThat(getQuickStartTaskTypeItem(quickStartBlock).subtitle)
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
    fun `given non zero completed tasks, when block is built, then completed tasks progress is non zero`() {
        val quickStartBlock = buildQuickStartBlock()

        val percentCompleted = 50
        assertThat(getQuickStartTaskTypeItem(quickStartBlock).progress).isEqualTo(percentCompleted)
    }

    @Test
    fun `given zero completed tasks, when block is built, then completed tasks progress is zero`() {
        val quickStartBlock = buildQuickStartBlock(emptyList())

        assertThat(getQuickStartTaskTypeItem(quickStartBlock).progress).isEqualTo(0)
    }

    @Test
    fun `when block is built, then progress color equals primary color`() {
        val quickStartBlock = buildQuickStartBlock(emptyList())

        assertThat(getQuickStartTaskTypeItem(quickStartBlock).progressColor).isEqualTo(R.color.colorPrimary)
    }

    /* ITEM CLICK */

    @Test
    fun `when block is built, then on click action is set on the task type item`() {
        val quickStartBlock = buildQuickStartBlock()

        val taskTypeItem = getQuickStartTaskTypeItem(quickStartBlock)
        assertThat(taskTypeItem.onClick)
                .isEqualTo(ListItemInteraction.create(taskTypeItem.quickStartTaskType, onItemClick))
    }

    /* REMOVE MENU ITEM */

    @Test
    fun `when block is built, then remove menu item click is set on the block`() {
        val quickStartBlock = buildQuickStartBlock()

        assertThat(quickStartBlock.onRemoveMenuItemClick).isNotNull
    }

    private fun buildQuickStartBlock(
        completedTasks: List<QuickStartTaskDetails>? = null,
        uncompletedTasks: List<QuickStartTaskDetails>? = null
    ): QuickStartBlock {
        val customizeCategory = buildQuickStartCategory(QuickStartTaskType.CUSTOMIZE, completedTasks, uncompletedTasks)
        val growCategory = buildQuickStartCategory(QuickStartTaskType.GROW, completedTasks, uncompletedTasks)
        return builder.build(listOf(customizeCategory, growCategory), onRemoveMenuItemClick, onItemClick)
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
        block: QuickStartBlock,
        type: QuickStartTaskType = QuickStartTaskType.CUSTOMIZE
    ) = block.taskTypeItems.first { it.quickStartTaskType == type }
}
