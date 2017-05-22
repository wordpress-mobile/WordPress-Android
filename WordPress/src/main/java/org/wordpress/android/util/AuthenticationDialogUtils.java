package org.wordpress.android.util;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;

import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.widgets.AuthErrorDialogFragment;

public class AuthenticationDialogUtils {
    public static void showAuthErrorView(Activity activity, SiteModel site) {
        showAuthErrorView(activity, AuthErrorDialogFragment.DEFAULT_RESOURCE_ID,
                AuthErrorDialogFragment.DEFAULT_RESOURCE_ID, site);
    }

    public static void showAuthErrorView(Activity activity, int titleResId, int messageResId,
                                         SiteModel site) {
        final String ALERT_TAG = "alert_ask_credentials";
        if (activity.isFinishing()) {
            return;
        }

        // TODO: check isJetpackConnected - there is probably something to do here for Jetpack sites connected to
        // a wpcom account different than the main account in the app. We could at least show a different message
        // like: "This configuration is not supported in the app, blablabla".

        // WP.com errors will show the sign in activity
        if (site.isWPCom()) {
            Intent signInIntent = new Intent(activity, SignInActivity.class);
            signInIntent.putExtra(SignInActivity.EXTRA_IS_AUTH_ERROR, true);
            signInIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivityForResult(signInIntent, SignInActivity.REQUEST_CODE);
            return;
        }

        // abort if the dialog is already visible
        if (activity.getFragmentManager().findFragmentByTag(ALERT_TAG) != null) {
            return;
        }

        FragmentTransaction ft = activity.getFragmentManager().beginTransaction();
        AuthErrorDialogFragment authAlert = new AuthErrorDialogFragment();
        authAlert.setArgs(titleResId, messageResId, site);
        ft.add(authAlert, ALERT_TAG);
        ft.commitAllowingStateLoss();
    }
}
