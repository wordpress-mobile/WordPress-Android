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
class QuickStartStore @Inject
constructor(private val quickStartSqlUtils: QuickStartSqlUtils, dispatcher: Dispatcher) : Store(dispatcher) {
    enum class QuickStartTask constructor(
        private val string: String,
        val taskType: QuickStartTaskType,
        val order: Int
    ) {
        CREATE_SITE("create_site", CUSTOMIZE, 0),
        UPLOAD_SITE_ICON("upload_site_icon", CUSTOMIZE, 1),
        CHOOSE_THEME("choose_theme", CUSTOMIZE, 2),
        CUSTOMIZE_SITE("customize_site", CUSTOMIZE, 3),
        CREATE_NEW_PAGE("create_new_page", CUSTOMIZE, 4),
        VIEW_SITE("view_site", CUSTOMIZE, 5),
        ENABLE_POST_SHARING("enable_post_sharing", GROW, 6),
        PUBLISH_POST("publish_post", GROW, 7),
        FOLLOW_SITE("follow_site", GROW, 8),
        CHECK_STATS("check_stats", GROW, 9),
        EXPLORE_PLANS("explore_plans", GROW, 10);

        override fun toString(): String {
            return string
        }

        companion object {
            fun fromString(string: String?): QuickStartTask {
                for (value in QuickStartTask.values()) {
                    if (string.equals(value.toString(), true)) {
                        return value
                    }
                }

                return CHOOSE_THEME
            }

            fun getTasksByType(taskType: QuickStartTaskType): List<QuickStartTask> {
                return QuickStartTask.values().filter { it.taskType == taskType }
            }
        }
    }

    enum class QuickStartTaskType(private val string: String) {
        CUSTOMIZE("customize"),
        GROW("grow"),
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

    fun hasDoneTask(siteId: Long, task: QuickStartTask): Boolean {
        return quickStartSqlUtils.hasDoneTask(siteId, task)
    }

    fun hasShownTask(siteId: Long, task: QuickStartTask): Boolean {
        return quickStartSqlUtils.hasShownTask(siteId, task)
    }

    fun setDoneTask(siteId: Long, task: QuickStartTask, isDone: Boolean) {
        quickStartSqlUtils.setDoneTask(siteId, task, isDone)
    }

    fun setShownTask(siteId: Long, task: QuickStartTask, isShown: Boolean) {
        quickStartSqlUtils.setShownTask(siteId, task, isShown)
    }

    fun getCompletedTasksByType(siteId: Long, taskType: QuickStartTaskType): List<QuickStartTask> {
        return QuickStartTask.getTasksByType(taskType).filter { quickStartSqlUtils.hasDoneTask(siteId, it) }
                .sortedBy { it.order }
    }

    fun getUncompletedTasksByType(siteId: Long, taskType: QuickStartTaskType): List<QuickStartTask> {
        return QuickStartTask.getTasksByType(taskType).filter { !quickStartSqlUtils.hasDoneTask(siteId, it) }
                .sortedBy { it.order }
    }

    fun getShownTasksByType(siteId: Long, taskType: QuickStartTaskType): List<QuickStartTask> {
        return QuickStartTask.getTasksByType(taskType).filter { quickStartSqlUtils.hasShownTask(siteId, it) }
                .sortedBy { it.order }
    }

    fun getUnshownTasksByType(siteId: Long, taskType: QuickStartTaskType): List<QuickStartTask> {
        return QuickStartTask.getTasksByType(taskType).filter { !quickStartSqlUtils.hasShownTask(siteId, it) }
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
