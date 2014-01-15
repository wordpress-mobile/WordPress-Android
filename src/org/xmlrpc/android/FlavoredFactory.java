package org.xmlrpc.android;

public class FlavoredFactory {
    public enum Mode {PRODUCTION, TEST}
    private static Mode mMode = Mode.PRODUCTION;

    public static Mode getMode() {
        return mMode;
    }

    public static void setMode(Mode mode) {
        mMode = mode;
    }
}
