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

    /* ICON */

    @Test
    fun `given uncompleted tasks exist, when block is built, then icon is enabled`() {
        val quickStartBlock = buildQuickStartBlock()

        assertThat(getQuickStartTaskTypeItem(quickStartBlock).iconEnabled).isTrue
    }

    @Test
    fun `given uncompleted tasks do not exist, when block is built, then icon is disabled`() {
        val quickStartBlock = buildQuickStartBlock(uncompletedTasks = emptyList())

        assertThat(getQuickStartTaskTypeItem(quickStartBlock).iconEnabled).isFalse
    }

    @Test
    fun `when customize task type item is built, then customize icon exists`() {
        val quickStartBlock = buildQuickStartBlock()

        assertThat(getQuickStartTaskTypeItem(quickStartBlock, QuickStartTaskType.CUSTOMIZE).icon)
                .isEqualTo(R.drawable.bg_oval_primary_40_customize_white_40dp_selector)
    }

    @Test
    fun `given uncompleted tasks exist, when grow task type item is built, then grow icon exists`() {
        val quickStartBlock = buildQuickStartBlock()

        assertThat(getQuickStartTaskTypeItem(quickStartBlock, QuickStartTaskType.GROW).icon)
                .isEqualTo(R.drawable.bg_oval_blue_50_multiple_users_white_40dp)
    }

    @Test
    fun `given uncompleted tasks do not exist, when grow task type item is built, then grow icon exists`() {
        val quickStartBlock = buildQuickStartBlock(uncompletedTasks = emptyList())

        assertThat(getQuickStartTaskTypeItem(quickStartBlock, QuickStartTaskType.GROW).icon)
                .isEqualTo(R.drawable.bg_oval_neutral_30_multiple_users_white_40dp)
    }

    /* TITLE */

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

    /* SUBTITLE */

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
