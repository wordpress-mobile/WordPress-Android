package org.wordpress.android.util;

import android.content.Context;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

public class WPActivityUtils {

    public static Context getThemedContext(Context context) {
        if (context instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity)context).getSupportActionBar();
            if (actionBar != null) {
                return actionBar.getThemedContext();
            }
        }
        return context;
    }
}
