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

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageContainer;

import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.posts.PostsActivity;
import org.wordpress.android.util.EscapeUtils;
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
        mFollowRow.getImageView().setImageUrl(getNote().getIconURL(), WordPress.imageLoader);
        SpannableStringBuilder html = (SpannableStringBuilder) getNote().getCommentBody();
        Html.ImageGetter imgGetter = new AsyncImageGetter(mCommentText);
        ImageSpan imgs[] = html.getSpans(0, html.length(), ImageSpan.class);
        for(ImageSpan img : imgs){
            // create a new image span using the image getter
            ImageSpan remote = new ImageSpan(imgGetter.getDrawable(img.getSource()), img.getSource());
            // now replace
            html.setSpan(remote, html.getSpanStart(img), html.getSpanEnd(img), html.getSpanFlags(img));
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
        mFollowRow.setDefaultText(EscapeUtils.unescapeHtml(getNote().queryJSON("body.items[-1].header_text", "")));
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
    
    class ReplyResponseHandler implements RestRequest.Listener, RestRequest.ErrorListener {
        private Toast mToast;
        private Note.Reply mReply;
        private ReplyRow mRow;
        private NotificationManager mNotificationManager;
        private Notification mFailureNotification;
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
                .setContentTitle("Reply failed")
                .setContentText("Tap to try again")
                .setTicker("Reply failed")
                .setWhen(0)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentIntent(PendingIntent.getActivity(getActivity(), 0x0, failureIntent, 0x0))
                .build();
            // Toast for notifying the user that comment was published successfully
            mToast = Toast.makeText(getActivity(), R.string.note_reply_successful, Toast.LENGTH_SHORT);
        }
        @Override
        public void onResponse(JSONObject response){
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
            mRow.setComplete(true);
            mNotificationManager.notify("reply", 0xFA, mFailureNotification);
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
            Drawable loading = getResources().getDrawable(R.drawable.app_icon);
            final RemoteDrawable remote = new RemoteDrawable(loading);
            // Kick off the async task of downloading the image
            WordPress.imageLoader.get(source, new ImageLoader.ImageListener(){
               @Override
               public void onErrorResponse(VolleyError error){
                   // TODO: display error image resource
               }
               @Override
               public void onResponse(ImageContainer response, boolean isImmediate){
                   if (response.getBitmap() != null) {
                       Drawable drawable = new BitmapDrawable(getResources(), response.getBitmap());
                       int oldHeight = remote.getBounds().height();
                       remote.remote = drawable;
                       remote.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
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
}