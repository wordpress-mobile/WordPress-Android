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
        if (map != null) {
            if (!map.containsKey("value")) {
                return defaultValue;
            }
            Object value = map.get("value");
            if (defaultValue.getClass().isInstance(value)) {
                return (T) value;
            }

            if (defaultValue instanceof String) {
                return (T) MapUtils.getMapStr(map, "value");
            } else if (defaultValue instanceof Boolean) {
                return (T) Boolean.valueOf(MapUtils.getMapBool(map, "value"));
            } else if (defaultValue instanceof Integer) {
                return (T) Integer.valueOf(MapUtils.getMapInt(map, "value", (Integer) defaultValue));
            } else if (defaultValue instanceof Long) {
                return (T) Long.valueOf(MapUtils.getMapLong(map, "value", (Long) defaultValue));
            } else if (defaultValue instanceof Float) {
                return (T) Float.valueOf(MapUtils.getMapFloat(map, "value", (Float) defaultValue));
            } else if (defaultValue instanceof Double) {
                return (T) Double.valueOf(MapUtils.getMapDouble(map, "value", (Double) defaultValue));
            }
        }
        return defaultValue;
    }
}
