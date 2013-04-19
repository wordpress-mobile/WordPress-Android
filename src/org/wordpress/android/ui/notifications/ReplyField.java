package org.wordpress.android.ui.notifications;

import android.view.View;
import android.view.KeyEvent;
import android.text.Editable;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.EditText;
import android.content.Context;
import android.util.AttributeSet;

import org.wordpress.android.R;

public class ReplyField extends LinearLayout {
    interface OnReplyListener {
        public void onReply(ReplyField field, Editable replyText);
    }
        
    EditText mTextField;
    Button mReplyButton;
    OnReplyListener mOnReplyListener;
    public ReplyField(Context context){
        super(context);
    }
    public ReplyField(Context context, AttributeSet attributes){
        super(context, attributes);
    }
    public ReplyField(Context context, AttributeSet attributes, int defStyle){
        super(context, attributes, defStyle);
    }
    public void setOnReplyListener(OnReplyListener l){
        mOnReplyListener = l;
    }
    public Button getReplyButton(){
        return mReplyButton;
    }
    public EditText getTextField(){
        return mTextField;
    }
    public Editable getText(){
        return mTextField.getText();
    }
    protected void onFinishInflate(){
        mTextField = (EditText) findViewById(R.id.note_reply_field);
        mReplyButton = (Button) findViewById(R.id.note_reply_button);
        mReplyButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                mOnReplyListener.onReply(ReplyField.this, getText());
            }
        });
        mTextField.setOnKeyListener(new View.OnKeyListener(){
           @Override
           public boolean onKey(View v, int keyCode, KeyEvent event){
               if (mOnReplyListener != null) {
               }
               return false;
           } 
        });
    }
}