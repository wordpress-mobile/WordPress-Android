package org.wordpress.android.util;

import android.content.Context;
import android.content.pm.ShortcutManager;

import org.wordpress.android.ui.Shortcut;

import javax.inject.Inject;

public class ShortcutUtils {
    private final Context mContext;

    @Inject public ShortcutUtils(Context context) {
        mContext = context;
    }

    public void reportShortcutUsed(Shortcut shortcut) {
        ShortcutManager shortcutManager = mContext.getSystemService(ShortcutManager.class);
        if (shortcutManager != null) {
            shortcutManager.reportShortcutUsed(shortcut.mId);
        }
    }
}
