package org.wordpress.android.fluxc.network.xmlrpc;

import android.support.annotation.NonNull;

import org.wordpress.android.util.MapUtils;

import java.util.Map;

public class XMLRPCUtils {
    /**
     * Get value from a deserialized XMLRPC Map response
     */
    @NonNull
    public static <T> T safeGetMapValue(@NonNull Map<?, ?> map, T defaultValue) {
        return safeGetMapValue(map, "value", defaultValue);
    }

    @NonNull
    public static <T> T safeGetMapValue(@NonNull Map<?, ?> map, String key, T defaultValue) {
        if (!map.containsKey(key)) {
            return defaultValue;
        }
        Class<T> clazz = (Class<T>) defaultValue.getClass();

        Object value = map.get(key);
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }

        Object result;
        if (defaultValue instanceof String) {
            result = MapUtils.getMapStr(map, key);
        } else if (defaultValue instanceof Boolean) {
            result = MapUtils.getMapBool(map, key);
        } else if (defaultValue instanceof Integer) {
            result = MapUtils.getMapInt(map, key, (Integer) defaultValue);
        } else if (defaultValue instanceof Long) {
            result = MapUtils.getMapLong(map, key, (Long) defaultValue);
        } else if (defaultValue instanceof Float) {
            result = MapUtils.getMapFloat(map, key, (Float) defaultValue);
        } else if (defaultValue instanceof Double) {
            result = MapUtils.getMapDouble(map, key, (Double) defaultValue);
        }

        if (result != null) {
            return clazz.cast(result);
        }

        return defaultValue;
    }

    public static <T> T safeGetNestedMapValue(@NonNull Map<?, ?> map, String key, T defaultValue) {
        if (!map.containsKey(key)) {
            return defaultValue;
        }
        Object objectMap = map.get(key);
        if (!(objectMap instanceof Map)) {
            return defaultValue;
        }
        return safeGetMapValue((Map<?, ?>) objectMap, defaultValue);
    }
}
