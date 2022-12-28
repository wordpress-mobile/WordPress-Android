package org.wordpress.android.ui.mysite.cards.quickstart

import org.wordpress.android.R
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard.QuickStartTaskTypeItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickStartCardBuilderParams
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository.QuickStartCategory
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import javax.inject.Inject
import kotlin.math.roundToInt

class QuickStartCardBuilder @Inject constructor() {
    fun build(params: QuickStartCardBuilderParams) = QuickStartCard(
        title = UiStringRes(R.string.quick_start_sites),
        toolbarVisible = shouldShowCardToolbar(params.quickStartCategories),
        onRemoveMenuItemClick = ListItemInteraction.create(params.onQuickStartBlockRemoveMenuItemClick),
        taskTypeItems = params.quickStartCategories.map {
            buildQuickStartTaskTypeItem(
                it,
                params.onQuickStartTaskTypeItemClick
            )
        }
    )

    private fun shouldShowCardToolbar(quickStartCategories: List<QuickStartCategory>) =
        !isNewQuickStartType(quickStartCategories)

    private fun isNewQuickStartType(quickStartCategories: List<QuickStartCategory>): Boolean {
        return quickStartCategories.any { it.taskType == QuickStartTaskType.GET_TO_KNOW_APP }
    }

    private fun buildQuickStartTaskTypeItem(
        category: QuickStartCategory,
        onItemClick: (QuickStartTaskType) -> Unit
    ): QuickStartTaskTypeItem {
        val quickStartTaskType = category.taskType
        val countCompleted = category.completedTasks.size
        val countUncompleted = category.uncompletedTasks.size
        val progress = getProgress(countCompleted, countCompleted + countUncompleted)
        val isNewQuickStartType = category.taskType == QuickStartTaskType.GET_TO_KNOW_APP

        return QuickStartTaskTypeItem(
            quickStartTaskType = quickStartTaskType,
            title = UiStringRes(getTitle(quickStartTaskType)),
            titleEnabled = countUncompleted > 0,
            subtitle = getSubtitle(
                isNewQuickStartType,
                progress,
                countCompleted,
                countCompleted + countUncompleted
            ),
            strikeThroughTitle = countUncompleted == 0,
            progressColor = getProgressColor(progress, isNewQuickStartType),
            progress = progress,
            onClick = ListItemInteraction.create(quickStartTaskType, onItemClick)
        )
    }

    private fun getSubtitle(
        isNewQuickStartType: Boolean,
        progress: Int,
        countCompleted: Int,
        totalCount: Int
    ): UiString {
        return if (progress == PERCENT_HUNDRED && isNewQuickStartType) {
            UiStringRes(
                R.string.quick_start_sites_type_all_tasks_completed
            )
        } else {
            UiStringResWithParams(
                R.string.quick_start_sites_type_tasks_completed,
                listOf(
                    UiStringText("$countCompleted"),
                    UiStringText("$totalCount")
                )
            )
        }
    }

    fun getProgressColor(progress: Int, isNewQuickStartType: Boolean): Int {
        return if (progress == PERCENT_HUNDRED && isNewQuickStartType) {
            R.color.green_40
        } else {
            R.color.colorPrimary
        }
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
