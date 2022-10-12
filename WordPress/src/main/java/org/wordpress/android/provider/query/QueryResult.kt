package org.wordpress.android.provider.query

import android.database.Cursor
import android.database.MatrixCursor
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import java.lang.reflect.Type
import javax.inject.Inject

class QueryResult @Inject constructor() {
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    inline fun <reified T : Any> getValue(cursor: Cursor, type: Type? = null): T? {
        cursor.moveToFirst()
        val value: String = cursor.getString(0)
        return try {
            if (type != null) Gson().fromJson(value, type) else Gson().fromJson(value, T::class.java)
        } catch (exception: Exception) {
            null
        }
    }

    inline fun <reified T : Any> createCursor(value: T): Cursor {
        val valueJson = Gson().toJson(value, value::class.java)
        val matrixCursor = MatrixCursor(arrayOf(KEY_QUERY_RESULT))
        matrixCursor.newRow().add(KEY_QUERY_RESULT, valueJson)
        return matrixCursor
    }
}

const val KEY_QUERY_RESULT = "KEY_QUERY_RESULT"
