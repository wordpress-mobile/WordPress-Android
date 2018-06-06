package org.wordpress.android.fluxc.network.rest.wpcom.stockmedia

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.annotations.JsonAdapter
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.model.StockMediaModel
import org.wordpress.android.fluxc.network.utils.getInt
import org.wordpress.android.fluxc.network.utils.getJsonObject
import org.wordpress.android.fluxc.network.utils.getString
import java.lang.reflect.Type
import java.util.ArrayList

/**
 * Response to GET request to search for stock media item
 */
@JsonAdapter(SearchStockMediaDeserializer::class)
class SearchStockMediaResponse {
    var found: Int = 0
    var nextPage: Int = 0
    var canLoadMore: Boolean = false
    var media: List<StockMediaModel> = ArrayList()
}

internal class SearchStockMediaDeserializer : JsonDeserializer<SearchStockMediaResponse> {
    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext): SearchStockMediaResponse {
        val jsonObject = json.asJsonObject
        val response = SearchStockMediaResponse()

        response.found = jsonObject.getInt("found")

        val jsonMeta = jsonObject.getJsonObject("meta")
            // note that "next_page" will be "false" rather than an int if this is the last page
        try {
            response.nextPage = jsonMeta.getInt("next_page")
            response.canLoadMore = true
        } catch (e: NumberFormatException) {
            response.nextPage = 0
            response.canLoadMore = false
        }

        // parse the media list
        val mediaList = jsonObject.getAsJsonArray("media")?.map {
            val jsonMedia = it.asJsonObject
            val media = StockMediaModel()

            media.name = StringEscapeUtils.unescapeHtml4(jsonMedia.getString("name"))
            media.title = StringEscapeUtils.unescapeHtml4(jsonMedia.getString("title"))

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
            return@map media
        }
        response.media = mediaList ?: ArrayList()

        return response
    }
}
