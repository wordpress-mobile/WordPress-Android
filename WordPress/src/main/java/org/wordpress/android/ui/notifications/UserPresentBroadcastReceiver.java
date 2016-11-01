package org.wordpress.android.ui.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.wordpress.android.push.GCMMessageService;

/*
 * Re-builds notifications when the user unlocks the screen
 */
public class UserPresentBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        GCMMessageService.rebuildAndUpdateNotifsOnSystemBarForRemainingNote(context);
    }
}
