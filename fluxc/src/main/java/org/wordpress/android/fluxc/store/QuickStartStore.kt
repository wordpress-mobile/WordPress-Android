package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.persistence.QuickStartSqlUtils
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuickStartStore @Inject constructor(
    private val quickStartSqlUtils: QuickStartSqlUtils,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    enum class QuickStartNewSiteTask constructor(
        private val string: String,
        val taskType: QuickStartTaskType,
        val order: Int
    ) {
        UNKNOWN("unknown", QuickStartTaskType.UNKNOWN, 0),
        CREATE_SITE("create_site", CUSTOMIZE, 0),
        UPDATE_SITE_TITLE("update_site_title", CUSTOMIZE, 1),
        UPLOAD_SITE_ICON("upload_site_icon", CUSTOMIZE, 2),
        EDIT_HOMEPAGE("edit_homepage", CUSTOMIZE, 3),
        REVIEW_PAGES("review_pages", CUSTOMIZE, 4),
        VIEW_SITE("view_site", CUSTOMIZE, 5),
        ENABLE_POST_SHARING("enable_post_sharing", GROW, 7),
        PUBLISH_POST("publish_post", GROW, 8),
        FOLLOW_SITE("follow_site", GROW, 9),
        CHECK_STATS("check_stats", GROW, 10),
        EXPLORE_PLANS("explore_plans", GROW, 11);

        override fun toString(): String {
            return string
        }

        companion object {
            fun fromString(string: String?): QuickStartNewSiteTask {
                for (value in QuickStartNewSiteTask.values()) {
                    if (string.equals(value.toString(), true)) {
                        return value
                    }
                }

                return UNKNOWN
            }

            fun getTasksByType(taskType: QuickStartTaskType): List<QuickStartNewSiteTask> {
                return QuickStartNewSiteTask.values().filter { it.taskType == taskType }
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

        companion object {
            fun fromString(string: String?): QuickStartTaskType {
                for (value in QuickStartTaskType.values()) {
                    if (string.equals(value.toString(), true)) {
                        return value
                    }
                }

                return UNKNOWN
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, QuickStartStore::class.java.simpleName + " onRegister")
    }

    fun getDoneCount(siteId: Long): Int {
        return quickStartSqlUtils.getDoneCount(siteId)
    }

    fun getShownCount(siteId: Long): Int {
        return quickStartSqlUtils.getShownCount(siteId)
    }

    fun hasDoneTask(siteId: Long, task: QuickStartNewSiteTask): Boolean {
        return quickStartSqlUtils.hasDoneTask(siteId, task)
    }

    fun hasShownTask(siteId: Long, task: QuickStartNewSiteTask): Boolean {
        return quickStartSqlUtils.hasShownTask(siteId, task)
    }

    fun setDoneTask(siteId: Long, task: QuickStartNewSiteTask, isDone: Boolean) {
        quickStartSqlUtils.setDoneTask(siteId, task, isDone)
    }

    fun setShownTask(siteId: Long, task: QuickStartNewSiteTask, isShown: Boolean) {
        quickStartSqlUtils.setShownTask(siteId, task, isShown)
    }

    fun getCompletedTasksByType(siteId: Long, taskType: QuickStartTaskType): List<QuickStartNewSiteTask> {
        return QuickStartNewSiteTask.getTasksByType(taskType)
            .filter { quickStartSqlUtils.hasDoneTask(siteId, it) }
            .sortedBy { it.order }
    }

    fun getUncompletedTasksByType(
        siteId: Long,
        taskType: QuickStartTaskType
    ): List<QuickStartNewSiteTask> {
        return QuickStartNewSiteTask.getTasksByType(taskType)
            .filter { !quickStartSqlUtils.hasDoneTask(siteId, it) }
            .sortedBy { it.order }
    }

    fun getShownTasksByType(siteId: Long, taskType: QuickStartTaskType): List<QuickStartNewSiteTask> {
        return QuickStartNewSiteTask.getTasksByType(taskType)
            .filter { quickStartSqlUtils.hasShownTask(siteId, it) }
            .sortedBy { it.order }
    }

    fun getUnshownTasksByType(siteId: Long, taskType: QuickStartTaskType): List<QuickStartNewSiteTask> {
        return QuickStartNewSiteTask.getTasksByType(taskType)
            .filter { !quickStartSqlUtils.hasShownTask(siteId, it) }
            .sortedBy { it.order }
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
}
