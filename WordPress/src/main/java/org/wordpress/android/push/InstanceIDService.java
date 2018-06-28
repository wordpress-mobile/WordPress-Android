package org.wordpress.android.push;

import android.content.Intent;

import com.google.firebase.iid.FirebaseInstanceIdService;

public class InstanceIDService extends FirebaseInstanceIdService {
    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is also called
     * when the InstanceID token is initially generated, so this is where
     * you retrieve the token.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        // Register for Cloud messaging
        GCMRegistrationIntentService.enqueueWork(this,
                new Intent(this, GCMRegistrationIntentService.class));
    }
}
