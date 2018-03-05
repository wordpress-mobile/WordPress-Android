package org.wordpress.android.fluxc.network.rest.wpcom.stockmedia;

import android.support.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.fluxc.model.StockMediaModel;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Response to GET request to search for stock media item
 */
@SuppressWarnings("WeakerAccess")
@JsonAdapter(SearchStockMediaDeserializer.class)
public class SearchStockMediaResponse {
    public int found;
    public int nextPage;
    public List<StockMediaModel> media;
}

class SearchStockMediaDeserializer implements JsonDeserializer<SearchStockMediaResponse> {
    @Override
    public SearchStockMediaResponse deserialize(JsonElement json,
                                                Type typeOfT,
                                                JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        SearchStockMediaResponse response = new SearchStockMediaResponse();

        response.found = getJsonInt(json, "found");

        if (jsonObject.has("meta")) {
            JsonElement jsonMeta = jsonObject.get("meta");
            // note that "next_page" will be "false" rather than an int if this is the last page
            try {
                response.nextPage = getJsonInt(jsonMeta, "next_page");
            } catch (NumberFormatException e) {
                response.nextPage = 0;
            }
        }

        // parse the media list
        response.media = new ArrayList<>();
        JsonArray jsonMediaList = jsonObject.getAsJsonArray("media");
        if (jsonMediaList != null) {
            for (JsonElement jsonMedia : jsonMediaList) {
                StockMediaModel media = new StockMediaModel();

                media.setName(getJsonUnescapedString(jsonMedia, "name"));
                media.setTitle(getJsonUnescapedString(jsonMedia, "title"));

                media.setDate(getJsonString(jsonMedia, "date"));
                media.setExtension(getJsonString(jsonMedia, "extension"));
                media.setFile(getJsonString(jsonMedia, "file"));
                media.setGuid(getJsonString(jsonMedia, "guid"));
                media.setHeight(getJsonInt(jsonMedia, "height"));
                media.setId(getJsonString(jsonMedia, "ID"));
                media.setType(getJsonString(jsonMedia, "type"));
                media.setUrl(getJsonString(jsonMedia, "URL"));
                media.setWidth(getJsonInt(jsonMedia, "width"));

                JsonElement jsonThumbnails = jsonMedia.getAsJsonObject().get("thumbnails");
                if (jsonThumbnails != null) {
                    media.setThumbnail(getJsonString(jsonThumbnails, "thumbnail"));
                    media.setLargeThumbnail(getJsonString(jsonThumbnails, "large"));
                    media.setMediumThumbnail(getJsonString(jsonThumbnails, "medium"));
                    media.setPostThumbnail(getJsonString(jsonThumbnails, "post_thumbnail"));
                }

                response.media.add(media);
            }
        }

        return response;
    }

    private @Nullable String getJsonString(JsonElement jsonElement, String property) {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (jsonObject.has(property)) {
            return jsonObject.get(property).getAsString();
        }
        return null;
    }

    private @Nullable String getJsonUnescapedString(JsonElement jsonElement, String property) {
        String value = getJsonString(jsonElement, property);
        if (value != null) {
            return StringEscapeUtils.unescapeHtml4(value);
        }
        return null;
    }

    private int getJsonInt(JsonElement jsonElement, String property) {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (jsonObject.has(property)) {
            return jsonObject.get(property).getAsInt();
        }
        return 0;
    }
}
