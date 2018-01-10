package org.wordpress.android.fluxc.network.wporg.plugin;

import android.support.annotation.Nullable;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;

@SuppressWarnings("WeakerAccess")
@JsonAdapter(WPOrgPluginDeserializer.class)
public class WPOrgPluginResponse {
    public String author;
    public String authorUrl;
    public String banner;
    public String homepageUrl;
    public String icon;
    public String lastUpdated;
    public String name;
    public String rating;
    public String requiredWordPressVersion;
    public String slug;
    public String version;
    public int downloadCount;

    // Sections
    public String descriptionAsHtml;
    public String faqAsHtml;
    public String installationInstructionsAsHtml;
    public String whatsNewAsHtml;

    // Ratings
    public int numberOfRatings;
    public int numberOfRatingsOfOne;
    public int numberOfRatingsOfTwo;
    public int numberOfRatingsOfThree;
    public int numberOfRatingsOfFour;
    public int numberOfRatingsOfFive;
}

class WPOrgPluginDeserializer implements JsonDeserializer<WPOrgPluginResponse> {
    @Override
    public WPOrgPluginResponse deserialize(JsonElement json, Type typeOfT,
                                           JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        WPOrgPluginResponse response = new WPOrgPluginResponse();
        // Sample string: <a href=\"https://automattic.com/wordpress-plugins/\">Automattic</a>
        String authorHtml = getStringFromJsonIfAvailable(jsonObject, "author");
        // Although we could use Regex matching or some other "nicer" way to achieve the same thing, since we'll be
        // performing these repeatedly and the return format is always the same, we can do it more efficiently
        response.author = getSubstringBetweenTwoStrings(authorHtml, "\">", "</a>");
        response.authorUrl = getSubstringBetweenTwoStrings(authorHtml, "href=\"", "\">");
        response.banner = getBannerFromJson(jsonObject);
        response.downloadCount = getIntFromJsonIfAvailable(jsonObject, "downloaded");
        response.icon = getIconFromJson(jsonObject);
        response.homepageUrl = getStringFromJsonIfAvailable(jsonObject, "homepage");
        response.lastUpdated = getStringFromJsonIfAvailable(jsonObject, "last_updated");
        response.name = getStringFromJsonIfAvailable(jsonObject, "name");
        response.rating = getStringFromJsonIfAvailable(jsonObject, "rating");
        response.requiredWordPressVersion = getStringFromJsonIfAvailable(jsonObject, "requires");
        response.slug = getStringFromJsonIfAvailable(jsonObject, "slug");
        response.version = getStringFromJsonIfAvailable(jsonObject, "version");

        // Sections
        if (jsonObject.has("sections")) {
            JsonObject sections = jsonObject.get("sections").getAsJsonObject();
            response.descriptionAsHtml = getStringFromJsonIfAvailable(sections, "description");
            response.faqAsHtml = getStringFromJsonIfAvailable(sections, "faq");
            response.installationInstructionsAsHtml = getStringFromJsonIfAvailable(sections, "installation");
            response.whatsNewAsHtml = getStringFromJsonIfAvailable(sections, "changelog");
        }

        // Ratings
        response.numberOfRatings = getIntFromJsonIfAvailable(jsonObject, "num_ratings");
        if (jsonObject.has("ratings")) {
            JsonObject ratings = jsonObject.get("ratings").getAsJsonObject();
            response.numberOfRatingsOfOne = getIntFromJsonIfAvailable(ratings, "1");
            response.numberOfRatingsOfTwo = getIntFromJsonIfAvailable(ratings, "2");
            response.numberOfRatingsOfThree = getIntFromJsonIfAvailable(ratings, "3");
            response.numberOfRatingsOfFour = getIntFromJsonIfAvailable(ratings, "4");
            response.numberOfRatingsOfFive = getIntFromJsonIfAvailable(ratings, "5");
        }

        return response;
    }

    private @Nullable String getStringFromJsonIfAvailable(JsonObject jsonObject, String property) {
        if (jsonObject.has(property)) {
            return jsonObject.get(property).getAsString();
        }
        return null;
    }

    private int getIntFromJsonIfAvailable(JsonObject jsonObject, String property) {
        if (jsonObject.has(property)) {
            return jsonObject.get(property).getAsInt();
        }
        return 0;
    }

    private @Nullable String getSubstringBetweenTwoStrings(String originalStr, String startStr, String endStr) {
        int beginIndex = originalStr.indexOf(startStr) + startStr.length();
        int endIndex = originalStr.indexOf(endStr);
        if (beginIndex != -1 && endIndex != -1 && beginIndex < endIndex && endIndex < originalStr.length()) {
            return originalStr.substring(beginIndex, endIndex);
        }
        return null;
    }

    private @Nullable String getBannerFromJson(JsonObject jsonObject) throws JsonParseException {
        if (jsonObject.has("banners")) {
            JsonObject banners = jsonObject.get("banners").getAsJsonObject();
            if (banners.has("high")) {
                String bannerUrlHigh = banners.get("high").getAsString();
                // When high version is not available API returns `false` instead of `null`
                if (!bannerUrlHigh.equalsIgnoreCase("false")) {
                    return bannerUrlHigh;
                }
            }
            // High version wasn't available
            if (banners.has("low")) {
                return banners.get("low").getAsString();
            }
        }
        return null;
    }

    private @Nullable String getIconFromJson(JsonObject jsonObject) throws JsonParseException {
        if (jsonObject.has("icons")) {
            JsonObject icons = jsonObject.get("icons").getAsJsonObject();
            if (icons.has("2x")) {
                return icons.get("2x").getAsString();
            } else if (icons.has("1x")) {
                return icons.get("1x").getAsString();
            }
        }
        return null;
    }
}
