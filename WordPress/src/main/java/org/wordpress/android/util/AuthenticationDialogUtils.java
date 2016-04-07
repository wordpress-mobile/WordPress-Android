package org.wordpress.android.util;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.widgets.AuthErrorDialogFragment;

import static org.wordpress.android.util.ToastUtils.showToast;

public class AuthenticationDialogUtils {
    /**
     * Shows a toast message, unless there is an authentication issue which will show an alert dialog.
     */
    public static void showToastOrAuthAlert(Context context, VolleyError error, String friendlyMessage) {
        if (context == null)
            return;

        String message = null;
        boolean isInvalidTokenError = false;
        JSONObject errorObj = VolleyUtils.volleyErrorToJSON(error);
        if (errorObj != null) {
            try {
                if (errorObj.has("error_description")) { // OAuth token request error
                    message = (String) errorObj.get("error_description");
                    String error_code = (String) errorObj.get("error");
                    if (error_code != null && error_code.equals("invalid_request") && message.toLowerCase().contains(
                            "incorrect username or password")) {
                        isInvalidTokenError = true;
                    }
                } else {
                    message = (String) errorObj.get("message");
                    String error_code = (String) errorObj.get("error");
                    if (error_code != null && error_code.equals("invalid_token")) {
                        isInvalidTokenError = true;
                    }
                }
            } catch (JSONException e) {
                AppLog.e(T.API, e);
            }
        } else {
            message = error.getMessage();
        }

        if (isInvalidTokenError && (context instanceof Activity)) {
            showAuthErrorView((Activity) context);
        } else {
            String fallbackErrorMessage = TextUtils.isEmpty(friendlyMessage) ? context.getString(
                    R.string.error_generic) : friendlyMessage;
            if (message != null && message.contains("Limit reached") ) {
                message = context.getString(R.string.limit_reached);
            }
            String errorMessage = TextUtils.isEmpty(message) ? fallbackErrorMessage :  message;
            showToast(context, errorMessage, Duration.LONG);
        }
    }


    public static void showAuthErrorView(Activity activity) {
        showAuthErrorView(activity, AuthErrorDialogFragment.DEFAULT_RESOURCE_ID, AuthErrorDialogFragment.DEFAULT_RESOURCE_ID);
    }

    public static void showAuthErrorView(Activity activity, int titleResId, int messageResId) {
        final String ALERT_TAG = "alert_ask_credentials";
        if (activity.isFinishing()) {
            return;
        }

        // WP.com errors will show the sign in activity
        if (WordPress.getCurrentBlog() == null || (WordPress.getCurrentBlog() != null && WordPress.getCurrentBlog().isDotcomFlag())) {
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
