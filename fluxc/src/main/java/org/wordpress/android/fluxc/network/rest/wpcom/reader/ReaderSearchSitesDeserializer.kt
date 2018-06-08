package org.wordpress.android.fluxc.network.rest.wpcom.reader

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import org.wordpress.android.fluxc.model.ReaderSiteModel
import org.wordpress.android.fluxc.network.utils.getBoolean
import org.wordpress.android.fluxc.network.utils.getInt
import org.wordpress.android.fluxc.network.utils.getJsonObject
import org.wordpress.android.fluxc.network.utils.getLong
import org.wordpress.android.fluxc.network.utils.getString
import java.lang.reflect.Type

class ReaderSearchSitesDeserializer : JsonDeserializer<ReaderSearchSitesResponse> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): ReaderSearchSitesResponse {
        val jsonObject = json.asJsonObject

        val sites = jsonObject.getAsJsonArray("feeds").map {
            val jsonFeed = it.asJsonObject
            val site = ReaderSiteModel()
            site.siteId = jsonFeed.getLong("blog_ID")
            site.feedId = jsonFeed.getLong("feed_ID")
            site.subscribeUrl = jsonFeed.getString("subscribe_URL")
            site.subscriberCount = jsonFeed.getInt("subscribers_count")
            site.url = jsonFeed.getString("URL")
            site.title = jsonFeed.getString("title", unescapeHtml4 = true)

            // parse the site meta data
            val jsonSite = jsonFeed.getJsonObject("meta").getJsonObject("data").getJsonObject("site")
            site.isFollowing = jsonSite.getBoolean("is_following")
            site.description = jsonSite.getString("description", unescapeHtml4 = true)
            site.iconUrl = jsonSite.getJsonObject("icon").getString("ico")

            site
        }

        return ReaderSearchSitesResponse(sites)
    }
}
