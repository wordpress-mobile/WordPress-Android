package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.QuickStartTaskModel
import org.wordpress.android.fluxc.persistence.QuickStartSqlUtils
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.UNKNOWN
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GET_TO_KNOW_APP
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuickStartStore @Inject constructor(
    private val quickStartSqlUtils: QuickStartSqlUtils,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    interface QuickStartTask {
        val string: String
        val taskType: QuickStartTaskType
        val order: Int

        companion object {
            fun getAllTasks(): List<QuickStartTask> =
                    QuickStartNewSiteTask.values().toList() +
                            QuickStartExistingSiteTask.values().toList()

            fun getTaskFromModel(model: QuickStartTaskModel) =
                    getAllTasks().find {
                        it.taskType.toString().equals(model.taskType, true) &&
                            it.string.equals(model.taskName.toString(), true)
                    } ?: UNKNOWN

            fun getTasksByTaskType(taskType: QuickStartTaskType) =
                    getAllTasks().filter { it.taskType == taskType }
        }
    }

    enum class QuickStartNewSiteTask constructor(
        override val string: String,
        override val taskType: QuickStartTaskType,
        override val order: Int
    ) : QuickStartTask {
        UNKNOWN(QUICK_START_UNKNOWN_LABEL, QuickStartTaskType.UNKNOWN, 0),
        CREATE_SITE(QUICK_START_CREATE_SITE_LABEL, CUSTOMIZE, 0),
        UPDATE_SITE_TITLE(QUICK_START_UPDATE_SITE_TITLE_LABEL, CUSTOMIZE, 1),
        UPLOAD_SITE_ICON(QUICK_START_UPLOAD_SITE_ICON_LABEL, CUSTOMIZE, 2),
        REVIEW_PAGES(QUICK_START_REVIEW_PAGES_LABEL, CUSTOMIZE, 3),
        VIEW_SITE(QUICK_START_VIEW_SITE_LABEL, CUSTOMIZE, 4),
        ENABLE_POST_SHARING(QUICK_START_ENABLE_POST_SHARING_LABEL, GROW, 6),
        PUBLISH_POST(QUICK_START_PUBLISH_POST_LABEL, GROW, 7),
        FOLLOW_SITE(QUICK_START_FOLLOW_SITE_LABEL, GROW, 8),
        CHECK_STATS(QUICK_START_CHECK_STATS_LABEL, GROW, 9);

        override fun toString(): String {
            return string
        }

        companion object {
            fun fromString(string: String?): QuickStartNewSiteTask {
                for (value in values()) {
                    if (string.equals(value.toString(), true)) {
                        return value
                    }
                }

                return UNKNOWN
            }
        }
    }

    enum class QuickStartExistingSiteTask constructor(
        override val string: String,
        override val taskType: QuickStartTaskType,
        override val order: Int
    ) : QuickStartTask {
        UNKNOWN(QUICK_START_UNKNOWN_LABEL, QuickStartTaskType.UNKNOWN, 0),
        CHECK_STATS(QUICK_START_CHECK_STATS_LABEL, GET_TO_KNOW_APP, 1),
        CHECK_NOTIFICATIONS(QUICK_START_CHECK_NOTIFIATIONS_LABEL, GET_TO_KNOW_APP, 2),
        VIEW_SITE(QUICK_START_VIEW_SITE_LABEL, GET_TO_KNOW_APP, 3),
        UPLOAD_MEDIA(QUICK_START_UPLOAD_MEDIA_LABEL, GET_TO_KNOW_APP, 4),
        FOLLOW_SITE(QUICK_START_FOLLOW_SITE_LABEL, GET_TO_KNOW_APP, 5);

        override fun toString(): String {
            return string
        }

        companion object {
            fun fromString(string: String?): QuickStartExistingSiteTask {
                for (value in values()) {
                    if (string.equals(value.toString(), true)) {
                        return value
                    }
                }

                return UNKNOWN
            }
        }
    }

    enum class QuickStartTaskType(private val string: String) {
        CUSTOMIZE("customize"),
        GROW("grow"),
        GET_TO_KNOW_APP("get_to_know_app"),
        UNKNOWN("unknown");

        override fun toString(): String {
            return string
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) = Unit // Do nothing (ignore)

    override fun onRegister() {
        AppLog.d(AppLog.T.API, QuickStartStore::class.java.simpleName + " onRegister")
    }

    fun getDoneCount(siteId: Long): Int {
        return quickStartSqlUtils.getDoneCount(siteId)
    }

    fun hasDoneTask(siteId: Long, task: QuickStartTask): Boolean {
        return quickStartSqlUtils.hasDoneTask(siteId, task)
    }

    fun setDoneTask(siteId: Long, task: QuickStartTask, isDone: Boolean) {
        quickStartSqlUtils.setDoneTask(siteId, task, isDone)
    }

    fun getCompletedTasksByType(siteId: Long, taskType: QuickStartTaskType): List<QuickStartTask> {
        return QuickStartTask.getTasksByTaskType(taskType)
                .filter { quickStartSqlUtils.hasDoneTask(siteId, it) }
                .sortedBy { it.order }
    }

    fun getUncompletedTasksByType(
        siteId: Long,
        taskType: QuickStartTaskType
    ): List<QuickStartTask> {
        return QuickStartTask.getTasksByTaskType(taskType)
                .filter { !quickStartSqlUtils.hasDoneTask(siteId, it) }
                .sortedBy { it.order }
    }

    fun isQuickStartStatusSet(siteId: Long): Boolean {
        return quickStartSqlUtils.getQuickStartStatus(siteId) != null
    }

    fun setQuickStartCompleted(siteId: Long, isCompleted: Boolean) {
        quickStartSqlUtils.setQuickStartCompleted(siteId, isCompleted)
    }

    fun getQuickStartCompleted(siteId: Long): Boolean {
        return quickStartSqlUtils.getQuickStartCompleted(siteId)
    }

    fun setQuickStartNotificationReceived(siteId: Long, isReceived: Boolean) {
        quickStartSqlUtils.setQuickStartNotificationReceived(siteId, isReceived)
    }

    fun getQuickStartNotificationReceived(siteId: Long): Boolean {
        return quickStartSqlUtils.getQuickStartNotificationReceived(siteId)
    }

    companion object {
        const val QUICK_START_UNKNOWN_LABEL = "unknown"
        const val QUICK_START_CREATE_SITE_LABEL = "create_site"
        const val QUICK_START_UPDATE_SITE_TITLE_LABEL = "update_site_title"
        const val QUICK_START_UPLOAD_SITE_ICON_LABEL = "upload_site_icon"
        const val QUICK_START_REVIEW_PAGES_LABEL = "review_pages"
        const val QUICK_START_VIEW_SITE_LABEL = "view_site"
        const val QUICK_START_ENABLE_POST_SHARING_LABEL = "enable_post_sharing"
        const val QUICK_START_PUBLISH_POST_LABEL = "publish_post"
        const val QUICK_START_FOLLOW_SITE_LABEL = "follow_site"
        const val QUICK_START_CHECK_STATS_LABEL = "check_stats"
        const val QUICK_START_CHECK_NOTIFIATIONS_LABEL = "check_notifications"
        const val QUICK_START_UPLOAD_MEDIA_LABEL = "upload_media"
    }
}
