package org.wordpress.android.fluxc.network.wporg.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

@JsonAdapter(FetchFeaturedPluginsResponseDeserializer.class)
public class FetchFeaturedPluginsResponse {
    public List<WPOrgPluginResponse> plugins;
}
class FetchFeaturedPluginsResponseDeserializer implements JsonDeserializer<FetchFeaturedPluginsResponse> {
    @Override
    public FetchFeaturedPluginsResponse deserialize(JsonElement json, Type typeOfT,
                                                    JsonDeserializationContext context) throws JsonParseException {
        JsonArray jsonArray = json.getAsJsonArray();
        FetchFeaturedPluginsResponse response = new FetchFeaturedPluginsResponse();

        Type collectionType = new TypeToken<List<WPOrgPluginResponse>>(){}.getType();
        response.plugins = context.deserialize(jsonArray, collectionType);
        return response;
    }
}
