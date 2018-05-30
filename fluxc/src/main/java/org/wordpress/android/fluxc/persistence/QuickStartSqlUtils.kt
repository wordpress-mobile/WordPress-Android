package org.wordpress.android.fluxc.persistence

import android.content.ContentValues
import com.wellsql.generated.QuickStartModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.QuickStartModel
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuickStartSqlUtils
@Inject constructor() {
    fun getDoneCount(siteId: Long): Int {
        return WellSql.select(QuickStartModel::class.java)
                .where().beginGroup()
                .equals(QuickStartModelTable.SITE_ID, siteId)
                .equals(QuickStartModelTable.IS_DONE, true)
                .endGroup().endWhere()
                .asModel.size
    }

    fun getShownCount(siteId: Long): Int {
        return WellSql.select(QuickStartModel::class.java)
                .where().beginGroup()
                .equals(QuickStartModelTable.SITE_ID, siteId)
                .equals(QuickStartModelTable.IS_SHOWN, true)
                .endGroup().endWhere()
                .asModel.size
    }

    fun hasDoneTask(siteId: Long, task: QuickStartTask): Boolean {
        val model = WellSql.select(QuickStartModel::class.java)
                .where().beginGroup()
                .equals(QuickStartModelTable.SITE_ID, siteId)
                .equals(QuickStartModelTable.TASK_NAME, task.toString())
                .endGroup().endWhere()
                .asModel.firstOrNull()

        return model?.isDone ?: false
    }

    fun hasShownTask(siteId: Long, task: QuickStartTask): Boolean {
        val model = WellSql.select(QuickStartModel::class.java)
                .where().beginGroup()
                .equals(QuickStartModelTable.SITE_ID, siteId)
                .equals(QuickStartModelTable.TASK_NAME, task.toString())
                .endGroup().endWhere()
                .asModel.firstOrNull()

        return model?.isShown ?: false
    }

    fun setDoneTask(siteId: Long, task: QuickStartTask, isDone: Boolean) {
        WellSql.update(QuickStartModel::class.java)
                .where()
                .equals(QuickStartModelTable.SITE_ID, siteId)
                .equals(QuickStartModelTable.TASK_NAME, task.toString())
                .endWhere()
                .put(isDone, { item ->
                    val values = ContentValues()
                    values.put(QuickStartModelTable.IS_DONE, item)
                    values
                })
                .execute()
    }

    fun setShownTask(siteId: Long, task: QuickStartTask, isShown: Boolean) {
        WellSql.update(QuickStartModel::class.java)
                .where()
                .equals(QuickStartModelTable.SITE_ID, siteId)
                .equals(QuickStartModelTable.TASK_NAME, task.toString())
                .endWhere()
                .put(isShown, { item ->
                    val values = ContentValues()
                    values.put(QuickStartModelTable.IS_SHOWN, item)
                    values
                })
                .execute()
    }
}
