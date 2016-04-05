package org.wordpress.android.models;

import android.content.Context;
import android.support.v4.content.ContextCompat;

import org.wordpress.android.R;

public enum Role {
    SUPER_ADMIN(R.string.role_super_admin, R.color.orange_fire),
    ADMIN(R.string.role_admin, R.color.grey_dark),
    EDITOR(R.string.role_editor, R.color.blue_dark),
    AUTHOR(R.string.role_author, R.color.blue_wordpress),
    CONTRIBUTOR(R.string.role_contributor, R.color.blue_wordpress),
    UNSUPPORTED(R.string.role_unsupported, R.color.blue_wordpress);

    private static String KEY_SUPER_ADMIN = "super_administrator";
    private static String KEY_ADMIN = "administrator";
    private static String KEY_EDITOR = "editor";
    private static String KEY_AUTHOR = "author";
    private static String KEY_CONTRIBUTOR = "contributor";

    private final int label;
    private final int backgroundColor;

    Role(int label, int backgroundColor) {
        this.label = label;
        this.backgroundColor = backgroundColor;
    }

    // Label to be used in UI
    public static String getLabel(Context context, Role role) {
        return context.getString(role.label);
    }

    // Returns the key for that role
    public static String toKey(Role role) {
        switch (role) {
            case SUPER_ADMIN:
                return KEY_SUPER_ADMIN;
            case ADMIN:
                return KEY_ADMIN;
            case EDITOR:
                return KEY_EDITOR;
            case AUTHOR:
                return KEY_AUTHOR;
            case CONTRIBUTOR:
                return KEY_CONTRIBUTOR;
            case UNSUPPORTED:
                return "unsupported";
        }
        // this is just a fallback for when we don't know the role returned by the server
        return "unsupported";
    }

    // This method is be used to determine the role of the user from network request & db
    public static Role fromKey(String value) {
        if (value == null)
            return Role.UNSUPPORTED;
        if (value.equals(KEY_SUPER_ADMIN))
            return Role.SUPER_ADMIN;
        if (value.equals(KEY_ADMIN))
            return Role.ADMIN;
        if (value.equals(KEY_EDITOR))
            return Role.EDITOR;
        if (value.equals(KEY_AUTHOR))
            return Role.AUTHOR;
        if (value.equals(KEY_CONTRIBUTOR))
            return Role.CONTRIBUTOR;
        return Role.UNSUPPORTED;
    }

    public static int backgroundColor(Context context, Role role) {
        return ContextCompat.getColor(context, role.backgroundColor);
    }
}
