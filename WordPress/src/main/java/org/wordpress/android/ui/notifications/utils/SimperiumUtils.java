/**
 * Simperium integration with WordPress.com
 * Currently used with Notifications
 */
package org.wordpress.android.ui.notifications.utils;

import android.content.Context;

import com.simperium.Simperium;
import com.simperium.client.Bucket;
import com.simperium.client.BucketNameInvalid;
import com.simperium.client.BucketObject;
import com.simperium.client.BucketObjectMissingException;
import com.simperium.client.Query;
import com.simperium.client.User;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.notifications.NotificationEvents;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

import de.greenrobot.event.EventBus;

public class SimperiumUtils {
    private static final String NOTE_TIMESTAMP = "timestamp";
    private static final String META_BUCKET_NAME = "meta";
    private static final String META_LAST_SEEN = "last_seen";

    private static Simperium mSimperium;
    private static Bucket<Note> mNotesBucket;
    private static Bucket<BucketObject> mMetaBucket;

    public static Bucket<Note> getNotesBucket() {
        return mNotesBucket;
    }

    public static Bucket<BucketObject> getMetaBucket() {
        return mMetaBucket;
    }

    public static synchronized Simperium configureSimperium(final Context context, String token) {
        // Create a new instance of Simperium if it doesn't exist yet.
        // In any case, authorize the user.
        if (mSimperium == null) {
            mSimperium = Simperium.newClient(BuildConfig.SIMPERIUM_APP_NAME,
                    BuildConfig.SIMPERIUM_APP_SECRET, context);

            try {
                mNotesBucket = mSimperium.bucket(new Note.Schema());
                mMetaBucket = mSimperium.bucket(META_BUCKET_NAME);

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
                                EventBus.getDefault().post(new NotificationEvents.SimperiumNotAuthorized());
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
            mNotesBucket = null;
        }
        if (mMetaBucket != null) {
            mMetaBucket.reset();
            mMetaBucket = null;
        }

        // Reset user status
        if (mSimperium != null) {
            mSimperium.getUser().setStatus(User.Status.UNKNOWN);
            mSimperium = null;
        }
    }

    // Returns true if we have unread notes with a timestamp greater than last_seen timestamp in the meta bucket
    public static boolean hasUnreadNotes() {
        if (getNotesBucket() == null || getMetaBucket() == null) return false;

        try {
            BucketObject meta = getMetaBucket().get(META_BUCKET_NAME);
            if (meta != null && meta.getProperty(META_LAST_SEEN) instanceof Integer) {
                Integer lastSeenTimestamp = (Integer)meta.getProperty(META_LAST_SEEN);

                Query<Note> query = new Query<>(getNotesBucket());
                query.where(Note.Schema.UNREAD_INDEX, Query.ComparisonType.EQUAL_TO, true);
                query.where(Note.Schema.TIMESTAMP_INDEX, Query.ComparisonType.GREATER_THAN, lastSeenTimestamp);
                return query.execute().getCount() > 0;
            }
        } catch (BucketObjectMissingException e) {
            return false;
        }

        return false;
    }

    // Updates the 'last_seen' field in the meta bucket with the latest note's timestamp
    public static boolean updateLastSeenTime() {
        if (getNotesBucket() == null || getMetaBucket() == null) return false;

        Query<Note> query = new Query<>(getNotesBucket());
        query.order(NOTE_TIMESTAMP, Query.SortType.DESCENDING);
        query.limit(1);

        Bucket.ObjectCursor<Note> cursor = query.execute();
        if (cursor.moveToFirst()) {
            long latestNoteTimestamp = cursor.getObject().getTimestamp();
            try {
                BucketObject meta = getMetaBucket().get(META_BUCKET_NAME);
                if (meta.getProperty(META_LAST_SEEN) instanceof Integer) {
                    int lastSeen = (int)meta.getProperty(META_LAST_SEEN);
                    if (lastSeen != latestNoteTimestamp) {
                        meta.setProperty(META_LAST_SEEN, latestNoteTimestamp);
                        meta.save();
                        return true;
                    }
                }
            } catch (BucketObjectMissingException e) {
                AppLog.e(AppLog.T.NOTIFS, "Meta bucket not found.");
            }
        }

        return false;
    }
}
