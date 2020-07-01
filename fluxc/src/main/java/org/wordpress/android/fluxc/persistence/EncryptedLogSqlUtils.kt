package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.EncryptedLogModelTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.EncryptedLog
import org.wordpress.android.fluxc.model.EncryptedLogModel
import org.wordpress.android.fluxc.model.EncryptedLogUploadState
import org.wordpress.android.fluxc.model.EncryptedLogUploadState.FAILED
import org.wordpress.android.fluxc.model.EncryptedLogUploadState.QUEUED
import org.wordpress.android.fluxc.model.EncryptedLogUploadState.UPLOADING
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedLogSqlUtils @Inject constructor() {
    fun insertOrUpdateEncryptedLog(encryptedLog: EncryptedLog) {
        // Since we have a unique constraint for uuid with 'on conflict replace', if there is an existing log,
        // it'll be replaced with the new one. No need to check if the log already exists.
        WellSql.insert(EncryptedLogModel.fromEncryptedLog(encryptedLog)).execute()
    }

    fun getEncryptedLog(uuid: String): EncryptedLog? {
        return getEncryptedLogModel(uuid)?.let { EncryptedLog.fromEncryptedLogModel(it) }
    }

    fun getNumberOfEncryptedLogsUploading(): Long = WellSql.select(EncryptedLogModel::class.java)
            .where()
            .equals(EncryptedLogModelTable.UPLOAD_STATE_DB_VALUE, UPLOADING)
            .endWhere()
            .count()

    // TODO: Update the tests for this
    fun deleteEncryptedLogs(encryptedLogList: List<EncryptedLog>) {
        if (encryptedLogList.isEmpty()) {
            return
        }
        WellSql.delete(EncryptedLogModel::class.java)
                .where()
                .equals(EncryptedLogModelTable.UUID, encryptedLogList.map { it.uuid })
                .endWhere()
                .execute()
    }

    // TODO: Add a unit test for this
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
}
