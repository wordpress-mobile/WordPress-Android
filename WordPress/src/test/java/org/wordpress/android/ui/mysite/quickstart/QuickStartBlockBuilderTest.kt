package org.wordpress.android.ui.mysite.quickstart

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.mysite.MySiteItem.QuickStartBlock
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText

@InternalCoroutinesApi
class QuickStartBlockBuilderTest : BaseUnitTest() {
    private lateinit var builder: QuickStartBlockBuilder

    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var quickStartStore: QuickStartStore
    @Mock lateinit var site: SiteModel

    private val siteId = 1L
    private val completedTasks: List<QuickStartTask> = listOf(QuickStartTask.UPDATE_SITE_TITLE)
    private val uncompletedTasks: List<QuickStartTask> = listOf(QuickStartTask.VIEW_SITE)

    @Before
    fun setUp() {
        builder = QuickStartBlockBuilder(selectedSiteRepository, quickStartStore)
        site.siteId = siteId
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
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
                                R.string.quick_start_sites_type_subtitle,
                                listOf(
                                        UiStringText("${completedTasks.size}"),
                                        UiStringText("${completedTasks.size + uncompletedTasks.size}")
                                )
                        )
                )
    }

    private fun buildQuickStartBlock(
        completedTasks: List<QuickStartTask>? = null,
        uncompletedTasks: List<QuickStartTask>? = null
    ): QuickStartBlock {
        whenever(quickStartStore.getCompletedTasksByType(anyLong(), any()))
                .thenReturn(completedTasks ?: this.completedTasks)
        whenever(quickStartStore.getUncompletedTasksByType(anyLong(), any()))
                .thenReturn(uncompletedTasks ?: this.uncompletedTasks)
        return builder.build()
    }

    private fun getQuickStartTaskTypeItem(
        block: QuickStartBlock,
        type: QuickStartTaskType = QuickStartTaskType.CUSTOMIZE
    ) = block.taskTypeItems.first { it.quickStartTaskType == type }
}
