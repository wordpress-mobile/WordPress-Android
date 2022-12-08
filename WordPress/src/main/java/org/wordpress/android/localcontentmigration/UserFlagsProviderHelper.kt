package org.wordpress.android.localcontentmigration

import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.QuickStartStatusModel
import org.wordpress.android.fluxc.model.QuickStartTaskModel
import org.wordpress.android.localcontentmigration.LocalContentEntityData.UserFlagsData
import org.wordpress.android.ui.prefs.AppPrefs.DeletablePrefKey
import org.wordpress.android.ui.prefs.AppPrefs.UndeletablePrefKey
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class UserFlagsProviderHelper @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    contextProvider: ContextProvider,
) : LocalDataProviderHelper {
    override fun getData(localEntityId: Int?): LocalContentEntityData =
            UserFlagsData(
                    flags = appPrefsWrapper.getAllPrefs().filter(::shouldInclude),
                    quickStartStatusList = getAllQuickStartStatus(),
                    quickStartTaskList = getAllQuickStartTask(),
            )

    private fun shouldInclude(flag: Map.Entry<String, Any?>) =
            userFlagsKeysSet.contains(flag.key) || userFlagsCompositeKeysSet.any { flag.key.startsWith(it) }

    private val userFlagsCompositeKeysSet: Set<String> = setOf(
            DeletablePrefKey.LAST_SELECTED_QUICK_START_TYPE.name,
            DeletablePrefKey.SHOULD_SHOW_WEEKLY_ROUNDUP_NOTIFICATION.name,
    )

    private fun getAllQuickStartTask() = checkNotNull(WellSql.select(QuickStartTaskModel::class.java).asModel) {
        "Provider failed because QuickStart task list was null."
    }

    private fun getAllQuickStartStatus() = checkNotNull(WellSql.select(QuickStartStatusModel::class.java).asModel) {
        "Provider failed because QuickStart status list was null."
    }

    private val userFlagsKeysSet: Set<String> = setOf(
            DeletablePrefKey.MAIN_PAGE_INDEX.name,
            DeletablePrefKey.IMAGE_OPTIMIZE_ENABLED.name,
            DeletablePrefKey.IMAGE_OPTIMIZE_WIDTH.name,
            DeletablePrefKey.IMAGE_OPTIMIZE_QUALITY.name,
            DeletablePrefKey.VIDEO_OPTIMIZE_ENABLED.name,
            DeletablePrefKey.VIDEO_OPTIMIZE_WIDTH.name,
            DeletablePrefKey.VIDEO_OPTIMIZE_QUALITY.name,
            DeletablePrefKey.STRIP_IMAGE_LOCATION.name,
            DeletablePrefKey.SUPPORT_EMAIL.name,
            DeletablePrefKey.SUPPORT_NAME.name,
            DeletablePrefKey.GUTENBERG_DEFAULT_FOR_NEW_POSTS.name,
            DeletablePrefKey.USER_IN_GUTENBERG_ROLLOUT_GROUP.name,
            DeletablePrefKey.SHOULD_AUTO_ENABLE_GUTENBERG_FOR_THE_NEW_POSTS.name,
            DeletablePrefKey.SHOULD_AUTO_ENABLE_GUTENBERG_FOR_THE_NEW_POSTS_PHASE_2.name,
            DeletablePrefKey.GUTENBERG_OPT_IN_DIALOG_SHOWN.name,
            DeletablePrefKey.GUTENBERG_FOCAL_POINT_PICKER_TOOLTIP_SHOWN.name,
            DeletablePrefKey.IS_QUICK_START_NOTICE_REQUIRED.name,
            DeletablePrefKey.LAST_SKIPPED_QUICK_START_TASK.name,
            DeletablePrefKey.POST_LIST_AUTHOR_FILTER.name,
            DeletablePrefKey.POST_LIST_VIEW_LAYOUT_TYPE.name,
            DeletablePrefKey.AZTEC_EDITOR_DISABLE_HW_ACC_KEYS.name,
            DeletablePrefKey.READER_DISCOVER_WELCOME_BANNER_SHOWN.name,
            DeletablePrefKey.SHOULD_SCHEDULE_CREATE_SITE_NOTIFICATION.name,
            DeletablePrefKey.SELECTED_SITE_LOCAL_ID.name,
            DeletablePrefKey.RECENTLY_PICKED_SITE_IDS.name,
            UndeletablePrefKey.THEME_IMAGE_SIZE_WIDTH.name,
            UndeletablePrefKey.BOOKMARKS_SAVED_LOCALLY_DIALOG_SHOWN.name,
            UndeletablePrefKey.IMAGE_OPTIMIZE_PROMO_REQUIRED.name,
            UndeletablePrefKey.SWIPE_TO_NAVIGATE_NOTIFICATIONS.name,
            UndeletablePrefKey.SWIPE_TO_NAVIGATE_READER.name,
            UndeletablePrefKey.IS_MAIN_FAB_TOOLTIP_DISABLED.name,
            UndeletablePrefKey.SHOULD_SHOW_STORIES_INTRO.name,
            UndeletablePrefKey.SHOULD_SHOW_STORAGE_WARNING.name,
            UndeletablePrefKey.LAST_USED_USER_ID.name,
            contextProvider.getContext().getString(R.string.pref_key_app_theme),
    )
}
