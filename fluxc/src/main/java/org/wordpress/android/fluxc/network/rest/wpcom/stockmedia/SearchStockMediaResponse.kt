package org.wordpress.android.fluxc.network.rest.wpcom.stockmedia

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.annotations.JsonAdapter
import org.wordpress.android.fluxc.model.StockMediaModel
import org.wordpress.android.fluxc.network.utils.getInt
import org.wordpress.android.fluxc.network.utils.getJsonObject
import org.wordpress.android.fluxc.network.utils.getString
import java.lang.reflect.Type

/**
 * Response to GET request to search for stock media item
 */
@JsonAdapter(SearchStockMediaDeserializer::class)
class SearchStockMediaResponse(
    val found: Int,
    val nextPage: Int,
    val canLoadMore: Boolean,
    val media: List<StockMediaModel>
)

private class SearchStockMediaDeserializer : JsonDeserializer<SearchStockMediaResponse> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): SearchStockMediaResponse {
        val jsonObject = json.asJsonObject
        val found = jsonObject.getInt("found")
        val mediaList = jsonObject.getAsJsonArray("media")?.map { getStockMedia(it.asJsonObject) } ?: ArrayList()
        val jsonMeta = jsonObject.getJsonObject("meta")
        return try {
            val nextPage = jsonMeta.getInt("next_page")
            SearchStockMediaResponse(found, nextPage, true, mediaList)
        } catch (e: NumberFormatException) {
            // note that "next_page" will be "false" rather than an int if this is the last page
            SearchStockMediaResponse(found, 0, false, mediaList)
        }
    }

    private fun getStockMedia(jsonMedia: JsonObject): StockMediaModel {
        val media = StockMediaModel()
        media.name = jsonMedia.getString("name", unescapeHtml4 = true)
        media.title = jsonMedia.getString("title", unescapeHtml4 = true)
        media.date = jsonMedia.getString("date")
        media.extension = jsonMedia.getString("extension")
        media.file = jsonMedia.getString("file")
        media.guid = jsonMedia.getString("guid")
        media.height = jsonMedia.getInt("height")
        media.id = jsonMedia.getString("ID")
        media.type = jsonMedia.getString("type")
        media.url = jsonMedia.getString("URL")
        media.width = jsonMedia.getInt("width")

        jsonMedia.get("thumbnails")?.let {
            val jsonThumbnails = it.asJsonObject
            media.thumbnail = jsonThumbnails.getString("thumbnail")
            media.largeThumbnail = jsonThumbnails.getString("large")
            media.mediumThumbnail = jsonThumbnails.getString("medium")
            media.postThumbnail = jsonThumbnails.getString("post_thumbnail")
        }
        return media
    }
}
