package org.wordpress.android.util;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.text.TextUtils;

import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
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
            showAuthErrorDialog((Activity) context);
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


    public static void showAuthErrorDialog(Activity activity) {
        showAuthErrorDialog(activity, AuthErrorDialogFragment.DEFAULT_RESOURCE_ID, AuthErrorDialogFragment.DEFAULT_RESOURCE_ID);
    }

    public static void showAuthErrorDialog(Activity activity, int titleResId, int messageResId) {
        final String ALERT_TAG = "alert_ask_credentials";
        if (activity.isFinishing()) {
            return;
        }
        // abort if the dialog is already visible
        if (activity.getFragmentManager().findFragmentByTag(ALERT_TAG) != null) {
            return;
        }
        FragmentTransaction ft = activity.getFragmentManager().beginTransaction();
        AuthErrorDialogFragment authAlert;
        // Default to isDotCom (If WordPress.getCurrentBlog() == null: no blogs found,
        // so the user is logged in wpcom and doesn't own any blog
        boolean isDotCom = true;
        if (WordPress.getCurrentBlog() != null) {
            isDotCom = WordPress.getCurrentBlog().isDotcomFlag();
        }
        authAlert = new AuthErrorDialogFragment();
        authAlert.setWPComTitleMessage(isDotCom, titleResId, messageResId);
        ft.add(authAlert, ALERT_TAG);
        ft.commitAllowingStateLoss();
    }
}
