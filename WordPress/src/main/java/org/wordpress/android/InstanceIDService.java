package org.wordpress.android;

import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;

public class InstanceIDService extends InstanceIDListenerService {
    @Override
    public void onTokenRefresh() {
        // Register for Cloud messaging
        startService(new Intent(this, GCMRegistrationIntentService.class));
    }
}
