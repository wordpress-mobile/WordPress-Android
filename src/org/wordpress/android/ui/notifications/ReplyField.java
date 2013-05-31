package org.wordpress.android.ui.notifications;

import android.view.View;
import android.view.KeyEvent;
import android.text.Editable;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.content.Context;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;

import org.wordpress.android.R;

public class ReplyField extends LinearLayout {
    interface OnReplyListener {
        public void onReply(ReplyField field, Editable replyText);
    }
        
    EditText mTextField;
    ImageButton mReplyButton;
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
    public void clearFocus(){
        getTextField().clearFocus();
    }
    public void setOnReplyListener(OnReplyListener l){
        mOnReplyListener = l;
    }
    public ImageButton getReplyButton(){
        return mReplyButton;
    }
    public EditText getTextField(){
        return mTextField;
    }
    public Editable getText(){
        return mTextField.getText();
    }
    public void setText(CharSequence text){
        mTextField.setText(text);
    }
    public boolean isEmpty(){
        return TextUtils.isEmpty(getText());
    }
    protected void onFinishInflate(){
        mTextField = (EditText) findViewById(R.id.note_reply_field);
        mReplyButton = (ImageButton) findViewById(R.id.note_reply_button);
        mReplyButton.setEnabled(!isEmpty());
        mReplyButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (!isEmpty()) {
                    mOnReplyListener.onReply(ReplyField.this, getText());
                }
            }
        });
        mTextField.addTextChangedListener(new TextWatcher(){
            @Override
            public void afterTextChanged(Editable text){
                mReplyButton.setEnabled(!isEmpty());
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after){
                // noop
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count){
                // noop
            }
        });
    }
}