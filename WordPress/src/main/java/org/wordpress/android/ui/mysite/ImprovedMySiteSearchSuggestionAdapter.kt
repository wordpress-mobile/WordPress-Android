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

class ImprovedMySiteSearchSuggestionAdapter(context: Context, val locale: Locale) : CursorAdapter(
        context,
        null,
        false
) {
    private val functions: List<Functionality> = context.resources.getStringArray(array.functionality_search_entries)
            .map {
                val parts = it.split(DELIMITER)
                Functionality(parts[0], parts[1].split(TERMS_DELIMITER), parts[2])
            }
    private val fuzzyScore = FuzzyScore(locale)

    private fun searchFunctions(query: String) = functions.filter { f ->
        f.terms.any { term ->
            query.toLowerCase(locale).contains(term) ||
                    fuzzyScore.fuzzyScore(query, term) > fuzzyScoreThreshold
        }
    }

    fun setQuery(query: String) {
        val columnNames = arrayOf(COL_ID, COL_DESCR, COL_LINK)
        val cursor = MatrixCursor(columnNames)
        val temp = arrayOfNulls<String>(COLUMNS_NUMBER)
        for ((id, function) in searchFunctions(query).withIndex()) {
            temp[0] = (id).toString()
            temp[1] = function.description
            temp[2] = function.deeplink
            cursor.addRow(temp)
        }
        swapCursor(cursor)
    }

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View =
            LayoutInflater.from(context).inflate(layout.my_site_search_item, parent, false)

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val description = cursor.getString(cursor.getColumnIndex(COL_DESCR))
        (view.findViewById(id.description) as TextView).text = description
    }

    fun getDeepLink(position: Int) = (getItem(position) as? Cursor)?.getString(cursor.getColumnIndex(COL_LINK))

    companion object {
        const val COLUMNS_NUMBER = 3
        const val COL_ID = "_id"
        const val COL_DESCR = "description"
        const val COL_LINK = "deeplink"
        const val DELIMITER = "|"
        const val TERMS_DELIMITER = ","
        const val fuzzyScoreThreshold = 6
    }

    private class Functionality(val description: String, val terms: List<String>, val deeplink: String)
}
