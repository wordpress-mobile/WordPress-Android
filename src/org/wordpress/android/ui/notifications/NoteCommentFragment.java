package org.wordpress.android.ui.notifications;

import android.content.Intent;
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
import android.graphics.Rect;
import android.util.Log;
import android.net.Uri;

import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;

import org.wordpress.android.R;
import org.wordpress.android.models.Note;
import org.wordpress.android.WordPress;

import org.json.JSONObject;

class NoteCommentFragment extends Fragment implements NotificationFragment {
    private static final String TAG="NoteComment";
    private static final String NOTE_ACTION_REPLY="replyto-comment";
    private static final String REPLY_CONTENT_PARAM_KEY="content";
    private static String LOREM="Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.\n\n";
    private TextView mCommentText;
    private ReplyField mReplyField;
    private Note mNote;
    private FollowRow mFollowRow;
    private DetailHeader mDetailHeader;
    private AsyncHttpClient httpClient = new AsyncHttpClient();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state){
        View view = inflater.inflate(R.layout.notifications_comment, parent, false);
        mReplyField = (ReplyField) view.findViewById(R.id.replyField);
        mCommentText = (TextView) view.findViewById(R.id.note_text);
        mFollowRow = (FollowRow) view.findViewById(R.id.follow_row);
        mDetailHeader = (DetailHeader) view.findViewById(R.id.header);
        return view;
    }

    @Override
    public void onStart(){
        super.onStart();
        httpClient.get(getNote().getIconURL(), new BitmapResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Bitmap bitmap){
                Log.d(TAG, String.format("Set the image bitmap on %s", mFollowRow));
                mFollowRow.getImageView().setImageBitmap(bitmap);
            }
        });
        mFollowRow.setText(getNote().queryJSON("body.items[-1].header_text", ""));
        // TODO: convert all <img> to links except for wp-smiley images which should be just text equivalents
        mCommentText.setText(Html.fromHtml(getNote().getCommentText(), new AsyncImageGetter(mCommentText), null));
        mReplyField.setOnReplyListener(new ReplyListener());
        mDetailHeader.setText(getNote().getSubject());
        final String url = getNote().queryJSON("body.items[last].header_link", "");
        mDetailHeader.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        });
        JSONObject followAction = getNote().queryJSON("body.items[last].action", new JSONObject());
        mFollowRow.setAction(followAction);
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
                .build();
                    
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
    
    private class AsyncImageGetter implements Html.ImageGetter {
        private TextView mView;
        private AsyncHttpClient httpClient = new AsyncHttpClient();
        public AsyncImageGetter(TextView view){
            mView = view;
        }
        @Override
        public Drawable getDrawable(final String source){
            Log.d(TAG, String.format("Requesting image from: %s", source));
            Drawable loading = getResources().getDrawable(R.drawable.app_icon);
            final RemoteDrawable remote = new RemoteDrawable(loading);
            // Kick off the async task of downloading the image
            httpClient.get(source, new BitmapResponseHandler(){
                @Override
                public void onSuccess(int statusCode, Bitmap bitmap){
                    Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                    int oldHeight = remote.getBounds().height();
                    remote.remote = drawable;
                    remote.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                    int newHeight = remote.getBounds().height();
                    mView.invalidate();
                    // For ICS
                    Log.d(TAG, String.format("Height is changing by %d", newHeight - oldHeight));
                    mView.setHeight(mView.getHeight() + newHeight - oldHeight);
                    
                    // Pre ICS
                    mView.setEllipsize(null);
                }
            });
            return remote;
        }
        
    }
    
    private class RemoteDrawable extends BitmapDrawable {
        protected Drawable remote;
        protected Drawable loading;
        public RemoteDrawable(Drawable loadingDrawable){
            loading = loadingDrawable;
            setBounds(0, 0, loading.getIntrinsicWidth(), loading.getIntrinsicHeight());
        }
        public void setBounds(int x, int y, int width, int height){
            super.setBounds(x, y, width, height);
            if (remote != null) {
                remote.setBounds(x, y, width, height);
                return;
            }
            if (loading != null) {
                loading.setBounds(x, y, width, height);
            }
        }
        public void draw(Canvas canvas){
            if (remote != null) {
                remote.draw(canvas);
            } else {
                loading.draw(canvas);
            }
        }
        
    }
    
    private abstract class BitmapResponseHandler extends BinaryHttpResponseHandler {
        public BitmapResponseHandler(){
            super(new String[]{ "image/jpeg", "image/gif", "image/png"});
        };
        abstract void onSuccess(int statusCode, Bitmap bitmap);
        protected void handleSuccessMessage(int statusCode, Bitmap bitmap){
            onSuccess(statusCode, bitmap);
        }
        @Override
        protected void sendSuccessMessage(int statusCode, byte[] responseBody) {
            // turn this beast into a bitmap
            Log.d(TAG, String.format("Decode the byte array, %d", responseBody.length));
            Bitmap bitmap = BitmapFactory.decodeByteArray(responseBody, 0, responseBody.length);
            if (bitmap == null) {
                super.sendSuccessMessage(statusCode, responseBody);
            } else {
                sendMessage(obtainMessage(SUCCESS_MESSAGE, new Object[]{statusCode, bitmap}));
            }
            // sendMessage(obtainMessage(SUCCESS_MESSAGE, new Object[]{statusCode, responseBody}));
        }
        // Methods which emulate android's Handler and Message methods
        @Override
        protected void handleMessage(Message msg) {
            Object[] response;
            switch(msg.what) {
                case SUCCESS_MESSAGE:
                    response = (Object[])msg.obj;
                    handleSuccessMessage(((Integer) response[0]).intValue() , (Bitmap) response[1]);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }
    

}