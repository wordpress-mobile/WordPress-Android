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
    public String descriptionAsHtml;
    public String icon;
    public String name;
    public String rating;
    public String slug;
    public String version;
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
        response.descriptionAsHtml = getStringFromJsonIfAvailable(jsonObject, "description");
        response.name = getStringFromJsonIfAvailable(jsonObject, "name");
        response.slug = getStringFromJsonIfAvailable(jsonObject, "slug");
        response.version = getStringFromJsonIfAvailable(jsonObject, "version");
        response.rating = getStringFromJsonIfAvailable(jsonObject, "rating");
        response.icon = getIconFromJson(jsonObject);
        return response;
    }

    private @Nullable String getStringFromJsonIfAvailable(JsonObject jsonObject, String property) {
        if (jsonObject.has(property)) {
            return jsonObject.get(property).getAsString();
        }
        return null;
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
