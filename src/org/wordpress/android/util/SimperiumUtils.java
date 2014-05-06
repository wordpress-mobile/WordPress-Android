/**
 * Simperium integration with WordPress.com
 * Currently used with Notifications
 */
package org.wordpress.android.util;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.simperium.Simperium;
import com.simperium.client.Bucket;
import com.simperium.client.BucketNameInvalid;
import com.simperium.client.BucketObject;
import com.simperium.client.User;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.models.Note;

public class SimperiumUtils {
    public static final String BROADCAST_ACTION_SIMPERIUM_SIGNED_IN = "simperium-signin";
    public static final String BROADCAST_ACTION_SIMPERIUM_NOT_AUTHORIZED = "simperium-not-authorized";

    private static com.simperium.Simperium mSimperium;
    private static Bucket<Note> mNotesBucket;
    private static Bucket<BucketObject> mMetaBucket;

    public static com.simperium.Simperium getSimperium() {
        return mSimperium;
    }

    public static void setSimperium(com.simperium.Simperium simperium) {
        mSimperium = simperium;
    }

    public static Bucket<Note> getNotesBucket() {
        return mNotesBucket;
    }

    public static void setNotesBucket(Bucket<Note> notesBucket) {
        mNotesBucket = notesBucket;
    }

    public static Bucket<BucketObject> getMetaBucket() {
        return mMetaBucket;
    }

    public static void setMetaBucket(Bucket<BucketObject> metaBucket) {
        mMetaBucket = metaBucket;
    }

    public static Simperium configureSimperium(final Context context, String token) {

        Simperium simperium = Simperium.newClient(BuildConfig.SIMPERIUM_APP_NAME,
                BuildConfig.SIMPERIUM_APP_SECRET, context);


        try {
            final Bucket<Note> notesBucket = simperium.bucket(new Note.Schema());
            final Bucket<BucketObject> metaBucket = simperium.bucket("meta");

            setSimperium(simperium);
            setNotesBucket(notesBucket);
            setMetaBucket(metaBucket);

            simperium.setUserStatusChangeListener( new User.StatusChangeListener() {

                @Override
                public void onUserStatusChange(User.Status status) {
                    switch (status) {
                        case AUTHORIZED:
                            notesBucket.start();
                            metaBucket.start();
                            Intent simperiumSignedInIntent = new Intent();
                            simperiumSignedInIntent.setAction(BROADCAST_ACTION_SIMPERIUM_SIGNED_IN);
                            context.sendBroadcast(simperiumSignedInIntent);
                            break;
                        case NOT_AUTHORIZED:
                            notesBucket.stop();
                            metaBucket.stop();
                            Intent simperiumNotAuthorizedIntent = new Intent();
                            simperiumNotAuthorizedIntent.setAction(BROADCAST_ACTION_SIMPERIUM_NOT_AUTHORIZED);
                            context.sendBroadcast(simperiumNotAuthorizedIntent);
                            break;
                        default:
                            AppLog.d(AppLog.T.SIMPERIUM, "User not authorized yet");
                            break;
                    }
                }

            });

        } catch (BucketNameInvalid e) {
            AppLog.e(AppLog.T.SIMPERIUM, e.getMessage());
        }

        authorizeUser(simperium, token);

        return simperium;
    }

    public static void authorizeUser(Simperium simperium, String token){

        if (TextUtils.isEmpty(token)) return;

        User user = simperium.getUser();

        String tokenFormat = "WPCC/%s/%s";
        String wpccToken = String.format(tokenFormat, BuildConfig.SIMPERIUM_APP_SECRET, token);

        user.setAccessToken(wpccToken);

        // we'll assume the user is AUTHORIZED, and catch NOT_AUTHORIZED if something goes wrong.
        user.setStatus(User.Status.AUTHORIZED);
    }

    public static void resetBuckets() {
        if (mNotesBucket != null) {
            mNotesBucket.reset();
        }
        if (mMetaBucket != null) {
            mMetaBucket.reset();
        }
    }
}