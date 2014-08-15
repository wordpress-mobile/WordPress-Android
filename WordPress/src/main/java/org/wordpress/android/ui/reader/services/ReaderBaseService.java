package org.wordpress.android.ui.reader.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

public class ReaderBaseService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    void sendLocalBroadcast(Intent intent) {
        if (intent != null) {
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

}
