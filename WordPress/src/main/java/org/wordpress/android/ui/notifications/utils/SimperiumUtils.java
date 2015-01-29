/**
 * Simperium integration with WordPress.com
 * Currently used with Notifications
 */
package org.wordpress.android.ui.notifications.utils;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;

import com.simperium.Simperium;
import com.simperium.client.Bucket;
import com.simperium.client.BucketNameInvalid;
import com.simperium.client.BucketObject;
import com.simperium.client.BucketObjectMissingException;
import com.simperium.client.User;

import org.json.JSONObject;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.models.Note;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

public class SimperiumUtils {
    public static final String BROADCAST_ACTION_SIMPERIUM_NOT_AUTHORIZED = "simperium-not-authorized";

    private static Simperium mSimperium;
    private static Bucket<Note> mNotesBucket;
    private static Bucket<BucketObject> mMetaBucket;

    public static Bucket<Note> getNotesBucket() {
        return mNotesBucket;
    }

    public static Bucket<BucketObject> getMetaBucket() {
        return mMetaBucket;
    }

    private static NoteLoadedCallback mNoteLoadedCallback;

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
                                startBuckets();
                                break;
                            case NOT_AUTHORIZED:
                                mNotesBucket.stop();
                                mMetaBucket.stop();
                                Intent simperiumNotAuthorizedIntent = new Intent();
                                simperiumNotAuthorizedIntent.setAction(BROADCAST_ACTION_SIMPERIUM_NOT_AUTHORIZED);
                                LocalBroadcastManager.getInstance(context).sendBroadcast(simperiumNotAuthorizedIntent);
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
        }

        authorizeUser(mSimperium, token);

        return mSimperium;
    }

    private static void authorizeUser(Simperium simperium, String token) {
        User user = simperium.getUser();

        String tokenFormat = "WPCC/%s/%s";
        String wpccToken = String.format(tokenFormat, BuildConfig.SIMPERIUM_APP_SECRET, StringUtils.notNullStr(token));

        user.setAccessToken(wpccToken);

        // we'll assume the user is AUTHORIZED, and catch NOT_AUTHORIZED if something goes wrong.
        user.setStatus(User.Status.AUTHORIZED);
    }

    public static boolean isUserAuthorized() {
        return mSimperium != null &&
                mSimperium.getUser() != null &&
                mSimperium.getUser().getStatus() == User.Status.AUTHORIZED;
    }

    public static boolean isUserNotAuthorized() {
        return mSimperium != null &&
                mSimperium.getUser() != null &&
                mSimperium.getUser().getStatus() == User.Status.NOT_AUTHORIZED;
    }

    public static void startBuckets() {
        if (mNotesBucket != null) {
            mNotesBucket.start();
        }

        if (mMetaBucket != null) {
            mMetaBucket.start();
        }
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

    public static void getNoteAsync(String noteId, NoteLoadedCallback callback) {
        if (noteId == null || callback == null) return;

        mNoteLoadedCallback = callback;

        new LoadNoteTask().execute(noteId);
    }

    private static class LoadNoteTask extends AsyncTask<String, Void, Note> {
        @Override
        protected Note doInBackground(String[] params) {
            if (params.length < 1 || params[0] == null) return null;

            String noteId = params[0];

            if (getNotesBucket() != null) {
                try {
                    return SimperiumUtils.getNotesBucket().get(noteId);
                } catch (BucketObjectMissingException e) {
                    return null;
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Note note) {
            if (mNoteLoadedCallback == null) return;

            if (note != null) {
                mNoteLoadedCallback.onNoteLoaded(note);
            } else {
                mNoteLoadedCallback.onError();
            }
        }
    }

    public interface NoteLoadedCallback {
        void onNoteLoaded(Note note);

        void onError();
    }
}