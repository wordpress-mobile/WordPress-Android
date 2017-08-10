package org.wordpress.android.fluxc.network.rest.wpcom.plugin;

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
    public FetchPluginInfoResponse deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        FetchPluginInfoResponse response = new FetchPluginInfoResponse();
        response.name = jsonObject.get("name").getAsString();
        response.slug = jsonObject.get("slug").getAsString();
        response.version = jsonObject.get("version").getAsString();
        response.rating = jsonObject.get("rating").getAsString();
        JsonObject icons = jsonObject.get("icons").getAsJsonObject();
        response.icon = (icons.has("2x") ? icons.get("2x") : icons.get("1x")).getAsString();
        return response;
    }
}
