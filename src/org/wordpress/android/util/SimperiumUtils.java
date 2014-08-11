/**
 * Simperium integration with WordPress.com
 * Currently used with Notifications
 */
package org.wordpress.android.util;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.simperium.Simperium;
import com.simperium.client.Bucket;
import com.simperium.client.BucketNameInvalid;
import com.simperium.client.BucketObject;
import com.simperium.client.User;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.models.Note;
import org.wordpress.android.util.stats.AnalyticsTracker;
import org.wordpress.android.util.stats.AnalyticsTracker.Stat;

public class SimperiumUtils {
    public static final String BROADCAST_ACTION_SIMPERIUM_NOT_AUTHORIZED = "simperium-not-authorized";

    private static Simperium mSimperium;
    private static Bucket<Note> mNotesBucket;
    private static Bucket<BucketObject> mMetaBucket;

    public static Simperium getSimperium() {
        return mSimperium;
    }

    public static Bucket<Note> getNotesBucket() {
        return mNotesBucket;
    }

    public static Bucket<BucketObject> getMetaBucket() {
        return mMetaBucket;
    }

    public static Simperium configureSimperium(final Context context, String token) {
        // Create a new instance of Simperium if it doesn't exist yet.
        // In any case, authorize the user.
        if (mSimperium == null) {
            mSimperium = Simperium.newClient(BuildConfig.SIMPERIUM_APP_NAME,
                    BuildConfig.SIMPERIUM_APP_SECRET, context);

            try {
                mNotesBucket = mSimperium.bucket(new Note.Schema());
                mMetaBucket = mSimperium.bucket("meta");

                mSimperium.setUserStatusChangeListener(new User.StatusChangeListener() {

                    @Override
                    public void onUserStatusChange(User.Status status) {
                        switch (status) {
                            case AUTHORIZED:
                                mNotesBucket.start();
                                mMetaBucket.start();
                                AnalyticsTracker.track(Stat.SIMPERIUM_USER_AUTHORIZED);
                                break;
                            case NOT_AUTHORIZED:
                                mNotesBucket.stop();
                                mMetaBucket.stop();
                                Intent simperiumNotAuthorizedIntent = new Intent();
                                simperiumNotAuthorizedIntent.setAction(BROADCAST_ACTION_SIMPERIUM_NOT_AUTHORIZED);
                                LocalBroadcastManager.getInstance(context).sendBroadcast(simperiumNotAuthorizedIntent);
                                AnalyticsTracker.track(Stat.SIMPERIUM_USER_NOT_AUTHORIZED);
                                break;
                            default:
                                AnalyticsTracker.track(Stat.SIMPERIUM_USER_UNKNOWN_STATUS);
                                AppLog.d(AppLog.T.SIMPERIUM, "User not authorized yet");
                                break;
                        }
                    }

                });

            } catch (BucketNameInvalid e) {
                AppLog.e(AppLog.T.SIMPERIUM, e.getMessage());
            }
        }

        authorizeUser(mSimperium, token);

        return mSimperium;
    }

    public static void authorizeUser(Simperium simperium, String token) {
        User user = simperium.getUser();

        String tokenFormat = "WPCC/%s/%s";
        String wpccToken = String.format(tokenFormat, BuildConfig.SIMPERIUM_APP_SECRET, StringUtils.notNullStr(token));

        user.setAccessToken(wpccToken);

        // we'll assume the user is AUTHORIZED, and catch NOT_AUTHORIZED if something goes wrong.
        user.setStatus(User.Status.AUTHORIZED);
    }

    public static void resetBucketsAndDeauthorize() {
        if (mNotesBucket != null) {
            mNotesBucket.reset();
        }
        if (mMetaBucket != null) {
            mMetaBucket.reset();
        }

        // Reset user status
        if (mSimperium != null) {
            mSimperium.getUser().setStatus(User.Status.UNKNOWN);
        }
    }
}