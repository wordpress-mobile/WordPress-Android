package org.wordpress.android.fluxc.network.rest.wpcom.activity

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient.ActivityTypesResponse.ActivityType
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient.ActivityTypesResponse.Groups
import java.lang.reflect.Type

class ActivityTypesDeserializer : JsonDeserializer<Groups?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Groups? {
        return if (context != null && json != null && json.isJsonObject) {
            /**
             * Response example:
             * {
             *   "post": {
             *   "name": "Posts and Pages",
             *   "count": 69
             *   },
             *   "attachment": {
             *     "name": "Media",
             *     "count": 5
             *   },
             *   "user": {
             *     "name": "People",
             *     "count": 2
             *   }
             * }
             */
            val item: ArrayList<ActivityType> = arrayListOf()

            val rowsJsonObject = json.asJsonObject
            rowsJsonObject.keySet().iterator().forEach { key ->
                val activityType = getActivityType(key, rowsJsonObject)
                activityType?.let { item.add(it) }
            }

            return Groups(item)
        } else {
            null
        }
    }

    @Suppress("SwallowedException")
    private fun getActivityType(
        key: String,
        groups: JsonObject
    ): ActivityType? {
        return groups.get(key)?.takeIf { it.isJsonObject }?.asJsonObject?.let { item ->
            try {
                ActivityType(
                        key = key,
                        name = item.get(NAME)?.asString,
                        count = item.get(COUNT)?.asInt
                )
            } catch (ex: ClassCastException) {
                null
            } catch (ex: IllegalStateException) {
                null
            }
        }
    }

    companion object {
        private const val NAME = "name"
        private const val COUNT = "count"
    }
}
