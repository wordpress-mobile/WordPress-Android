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

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.passcodelock.AppLockManager;

class ReplyRow extends LinearLayout {
    private ImageView mButton, mErrorIndicator;
    private NetworkImageView mAvatar;
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
        mAvatar = (NetworkImageView) findViewById(R.id.avatar);
        mTextField = (TextView) findViewById(R.id.text);
        mProgress = (ProgressBar) findViewById(R.id.progress);
        mErrorIndicator = (ImageView) findViewById(R.id.error);
    }
    
    public void setComplete(boolean complete){
        if (complete) {
            mProgress.setVisibility(View.GONE);
            mAvatar.setVisibility(View.VISIBLE);
        } else {
            mProgress.setVisibility(View.VISIBLE);
            mAvatar.setVisibility(View.GONE);
        }
    }

    public void setFailed(boolean failed){
        if (failed) {
            mProgress.setVisibility(View.GONE);
            mAvatar.setVisibility(View.GONE);
            mErrorIndicator.setVisibility(View.VISIBLE);
        } else {
            mErrorIndicator.setVisibility(View.GONE);
        }
    }

    public void setUrl(final String url){
        if (url == null) {
            mButton.setVisibility(View.GONE);
            setOnClickListener(null);
        } else {
            mButton.setVisibility(View.VISIBLE);
            setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view){
                    Uri uri = Uri.parse(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    Context context = getContext();
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
                    context.startActivity(intent);
                    AppLockManager.getInstance().setExtendedTimeout();
                } 
            });
        }                
    }
    
    public void setText(CharSequence text){
        mTextField.setText(text);
    }
    
    public void setText(int stringRes){
        mTextField.setText(stringRes);
    }

    public NetworkImageView getImageView(){
        return mAvatar;
    }
}