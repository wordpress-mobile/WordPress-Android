package org.wordpress.android.util;

import android.content.Context;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * Created by nbradbury on 6/20/13.
 * EditText utils
 */
public class EditTextUtils {

    private EditTextUtils() {
        throw new AssertionError();
    }

    /*
     * returns text string from passed EditText
     */
    public static String getText(EditText edit) {
        if (edit.getText()==null)
            return "";
        return edit.getText().toString();
    }

    /*
     * moves caret to end of text
     */
    public static void moveToEnd(EditText edit) {
        if (edit.getText()==null)
            return;
        edit.setSelection(edit.getText().toString().length());
    }

    /*
     * returns true if nothing has been entered into passed editor
     */
    public static boolean isEmpty(EditText edit) {
        return TextUtils.isEmpty(getText(edit));
    }

    /*
     * hide the soft keyboard for the passed EditText
      */
    public static void hideSoftInput(EditText edit) {
        if (edit==null)
            return;
        Context context = edit.getContext();
        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm!=null)
            imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
    }

}
