package org.wordpress.android.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;

public class ActivityUtils {
    /**
     * Hides the keyboard in the given {@link Activity}'s current focus using the
     * {@link InputMethodManager#HIDE_NOT_ALWAYS} flag, which will hide the keyboard unless it was originally shown
     * with {@link InputMethodManager#SHOW_FORCED}.
     */
    public static void hideKeyboard(Activity activity) {
        if (activity != null && activity.getCurrentFocus() != null) {
            InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(),
                                                 InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /**
     * Hides the keyboard for the given {@link View}. No {@link InputMethodManager} flag is used, therefore the
     * keyboard is forcibly hidden regardless of the circumstances.
     */
    public static void hideKeyboardForced(@Nullable final View view) {
        if (view == null) {
            return;
        }
        InputMethodManager inputMethodManager =
                (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * Shows the keyboard for the given {@link View} using the {@link InputMethodManager#SHOW_IMPLICIT} flag,
     * which is an implicit request (i.e. not requested by the user) to show the keyboard.
     */
    public static void showKeyboard(@Nullable final View view) {
        if (view == null) {
            return;
        }
        InputMethodManager inputMethodManager =
                (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    public static boolean isDeepLinking(Intent intent) {
        return Intent.ACTION_VIEW.equals(intent.getAction());
    }
}
