package org.wordpress.android.ui.publicize;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.StringRes;

import com.google.android.material.button.MaterialButton;

import org.wordpress.android.R;
import org.wordpress.android.ui.publicize.PublicizeConstants.ConnectAction;

/**
 * Publicize connect/disconnect/reconnect button
 */
public class ConnectButton extends MaterialButton {
    private ConnectAction mConnectAction = ConnectAction.CONNECT;

    public ConnectButton(Context context) {
        super(context);
        updateView();
    }

    public ConnectButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        updateView();
    }

    public ConnectButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
        setText(captionResId);
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
