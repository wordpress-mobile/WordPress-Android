package org.wordpress.android.ui.mysite.search

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cursoradapter.widget.CursorAdapter
import org.wordpress.android.R.id
import org.wordpress.android.R.layout

class SearchSuggestionAdapter(context: Context) : CursorAdapter(context, null, false) {
    fun setSuggestions(suggestions: List<Functionality>) {
        val columnNames = arrayOf(COL_ID, COL_DESCR, COL_LINK)
        val cursor = MatrixCursor(columnNames)
        val temp = arrayOfNulls<String>(COLUMNS_NUMBER)
        for ((id, function) in suggestions.withIndex()) {
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
        private const val COLUMNS_NUMBER = 3
        private const val COL_ID = "_id"
        private const val COL_DESCR = "description"
        private const val COL_LINK = "deeplink"
    }
}
