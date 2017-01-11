package org.wordpress.android.fluxc.network.rest;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Deserializes a response that is either an arbitrary JSON object, or an empty JSON array.
 * For example, if we want to use CustomServerResponse.class to represent the result of an API call that returns either
 * an object or [], this will deserialize the JSON object into CustomServerResponse.class, or return a null
 * MyServerResponse if the server response was [].
 */
public class JsonObjectOrEmptyArrayDeserializer implements JsonDeserializer<JsonObjectOrEmptyArray> {
    @Override
    public JsonObjectOrEmptyArray deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        if (json.isJsonObject()) {
            return new Gson().fromJson(json, typeOfT);
        }
        return null;
    }
}
