package org.wordpress.android.fluxc.network.wporg.plugin

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.JsonAdapter
import org.wordpress.android.fluxc.network.utils.getInt
import org.wordpress.android.fluxc.network.utils.getJsonObject
import org.wordpress.android.fluxc.network.utils.getString
import java.lang.reflect.Type

@JsonAdapter(WPOrgPluginDeserializer::class)
class WPOrgPluginResponse {
    var authorAsHtml: String? = null
    var banner: String? = null
    var homepageUrl: String? = null
    var icon: String? = null
    var lastUpdated: String? = null
    var name: String? = null
    var rating: String? = null
    var requiredWordPressVersion: String? = null
    var slug: String? = null
    var version: String? = null
    var downloadCount: Int = 0

    // Sections
    var descriptionAsHtml: String? = null
    var faqAsHtml: String? = null
    var installationInstructionsAsHtml: String? = null
    var whatsNewAsHtml: String? = null

    // Ratings
    var numberOfRatings: Int = 0
    var numberOfRatingsOfOne: Int = 0
    var numberOfRatingsOfTwo: Int = 0
    var numberOfRatingsOfThree: Int = 0
    var numberOfRatingsOfFour: Int = 0
    var numberOfRatingsOfFive: Int = 0

    // Errors are returned with success code
    var errorMessage: String? = null
}

class WPOrgPluginDeserializer : JsonDeserializer<WPOrgPluginResponse> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): WPOrgPluginResponse {
        val jsonObject = json.asJsonObject
        val response = WPOrgPluginResponse()
        response.errorMessage = jsonObject.getString("error")
        // Response has an error instead of the plugin information
        if (!response.errorMessage.isNullOrEmpty()) {
            return response
        }
        response.authorAsHtml = jsonObject.getString("author")
        response.banner = getBannerFromJson(jsonObject)
        response.downloadCount = jsonObject.getInt("downloaded")
        response.icon = getIconFromJson(jsonObject)
        response.homepageUrl = jsonObject.getString("homepage")
        response.lastUpdated = jsonObject.getString("last_updated")
        response.name = jsonObject.getString("name")
        response.rating = jsonObject.getString("rating")
        response.requiredWordPressVersion = jsonObject.getString("requires")
        response.slug = jsonObject.getString("slug")
        response.version = jsonObject.getString("version")

        // Sections
        val sections = jsonObject.getJsonObject("sections")
        response.descriptionAsHtml = sections?.getString("description")
        response.faqAsHtml = sections?.getString("faq")
        response.installationInstructionsAsHtml = sections?.getString("installation")
        response.whatsNewAsHtml = sections?.getString("changelog")

        // Ratings
        response.numberOfRatings = jsonObject.getInt("num_ratings")
        val ratings = jsonObject.getJsonObject("ratings")
        response.numberOfRatingsOfOne = ratings?.getInt("1") ?: 0
        response.numberOfRatingsOfTwo = ratings?.getInt("2") ?: 0
        response.numberOfRatingsOfThree = ratings?.getInt("3") ?: 0
        response.numberOfRatingsOfFour = ratings?.getInt("4") ?: 0
        response.numberOfRatingsOfFive = ratings?.getInt("5") ?: 0

        return response
    }

    private fun getBannerFromJson(jsonObject: JsonObject): String? {
        if (jsonObject.has("banners") && jsonObject.get("banners").isJsonObject) {
            val banners = jsonObject.get("banners").asJsonObject
            if (banners.has("high")) {
                val bannerUrlHigh = banners.get("high").asString
                // When high version is not available API returns `false` instead of `null`
                if (!bannerUrlHigh.equals("false", ignoreCase = true)) {
                    return bannerUrlHigh
                }
            }
            // High version wasn't available
            if (banners.has("low")) {
                return banners.get("low").asString
            }
        }
        return null
    }

    private fun getIconFromJson(jsonObject: JsonObject): String? {
        if (jsonObject.has("icons") && jsonObject.get("icons").isJsonObject) {
            val icons = jsonObject.get("icons").asJsonObject
            if (icons.has("2x")) {
                return icons.get("2x").asString
            } else if (icons.has("1x")) {
                return icons.get("1x").asString
            }
        }
        return null
    }
}
