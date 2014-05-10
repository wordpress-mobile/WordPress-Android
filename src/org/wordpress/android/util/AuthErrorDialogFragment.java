package org.wordpress.android.util;

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
    private static boolean mIsWPCom;

    public static AuthErrorDialogFragment newInstance(boolean isWPCom) {
        mIsWPCom = isWPCom;
        return new AuthErrorDialogFragment();
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
        b.setTitle(R.string.wpcom_signin_dialog_title);
        if (mIsWPCom) {
            b.setMessage(getResources().getText(R.string.incorrect_credentials) + " " + getResources().getText(
                    R.string.please_sign_in));
            b.setPositiveButton(R.string.sign_in, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent authIntent = new Intent(getActivity(), WPComLoginActivity.class);
                    authIntent.putExtra("wpcom", true);
                    authIntent.putExtra("auth-only", true);
                    getActivity().startActivity(authIntent);
                }
            });
        } else {
            b.setMessage(getResources().getText(R.string.incorrect_credentials) + " " + getResources().getText(
                    R.string.load_settings));
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