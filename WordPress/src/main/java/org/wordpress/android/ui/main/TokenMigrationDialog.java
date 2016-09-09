package org.wordpress.android.ui.main;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import org.wordpress.android.R;

public class TokenMigrationDialog extends DialogFragment {
    private static final String HAS_ACCOUNT_KEY = "has-account";
    private static final String HAS_SITES_KEY = "has-sites";

    private boolean mHasAccount;
    private boolean mHasSites;

    public static TokenMigrationDialog newInstance(boolean hasAccount, boolean hasSites) {
        TokenMigrationDialog dialog = new TokenMigrationDialog();
        Bundle args = new Bundle();
        args.putBoolean(HAS_ACCOUNT_KEY, hasAccount);
        args.putBoolean(HAS_SITES_KEY, hasSites);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mHasAccount = args.getBoolean(HAS_ACCOUNT_KEY, false);
            mHasSites = args.getBoolean(HAS_SITES_KEY, false);
        }

        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.progress_dialog, container);
    }

    public void setHasAccount(boolean hasAccount) {
        mHasAccount = hasAccount;
    }

    public void setHasSites(boolean hasSites) {
        mHasSites = hasSites;
    }

    public boolean hasData() {
        return mHasSites && mHasAccount;
    }
}
