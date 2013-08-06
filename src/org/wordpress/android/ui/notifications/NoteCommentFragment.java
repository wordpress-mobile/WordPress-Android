package org.wordpress.android.ui.notifications;

import java.util.Map;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageContainer;

import com.wordpress.rest.RestRequest;

import org.json.JSONObject;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.posts.PostsActivity;
import org.wordpress.android.util.JSONUtil;

public class NoteCommentFragment extends Fragment implements NotificationFragment {
    private static final String TAG="NoteComment";
    private TextView mCommentText, mModeratingText;
    private ReplyField mReplyField;
    private Note mNote;
    private FollowRow mFollowRow;
    private DetailHeader mDetailHeader;
    private ReplyList mReplyList;
    private ScrollView mScrollView;
    private ImageButton mApproveButton, mSpamButton, mTrashButton;
    private LinearLayout mModerateContainer, mModerateSection;
    
    private static final String APPROVE_TAG = "approve-comment";
    private static final String UNAPPROVE_TAG = "unapprove-comment";
    private static final String SPAM_TAG = "spam-comment";
    private static final String TRASH_TAG = "trash-comment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state){
        View view = inflater.inflate(R.layout.notifications_comment, parent, false);
        mReplyField = (ReplyField) view.findViewById(R.id.replyField);
        mCommentText = (TextView) view.findViewById(R.id.note_text);
        mFollowRow = (FollowRow) view.findViewById(R.id.follow_row);
        mDetailHeader = (DetailHeader) view.findViewById(R.id.header);
        mReplyList = (ReplyList) view.findViewById(R.id.replies);
        mScrollView = (ScrollView) view.findViewById(R.id.scroll_view);
        mApproveButton = (ImageButton)view.findViewById(R.id.note_moderate_approve);
        mSpamButton = (ImageButton)view.findViewById(R.id.note_moderate_spam);
        mTrashButton = (ImageButton)view.findViewById(R.id.note_moderate_trash);
        mModeratingText = (TextView)view.findViewById(R.id.comment_moderating);
        mModerateContainer = (LinearLayout)view.findViewById(R.id.moderate_buttons_container);
        mModerateSection = (LinearLayout)view.findViewById(R.id.moderate_section);
        
        ((TextView) view.findViewById(R.id.moderate_comment_header)).setText(getResources().getString(R.string.moderate_comment).toUpperCase());
        
