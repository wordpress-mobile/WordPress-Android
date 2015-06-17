package org.wordpress.android.util;

import android.content.Context;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.widget.TextView;

public class WPActivityUtils {

    public static Context getThemedContext(Context context) {
        if (context instanceof ActionBarActivity) {
            ActionBar actionBar = ((ActionBarActivity)context).getSupportActionBar();
            if (actionBar != null) {
                return actionBar.getThemedContext();
            }
        }
        return context;
    }

    public static String getNonNullString(TextView textView) {
        if (textView != null && textView.getText().length() != 0) {
            return textView.getText().toString();
        }

        return "";
    }
}
