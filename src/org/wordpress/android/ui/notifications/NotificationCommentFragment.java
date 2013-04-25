package org.wordpress.android.ui.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.text.Html;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.RemoteViews;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;

import org.wordpress.android.R;
import org.wordpress.android.models.Note;
import org.wordpress.android.WordPress;

import org.json.JSONObject;

class NotificationsCommentFragment extends Fragment implements NotificationFragment {
    private static final String TAG="NoteComment";
    private static final String NOTE_ACTION_REPLY="replyto-comment";
    private static final String REPLY_CONTENT_PARAM_KEY="content";
    private static String LOREM="Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.\n\n";
    private TextView mCommentText;
    private ReplyField mReplyField;
    private Note mNote;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state){
        View view = inflater.inflate(R.layout.notifications_comment, parent, false);
        mReplyField = (ReplyField) view.findViewById(R.id.replyField);
        mCommentText = (TextView) view.findViewById(R.id.note_text);
        return view;
    }

    @Override
    public void onStart(){
        super.onStart();
        // TODO: convert all <img> to links except for wp-smiley images which should be just text equivalents
        mCommentText.setText(Html.fromHtml(getNote().getCommentText()));
        mReplyField.setOnReplyListener(new ReplyListener());
    }
    
    class ReplyListener implements ReplyField.OnReplyListener {
        @Override
        public void onReply(ReplyField field, Editable replyText){
            JSONObject replyAction = getNote().getActions().get(NOTE_ACTION_REPLY);
            Integer siteId = Note.queryJSON(replyAction, "params.blog_id", (Integer) 0);
            String commentId = Note.queryJSON(replyAction, "params.comment_id", "");
            WordPress.restClient.replyToComment(siteId.toString(), commentId.toString(), replyText.toString(), new ReplyResponseHandler());
        }
    }
    
    class ReplyResponseHandler extends JsonHttpResponseHandler {
        protected Notification mNotification;
        ReplyResponseHandler(){
            super();
            Intent intent = new Intent(getActivity(), NotificationsActivity.class);
            intent.putExtra(NotificationsActivity.NOTE_ID_EXTRA, getNote().getId());
            RemoteViews content = new RemoteViews(getActivity().getPackageName(), R.layout.notification_replying);
            // content.setProgressBar(R.id.notification_progress, 0, 0, true);
            mNotification = new NotificationCompat.Builder(getActivity())
                .setContentTitle("Replying")
                .setContentText("Publishing your reply")
                .setWhen(0)
                .setTicker("Publishing your reply")
                .setSmallIcon(R.drawable.notification_icon)
                .setOngoing(true)
                .setContent(content)
                .setContentIntent(PendingIntent.getActivity(getActivity(), 0x0, intent, 0x0))
                .getNotification();
            // mNotification.contentView = ;
                    
        }
        @Override
        public void onStart(){
            NotificationManager nm = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify("reply", 0xFF, mNotification);
        }
        @Override
        public void onSuccess(int statusCode, JSONObject response){
            Log.d(TAG, String.format("Comment successful! %s", response));
        }
        @Override
        public void onFailure(Throwable e, JSONObject response){
            Log.e(TAG, String.format("Failed to reply: %s", response), e);
        }
        @Override
        public void onFailure(Throwable e, String response){
            Log.e(TAG, String.format("Failed to reply: %s", response), e);
        }
        @Override
        public void onFinish(){
            NotificationManager nm = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            // nm.cancel("reply", 0xFF);
        }
        
    }
    
    public void setNote(Note note){
        mNote = note;
    }
    
    public Note getNote(){
        return mNote;
    }

}