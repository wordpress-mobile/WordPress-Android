package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.QuickStartStatusModelTable
import com.wellsql.generated.QuickStartTaskModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.QuickStartStatusModel
import org.wordpress.android.fluxc.model.QuickStartTaskModel
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuickStartSqlUtils
@Inject constructor() {
    fun getDoneCount(siteId: Long): Int {
        return WellSql.select(QuickStartTaskModel::class.java)
                .where().beginGroup()
                .equals(QuickStartTaskModelTable.SITE_ID, siteId)
                .equals(QuickStartTaskModelTable.IS_DONE, true)
                .endGroup().endWhere()
                .asModel.size
    }

    fun getDoneCountByType(siteId: Long, taskType: QuickStartTaskType): Int {
        return WellSql.select(QuickStartTaskModel::class.java)
                .where().beginGroup()
                .equals(QuickStartTaskModelTable.SITE_ID, siteId)
                .equals(QuickStartTaskModelTable.IS_DONE, true)
                .equals(QuickStartTaskModelTable.TASK_TYPE, taskType.toString())
                .endGroup().endWhere()
                .asModel.size
    }

    fun getShownCountByType(siteId: Long, taskType: QuickStartTaskType): Int {
        return WellSql.select(QuickStartTaskModel::class.java)
                .where().beginGroup()
                .equals(QuickStartTaskModelTable.SITE_ID, siteId)
                .equals(QuickStartTaskModelTable.IS_SHOWN, true)
                .equals(QuickStartTaskModelTable.TASK_TYPE, taskType.toString())
                .endGroup().endWhere()
                .asModel.size
    }

    private fun getTask(siteId: Long, task: QuickStartTask): QuickStartTaskModel? {
        return WellSql.select(QuickStartTaskModel::class.java)
                .where().beginGroup()
                .equals(QuickStartTaskModelTable.SITE_ID, siteId)
                .equals(QuickStartTaskModelTable.TASK_NAME, task.toString())
                .equals(QuickStartTaskModelTable.TASK_TYPE, task.taskType.toString())
                .endGroup().endWhere()
                .asModel.firstOrNull()
    }

    fun getQuickStartStatus(siteId: Long): QuickStartStatusModel? {
        return WellSql.select(QuickStartStatusModel::class.java)
                .where().beginGroup()
                .equals(QuickStartStatusModelTable.SITE_ID, siteId)
                .endGroup().endWhere()
                .asModel.firstOrNull()
    }

    fun hasDoneTask(siteId: Long, task: QuickStartTask): Boolean {
        return getTask(siteId, task)?.isDone ?: false
    }

    private fun insertOrUpdateQuickStartTaskModel(newTaskModel: QuickStartTaskModel) {
        val oldModel = getTask(newTaskModel.siteId, QuickStartTask.getTaskFromModel(newTaskModel))
        oldModel?.let {
            WellSql.update(QuickStartTaskModel::class.java)
                    .whereId(it.id)
                    .put(newTaskModel, UpdateAllExceptId(QuickStartTaskModel::class.java))
                    .execute()
            return
        }
        WellSql.insert(newTaskModel).execute()
    }

    private fun insertOrUpdateQuickStartStatusModel(newQuickStartStatus: QuickStartStatusModel) {
        val oldModel = getQuickStartStatus(newQuickStartStatus.siteId)
        oldModel?.let {
            WellSql.update(QuickStartStatusModel::class.java)
                    .whereId(it.id)
                    .put(newQuickStartStatus, UpdateAllExceptId(QuickStartStatusModel::class.java))
                    .execute()
            return
        }
        WellSql.insert(newQuickStartStatus).execute()
    }

    fun setDoneTask(siteId: Long, task: QuickStartTask, isDone: Boolean) {
        val model = getTask(siteId, task) ?: QuickStartTaskModel()
        model.siteId = siteId
        model.taskName = task.toString()
        model.taskType = task.taskType.toString()
        model.isDone = isDone
        insertOrUpdateQuickStartTaskModel(model)
    }

    fun setQuickStartCompleted(siteId: Long, isCompleted: Boolean) {
        val model = getQuickStartStatus(siteId) ?: QuickStartStatusModel()
        model.siteId = siteId
        model.isCompleted = isCompleted
        insertOrUpdateQuickStartStatusModel(model)
    }

    fun getQuickStartCompleted(siteId: Long): Boolean {
        return getQuickStartStatus(siteId)?.isCompleted ?: false
    }

    fun setQuickStartNotificationReceived(siteId: Long, isReceived: Boolean) {
        val model = getQuickStartStatus(siteId) ?: QuickStartStatusModel()
        model.siteId = siteId
        model.isNotificationReceived = isReceived
        insertOrUpdateQuickStartStatusModel(model)
    }

    fun getQuickStartNotificationReceived(siteId: Long): Boolean {
        return getQuickStartStatus(siteId)?.isNotificationReceived ?: false
    }
}
