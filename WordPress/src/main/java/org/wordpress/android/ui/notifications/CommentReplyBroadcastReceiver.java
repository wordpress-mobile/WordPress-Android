package org.wordpress.android.ui.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;
import android.text.TextUtils;

import org.wordpress.android.GCMIntentService;
import org.wordpress.android.ui.posts.PostsActivity;

/**
 * Processes a comment reply notification action
 * Responds to Android Wear replies or continues along to PostsActivity
 */
public class CommentReplyBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null && extras.getBoolean(NotificationsActivity.FROM_NOTIFICATION_EXTRA)) {
            // Check for an Android Wear voice reply
            String noteId = extras.getString(NotificationsActivity.NOTE_ID_EXTRA);
            Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);

            if (remoteInput != null && noteId != null) {
                CharSequence voiceReplyText = remoteInput.getCharSequence(GCMIntentService.EXTRA_COMMENT_VOICE_REPLY);
                if (!TextUtils.isEmpty(voiceReplyText)) {
                    NotificationUtils.sendCommentReply(context, voiceReplyText, noteId);
                    return;
                }
            }
        }

        // If we aren't processing a voice reply, just pass the extras along to PostsActivity
        Intent i = new Intent(context, PostsActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (extras != null) {
            i.putExtras(extras);
        }

        context.startActivity(i);
    }
}
