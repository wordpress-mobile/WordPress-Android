package org.wordpress.android.ui.mysite.quickstart

import org.wordpress.android.R
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.mysite.MySiteItem.QuickStartBlock
import org.wordpress.android.ui.mysite.MySiteItem.QuickStartBlock.QuickStartTaskTypeItem
import org.wordpress.android.ui.mysite.QuickStartRepository.QuickStartCategory
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import javax.inject.Inject

class QuickStartBlockBuilder @Inject constructor() {
    fun build(
        categories: List<QuickStartCategory>,
        onRemoveMenuItemClick: () -> Unit,
        onItemClick: (QuickStartTaskType) -> Unit
    ) = QuickStartBlock(
            onRemoveMenuItemClick = ListItemInteraction.create { onRemoveMenuItemClick.invoke() },
            taskTypeItems = categories.map { buildQuickStartTaskTypeItem(it, onItemClick) }
    )

    private fun buildQuickStartTaskTypeItem(
        category: QuickStartCategory,
        onItemClick: (QuickStartTaskType) -> Unit
    ): QuickStartTaskTypeItem {
        val quickStartTaskType = category.taskType
        val countCompleted = category.completedTasks.size
        val countUncompleted = category.uncompletedTasks.size

        return QuickStartTaskTypeItem(
                quickStartTaskType = quickStartTaskType,
                icon = getIcon(taskType = quickStartTaskType, isCompleted = countUncompleted == 0),
                iconEnabled = countUncompleted > 0,
                title = UiStringRes(getTitle(quickStartTaskType)),
                titleEnabled = countUncompleted > 0,
                subtitle = UiStringResWithParams(
                        R.string.quick_start_sites_type_tasks_completed,
                        listOf(
                                UiStringText("$countCompleted"),
                                UiStringText("${countCompleted + countUncompleted}")
                        )
                ),
                strikeThroughTitle = countUncompleted == 0,
                onClick = ListItemInteraction.create(quickStartTaskType, onItemClick)
        )
    }

    fun getTitle(taskType: QuickStartTaskType): Int {
        return when (taskType) {
            QuickStartTaskType.CUSTOMIZE -> R.string.quick_start_sites_type_customize
            QuickStartTaskType.GROW -> R.string.quick_start_sites_type_grow
            QuickStartTaskType.UNKNOWN -> throw IllegalArgumentException("Unexpected quick start type")
        }
    }

    private fun getIcon(taskType: QuickStartTaskType, isCompleted: Boolean): Int {
        return when (taskType) {
            QuickStartTaskType.CUSTOMIZE -> R.drawable.bg_oval_primary_40_customize_white_40dp_selector
            QuickStartTaskType.GROW -> if (isCompleted) {
                R.drawable.bg_oval_neutral_30_multiple_users_white_40dp
            } else {
                R.drawable.bg_oval_blue_50_multiple_users_white_40dp
            }
            QuickStartTaskType.UNKNOWN -> throw IllegalArgumentException("Unexpected quick start type")
        }
    }
}
