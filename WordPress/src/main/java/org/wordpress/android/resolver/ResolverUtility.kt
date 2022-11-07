package org.wordpress.android.resolver

import com.wellsql.generated.SiteModelMapper
import com.yarolegovich.wellsql.mapper.MapperAdapter
import dagger.Reusable
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

@Reusable
class ResolverUtility @Inject constructor(
    private val dbWrapper: DbWrapper
) {
    fun copySitesWithIndexes(sites: List<SiteModel>) {
        val db = dbWrapper.giveMeWritableDb()
        db.beginTransaction()
        try {
            db.delete("SiteModel", null, null)
            db.delete("sqlite_sequence", "name='SiteModel'", null)
            val mapperAdapter = MapperAdapter(SiteModelMapper())
            val orderedSites = sites.sortedBy { it.id }

            // pre-populate sites; this also has the effect of adding and logging in self-hosted sites if present
            for ((index, site) in orderedSites.withIndex()) {
                val sqlStatement = if (index == 0) {
                    db.compileStatement("INSERT INTO SQLITE_SEQUENCE (name,seq) VALUES ('SiteModel', ?)")
                } else {
                    db.compileStatement("UPDATE SQLITE_SEQUENCE SET seq=? WHERE name='SiteModel'")
                }

                sqlStatement.bindLong(1, (site.id - 1).toLong())
                sqlStatement.execute()

                db.insert("SiteModel", null, mapperAdapter.toCv(site))
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
