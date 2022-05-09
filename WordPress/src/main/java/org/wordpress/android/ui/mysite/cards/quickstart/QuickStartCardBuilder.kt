package org.wordpress.android.ui.mysite.cards.quickstart

import org.wordpress.android.R
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard.QuickStartTaskTypeItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickStartCardBuilderParams
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository.QuickStartCategory
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import javax.inject.Inject
import kotlin.math.roundToInt

class QuickStartCardBuilder @Inject constructor() {
    fun build(params: QuickStartCardBuilderParams) = QuickStartCard(
            title = UiStringRes(R.string.quick_start_sites),
            onRemoveMenuItemClick = ListItemInteraction.create(params.onQuickStartBlockRemoveMenuItemClick),
            taskTypeItems = params.quickStartCategories.map {
                buildQuickStartTaskTypeItem(
                        it,
                        params.onQuickStartTaskTypeItemClick
                )
            }
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
                progressColor = R.color.colorPrimary,
                progress = getProgress(countCompleted, countCompleted + countUncompleted),
                onClick = ListItemInteraction.create(quickStartTaskType, onItemClick)
        )
    }

    fun getTitle(taskType: QuickStartTaskType): Int {
        return when (taskType) {
            QuickStartTaskType.CUSTOMIZE -> R.string.quick_start_sites_type_customize
            QuickStartTaskType.GROW -> R.string.quick_start_sites_type_grow
            QuickStartTaskType.GET_TO_KNOW_APP -> R.string.quick_start_sites_type_get_to_know_app
            QuickStartTaskType.UNKNOWN -> throw IllegalArgumentException(UNEXPECTED_QUICK_START_TYPE)
        }
    }

    private fun getProgress(countCompleted: Int, totalCount: Int) =
            if (totalCount > 0) ((countCompleted / totalCount.toFloat()) * PERCENT_HUNDRED).roundToInt() else 0

    companion object {
        private const val UNEXPECTED_QUICK_START_TYPE = "Unexpected quick start type"
        private const val PERCENT_HUNDRED = 100
    }
}
