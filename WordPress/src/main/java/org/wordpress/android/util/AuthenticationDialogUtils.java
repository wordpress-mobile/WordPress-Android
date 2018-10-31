package org.wordpress.android.util;

import android.content.Intent;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.login.LoginMode;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.accounts.LoginActivity;
import org.wordpress.android.widgets.AuthErrorDialogFragment;

public class AuthenticationDialogUtils {
    public static void showAuthErrorView(AppCompatActivity activity, SiteStore siteStore, SiteModel site) {
        showAuthErrorView(activity, siteStore, AuthErrorDialogFragment.DEFAULT_RESOURCE_ID,
                          AuthErrorDialogFragment.DEFAULT_RESOURCE_ID, site);
    }

    public static void showAuthErrorView(AppCompatActivity activity, SiteStore siteStore, int titleResId,
                                         int messageResId, SiteModel site) {
        final String alertTag = "alert_ask_credentials";
        if (activity.isFinishing()) {
            return;
        }

        // TODO: check isJetpackConnected - there is probably something to do here for Jetpack sites connected to
        // a wpcom account different than the main account in the app. We could at least show a different message
        // like: "This configuration is not supported in the app, blablabla".

        // WP.com errors will show the sign in activity
        if (site.isWPCom()) {
            if (siteStore.hasSiteAccessedViaXMLRPC()) {
                // show site picker since there are site besides WPCOM ones
                ActivityLauncher.showSitePickerForResult(activity, site);
            } else {
                // only WPCOM sites are available so, need to ask the user to log in again
                Intent intent = new Intent(activity, LoginActivity.class);
                LoginMode.WPCOM_REAUTHENTICATE.putInto(intent);
                activity.startActivityForResult(intent, RequestCodes.REAUTHENTICATE);
            }

            return;
        }

        // abort if the dialog is already visible
        if (activity.getFragmentManager().findFragmentByTag(alertTag) != null) {
            return;
        }

        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        AuthErrorDialogFragment authAlert = new AuthErrorDialogFragment();
        authAlert.setArgs(titleResId, messageResId, site);
        ft.add(authAlert, alertTag);
        ft.commitAllowingStateLoss();
    }
}
