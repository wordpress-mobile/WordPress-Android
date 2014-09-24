package org.wordpress.android.widgets;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.accounts.WPComLoginActivity;
import org.wordpress.android.ui.prefs.BlogPreferencesActivity;

/**
 * An alert dialog fragment for XML-RPC authentication failures
 */
public class AuthErrorDialogFragment extends DialogFragment {
    public static int DEFAULT_RESOURCE_ID = -1;

    private boolean mIsWPCom;
    private int mMessageId = R.string.incorrect_credentials;
    private int mTitleId = R.string.connection_error;

    public void setWPComTitleMessage(boolean isWPCom, int titleResourceId, int messageResourceId) {
        mIsWPCom = isWPCom;

        if (titleResourceId != DEFAULT_RESOURCE_ID) {
            mTitleId = titleResourceId;
        } else if (mIsWPCom) {
            mTitleId = R.string.wpcom_signin_dialog_title;
        } else {
            mTitleId = R.string.connection_error;
        }

        if (messageResourceId != DEFAULT_RESOURCE_ID) {
            mMessageId = messageResourceId;
        } else {
            mMessageId = R.string.incorrect_credentials;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setCancelable(true);
        int style = DialogFragment.STYLE_NORMAL, theme = 0;
        setStyle(style, theme);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        b.setTitle(mTitleId);
        b.setMessage(mMessageId);
        if (mIsWPCom) {
            b.setPositiveButton(R.string.sign_in, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent authIntent = new Intent(getActivity(), WPComLoginActivity.class);
                    authIntent.putExtra("wpcom", true);
                    authIntent.putExtra("auth-only", true);
                    getActivity().startActivityForResult(authIntent, WPComLoginActivity.REQUEST_CODE);
                }
            });
        } else {
            b.setCancelable(true);
            b.setPositiveButton(R.string.settings, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent settingsIntent = new Intent(getActivity(), BlogPreferencesActivity.class);
                    settingsIntent.putExtra("id", WordPress.getCurrentBlog().getLocalTableBlogId());
                    getActivity().startActivity(settingsIntent);
                }
            });
            b.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
        }
        return b.create();
    }
}