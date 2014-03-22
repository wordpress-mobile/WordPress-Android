package org.wordpress.android.util;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Toast;

import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog.T;

/**
 * Created by nbradbury on 6/20/13.
 * Provides a simplified way to show toast messages without having to create the toast, set the
 * desired gravity, etc.
 */
public class ToastUtils {
    public enum Duration {SHORT, LONG}

    private ToastUtils() {
        throw new AssertionError();
    }

    public static void showToast(Context context, int stringResId) {
        showToast(context, stringResId, Duration.SHORT);
    }

    public static void showToast(Context context, int stringResId, Duration duration) {
        showToast(context, context.getString(stringResId), duration);
    }

    public static void showToast(Context context, String text) {
        showToast(context, text, Duration.SHORT);
    }

    public static void showToast(Context context, String text, Duration duration) {
        Toast toast = Toast.makeText(context, text,
                (duration == Duration.SHORT ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG));
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    /*
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

        if (isInvalidTokenError && (context instanceof FragmentActivity)) {
            showAuthErrorDialog((FragmentActivity) context);
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

    public static void showAuthErrorDialog(FragmentActivity activity) {
        final String ALERT_TAG = "alert_ask_credentials";
        if (activity.isFinishing()) {
            return;
        }
        // abort if the dialog is already visible
        if (activity.getSupportFragmentManager().findFragmentByTag(ALERT_TAG) != null) {
            return;
        }
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        AuthErrorDialogFragment authAlert;
        if (WordPress.getCurrentBlog() == null) {
            // No blogs found, so the user is logged in wpcom and doesn't own any blog
            authAlert = AuthErrorDialogFragment.newInstance(true);
        } else {
            authAlert = AuthErrorDialogFragment.newInstance(WordPress.getCurrentBlog().isDotcomFlag());
        }
        ft.add(authAlert, ALERT_TAG);
        ft.commitAllowingStateLoss();
    }
}
