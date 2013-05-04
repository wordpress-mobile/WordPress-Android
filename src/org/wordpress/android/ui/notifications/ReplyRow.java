package org.wordpress.android.ui.notifications;

import android.content.Intent;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.net.Uri;
import android.provider.Browser;

import org.wordpress.android.R;
import org.wordpress.android.models.Note;

class ReplyRow extends LinearLayout {
    private ImageView mButton;
    private ImageView mAvatar;
    private TextView mTextField;
    private ProgressBar mProgress;
    public ReplyRow(Context context){
        super(context);
    }
    public ReplyRow(Context context, AttributeSet attrs){
        super(context, attrs);
    }
    public ReplyRow(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
    }
    protected void onFinishInflate(){
        mButton = (ImageView) findViewById(R.id.button);
        mAvatar = (ImageView) findViewById(R.id.avatar);
        mTextField = (TextView) findViewById(R.id.text);
        mProgress = (ProgressBar) findViewById(R.id.progress);
        android.util.Log.d("NoteComment", String.format("%s %s %s %s", mButton, mAvatar, mTextField, mProgress));
    }
    public void setReply(final Note.Reply reply){
        if (reply.isComplete()) {
            // update the row text and button
            mButton.setVisibility(View.VISIBLE);
            mProgress.setVisibility(View.GONE);
            mAvatar.setVisibility(View.VISIBLE);
            setOnClickListener(new View.OnClickListener(){
               @Override
               public void onClick(View view){
                   Uri uri = Uri.parse(reply.getUrl());
                   Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                   Context context = getContext();
                   intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
                   context.startActivity(intent);
               } 
            });
        }
    }
    public ImageView getImageView(){
        return mAvatar;
    }
}