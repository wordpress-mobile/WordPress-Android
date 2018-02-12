package org.wordpress.android.ui.publicize;

import android.content.Context;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.publicize.PublicizeConstants.ConnectAction;

/**
 * Publicize connect/disconnect/reconnect button
 */
public class ConnectButton extends FrameLayout {

    private ConnectAction mConnectAction = ConnectAction.CONNECT;

    public ConnectButton(Context context){
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
        TextView txtConnect = (TextView) findViewById(R.id.text_connect);
        txtConnect.setText(captionResId);
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
}