package org.wordpress.android.login;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextThemeWrapper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class LoginSiteAddressHelpDialogFragment extends DialogFragment {
    public static final String TAG = "login_site_address_help_dialog_fragment_tag";

    private LoginListener mLoginListener;

    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;

    @Inject LoginAnalyticsListener mAnalyticsListener;

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
        if (context instanceof LoginListener) {
            mLoginListener = (LoginListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement LoginListener");
        }
    }

    @Override public void onDetach() {
        super.onDetach();
        mLoginListener = null;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder alert = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.LoginTheme));
        alert.setTitle(R.string.login_site_address_help_title);

        //noinspection InflateParams
        alert.setView(getActivity().getLayoutInflater().inflate(R.layout.login_alert_site_address_help, null));
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        alert.setNeutralButton(R.string.login_site_address_more_help, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mLoginListener.helpFindingSiteAddress(mAccountStore.getAccount().getUserName(), mSiteStore);
            }
        });

        if (savedInstanceState == null) {
            mAnalyticsListener.trackUrlHelpScreenViewed();
        }

        return alert.create();
    }
}
