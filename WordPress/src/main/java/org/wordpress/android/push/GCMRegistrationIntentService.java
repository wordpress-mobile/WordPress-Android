package org.wordpress.android.push;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.text.TextUtils;

import com.google.firebase.iid.FirebaseInstanceId;

import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.support.ZendeskHelper;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.UUID;

import javax.inject.Inject;

import static org.wordpress.android.JobServiceId.JOB_GCM_REG_SERVICE_ID;

public class GCMRegistrationIntentService extends JobIntentService {
    @Inject AccountStore mAccountStore;
    @Inject ZendeskHelper mZendeskHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        ((WordPress) getApplication()).component().inject(this);
    }

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, GCMRegistrationIntentService.class, JOB_GCM_REG_SERVICE_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        try {
            String token = FirebaseInstanceId.getInstance().getToken();
            sendRegistrationToken(token);
        } catch (Exception e) {
            // SecurityException can happen on some devices without Google services (these devices probably strip
            // the AndroidManifest.xml and remove unsupported permissions).
            AppLog.e(T.NOTIFS, "Google Play Services unavailable: ", e);
        }
    }

    @Override
    public boolean onStopCurrentWork() {
        // if this job is stopped, we really need this to be re-scheduled and re-register the token with
        // our servers in order to keep receiving notifications, so let's just return `true`.
        return true;
    }

    public void sendRegistrationToken(String gcmToken) {
        if (!TextUtils.isEmpty(gcmToken)) {
            AppLog.i(T.NOTIFS, "Sending GCM token to our remote services: " + gcmToken);
            // Register to WordPress.com notifications
            if (mAccountStore.hasAccessToken()) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                // Get or create UUID for WP.com notes api
                String uuid = preferences.getString(NotificationsUtils.WPCOM_PUSH_DEVICE_UUID, null);
                if (uuid == null) {
                    uuid = UUID.randomUUID().toString();
                    preferences.edit().putString(NotificationsUtils.WPCOM_PUSH_DEVICE_UUID, uuid).apply();
                }
                preferences.edit().putString(NotificationsUtils.WPCOM_PUSH_DEVICE_TOKEN, gcmToken).apply();
                NotificationsUtils.registerDeviceForPushNotifications(this, gcmToken);
            }

            mZendeskHelper.enablePushNotifications();
        } else {
            AppLog.w(T.NOTIFS, "Empty GCM token, can't register the id on remote services");
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                             .remove(NotificationsUtils.WPCOM_PUSH_DEVICE_TOKEN).apply();
        }
    }
}
