package org.wordpress.android.widgets;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextThemeWrapper;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.ui.ActivityLauncher;

/**
 * An alert dialog fragment for XML-RPC authentication failures
 */
public class AuthErrorDialogFragment extends DialogFragment {
    public static final int DEFAULT_RESOURCE_ID = -1;

    private int mMessageId = R.string.incorrect_credentials;
    private int mTitleId = R.string.connection_error;
    private SiteModel mSite;

    public void setArgs(int titleResourceId, int messageResourceId, SiteModel site) {
        mSite = site;
        if (titleResourceId != DEFAULT_RESOURCE_ID) {
            mTitleId = titleResourceId;
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
        AlertDialog.Builder b = new AlertDialog.Builder(
                new ContextThemeWrapper(getActivity(), R.style.Calypso_Dialog_Alert));
        b.setTitle(mTitleId);
        b.setMessage(mMessageId);
        b.setCancelable(true);
        b.setPositiveButton(R.string.settings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityLauncher.viewBlogSettingsForResult(getActivity(), mSite);
            }
        });
        b.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        return b.create();
    }
}
