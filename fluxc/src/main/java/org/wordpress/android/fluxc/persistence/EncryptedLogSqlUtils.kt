package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.EncryptedLogModelTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptedLog
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptedLogModel
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptedLogUploadState.FAILED
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptedLogUploadState.QUEUED
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptedLogUploadState.UPLOADING
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedLogSqlUtils @Inject constructor() {
    fun insertOrUpdateEncryptedLog(encryptedLog: EncryptedLog) {
        insertOrUpdateEncryptedLogs(listOf(encryptedLog))
    }

    fun insertOrUpdateEncryptedLogs(encryptedLogs: List<EncryptedLog>) {
        val encryptedLogModels = encryptedLogs.map { EncryptedLogModel.fromEncryptedLog(it) }
        // Since we have a unique constraint for uuid with 'on conflict replace', if there is an existing log,
        // it'll be replaced with the new one. No need to check if the log already exists.
        WellSql.insert(encryptedLogModels).execute()
    }

    fun getEncryptedLog(uuid: String): EncryptedLog? {
        return getEncryptedLogModel(uuid)?.let { EncryptedLog.fromEncryptedLogModel(it) }
    }

    // TODO: Add unit tests
    fun getUploadingEncryptedLogs(): List<EncryptedLog> =
            getUploadingEncryptedLogsQuery().asModel.map { EncryptedLog.fromEncryptedLogModel(it) }

    fun getNumberOfUploadingEncryptedLogs(): Long = getUploadingEncryptedLogsQuery().count()

    fun deleteEncryptedLogs(encryptedLogList: List<EncryptedLog>) {
        if (encryptedLogList.isEmpty()) {
            return
        }
        WellSql.delete(EncryptedLogModel::class.java)
                .where()
                .isIn(EncryptedLogModelTable.UUID, encryptedLogList.map { it.uuid })
                .endWhere()
                .execute()
    }

    fun getEncryptedLogsForUpload(): List<EncryptedLog> {
        val uploadStates = listOf(QUEUED, FAILED).map { it.value }
        return WellSql.select(EncryptedLogModel::class.java)
                .where()
                .isIn(EncryptedLogModelTable.UPLOAD_STATE_DB_VALUE, uploadStates)
                .endWhere()
                // Queued status should have priority over failed status
                .orderBy(EncryptedLogModelTable.UPLOAD_STATE_DB_VALUE, SelectQuery.ORDER_ASCENDING)
                // First log that's queued should have priority
                .orderBy(EncryptedLogModelTable.DATE_CREATED, SelectQuery.ORDER_ASCENDING)
                .asModel
                .map {
                    EncryptedLog.fromEncryptedLogModel(it)
                }
    }

    private fun getEncryptedLogModel(uuid: String): EncryptedLogModel? {
        return WellSql.select(EncryptedLogModel::class.java)
                .where()
                .equals(EncryptedLogModelTable.UUID, uuid)
                .endWhere()
                .asModel
                .firstOrNull()
    }

    private fun getUploadingEncryptedLogsQuery(): SelectQuery<EncryptedLogModel> {
        return WellSql.select(EncryptedLogModel::class.java)
            .where()
            .equals(EncryptedLogModelTable.UPLOAD_STATE_DB_VALUE, UPLOADING.value)
            .endWhere()
    }
}
