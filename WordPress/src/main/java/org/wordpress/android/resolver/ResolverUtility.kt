package org.wordpress.android.resolver

import com.wellsql.generated.QuickStartStatusModelMapper
import com.wellsql.generated.QuickStartTaskModelMapper
import com.wellsql.generated.SiteModelMapper
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.mapper.MapperAdapter
import dagger.Reusable
import org.wordpress.android.fluxc.model.QuickStartStatusModel
import org.wordpress.android.fluxc.model.QuickStartTaskModel
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

@Reusable
class ResolverUtility @Inject constructor(
    private val dbWrapper: DbWrapper
) {
    private fun <T : Identifiable> copyWithIndexes(
        tableName: String,
        mapperAdapter: MapperAdapter<T>,
        items: List<T>
    ): Boolean {
        if (items.isEmpty()) return true

        var wasSuccessful: Boolean

        val db = dbWrapper.giveMeWritableDb()
        db.beginTransaction()
        try {
            db.delete(tableName, null, null)
            db.delete("sqlite_sequence", "name='$tableName'", null)
            val orderedItems = items.sortedBy { it.id }

            for ((index, item) in orderedItems.withIndex()) {
                val sqlStatement = if (index == 0) {
                    db.compileStatement("INSERT INTO SQLITE_SEQUENCE (name,seq) VALUES ('$tableName', ?)")
                } else {
                    db.compileStatement("UPDATE SQLITE_SEQUENCE SET seq=? WHERE name='$tableName'")
                }

                sqlStatement.bindLong(1, (item.id - 1).toLong())
                sqlStatement.execute()

                db.insert(tableName, null, mapperAdapter.toCv(item))
            }

            db.setTransactionSuccessful()
            wasSuccessful = true
        } finally {
            db.endTransaction()
        }
        return wasSuccessful
    }

    fun copySitesWithIndexes(sites: List<SiteModel>) {
        copyWithIndexes("SiteModel", MapperAdapter(SiteModelMapper()), sites)
    }

    fun copyQsDataWithIndexes(statusList: List<QuickStartStatusModel>, taskList: List<QuickStartTaskModel>): Boolean {
        var wasSuccessful = false

        val db = dbWrapper.giveMeWritableDb()
        db.beginTransaction()

        try {
            if (
                copyWithIndexes(
                    "QuickStartStatusModel",
                    MapperAdapter(QuickStartStatusModelMapper()),
                    statusList
                )
                && copyWithIndexes(
                    "QuickStartTaskModel",
                    MapperAdapter(QuickStartTaskModelMapper()),
                    taskList
                )
            ) {
                db.setTransactionSuccessful()
                wasSuccessful = true
            }
        } finally {
            db.endTransaction()
        }

        return wasSuccessful
    }
}
