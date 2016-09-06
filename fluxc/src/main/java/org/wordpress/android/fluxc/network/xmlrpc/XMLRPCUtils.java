package org.wordpress.android.fluxc.network.xmlrpc;

import android.support.annotation.NonNull;

import org.wordpress.android.util.MapUtils;

import java.util.Map;

public class XMLRPCUtils {
    /**
     * Get value from a deserialized XMLRPC Map response
     * @return the value
     */
    @NonNull
    public static <T> T safeGetMapValue(@NonNull Map<?, ?> map, Class<T> type, T defaultValue) {
        if (map != null) {
            Object value = map.get("value");
            if (type.isInstance(value)) {
                return (T) value;
            }

            if (type == String.class) {
                return (T) MapUtils.getMapStr(map, "value");
            } else if (type == Boolean.class) {
                return (T) Boolean.valueOf(MapUtils.getMapBool(map, "value"));
            } else if (type == Integer.class) {
                return (T) Integer.valueOf(MapUtils.getMapInt(map, "value", defaultValue));
            } else if (type == Float.class) {
                return (T) Float.valueOf(MapUtils.getMapFloat(map, "value", defaultValue));
            } else if (type == Double.class) {
                return (T) Float.valueOf(MapUtils.getMapDouble(map, "value", defaultValue));
            }
        }
        return defaultValue;
    }
}
