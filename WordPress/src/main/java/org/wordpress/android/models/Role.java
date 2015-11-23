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

    private final int label;
    private final int backgroundColor;

    Role(int label, int backgroundColor) {
        this.label = label;
        this.backgroundColor = backgroundColor;
    }

    public static String toString(Context context, Role role) {
        return context.getString(role.label);
    }

    // This method will be used to determine the role of the user from network request
    public static Role fromString(String value) {
        if (value == null)
            return Role.UNSUPPORTED;
        if (value.equals("administrator"))
            return Role.ADMIN;
        if (value.equals("editor"))
            return Role.EDITOR;
        if (value.equals("author"))
            return Role.AUTHOR;
        if (value.equals("contributor"))
            return Role.CONTRIBUTOR;
        return Role.UNSUPPORTED;
    }

    public static int backgroundColor(Context context, Role role) {
        return ContextCompat.getColor(context, role.backgroundColor);
    }
}
