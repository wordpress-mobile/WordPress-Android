package org.wordpress.android.fluxc.network.rest.wpcom.scan.threat

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.DatabaseThreatModel.Row
import java.lang.reflect.Type

class RowsDeserializer : JsonDeserializer<List<Row>?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): List<Row>? {
        return if (context != null && json != null && json.isJsonObject) {
            /**
             * Rows example:
             * "rows": {
             * "949": {
             *   "id": 1849,
             *   "description": "KiethAbare - 2019-01-20 18:41:40",
             *   "url": "http://to.ht/bestforex48357\\n"
             *   },
             *   "950": {
             *   "id": 1850,
             *   "description": "KiethAbare - 2019-01-20 18:41:45",
             *   "url": "http://to.ht/bestforex48357\\n"
             *   }
             * }
             */
            val rows: ArrayList<Row> = arrayListOf()

            val rowsJsonObject = json.asJsonObject
            rowsJsonObject.keySet().iterator().forEach { key ->
                val row = getRow(key, rowsJsonObject)
                row?.let { rows.add(it) }
            }

            if (rows.isNotEmpty()) {
                rows.sortBy(Row::rowNumber)
            }

            return rows
        } else {
            null
        }
    }

    @Suppress("SwallowedException")
    private fun getRow(
        key: String,
        rowJsonObject: JsonObject
    ): Row? {
        return rowJsonObject.get(key)?.takeIf { it.isJsonObject }?.asJsonObject?.let { contents ->
            try {
                Row(
                    rowNumber = key.toInt(),
                    id = contents.get(ID)?.asInt ?: 0,
                    description = contents.get(DESCRIPTION)?.asString,
                    code = contents.get(CODE)?.asString,
                    url = contents.get(URL)?.asString
                )
            } catch (ex: ClassCastException) {
                null
            } catch (ex: IllegalStateException) {
                null
            } catch (ex: NumberFormatException) {
                null
            }
        }
    }

    companion object {
        private const val ID = "id"
        private const val DESCRIPTION = "description"
        private const val CODE = "code"
        private const val URL = "url"
    }
}
