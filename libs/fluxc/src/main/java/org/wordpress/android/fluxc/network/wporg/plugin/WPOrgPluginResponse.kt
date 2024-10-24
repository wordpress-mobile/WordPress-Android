package org.wordpress.android.fluxc.network.wporg.plugin

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.annotations.JsonAdapter
import org.wordpress.android.fluxc.network.utils.getInt
import org.wordpress.android.fluxc.network.utils.getJsonObject
import org.wordpress.android.fluxc.network.utils.getString
import java.lang.reflect.Type

@JsonAdapter(WPOrgPluginDeserializer::class)
class WPOrgPluginResponse(
    val authorAsHtml: String? = null,
    val banner: String? = null,
    val homepageUrl: String? = null,
    val icon: String? = null,
    val lastUpdated: String? = null,
    val name: String? = null,
    val rating: String? = null,
    val requiredWordPressVersion: String? = null,
    val slug: String? = null,
    val version: String? = null,
    val downloadCount: Int = 0,

    // Sections,
    val descriptionAsHtml: String? = null,
    val faqAsHtml: String? = null,
    val installationInstructionsAsHtml: String? = null,
    val whatsNewAsHtml: String? = null,

    // Ratings,
    val numberOfRatings: Int = 0,
    val numberOfRatingsOfOne: Int = 0,
    val numberOfRatingsOfTwo: Int = 0,
    val numberOfRatingsOfThree: Int = 0,
    val numberOfRatingsOfFour: Int = 0,
    val numberOfRatingsOfFive: Int = 0,
    val errorMessage: String? = null
)

private class WPOrgPluginDeserializer : JsonDeserializer<WPOrgPluginResponse> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): WPOrgPluginResponse {
        val jsonObject = json.asJsonObject

        val errorMessage = jsonObject.getString("error")
        // Response has an error instead of the plugin information
        if (!errorMessage.isNullOrEmpty()) {
            return WPOrgPluginResponse(errorMessage = errorMessage)
        }
        val sections = jsonObject.getJsonObject("sections")
        val ratings = jsonObject.getJsonObject("ratings")
        return WPOrgPluginResponse(
                authorAsHtml = jsonObject.getString("author"),
                banner = getBannerFromJson(jsonObject),
                downloadCount = jsonObject.getInt("downloaded"),
                icon = getIconFromJson(jsonObject),
                homepageUrl = jsonObject.getString("homepage"),
                lastUpdated = jsonObject.getString("last_updated"),
                name = jsonObject.getString("name"),
                rating = jsonObject.getString("rating"),
                requiredWordPressVersion = jsonObject.getString("requires"),
                slug = jsonObject.getString("slug"),
                version = jsonObject.getString("version"),

                // sections
                descriptionAsHtml = sections.getString("description"),
                faqAsHtml = sections.getString("faq"),
                installationInstructionsAsHtml = sections.getString("installation"),
                whatsNewAsHtml = sections.getString("changelog"),

                // Ratings
                numberOfRatings = jsonObject.getInt("num_ratings"),
                numberOfRatingsOfOne = ratings.getInt("1"),
                numberOfRatingsOfTwo = ratings.getInt("2"),
                numberOfRatingsOfThree = ratings.getInt("3"),
                numberOfRatingsOfFour = ratings.getInt("4"),
                numberOfRatingsOfFive = ratings.getInt("5")
        )
    }

    private fun getBannerFromJson(jsonObject: JsonObject): String? {
        val banners = jsonObject.getJsonObject("banners")
        banners.getString("high")?.let { bannerUrlHigh ->
            // When high version is not available API returns `false` instead of `null`
            if (!bannerUrlHigh.equals("false", ignoreCase = true)) {
                return bannerUrlHigh
            }
        }
        // High version wasn't available
        return banners.getString("low")
    }

    private fun getIconFromJson(jsonObject: JsonObject): String? {
        val icons = jsonObject.getJsonObject("icons")
        return icons.getString("2x") ?: icons.getString("1x")
    }
}
