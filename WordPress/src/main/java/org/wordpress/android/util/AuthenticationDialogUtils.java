package org.wordpress.android.util;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;

import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.widgets.AuthErrorDialogFragment;

public class AuthenticationDialogUtils {
    public static void showAuthErrorView(Activity activity, boolean isCurrentSiteWPCom) {
        showAuthErrorView(activity, AuthErrorDialogFragment.DEFAULT_RESOURCE_ID,
                AuthErrorDialogFragment.DEFAULT_RESOURCE_ID, isCurrentSiteWPCom);
    }

    public static void showAuthErrorView(Activity activity, int titleResId, int messageResId,
                                         boolean isCurrentSiteWPCom) {
        final String ALERT_TAG = "alert_ask_credentials";
        if (activity.isFinishing()) {
            return;
        }

        // WP.com errors will show the sign in activity
        if (isCurrentSiteWPCom) {
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
        authAlert.setWPComTitleMessage(titleResId, messageResId);
        ft.add(authAlert, ALERT_TAG);
        ft.commitAllowingStateLoss();
    }
}
