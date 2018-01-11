package org.wordpress.android.fluxc.model.plugin;

public enum PluginDirectoryType {
    APPEARANCE,
    ENGAGEMENT,
    NEW,
    POPULAR,
    SECURITY,
    WRITING;

    public String toString() {
        return this.name();
    }

    public static PluginDirectoryType fromString(String string) {
        if (string != null) {
            for (PluginDirectoryType type : PluginDirectoryType.values()) {
                if (string.equalsIgnoreCase(type.name())) {
                    return type;
                }
            }
        }
        return POPULAR;
    }
}
