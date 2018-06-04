package org.wordpress.android.fluxc.network.rest.wpcom.reader;

import android.support.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.fluxc.model.ReaderFeedModel;

import java.lang.reflect.Type;
import java.util.ArrayList;

class ReaderSearchSitesDeserializer implements JsonDeserializer<ReaderSearchSitesResponse> {
    @Override
    public ReaderSearchSitesResponse deserialize(JsonElement json,
                                                Type typeOfT,
                                                JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        ReaderSearchSitesResponse response = new ReaderSearchSitesResponse();


        response.offset = getJsonInt(json, "offset");

        response.feeds = new ArrayList<ReaderFeedModel>();
        JsonArray jsonFeedsList = jsonObject.getAsJsonArray("feeds");
        if (jsonFeedsList != null) {
            for (JsonElement jsonFeed : jsonFeedsList) {
                ReaderFeedModel feed = new ReaderFeedModel();
                feed.setFeedId(getJsonLong(jsonFeed, "feed_ID"));
                feed.setSubscribeUrl(getJsonString(jsonFeed, "subscribe_URL"));
                feed.setSubscriberCount(getJsonInt(jsonFeed, "subscribers_count"));
                feed.setUrl(getJsonString(jsonFeed, "URL"));
                String title = getJsonString(jsonFeed, "title");
                if (title != null) {
                    feed.setTitle(StringEscapeUtils.unescapeHtml4(title));
                }
                response.feeds.add(feed);
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

    private int getJsonInt(JsonElement jsonElement, String property) {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (jsonObject.has(property)) {
            return jsonObject.get(property).getAsInt();
        }
        return 0;
    }

    private long getJsonLong(JsonElement jsonElement, String property) {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (jsonObject.has(property)) {
            return jsonObject.get(property).getAsLong();
        }
        return 0;
    }
}
