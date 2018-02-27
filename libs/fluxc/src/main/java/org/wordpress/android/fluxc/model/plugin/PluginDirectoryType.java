package org.wordpress.android.fluxc.model.plugin;

import java.util.Locale;

public enum PluginDirectoryType {
    FEATURED,
    NEW,
    POPULAR,
    SITE;

    @Override
    public String toString() {
        return this.name().toLowerCase(Locale.US);
    }

    public static PluginDirectoryType fromString(String string) {
        if (string != null) {
            for (PluginDirectoryType type : PluginDirectoryType.values()) {
                if (string.equalsIgnoreCase(type.name())) {
                    return type;
                }
            }
        }
        return NEW;
    }
}
