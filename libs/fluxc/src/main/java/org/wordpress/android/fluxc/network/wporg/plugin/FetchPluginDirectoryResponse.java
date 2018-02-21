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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonAdapter(FetchPluginDirectoryResponseDeserializer.class)
public class FetchPluginDirectoryResponse {
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
        if (jsonObject.has("info")) {
            response.info = context.deserialize(jsonObject.get("info"), FetchPluginDirectoryResponseInfo.class);
        }
        if (jsonObject.has("plugins")) {
            JsonElement pluginsEl = jsonObject.get("plugins");
            if (pluginsEl.isJsonArray()) {
                JsonArray pluginsJsonArray = pluginsEl.getAsJsonArray();
                Type collectionType = new TypeToken<List<WPOrgPluginResponse>>(){}.getType();
                response.plugins = context.deserialize(pluginsJsonArray, collectionType);
            } else if (pluginsEl.isJsonObject()) {
                JsonObject pluginsJsonObject = pluginsEl.getAsJsonObject();
                response.plugins = new ArrayList<>();
                for (Map.Entry<String, JsonElement> entry : pluginsJsonObject.entrySet()) {
                    WPOrgPluginResponse pluginResponse = context.deserialize(entry.getValue(),
                            WPOrgPluginResponse.class);
                    if (pluginResponse != null) {
                        response.plugins.add(pluginResponse);
                    }
                }
            }
        }
        return response;
    }
}

class FetchPluginDirectoryResponseInfo {
    public int page;
    public int pages;
}
