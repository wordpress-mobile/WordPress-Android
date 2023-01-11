package org.wordpress.android.ui.mysite.dynamiccards.quickstart

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GET_TO_KNOW_APP
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.UNKNOWN
import org.wordpress.android.ui.mysite.MySiteCardAndItem.DynamicCard.QuickStartDynamicCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.DynamicCard.QuickStartDynamicCard.QuickStartTaskCard
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository.QuickStartCategory
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuFragment.DynamicCardMenuModel
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject
import kotlin.math.roundToInt

class QuickStartItemBuilder
@Inject constructor(
    val quickStartRepository: QuickStartRepository
) {
    fun build(
        quickStartCategory: QuickStartCategory,
        pinnedDynamicCardType: DynamicCardType?,
        onQuickStartCardMoreClick: (DynamicCardMenuModel) -> Unit,
        onQuickStartTaskClick: (QuickStartTask) -> Unit
    ): QuickStartDynamicCard {
        val accentColor = getAccentColor(quickStartCategory.taskType)
        val tasks = mutableListOf<QuickStartTaskCard>()
        tasks.addAll(quickStartCategory.uncompletedTasks.map { it.toUiItem(false, accentColor, onQuickStartTaskClick) })
        val completedTasks = quickStartCategory.completedTasks.map {
            it.toUiItem(
                true,
                accentColor,
                onQuickStartTaskClick
            )
        }
        tasks.addAll(completedTasks)
        val dynamicCardType = quickStartCategory.taskType.toDynamicCardType()
        val isPinned = pinnedDynamicCardType == dynamicCardType
        return QuickStartDynamicCard(
            dynamicCardType,
            UiStringRes(getTitle(quickStartCategory.taskType)),
            tasks,
            accentColor,
            getProgress(tasks, completedTasks),
            ListItemInteraction.create(DynamicCardMenuModel(dynamicCardType, isPinned), onQuickStartCardMoreClick)
        )
    }

    private fun QuickStartTaskType.toDynamicCardType(): DynamicCardType {
        return when (this) {
            CUSTOMIZE -> DynamicCardType.CUSTOMIZE_QUICK_START
            GROW -> DynamicCardType.GROW_QUICK_START
            GET_TO_KNOW_APP -> DynamicCardType.GET_TO_KNOW_APP_QUICK_START
            UNKNOWN -> throw IllegalArgumentException("Unexpected quick start type")
        }
    }

    private fun getProgress(tasks: List<QuickStartTaskCard>, completedTasks: List<QuickStartTaskCard>): Int {
        return if (tasks.isNotEmpty()) ((completedTasks.size / tasks.size.toFloat()) * 100).roundToInt() else 0
    }

    @ColorRes
    private fun getAccentColor(taskType: QuickStartTaskType): Int {
        return when (taskType) {
            CUSTOMIZE -> R.color.green_20
            GROW -> R.color.orange_40
            GET_TO_KNOW_APP -> R.color.green_20
            UNKNOWN -> throw IllegalArgumentException("Unexpected quick start type")
        }
    }

    private fun getTitle(taskType: QuickStartTaskType): Int {
        return when (taskType) {
            CUSTOMIZE -> R.string.quick_start_sites_type_customize
            GROW -> R.string.quick_start_sites_type_grow
            GET_TO_KNOW_APP -> R.string.quick_start_sites_type_get_to_know_app
            UNKNOWN -> throw IllegalArgumentException("Unexpected quick start type")
        }
    }

    private fun QuickStartTaskDetails.toUiItem(
        done: Boolean,
        accentColor: Int,
        onQuickStartTaskClick: (QuickStartTask) -> Unit
    ): QuickStartTaskCard {
        val task = quickStartRepository.quickStartType.getTaskFromString(this.taskString)
        return QuickStartTaskCard(
            task,
            UiStringRes(this.titleResId),
            UiStringRes(this.subtitleResId),
            getIllustration(task),
            accentColor,
            done,
            ListItemInteraction.create(task, onQuickStartTaskClick)
        )
    }

    @DrawableRes
    private fun getIllustration(task: QuickStartTask): Int {
        return when (task.string) {
            QuickStartStore.QUICK_START_UPDATE_SITE_TITLE_LABEL ->
                R.drawable.img_illustration_quick_start_task_set_site_title
            QuickStartStore.QUICK_START_UPLOAD_SITE_ICON_LABEL ->
                R.drawable.img_illustration_quick_start_task_edit_site_icon
            QuickStartStore.QUICK_START_VIEW_SITE_LABEL -> R.drawable.img_illustration_quick_start_task_visit_your_site
            QuickStartStore.QUICK_START_ENABLE_POST_SHARING_LABEL ->
                R.drawable.img_illustration_quick_start_task_enable_post_sharing
            QuickStartStore.QUICK_START_PUBLISH_POST_LABEL -> R.drawable.img_illustration_quick_start_task_publish_post
            QuickStartStore.QUICK_START_FOLLOW_SITE_LABEL ->
                R.drawable.img_illustration_quick_start_task_follow_other_sites
            QuickStartStore.QUICK_START_CHECK_STATS_LABEL ->
                R.drawable.img_illustration_quick_start_task_check_site_stats
            QuickStartStore.QUICK_START_REVIEW_PAGES_LABEL ->
                R.drawable.img_illustration_quick_start_task_review_site_pages
            QuickStartStore.QUICK_START_CREATE_SITE_LABEL -> R.drawable.img_illustration_quick_start_task_create_site
            QuickStartStore.QUICK_START_CHECK_NOTIFIATIONS_LABEL ->
                R.drawable.img_illustration_quick_start_task_placeholder
            else -> R.drawable.img_illustration_quick_start_task_placeholder
        }
    }
}
