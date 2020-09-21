package org.wordpress.android.util;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

/**
 * EditText utils
 */
public class EditTextUtils {
    private EditTextUtils() {
        throw new AssertionError();
    }

    /**
     * returns non-null text string from passed TextView
     */
    public static String getText(TextView textView) {
        return (textView != null) ? textView.getText().toString() : "";
    }

    /**
     * moves caret to end of text
     */
    public static void moveToEnd(EditText edit) {
        if (edit.getText() == null) {
            return;
        }
        edit.setSelection(edit.getText().toString().length());
    }

    /**
     * returns true if nothing has been entered into passed editor
     */
    public static boolean isEmpty(EditText edit) {
        return TextUtils.isEmpty(getText(edit));
    }

    /**
     * hide the soft keyboard for the passed EditText
     *
     * @deprecated Use {@link ActivityUtils#hideKeyboard(Activity)} or {@link ActivityUtils#hideKeyboardForced(View)}
     * instead.
     */
    // TODO: Replace instances with ActivityUtils#showKeyboard(Activity) or ActivityUtils#showKeyboardForced(View) to
    // consolidate similar methods and favor library version.
    @Deprecated
    public static void hideSoftInput(EditText edit) {
        if (edit == null) {
            return;
        }

        InputMethodManager imm = getInputMethodManager(edit);
        if (imm != null) {
            imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
        }
    }

    /**
     * show the soft keyboard for the passed EditText
     *
     * @deprecated Use {@link ActivityUtils#showKeyboard(View)} instead.
     */
    // TODO: Replace instances with ActivityUtils#showKeyboard(View) to consolidate similar methods and favor library
    // version.
    @Deprecated
    public static void showSoftInput(EditText edit) {
        if (edit == null) {
            return;
        }

        edit.requestFocus();

        InputMethodManager imm = getInputMethodManager(edit);
        if (imm != null) {
            imm.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private static InputMethodManager getInputMethodManager(EditText edit) {
        Context context = edit.getContext();
        return (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    }
}
