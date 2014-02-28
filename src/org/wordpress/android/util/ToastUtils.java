package org.wordpress.android.util;

import static org.wordpress.android.WordPress.getContext;

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
        Toast toast = Toast.makeText(context, text, (duration== Duration.SHORT ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG));
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
                if (errorObj.has("error_description")) { //OAuth token request error
                    //{"error_description":"Incorrect username or password.","error":"invalid_request"}
                    message = (String) errorObj.get("error_description");
                    String error_code = (String) errorObj.get("error");
                    if (error_code!=null && error_code.equals("invalid_request") && message.toLowerCase().contains("incorrect username or password"))
                        isInvalidTokenError = true;
                } else {
                    message = (String) errorObj.get("message");
                    String error_code = (String) errorObj.get("error");
                    if (error_code!=null && error_code.equals("invalid_token"))
                        isInvalidTokenError = true;
                }
            } catch (JSONException e) {
                AppLog.e(T.API, e);
            }
        } else {
            message = error.getMessage();
        }

        if (isInvalidTokenError && (context instanceof FragmentActivity)) {
            FragmentActivity activity = (FragmentActivity) context;
            if(activity.isFinishing())
                return;
            // Invalid credentials, show auth alert
            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
            AuthErrorDialogFragment authAlert = AuthErrorDialogFragment.newInstance(WordPress.getCurrentBlog().isDotcomFlag());
            ft.add(authAlert, "alert");
            ft.commitAllowingStateLoss(); 
        } else {
            String fallbackErrorMessage = TextUtils.isEmpty(friendlyMessage) ? context.getString(R.string.error_generic) : friendlyMessage;
            if (message != null && message.contains("Limit reached") ) {
                message = context.getString(R.string.limit_reached);
            }
            String errorMessage = TextUtils.isEmpty(message) ? fallbackErrorMessage :  message;
            showToast(context, errorMessage, Duration.LONG);
        }
    }
    
    /*
     * Shows a toast message, unless there is an authentication issue which will show an alert dialog.
     */
    public static void showToastOrAuthAlert(Context context, String xmlrpcMessage, String friendlyMessage) {
        if (context == null)
            return;

        boolean isLoginLimitReached = false;
        boolean is2StepsAuthEnabled = false;
        if (!TextUtils.isEmpty(xmlrpcMessage)) {
            String lowerCaseXmlrpcMessage = xmlrpcMessage.toLowerCase();
            if (lowerCaseXmlrpcMessage.contains("code 503") && ( lowerCaseXmlrpcMessage.contains("limit reached") || lowerCaseXmlrpcMessage.contains("login limit")))
                isLoginLimitReached = true;
            else if (lowerCaseXmlrpcMessage.contains("code 425"))
                is2StepsAuthEnabled = true;
        }

        if ((context instanceof FragmentActivity) && !TextUtils.isEmpty(xmlrpcMessage) && (xmlrpcMessage.contains("code 403") || is2StepsAuthEnabled)) {
            FragmentActivity activity = (FragmentActivity) context;
            if(activity.isFinishing())
                return;
            // Invalid credentials, show auth alert
            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
            AuthErrorDialogFragment authAlert = AuthErrorDialogFragment.newInstance(WordPress.getCurrentBlog().isDotcomFlag());
            ft.add(authAlert, "alert");
            ft.commitAllowingStateLoss(); 
        } else {
            String errorMessage = null;
            if (isLoginLimitReached) {
                errorMessage = context.getString(R.string.limit_reached);
            } else if (is2StepsAuthEnabled) {
                errorMessage = context.getString(R.string.account_two_step_auth_enabled);
            } else {
                errorMessage = TextUtils.isEmpty(friendlyMessage) ? context.getString(R.string.error_generic) : friendlyMessage;
            }
            showToast(context, errorMessage, Duration.LONG);
        }
    }
}
