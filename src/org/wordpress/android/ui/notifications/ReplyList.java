package org.wordpress.android.ui.notifications;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.os.Build;

import org.wordpress.android.R;
import org.wordpress.android.models.Note;

public class ReplyList extends LinearLayout {
    // Gingerbread has weird layout issues regarding clipChildren and clipToPadding so we're
    // disabling the animations for anything before Jelly Bean
    private static final boolean ANIMATE=Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    public ReplyList(Context context){
        super(context);
    }
    public ReplyList(Context context, AttributeSet attrs){
        super(context, attrs);
    }
    public ReplyList(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
    }
    protected void onFinishInflate(){
        if (ANIMATE) {
            setClipChildren(false);
            setClipToPadding(false);
        }
    }
    /**
     * Add a reply item
     */
    public ReplyRow addReply(Note.Reply reply){
        // inflate the view
        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingLeft());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        ReplyRow row = (ReplyRow) inflater.inflate(R.layout.notifications_reply_row, this, false);
        addView(row);
        
        if (ANIMATE) {
            Animation zoom = AnimationUtils.loadAnimation(getContext(), R.anim.zoom);
            row.startAnimation(zoom);
        }
        
        return row;
    }
}