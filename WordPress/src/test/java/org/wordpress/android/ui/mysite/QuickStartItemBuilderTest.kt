package org.wordpress.android.ui.mysite

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.PUBLISH_POST
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.VIEW_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW
import org.wordpress.android.ui.mysite.MySiteItem.DynamicCard.QuickStartCard.QuickStartTaskCard
import org.wordpress.android.ui.mysite.QuickStartRepository.QuickStartCategory
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuFragment.DynamicCardMenuModel
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardType.CUSTOMIZE_QUICK_START
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardType.GROW_QUICK_START
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails.PUBLISH_POST_TUTORIAL
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails.UPDATE_SITE_TITLE
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails.VIEW_SITE_TUTORIAL
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes

@RunWith(MockitoJUnitRunner::class)
class QuickStartItemBuilderTest {
    private val builder = QuickStartItemBuilder()
    private val onQuickStartTaskCardClick: (QuickStartTask) -> Unit = {}

    @Test
    fun `builds a customize category into quick start card`() {
        val quickStartCategory = QuickStartCategory(CUSTOMIZE, listOf(), listOf())
        var clickedId: DynamicCardMenuModel? = null

        val quickStartCard = builder.build(quickStartCategory, null, { id ->
            clickedId = id
        }, onQuickStartTaskCardClick)

        assertThat(quickStartCard.id).isEqualTo(CUSTOMIZE_QUICK_START)
        assertThat(quickStartCard.title).isEqualTo(UiStringRes(R.string.quick_start_sites_type_customize))
        assertThat(quickStartCard.accentColor).isEqualTo(R.color.green_20)
        assertThat(quickStartCard.progress).isEqualTo(0)
        assertThat(quickStartCard.taskCards).isEmpty()

        quickStartCard.onMoreClick.click()
        assertThat(clickedId).isEqualTo(DynamicCardMenuModel(CUSTOMIZE_QUICK_START, false))
    }

    @Test
    fun `builds a pinned dynamic card`() {
        val quickStartCategory = QuickStartCategory(CUSTOMIZE, listOf(), listOf())
        var clickedId: DynamicCardMenuModel? = null

        val quickStartCard = builder.build(quickStartCategory, CUSTOMIZE_QUICK_START, { id ->
            clickedId = id
        }, onQuickStartTaskCardClick)

        assertThat(quickStartCard.id).isEqualTo(CUSTOMIZE_QUICK_START)
        assertThat(quickStartCard.title).isEqualTo(UiStringRes(R.string.quick_start_sites_type_customize))
        assertThat(quickStartCard.accentColor).isEqualTo(R.color.green_20)
        assertThat(quickStartCard.progress).isEqualTo(0)
        assertThat(quickStartCard.taskCards).isEmpty()

        quickStartCard.onMoreClick.click()
        assertThat(clickedId).isEqualTo(DynamicCardMenuModel(CUSTOMIZE_QUICK_START, true))
    }

    @Test
    fun `builds a grow category into quick start card`() {
        val quickStartCategory = QuickStartCategory(GROW, listOf(), listOf())
        var clickedId: DynamicCardMenuModel? = null

        val quickStartCard = builder.build(quickStartCategory, null, { id ->
            clickedId = id
        }, onQuickStartTaskCardClick)

        assertThat(quickStartCard.id).isEqualTo(GROW_QUICK_START)
        assertThat(quickStartCard.title).isEqualTo(UiStringRes(R.string.quick_start_sites_type_grow))
        assertThat(quickStartCard.accentColor).isEqualTo(R.color.orange_40)
        assertThat(quickStartCard.progress).isEqualTo(0)
        assertThat(quickStartCard.taskCards).isEmpty()

        quickStartCard.onMoreClick.click()
        assertThat(clickedId).isEqualTo(DynamicCardMenuModel(GROW_QUICK_START, false))
    }

    @Test
    fun `builds 1 completed and 1 uncompleted task with 50 progress`() {
        val quickStartCategory = QuickStartCategory(CUSTOMIZE, listOf(VIEW_SITE_TUTORIAL), listOf(UPDATE_SITE_TITLE))

        val quickStartCard = builder.build(quickStartCategory, null, { }, onQuickStartTaskCardClick)

        assertThat(quickStartCard.progress).isEqualTo(50)
        assertThat(quickStartCard.taskCards).containsExactly(
                QuickStartTaskCard(
                        VIEW_SITE,
                        UiStringRes(VIEW_SITE_TUTORIAL.titleResId),
                        UiStringRes(VIEW_SITE_TUTORIAL.subtitleResId),
                        R.drawable.img_illustration_quick_start_task_visit_your_site,
                        R.color.green_20,
                        false,
                        ListItemInteraction.create(VIEW_SITE, onQuickStartTaskCardClick)
                ),
                QuickStartTaskCard(
                        QuickStartTask.UPDATE_SITE_TITLE,
                        UiStringRes(UPDATE_SITE_TITLE.titleResId),
                        UiStringRes(UPDATE_SITE_TITLE.subtitleResId),
                        R.drawable.img_illustration_quick_start_task_set_site_title,
                        R.color.green_20,
                        true,
                        ListItemInteraction.create(QuickStartTask.UPDATE_SITE_TITLE, onQuickStartTaskCardClick)
                )
        )
    }

    @Test
    fun `builds 0 completed and 1 uncompleted task with 0 progress`() {
        val quickStartCategory = QuickStartCategory(GROW, listOf(PUBLISH_POST_TUTORIAL), listOf())

        val quickStartCard = builder.build(quickStartCategory, null, { }, onQuickStartTaskCardClick)

        assertThat(quickStartCard.progress).isEqualTo(0)
        assertThat(quickStartCard.taskCards).containsExactly(
                QuickStartTaskCard(
                        PUBLISH_POST,
                        UiStringRes(PUBLISH_POST_TUTORIAL.titleResId),
                        UiStringRes(PUBLISH_POST_TUTORIAL.subtitleResId),
                        R.drawable.img_illustration_quick_start_task_publish_post,
                        R.color.orange_40,
                        false,
                        ListItemInteraction.create(PUBLISH_POST, onQuickStartTaskCardClick)
                )
        )
    }

    @Test
    fun `builds 1 completed and 0 uncompleted task with 100 progress`() {
        val quickStartCategory = QuickStartCategory(CUSTOMIZE, listOf(), listOf(UPDATE_SITE_TITLE))

        val quickStartCard = builder.build(quickStartCategory, null, { }, onQuickStartTaskCardClick)

        assertThat(quickStartCard.progress).isEqualTo(100)
        assertThat(quickStartCard.taskCards).containsExactly(
                QuickStartTaskCard(
                        QuickStartTask.UPDATE_SITE_TITLE,
                        UiStringRes(UPDATE_SITE_TITLE.titleResId),
                        UiStringRes(UPDATE_SITE_TITLE.subtitleResId),
                        R.drawable.img_illustration_quick_start_task_set_site_title,
                        R.color.green_20,
                        true,
                        ListItemInteraction.create(QuickStartTask.UPDATE_SITE_TITLE, onQuickStartTaskCardClick)
                )
        )
    }
}
