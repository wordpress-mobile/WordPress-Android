package org.wordpress.android.util;

import android.content.ActivityNotFoundException;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.view.MotionEvent;
import android.widget.TextView;

/**
 * Created by nbradbury on 1/8/14.
 * Android's LinkMovementMethod crashes on malformed links, including links that have no
 * protocol (ex: "example.com" instead of "http://example.com"). This class extends
 * LinkMovementMethod to catch and ignore the exception.
 */

public class WPLinkMovementMethod extends LinkMovementMethod {
    private static WPLinkMovementMethod mMovementMethod;

    public static WPLinkMovementMethod getInstance() {
        if (mMovementMethod == null)
            mMovementMethod = new WPLinkMovementMethod();
        return mMovementMethod;
    }

    @Override
    public boolean onTouchEvent(TextView textView, Spannable buffer, MotionEvent event) {
        try {
            return super.onTouchEvent(textView, buffer, event) ;
        } catch (ActivityNotFoundException e) {
            AppLog.e(AppLog.T.UTILS, e);
            return true;
        }
    }
}
