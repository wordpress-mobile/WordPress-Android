package org.wordpress.android.ui.mysite.quickstart

import org.wordpress.android.R
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.mysite.MySiteItem.QuickStartBlock
import org.wordpress.android.ui.mysite.MySiteItem.QuickStartBlock.QuickStartTaskTypeItem
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import javax.inject.Inject

class QuickStartBlockBuilder
@Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val quickStartStore: QuickStartStore
) {
    fun build(): QuickStartBlock {
        val localSiteId = appPrefsWrapper.getSelectedSite().toLong()

        val taskTypeItems = mutableListOf<QuickStartTaskTypeItem>()
        taskTypeItems.add(buildQuickStartTaskTypeItem(localSiteId, QuickStartTaskType.CUSTOMIZE))
        taskTypeItems.add(buildQuickStartTaskTypeItem(localSiteId, QuickStartTaskType.GROW))

        return QuickStartBlock(taskTypeItems = taskTypeItems)
    }

    private fun buildQuickStartTaskTypeItem(
        localSiteId: Long,
        quickStartTaskType: QuickStartTaskType
    ): QuickStartTaskTypeItem {
        val countCompleted = quickStartStore.getCompletedTasksByType(localSiteId, quickStartTaskType).size
        val countUncompleted = quickStartStore.getUncompletedTasksByType(localSiteId, quickStartTaskType).size

        return QuickStartTaskTypeItem(
                quickStartTaskType = quickStartTaskType,
                icon = getIcon(taskType = quickStartTaskType, isCompleted = countUncompleted == 0),
                iconEnabled = countUncompleted > 0,
                title = UiStringRes(getTitle(quickStartTaskType)),
                titleEnabled = countUncompleted > 0,
                subtitle = UiStringResWithParams(
                        R.string.quick_start_sites_type_subtitle,
                        listOf(
                                UiStringText("$countCompleted"),
                                UiStringText("${countCompleted + countUncompleted}")
                        )
                ),
                strikeThroughTitle = countUncompleted == 0
        )
    }

    private fun getTitle(taskType: QuickStartTaskType): Int {
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
