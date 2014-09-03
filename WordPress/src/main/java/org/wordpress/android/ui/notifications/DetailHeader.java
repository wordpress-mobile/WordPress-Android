/**
 * Set a line of text and a URL to open in the browser when clicked
 */
package org.wordpress.android.ui.notifications;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.Note;

public class DetailHeader extends LinearLayout {
    private NotificationFragment.OnPostClickListener mOnPostClickListener;
    private NotificationFragment.OnCommentClickListener mOnCommentClickListener;

    public DetailHeader(Context context){
        super(context);
    }
    public DetailHeader(Context context, AttributeSet attributes){
        super(context, attributes);
    }
    public DetailHeader(Context context, AttributeSet attributes, int defStyle){
        super(context, attributes, defStyle);
    }
    TextView getTextView(){
        return (TextView) findViewById(R.id.label);
    }
    public void setText(CharSequence text){
        getTextView().setText(text);
    }

    /*
     * set by the owning fragment, calls listener in NotificationsActivity to
     * display the post/comment associated with this notification (if any)
     */
    public void setOnPostClickListener(NotificationFragment.OnPostClickListener listener) {
        mOnPostClickListener = listener;
    }
    public void setOnCommentClickListener(NotificationFragment.OnCommentClickListener listener) {
        mOnCommentClickListener = listener;
    }

    /*
     * owning fragment calls this to pass it the note so the post or comment associated with
     * the note can be opened. if there is no associated post or comment, then the passed
     * url is navigated to instead.
     */
    public void setNote(final Note note, final String url) {
        final boolean isComment = (note != null && note.getBlogId() != 0 && note.getPostId() != 0 && note.getCommentId() != 0);
        final boolean isPost = (note != null && note.getBlogId() != 0 && note.getPostId() != 0 && note.getCommentId() == 0);

        if (isPost || isComment) {
            setClickable(true);
            setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isComment && mOnCommentClickListener != null) {
                        mOnCommentClickListener.onCommentClicked(note, note.getBlogId(), note.getCommentId());
                    } else if (isPost && mOnPostClickListener != null) {
                        mOnPostClickListener.onPostClicked(note, note.getBlogId(), note.getPostId());
                    }
                }
            });
        } else if (!TextUtils.isEmpty(url)) {
            setClickable(true);
            setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    NotificationsWebViewActivity.openUrl(getContext(), url);
                }
            });
        } else {
            setClickable(false);
            setOnClickListener(null);
        }
    }
}