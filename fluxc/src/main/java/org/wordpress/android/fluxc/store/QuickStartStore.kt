package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.persistence.QuickStartSqlUtils
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuickStartStore @Inject
constructor(private val quickStartSqlUtils: QuickStartSqlUtils, dispatcher: Dispatcher) : Store(dispatcher) {
    enum class QuickStartTask constructor(private val string: String) {
        CHOOSE_THEME("choose_theme"),
        CREATE_SITE("create_site"),
        CUSTOMIZE_SITE("customize_site"),
        FOLLOW_SITE("follow_site"),
        PUBLISH_POST("publish_post"),
        SHARE_SITE("share_site"),
        VIEW_SITE("view_site");

        override fun toString(): String {
            return string
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

    fun showQuickStart(siteId: Long): Boolean {
        return hasDoneTask(siteId, QuickStartTask.CREATE_SITE)
    }
}
