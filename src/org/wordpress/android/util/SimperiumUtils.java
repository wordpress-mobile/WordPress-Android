package org.wordpress.android.util;

import android.content.Context;
import android.content.Intent;

import com.simperium.Simperium;
import com.simperium.client.Bucket;
import com.simperium.client.BucketNameInvalid;
import com.simperium.client.BucketObject;
import com.simperium.client.User;

import org.wordpress.android.Config;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Note;

public class SimperiumUtils {

    private static String TOKEN_FORMAT="WPCC/%s/%s";

    public static Simperium configureSimperium(final Context context, String token) {

        Simperium simperium = Simperium.newClient(Config.SIMPERIUM_APP_NAME,
            Config.SIMPERIUM_APP_SECRET, context);


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
                            Intent broadcastIntent = new Intent();
                            broadcastIntent.setAction(WordPress.BROADCAST_ACTION_SIMPERIUM_SIGNED_IN);
                            context.sendBroadcast(broadcastIntent);
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

        if (token == null) return;

        User user = simperium.getUser();

        String wpccToken = String.format(TOKEN_FORMAT, Config.SIMPERIUM_APP_SECRET, token);

        user.setAccessToken(wpccToken);

        user.setStatus(User.Status.AUTHORIZED);

    }

}
