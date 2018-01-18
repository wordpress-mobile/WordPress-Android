package org.wordpress.android.fluxc.model.plugin;

public enum PluginDirectoryType {
    NEW,
    POPULAR;

    public String toString() {
        return this.name().toLowerCase();
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
