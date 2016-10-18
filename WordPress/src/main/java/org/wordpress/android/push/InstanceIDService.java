package org.wordpress.android.push;

import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;

import org.wordpress.android.push.GCMRegistrationIntentService;

public class InstanceIDService extends InstanceIDListenerService {
    @Override
    public void onTokenRefresh() {
        // Register for Cloud messaging
        startService(new Intent(this, GCMRegistrationIntentService.class));
    }
}
