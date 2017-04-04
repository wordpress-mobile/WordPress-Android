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
        if (map != null) {
            if (!map.containsKey(key)) {
                return defaultValue;
            }
            Object value = map.get(key);
            if (defaultValue.getClass().isInstance(value)) {
                return (T) value;
            }

            if (defaultValue instanceof String) {
                return (T) MapUtils.getMapStr(map, key);
            } else if (defaultValue instanceof Boolean) {
                return (T) Boolean.valueOf(MapUtils.getMapBool(map, key));
            } else if (defaultValue instanceof Integer) {
                return (T) Integer.valueOf(MapUtils.getMapInt(map, key, (Integer) defaultValue));
            } else if (defaultValue instanceof Long) {
                return (T) Long.valueOf(MapUtils.getMapLong(map, key, (Long) defaultValue));
            } else if (defaultValue instanceof Float) {
                return (T) Float.valueOf(MapUtils.getMapFloat(map, key, (Float) defaultValue));
            } else if (defaultValue instanceof Double) {
                return (T) Double.valueOf(MapUtils.getMapDouble(map, key, (Double) defaultValue));
            }
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
