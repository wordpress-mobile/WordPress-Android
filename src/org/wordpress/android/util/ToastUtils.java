package org.wordpress.android.util;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

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
    public static void showToastOrAuthAlert(Context context, String xmlrpcMessage, String friendlyMessage) {
        if (context == null)
            return;
        if ((context instanceof FragmentActivity) && !TextUtils.isEmpty(xmlrpcMessage) && xmlrpcMessage.contains("code 403") || xmlrpcMessage.contains("code 503")) {
            FragmentActivity activity = (FragmentActivity) context;
            // Invalid credentials, show auth alert
            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
            AuthErrorDialogFragment authAlert = AuthErrorDialogFragment.newInstance(WordPress.getCurrentBlog().isDotcomFlag());
            ft.add(authAlert, "alert");
            ft.commitAllowingStateLoss(); 
        } else {
            String errorMessage = TextUtils.isEmpty(friendlyMessage) ? context.getString(R.string.error_generic) : friendlyMessage;
            showToast(context, errorMessage);
        }
    }
}
