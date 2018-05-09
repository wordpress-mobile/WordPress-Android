package org.wordpress.android.util;

import android.content.Context;
import android.content.pm.ShortcutManager;
import android.os.Build.VERSION_CODES;

import org.wordpress.android.ui.Shortcut;

import javax.inject.Inject;

public class ShortcutUtils {
    private final Context mContext;

    @Inject public ShortcutUtils(Context context) {
        mContext = context;
    }

    public void reportShortcutUsed(Shortcut shortcut) {
        if (android.os.Build.VERSION.SDK_INT >= VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = mContext.getSystemService(ShortcutManager.class);
            if (shortcutManager != null) {
                shortcutManager.reportShortcutUsed(shortcut.mId);
            }
        }
    }
}