        return view;
    }

    @Override
    public void onStart(){
        super.onStart();

        if (getNote() == null && getActivity() != null) {
            ((NotificationsActivity)getActivity()).popNoteDetail();
            return;
        }

        mFollowRow.getImageView().setImageUrl(getNote().getIconURL(), WordPress.imageLoader);
        SpannableStringBuilder html = (SpannableStringBuilder) getNote().getCommentBody();
        final Html.ImageGetter imgGetter = new AsyncImageGetter(mCommentText);
        ImageSpan imgs[] = html.getSpans(0, html.length(), ImageSpan.class);
        for(ImageSpan img : imgs){
            // create a new image span using the image getter
            final String src = img.getSource();
            final RemoteDrawable remoteDrawable = (RemoteDrawable) imgGetter.getDrawable(src);
            ClickableSpan clickListener = new ClickableSpan(){
                @Override
                public void onClick(View v){
                    if (remoteDrawable.didFail()) {
                        imgGetter.getDrawable(src);
                    }
                }
            };
            ImageSpan remote = new ImageSpan(remoteDrawable, img.getSource());
            // now replace
            int spanStart = html.getSpanStart(img);
            int spanEnd = html.getSpanEnd(img);
            int spanFlags = html.getSpanFlags(img);
            html.setSpan(remote, spanStart, spanEnd, spanFlags);
            html.setSpan(clickListener, spanStart, spanEnd, spanFlags);
            html.removeSpan(img);
        }
        mCommentText.setText(html);
        mCommentText.setMovementMethod(LinkMovementMethod.getInstance());
        mReplyField.setOnReplyListener(mReplyListener);
        mDetailHeader.setText(getNote().getSubject());
        
        Map<String, JSONObject> noteActions = getNote().getActions();
        boolean hasModerateAction = false;
        if (noteActions.containsKey(APPROVE_TAG)) {
            hasModerateAction = true;
            mApproveButton.setImageResource(R.drawable.moderate_approve);
            mApproveButton.setVisibility(View.VISIBLE);
            mApproveButton.setOnClickListener(mModerateClickListener);
            mApproveButton.setTag(APPROVE_TAG);
        }
        if (noteActions.containsKey(UNAPPROVE_TAG)) {
            hasModerateAction = true;
            mApproveButton.setImageResource(R.drawable.moderate_unapprove);
            mApproveButton.setVisibility(View.VISIBLE);
            mApproveButton.setOnClickListener(mModerateClickListener);
            mApproveButton.setTag(UNAPPROVE_TAG);
        }
        if (noteActions.containsKey(SPAM_TAG)) {
            hasModerateAction = true;
            mSpamButton.setVisibility(View.VISIBLE);
            mSpamButton.setOnClickListener(mModerateClickListener);
            mSpamButton.setTag(SPAM_TAG);
        } else {
            mSpamButton.setVisibility(View.GONE);
        }
        if (noteActions.containsKey(TRASH_TAG)) {
            hasModerateAction = true;
            mTrashButton.setVisibility(View.VISIBLE);
            mTrashButton.setOnClickListener(mModerateClickListener);
            mTrashButton.setTag(TRASH_TAG);
        } else {
            mTrashButton.setVisibility(View.GONE);
        }
        
        if (!hasModerateAction)
            mModerateSection.setVisibility(View.GONE);
        
        String url = getNote().queryJSON("body.items[last].header_link", "");
        if (!url.equals("")) {
            mDetailHeader.setUrl(url);
        }
        JSONObject followAction = getNote().queryJSON("body.items[last].action", new JSONObject());
        mFollowRow.setDefaultText(Html.fromHtml(getNote().queryJSON("body.items[-1].header_text", "")));
        mFollowRow.setAction(followAction);
        mFollowRow.setListener(new FollowListener(getActivity().getApplicationContext()));
        Bundle arguments = getArguments();
        if (arguments != null && (arguments.containsKey(NotificationsActivity.NOTE_REPLY_EXTRA) || arguments.containsKey(NotificationsActivity.NOTE_INSTANT_REPLY_EXTRA))) {
            if (arguments.containsKey(NotificationsActivity.NOTE_REPLY_EXTRA))
                mReplyField.setText(arguments.getString(NotificationsActivity.NOTE_REPLY_EXTRA));
            
            mReplyField.mTextField.requestFocus();
            InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null)
                inputMethodManager.showSoftInput(mReplyField.mTextField, 0);
        }
        
    }
    
    @Override
    public void onPause(){
        super.onPause();
        dismissKeyboard();
    }
    
    protected void dismissKeyboard(){
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0x0);
    }
    
    private OnClickListener mModerateClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {

            String tag = (String) v.getTag();
            if (getNote().getActions().containsKey(tag)) {

                animateModeration(true);
                JSONObject moderateAction = getNote().getActions().get(tag);
                String siteId = String.valueOf(JSONUtil.queryJSON(moderateAction, "params.site_id",
                        -1));
                String commentId = String.valueOf(JSONUtil.queryJSON(moderateAction,
                        "params.comment_id", -1));
                String status = JSONUtil.queryJSON(moderateAction, "params.rest_body.status", "");
                
                if (getActivity() != null) {
                    ((NotificationsActivity)getActivity()).moderateComment(siteId, commentId, status, getNote());
                }
                
            }
        }
    };
    
    public void animateModeration(boolean start) {
        // show some fancy animations
        for (int i = 0; i < mModerateContainer.getChildCount(); i++) {
            View view = mModerateContainer.getChildAt(i);
            if (view instanceof ImageButton && view.getVisibility() == View.VISIBLE) {
                if (start)
                    view.setClickable(false);
                else
                    view.setClickable(true);
                Animation zoom = AnimationUtils.loadAnimation(getActivity()
                        .getBaseContext(), (start) ? R.anim.rotate_zoom_out : R.anim.rotate_zoom_in);
                zoom.setStartOffset(i * 100);
                view.startAnimation(zoom);
            }
        }

        Animation moderatingAnimation = AnimationUtils.loadAnimation(getActivity()
                .getBaseContext(), (start) ? R.anim.blink : R.anim.fade_out);
        mModeratingText.setVisibility((start) ? View.VISIBLE : View.GONE);
        mModeratingText.startAnimation(moderatingAnimation);
        
    }
    
    private ReplyField.OnReplyListener mReplyListener = new ReplyField.OnReplyListener() {
        @Override
        public void onReply(ReplyField field, Editable replyText){
            Note.Reply reply = getNote().buildReply(replyText.toString());
            replyText.clear();
            dismissKeyboard();
            ReplyRow row = mReplyList.addReply(reply);
            ReplyResponseHandler handler = new ReplyResponseHandler(reply, row);
            WordPress.restClient.replyToComment(reply, handler, handler);
            mScrollView.scrollTo(0, mReplyList.getBottom());
        }
    };
    
    class ReplyResponseHandler implements RestRequest.Listener, RestRequest.ErrorListener, View.OnClickListener {
        private Toast mToast;
        private Note.Reply mReply;
        private ReplyRow mRow;
        private NotificationManager mNotificationManager;
        private Notification mFailureNotification;
        private static final int NOTE_ID = 0x0;
        ReplyResponseHandler(Note.Reply reply, ReplyRow row){
            mReply = reply;
            mRow = row;
            mNotificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            // create intent for failure case
            Intent failureIntent = new Intent(getActivity(), PostsActivity.class);
            failureIntent.setAction(Intent.ACTION_EDIT);
            failureIntent.addFlags(NotificationsActivity.FLAG_FROM_NOTE);
            failureIntent.putExtra(NotificationsActivity.NOTE_ID_EXTRA, getNote().getId());
            failureIntent.putExtra(NotificationsActivity.NOTE_REPLY_EXTRA, mReply.getContent());
            failureIntent.putExtra(NotificationsActivity.FROM_NOTIFICATION_EXTRA, true);
            // TODO: Improve failure text. Who was it they tried to reply to and a better
            // reason why it failed. Need to make sure id's are unique.
            mFailureNotification = new NotificationCompat.Builder(getActivity())
                .setContentTitle(getString(R.string.reply_failed))
                .setContentText(getString(R.string.tap_retry))
                .setTicker(getString(R.string.reply_failed))
                .setWhen(0)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentIntent(PendingIntent.getActivity(getActivity(), NOTE_ID, failureIntent, NOTE_ID))
                .build();
            // Toast for notifying the user that comment was published successfully
            mToast = Toast.makeText(getActivity(), R.string.note_reply_successful, Toast.LENGTH_SHORT);
        }
        @Override
        public void onResponse(JSONObject response){
            // remove the notification if it's there
            mNotificationManager.cancel(NOTE_ID);
            if (getActivity() != null) {
                mReply.setCommentJson(response);
                mRow.setComplete(true);
                mRow.setUrl(mReply.getUrl());
                mRow.setText(String.format("\u201c%s\u201d", mReply.getCommentPreview()));
                mRow.getImageView().setImageUrl(mReply.getAvatarUrl(), WordPress.imageLoader);
            } else {
                mToast.show();
            }
        }

        @Override
        public void onErrorResponse(VolleyError error){
            if (error.networkResponse != null) {
                String body = new String(error.networkResponse.data);
                Log.e(TAG, body, error);
            }
            mRow.setFailed(true);
            mRow.setText(R.string.retry_reply);
            mRow.setOnClickListener(this);
            mNotificationManager.notify("reply", 0xFA, mFailureNotification);
        }

        @Override
        public void onClick(View v){
            mRow.setFailed(false);
            mRow.setComplete(false);
            WordPress.restClient.replyToComment(mReply, this, this);
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
        public AsyncImageGetter(TextView view){
            mView = view;
        }
        @Override
        public Drawable getDrawable(final String source){
            // TODO: cancel any requests when the view is destroyed
            if (getActivity() == null) {
                return null;
            }
            Drawable loading = getResources().getDrawable(R.drawable.remote_image);
            Drawable failed = getResources().getDrawable(R.drawable.remote_failed);
            final RemoteDrawable remote = new RemoteDrawable(loading, failed);
            // Kick off the async task of downloading the image
            WordPress.imageLoader.get(source, new ImageLoader.ImageListener(){
               @Override
               public void onErrorResponse(VolleyError error){
                   Log.e(TAG, "Failed to load image", error);
                   remote.displayFailed();
                   mView.invalidate();
               }
               @Override
               public void onResponse(ImageContainer response, boolean isImmediate){
                   if (response.getBitmap() != null) {
                       // view is gone? then stop
                       if (mView == null) {
                           return;
                       }
                       Drawable drawable = new BitmapDrawable(getResources(), response.getBitmap());
                       final int oldHeight = remote.getBounds().height();
                       int maxWidth = mView.getWidth() - mView.getPaddingLeft() - mView.getPaddingRight();
                       remote.setRemoteDrawable(drawable, maxWidth);
                       // TODO: resize image to fit visibliy within the TextView
                       // image is from cache? don't need to modify view height
                       if (isImmediate) {
                           return;
                       }
                       int newHeight = remote.getBounds().height();
                       mView.invalidate();
                       // For ICS
                       mView.setHeight(mView.getHeight() + newHeight - oldHeight);
                       // Pre ICS
                       mView.setEllipsize(null);
                   }
               }
            });
            return remote;
        }
        
    }

    private class RemoteDrawable extends BitmapDrawable {
        protected Drawable mRemoteDrawable;
        protected Drawable mLoadingDrawable;
        protected Drawable mFailedDrawable;
        private boolean mDidFail=false;
        public RemoteDrawable(Drawable loadingDrawable, Drawable failedDrawable){
            mLoadingDrawable = loadingDrawable;
            mFailedDrawable = failedDrawable;
            setBounds(0, 0, mLoadingDrawable.getIntrinsicWidth(), mLoadingDrawable.getIntrinsicHeight());
        }
        public void displayFailed(){
            mDidFail = true;
        }
        public void setBounds(int x, int y, int width, int height){
            super.setBounds(x, y, width, height);
            if (mRemoteDrawable != null) {
                mRemoteDrawable.setBounds(x, y, width, height);
                return;
            }
            if (mLoadingDrawable != null) {
                mLoadingDrawable.setBounds(x, y, width, height);
                mFailedDrawable.setBounds(x, y, width, height);
            }
        }
        public void setRemoteDrawable(Drawable remote){
            mRemoteDrawable = remote;
            setBounds(0, 0, mRemoteDrawable.getIntrinsicWidth(), mRemoteDrawable.getIntrinsicHeight());
        }
        public void setRemoteDrawable(Drawable remote, int maxWidth){
            // null sentinel for now
            if (remote == null) {
                // throw error
                return;
            }
            mRemoteDrawable = remote;
            // determine if we need to scale the image to fit in view
            int imgWidth = remote.getIntrinsicWidth();
            int imgHeight = remote.getIntrinsicHeight();
            float xScale = (float) imgWidth/(float) maxWidth;
            if (xScale > 1.0f) {
                setBounds(0, 0, Math.round(imgWidth/xScale), Math.round(imgHeight/xScale));
            } else {
                setBounds(0, 0, imgWidth, imgHeight);
            }
        }
        public boolean didFail(){
            return mDidFail;
        }
        public void draw(Canvas canvas){
            if (mRemoteDrawable != null) {
                mRemoteDrawable.draw(canvas);
            } else if (didFail()) {
                mFailedDrawable.draw(canvas);
            } else {                
                mLoadingDrawable.draw(canvas);
            }
        }
        
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }

}