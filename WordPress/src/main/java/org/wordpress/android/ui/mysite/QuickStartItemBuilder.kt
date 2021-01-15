package org.wordpress.android.ui.mysite

import androidx.annotation.ColorRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.UNKNOWN
import org.wordpress.android.ui.mysite.MySiteItem.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteItem.QuickStartCard.QuickStartTaskItem
import org.wordpress.android.ui.mysite.QuickStartRepository.QuickStartCategory
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject
import kotlin.math.roundToInt

class QuickStartItemBuilder
@Inject constructor() {
    fun build(
        quickStartCategory: QuickStartCategory,
        onQuickStartCardMoreClick: (String) -> Unit
    ): QuickStartCard {
        val tasks = mutableListOf<QuickStartTaskItem>()
        tasks.addAll(quickStartCategory.uncompletedTasks.map { it.toUiItem(false) })
        val completedTasks = quickStartCategory.completedTasks.map { it.toUiItem(true) }
        tasks.addAll(completedTasks)
        val id = quickStartCategory.taskType.toString()
        return QuickStartCard(
                id,
                UiStringRes(getTitle(quickStartCategory.taskType)),
                tasks,
                getAccentColor(quickStartCategory.taskType),
                getProgress(tasks, completedTasks),
                ListItemInteraction.create(id, onQuickStartCardMoreClick)
        )
    }

    private fun getProgress(tasks: List<QuickStartTaskItem>, completedTasks: List<QuickStartTaskItem>): Int {
        return if (tasks.isNotEmpty()) ((completedTasks.size / tasks.size.toFloat()) * 100).roundToInt() else 0
    }

    @ColorRes
    private fun getAccentColor(taskType: QuickStartTaskType): Int {
        return when (taskType) {
            CUSTOMIZE -> R.color.green_20
            GROW -> R.color.orange_40
            UNKNOWN -> throw IllegalArgumentException("Unexpected quick start type")
        }
    }

    private fun getTitle(taskType: QuickStartTaskType): Int {
        return when (taskType) {
            CUSTOMIZE -> R.string.quick_start_sites_type_customize
            GROW -> R.string.quick_start_sites_type_grow
            UNKNOWN -> throw IllegalArgumentException("Unexpected quick start type")
        }
    }

    private fun QuickStartTaskDetails.toUiItem(done: Boolean): QuickStartTaskItem {
        return QuickStartTaskItem(this.task, UiStringRes(this.titleResId), UiStringRes(this.subtitleResId), done)
    }
}
