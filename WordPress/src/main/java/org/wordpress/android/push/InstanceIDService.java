package org.wordpress.android.push;

import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;

public class InstanceIDService extends InstanceIDListenerService {
    @Override
    public void onTokenRefresh() {
        // Register for Cloud messaging
        GCMRegistrationIntentService.enqueueWork(this,
                new Intent(this, GCMRegistrationIntentService.class));
    }
}
