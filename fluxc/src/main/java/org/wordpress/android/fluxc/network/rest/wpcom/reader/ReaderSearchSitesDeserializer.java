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
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

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

                // parse the site meta data
                try {
                    JsonElement jsonMetaSite = jsonFeed.getAsJsonObject()
                                                       .getAsJsonObject("meta")
                                                       .getAsJsonObject("data")
                                                       .getAsJsonObject("site");
                    feed.setFollowing(getJsonBoolean(jsonMetaSite, "is_following"));
                    String description = getJsonString(jsonMetaSite, "description");
                    if (description != null) {
                        feed.setDescription(StringEscapeUtils.unescapeHtml4(description));
                    }

                    JsonElement jsonIcon = jsonMetaSite.getAsJsonObject().getAsJsonObject("icon");
                    feed.setIconUrl(getJsonString(jsonIcon, "ico"));
                } catch (NullPointerException e) {
                    AppLog.e(T.API, "NPE parsing reader site metadata", e);
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

    private boolean getJsonBoolean(JsonElement jsonElement, String property) {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (jsonObject.has(property)) {
            return jsonObject.get(property).getAsBoolean();
        }
        return false;
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
