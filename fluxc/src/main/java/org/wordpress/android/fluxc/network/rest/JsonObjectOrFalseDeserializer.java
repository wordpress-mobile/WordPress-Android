package org.wordpress.android.fluxc.network.rest;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class JsonObjectOrFalseDeserializer implements JsonDeserializer<JsonObjectOrFalse> {
    @Override
    public JsonObjectOrFalse deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObjectOrFalse result;

        try {
            Class<?> clazz = (Class) typeOfT;
            Constructor constructor = (Constructor) clazz.getDeclaredConstructor();
            result = (JsonObjectOrFalse) constructor.newInstance();

            if (json.isJsonPrimitive()) {
                // If the value is a JSON primitive (it should be 'false', specifically), it signifies a null object
                return null;
            }

            Field[] fields = clazz.getFields();
            Gson gson = new Gson();
            for (Field field : fields) {
                JsonElement element = json.getAsJsonObject().get(field.getName());
                if (!element.isJsonPrimitive()) {
                    gson.fromJson(element, field.getType());
                    continue;
                }
                Object elementToPrimitive = jsonPrimitiveToJavaPrimitive(field.getType(), element);

                if (elementToPrimitive == null) {
                    gson.fromJson(element, field.getType());
                } else {
                    field.set(result, jsonPrimitiveToJavaPrimitive(field.getType(), element));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new JsonParseException(e.getMessage());
        }
        return result;
    }

    private static Object jsonPrimitiveToJavaPrimitive(Class<?> type, JsonElement element) {
        if (type == String.class) {
            return element.getAsString();
        } else if (type == boolean.class) {
            return element.getAsBoolean();
        } else if (type == byte.class) {
            return element.getAsByte();
        } else if (type == char.class) {
            return element.getAsByte();
        } else if (type == short.class) {
            return element.getAsShort();
        } else if (type == int.class) {
            return element.getAsInt();
        } else if (type == long.class) {
            return element.getAsLong();
        } else if (type == float.class) {
            return element.getAsFloat();
        } else if (type == double.class) {
            return element.getAsDouble();
        }
        return null;
    }
}
