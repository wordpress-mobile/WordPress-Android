package org.wordpress.android.ui.mysite

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

class QuickStartItemBuilder
@Inject constructor() {
    fun build(
        quickStartCategory: QuickStartCategory,
        onQuickStartCardMoreClick: (String) -> Unit
    ): QuickStartCard {
        val tasks = mutableListOf<QuickStartTaskItem>()
        tasks.addAll(quickStartCategory.uncompletedTasks.map { it.toUiItem(false) })
        tasks.addAll(quickStartCategory.completedTasks.map { it.toUiItem(true) })
        return QuickStartCard(
                quickStartCategory.taskType.name,
                UiStringRes(getTitle(quickStartCategory.taskType)),
                tasks,
                R.color.green_20,
                ListItemInteraction.create(quickStartCategory.taskType.name, onQuickStartCardMoreClick)
        )
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
