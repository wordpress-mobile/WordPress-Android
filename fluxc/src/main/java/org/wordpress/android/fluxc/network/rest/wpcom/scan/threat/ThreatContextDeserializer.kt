package org.wordpress.android.fluxc.network.rest.wpcom.scan.threat

import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.FileThreatModel.ThreatContext
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.FileThreatModel.ThreatContext.ContextLine
import java.lang.reflect.Type

class ThreatContextDeserializer : JsonDeserializer<ThreatContext?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ThreatContext? {
        return if (context != null && json != null) {
            /**
             *  Threat context obj can either be:
             * - a key, value map
             * - an empty string
             * - not present
             */
            when {
                json.isJsonObject -> {
                    getThreatContext(json)
                }
                json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
                    ThreatContext(emptyList())
                }
                else -> {
                    null
                }
            }
        } else {
            null
        }
    }

    private fun getThreatContext(json: JsonElement): ThreatContext? {
        /**
         * ThreatContext example:
         * 3: start test
         * 4: VIRUS_SIG
         * 5: end test
         * marks: 4: [0, 9]
         */
        var threatContext: ThreatContext? = null

        val threatContextJsonObject = json.asJsonObject

        val marks = threatContextJsonObject.get(MARKS)
        val marksJsonObject = if (marks?.isJsonObject == true) marks.asJsonObject else null

        val lines: ArrayList<ContextLine> = arrayListOf()
        threatContextJsonObject.keySet().iterator().forEach { key ->
            if (key != MARKS) {
                val contextLine = getContextLine(threatContextJsonObject, marksJsonObject, key)
                contextLine?.let { lines.add(it) }
            }
        }
        if (lines.isNotEmpty()) {
            lines.sortBy(ContextLine::lineNumber)
            threatContext = ThreatContext(lines)
        }

        return threatContext
    }

    private fun getContextLine(
        threatContextJsonObject: JsonObject,
        marksJsonObject: JsonObject? = null,
        key: String
    ): ContextLine? {
        var contextLine: ContextLine? = null

        val lineNumber = try {
            key.toInt()
        } catch (ex: NumberFormatException) {
            return null
        }

        val contentsForKey = threatContextJsonObject.get(key)
        val contentsStringForKey = if (
            contentsForKey?.isJsonPrimitive == true &&
            contentsForKey.asJsonPrimitive?.isString == true
        ) contentsForKey.asString else null

        contentsStringForKey?.let {
            contextLine = ContextLine(
                lineNumber = lineNumber,
                contents = it,
                highlights = getHighlightsFromMarks(marksJsonObject?.get(key))
            )
        }
        return contextLine
    }

    private fun getHighlightsFromMarks(marksForKey: JsonElement?): List<Pair<Int, Int>>? {
        val marksJsonArrayForKey = if (marksForKey?.isJsonArray == true) marksForKey.asJsonArray else null
        return marksJsonArrayForKey?.let { getHighlightsFromMarksArray(it) }
    }

    private fun getHighlightsFromMarksArray(marksForKeyArray: JsonArray): ArrayList<Pair<Int, Int>>? {
        var highlights: ArrayList<Pair<Int, Int>>? = null

        for (rangeArrayJsonElement in marksForKeyArray) {
            val selectionRange = getSelectionRange(rangeArrayJsonElement)
            selectionRange?.let { range ->
                if (highlights == null) {
                    highlights = arrayListOf()
                }
                highlights?.add(range)
            }
        }

        return highlights
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun getSelectionRange(rangeArrayJsonElement: JsonElement): Pair<Int, Int>? {
        return try {
            val startIndex = rangeArrayJsonElement.asJsonArray.get(0).asInt
            val endIndex = rangeArrayJsonElement.asJsonArray.get(1).asInt
            Pair(startIndex, endIndex)
        } catch (ex: ClassCastException) {
            null
        } catch (ex: IllegalStateException) {
            null
        } catch (ex: IndexOutOfBoundsException) {
            null
        }
    }

    companion object {
        private const val MARKS = "marks"
    }
}
