package org.wordpress.android.ui.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.wordpress.android.push.GCMMessageService;

/*
 * Re-builds notifications when the user locks/unlocks the screen
 */
public class ScreenLockUnlockBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (Intent.ACTION_SCREEN_OFF.equals(action) || Intent.ACTION_USER_PRESENT.equals(action)) {
            GCMMessageService.rebuildAndUpdateNotifsOnSystemBarForRemainingNote(context);
        }
    }
}
