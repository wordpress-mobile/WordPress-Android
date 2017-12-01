package org.wordpress.android.login.util;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class ActivityUtils {
    // TODO: Move to utils library
    public static void hideKeyboard(@Nullable final View view) {
        if (view == null) return;
        InputMethodManager inputMethodManager = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
