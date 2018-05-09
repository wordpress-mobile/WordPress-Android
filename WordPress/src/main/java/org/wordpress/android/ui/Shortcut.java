package org.wordpress.android.ui;

public enum Shortcut {
    OPEN_STATS("stats", "org.wordpress.android.ui.ShortcutsNavigator.OPEN_STATS"),
    CREATE_NEW_POST("notifications", "org.wordpress.android.ui.ShortcutsNavigator.CREATE_NEW_POST"),
    OPEN_NOTIFICATIONS("new_post", "org.wordpress.android.ui.ShortcutsNavigator.OPEN_NOTIFICATIONS");

    public String mAction;
    public String mId;

    Shortcut(String id, String action) {
        mId = id;
        mAction = action;
    }

    public static Shortcut fromActionString(String action) {
        for (Shortcut item : Shortcut.values()) {
            if (item.mAction.equalsIgnoreCase(action)) {
                return item;
            }
        }
        return null;
    }
}
