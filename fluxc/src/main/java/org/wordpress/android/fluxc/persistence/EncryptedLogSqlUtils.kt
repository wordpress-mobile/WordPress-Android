package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.EncryptedLogModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.EncryptedLog
import org.wordpress.android.fluxc.model.EncryptedLogModel
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

    fun deleteEncryptedLog(uuid: String) {
        WellSql.delete(EncryptedLogModel::class.java)
                .where()
                .equals(EncryptedLogModelTable.UUID, uuid)
                .endWhere()
                .execute()
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
