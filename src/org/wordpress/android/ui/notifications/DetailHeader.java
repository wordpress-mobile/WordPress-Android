/**
 * Set a line of text and a URL to open in the browser when clicked
 */
package org.wordpress.android.ui.notifications;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.Note;

public class DetailHeader extends LinearLayout {

    public DetailHeader(Context context){
        super(context);
    }
    public DetailHeader(Context context, AttributeSet attributes){
        super(context, attributes);
    }
    public DetailHeader(Context context, AttributeSet attributes, int defStyle){
        super(context, attributes, defStyle);
    }
    public TextView getTextView(){
        return (TextView) findViewById(R.id.label);
    }
    public void setText(CharSequence text){
        getTextView().setText(text);
    }

    public void setNote(final Note note, final String url) {
        final ImageView indicator = (ImageView) findViewById(R.id.indicator);

        if (note == null) {
            indicator.setVisibility(View.GONE);
            setClickable(false);
            setOnClickListener(null);
            return;
        }

        boolean isComment = (note.getBlogId() != 0 && note.getPostId() != 0 && note.getCommentId() != 0);
        boolean isPost = (note.getBlogId() != 0 && note.getPostId() != 0 && note.getCommentId() == 0);

        if (isPost || isComment) {
            indicator.setVisibility(View.VISIBLE);
            indicator.setImageResource(R.drawable.ic_navigate_next);
            setClickable(true);
            setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showPost(note.getBlogId(), note.getPostId());
                }
            });
        } else if (!TextUtils.isEmpty(url)) {
            indicator.setVisibility(View.VISIBLE);
            indicator.setImageResource(R.drawable.dashboard_icon_wp);
            setClickable(true);
            setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showUrl(url);
                }
            });
        } else {
            indicator.setVisibility(View.GONE);
            setClickable(false);
            setOnClickListener(null);
        }
    }

    private void showUrl(String url) {
        Context context = getContext();
        Intent intent = new Intent(context, NotificationsWebViewActivity.class);
        intent.putExtra(NotificationsWebViewActivity.URL_TO_LOAD, url);
        context.startActivity(intent);
    }

    private void showPost(int blogId, int postId) {
        // TODO
    }
}