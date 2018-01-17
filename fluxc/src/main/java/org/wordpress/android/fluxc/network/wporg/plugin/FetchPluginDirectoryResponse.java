package org.wordpress.android.fluxc.network.wporg.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

@JsonAdapter(FetchPluginDirectoryResponseDeserializer.class)
public class FetchPluginDirectoryResponse {
    public class FetchPluginDirectoryResponseInfo {
        public int page;
        public int pages;
    }

    public FetchPluginDirectoryResponseInfo info;
    public List<WPOrgPluginResponse> plugins;

    FetchPluginDirectoryResponse() {
        info = new FetchPluginDirectoryResponseInfo();
    }
}

class FetchPluginDirectoryResponseDeserializer implements JsonDeserializer<FetchPluginDirectoryResponse> {
    @Override
    public FetchPluginDirectoryResponse deserialize(JsonElement json, Type typeOfT,
                                                    JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        FetchPluginDirectoryResponse response = new FetchPluginDirectoryResponse();

        if (jsonObject.has("plugins")) {
            JsonElement pluginsEl = jsonObject.get("plugins");
            if (pluginsEl.isJsonArray()) {
                JsonArray pluginsJsonArray = pluginsEl.getAsJsonArray();
                Type collectionType = new TypeToken<List<WPOrgPluginResponse>>(){}.getType();
                response.plugins = context.deserialize(pluginsJsonArray, collectionType);
            }
        }
        if (jsonObject.has("info")) {
            JsonElement infoEl = jsonObject.get("info");
            if (infoEl.isJsonObject()) {
                JsonObject info = jsonObject.get("info").getAsJsonObject();
                if (info.has("page")) {
                    response.info.page = info.get("page").getAsInt();
                }
                if (info.has("pages")) {
                    response.info.pages = info.get("pages").getAsInt();
                }
            }
        }
        return response;
    }
}
