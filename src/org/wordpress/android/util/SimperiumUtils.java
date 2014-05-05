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
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Note;

public class SimperiumUtils {

    private static String TOKEN_FORMAT="WPCC/%s/%s";

    public static Simperium configureSimperium(final Context context, String token) {

        Simperium simperium = Simperium.newClient(BuildConfig.SIMPERIUM_APP_NAME,
                BuildConfig.SIMPERIUM_APP_SECRET, context);


        try {
            final Bucket<Note> notesBucket = simperium.bucket(new Note.Schema());
            final Bucket<BucketObject> metaBucket = simperium.bucket("meta");

            WordPress.simperium = simperium;
            WordPress.notesBucket = notesBucket;
            WordPress.metaBucket = metaBucket;

            simperium.setUserStatusChangeListener( new User.StatusChangeListener() {

                @Override
                public void onUserStatusChange(User.Status status) {
                    switch (status) {
                        case AUTHORIZED:
                            notesBucket.start();
                            metaBucket.start();
                            Intent simperiumSignedInIntent = new Intent();
                            simperiumSignedInIntent.setAction(WordPress.BROADCAST_ACTION_SIMPERIUM_SIGNED_IN);
                            context.sendBroadcast(simperiumSignedInIntent);
                            break;
                        case NOT_AUTHORIZED:
                            notesBucket.stop();
                            metaBucket.stop();
                            Intent simperiumNotAuthorizedIntent = new Intent();
                            simperiumNotAuthorizedIntent.setAction(WordPress.BROADCAST_ACTION_SIMPERIUM_NOT_AUTHORIZED);
                            context.sendBroadcast(simperiumNotAuthorizedIntent);
                            break;
                        default:
                            android.util.Log.d("WordPress", "User not authorized yet");
                            break;
                    }
                }

            });

        } catch (BucketNameInvalid e) {
            throw new RuntimeException("Failed to configure simperium", e);
        }

        authorizeUser(simperium, token);

        return simperium;
    }

    public static void authorizeUser(Simperium simperium, String token){

        if (TextUtils.isEmpty(token)) return;

        User user = simperium.getUser();

        String wpccToken = String.format(TOKEN_FORMAT, BuildConfig.SIMPERIUM_APP_SECRET, token);

        user.setAccessToken(wpccToken);

        // we'll assume the user is AUTHORIZED, and catch NOT_AUTHORIZED if something goes wrong.
        user.setStatus(User.Status.AUTHORIZED);
    }

}
