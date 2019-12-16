package org.wordpress.android.ui.publicize;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.wordpress.android.R;
import org.wordpress.android.ui.publicize.PublicizeConstants.ConnectAction;

/**
 * Publicize connect/disconnect/reconnect button
 */
public class ConnectButton extends FrameLayout {
    private ConnectAction mConnectAction = ConnectAction.CONNECT;
    private Button mConnectButton;

    public ConnectButton(Context context) {
        super(context);
        initView(context);
    }

    public ConnectButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ConnectButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    private void initView(Context context) {
        inflate(context, R.layout.publicize_connect_button, this);
        mConnectButton = findViewById(R.id.text_connect);
        updateView();
    }

    private void updateView() {
        @StringRes int captionResId;
        switch (mConnectAction) {
            case CONNECT:
                captionResId = R.string.share_btn_connect;
                break;
            case DISCONNECT:
                captionResId = R.string.share_btn_disconnect;
                break;
            case RECONNECT:
                captionResId = R.string.share_btn_reconnect;
                break;
            case CONNECT_ANOTHER_ACCOUNT:
                captionResId = R.string.share_btn_connect_another_account;
                break;
            default:
                return;
        }
        mConnectButton.setText(captionResId);
    }

    public ConnectAction getAction() {
        return mConnectAction;
    }

    public void setAction(ConnectAction newAction) {
        if (newAction != null && !newAction.equals(mConnectAction)) {
            mConnectAction = newAction;
            updateView();
        }
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        super.setOnClickListener(l);
        mConnectButton.setOnClickListener(l);
    }
}
