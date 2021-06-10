package org.wordpress.android.ui.mysite

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cursoradapter.widget.CursorAdapter
import org.apache.commons.text.similarity.FuzzyScore
import org.wordpress.android.R.array
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import java.util.Locale

class ImprovedMySiteSearchSuggestionAdapter(context: Context, locale: Locale) : CursorAdapter(context, null, false) {
    private val functions = context.resources.getStringArray(array.functionality_search_entries)
    private val fuzzyScore = FuzzyScore(locale)

    private fun searchFunctions(query: String) = functions.filter { term ->
        fuzzyScore.fuzzyScore(term, query) > fuzzyScoreThreshold
    }

    fun setQuery(query: String) {
        val columnNames = arrayOf(COL_ID, COL_DESCR, COL_LINK)
        val cursor = MatrixCursor(columnNames)
        val temp = arrayOfNulls<String>(COLUMNS_NUMBER)
        var id = 0
        for (item in searchFunctions(query)) {
            temp[0] = (id++).toString()
            temp[1] = item.split(DELIMITER)[0]
            temp[2] = item.split(DELIMITER)[1]
            cursor.addRow(temp)
        }
        swapCursor(cursor)
    }

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View =
            LayoutInflater.from(context).inflate(layout.my_site_search_item, parent, false)

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val query = cursor.getString(cursor.getColumnIndex(COL_DESCR))
        (view.findViewById(id.description) as TextView).text = query
    }

    fun getDeepLink(position: Int) = (getItem(position) as? Cursor)?.getString(cursor.getColumnIndex(COL_LINK))

    companion object {
        const val COLUMNS_NUMBER = 3
        const val COL_ID = "_id"
        const val COL_DESCR = "description"
        const val COL_LINK = "deeplink"
        const val DELIMITER = "|"
        const val fuzzyScoreThreshold = 5
    }
}
