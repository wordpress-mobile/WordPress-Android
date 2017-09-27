package org.wordpress.android.fluxc.network.wporg.plugin;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;

@JsonAdapter(PluginInfoDeserializer.class)
public class FetchPluginInfoResponse {
    public String name;
    public String slug;
    public String version;
    public String rating;
    public String icon;
}

class PluginInfoDeserializer implements JsonDeserializer<FetchPluginInfoResponse> {
    @Override
    public FetchPluginInfoResponse deserialize(JsonElement json, Type typeOfT,
                                               JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        FetchPluginInfoResponse response = new FetchPluginInfoResponse();
        response.name = getStringFromJsonIfAvailable(jsonObject, "name");
        response.slug = getStringFromJsonIfAvailable(jsonObject, "slug");
        response.version = getStringFromJsonIfAvailable(jsonObject, "version");
        response.rating = getStringFromJsonIfAvailable(jsonObject, "rating");
        if (jsonObject.has("icons")) {
            JsonObject icons = jsonObject.get("icons").getAsJsonObject();
            if (icons.has("2x")) {
                response.icon = icons.get("2x").getAsString();
            } else if (icons.has("1x")) {
                response.icon = icons.get("1x").getAsString();
            }
        }
        return response;
    }

    private String getStringFromJsonIfAvailable(JsonObject jsonObject, String property) {
        if (jsonObject.has(property)) {
            return jsonObject.get(property).getAsString();
        }
        return null;
    }
}
