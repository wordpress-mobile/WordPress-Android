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
        jsonObject.getJsonObject("sections")?.let { sections ->
            response.descriptionAsHtml = sections.getString("description")
            response.faqAsHtml = sections.getString("faq")
            response.installationInstructionsAsHtml = sections.getString("installation")
            response.whatsNewAsHtml = sections.getString("changelog")
        }

        // Ratings
        response.numberOfRatings = jsonObject.getInt("num_ratings")
        jsonObject.getJsonObject("ratings")?.let { ratings ->
            response.numberOfRatingsOfOne = ratings.getInt("1")
            response.numberOfRatingsOfTwo = ratings.getInt("2")
            response.numberOfRatingsOfThree = ratings.getInt("3")
            response.numberOfRatingsOfFour = ratings.getInt("4")
            response.numberOfRatingsOfFive = ratings.getInt("5")
        }

        return response
    }

    private fun getBannerFromJson(jsonObject: JsonObject): String? {
        val banners = jsonObject.getJsonObject("banners")
        banners?.getString("high")?.let { bannerUrlHigh ->
            // When high version is not available API returns `false` instead of `null`
            if (!bannerUrlHigh.equals("false", ignoreCase = true)) {
                return bannerUrlHigh
            }
        }
        // High version wasn't available
        return banners?.getString("low")
    }

    private fun getIconFromJson(jsonObject: JsonObject): String? {
        val icons = jsonObject.getJsonObject("icons")
        return icons?.getString("2x") ?: icons?.getString("1x")
    }
}
