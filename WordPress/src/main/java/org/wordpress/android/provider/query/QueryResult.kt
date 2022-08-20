package org.wordpress.android.provider.query

import android.database.Cursor
import android.database.MatrixCursor
import com.google.gson.Gson
import javax.inject.Inject

class QueryResult @Inject constructor() {
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    inline fun <reified T> getValue(cursor: Cursor): T? {
        cursor.moveToFirst()
        val value: String = cursor.getString(0)
        return try {
            Gson().fromJson(value, T::class.java)
        } catch (exception: Exception) {
            null
        }
    }

    inline fun <reified T> createCursor(value: T): Cursor {
        val valueJson = Gson().toJson(value)
        val matrixCursor = MatrixCursor(arrayOf(KEY_QUERY_RESULT))
        matrixCursor.newRow().add(KEY_QUERY_RESULT, valueJson)
        return matrixCursor
    }
}

const val KEY_QUERY_RESULT = "KEY_QUERY_RESULT"
