package org.wordpress.android.datasets.wrappers

import dagger.Reusable
import org.wordpress.android.datasets.ReaderSearchTable
import javax.inject.Inject

@Reusable
class ReaderSearchTableWrapper @Inject constructor() {
    /*
     * returns the most recently used queries starting with [prefix], or all of them when it's null
     */
    fun getQueries(prefix: String?, max: Int): List<String> =
        ReaderSearchTable.getQueryStringCursor(prefix, max).use { cursor ->
            val column = cursor.getColumnIndexOrThrow(ReaderSearchTable.COL_QUERY)
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(column))
            }
        }

    fun addOrUpdateQuery(query: String) = ReaderSearchTable.addOrUpdateQueryString(query)

    fun deleteQuery(query: String) = ReaderSearchTable.deleteQueryString(query)

    fun deleteAllQueries() = ReaderSearchTable.deleteAllQueries()
}
