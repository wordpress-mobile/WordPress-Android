package org.wordpress.android.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.MotionEvent;
import android.widget.TextView;

import org.wordpress.android.R;

/**
 * Android's LinkMovementMethod crashes on malformed links, including links that have no
 * protocol (ex: "example.com" instead of "http://example.com"). This class extends
 * LinkMovementMethod to catch and ignore the exception.
 */

public class WPLinkMovementMethod extends LinkMovementMethod {
    protected static WPLinkMovementMethod mMovementMethod;

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
            // attempt to correct the tapped url then launch the intent to display it
            showTappedUrl(textView.getContext(), fixTappedUrl(buffer));
            return true;
        }
    }

    private static String fixTappedUrl(Spannable buffer) {
        if (buffer == null)
            return null;

        URLSpan urlSpans[] = buffer.getSpans(0, buffer.length(), URLSpan.class);
        if (urlSpans.length == 0)
            return null;

        // note that there will be only one URLSpan (the one that was tapped)
        String url = StringUtils.notNullStr(urlSpans[0].getURL());
        if (Uri.parse(url).getScheme() == null)
            return "http://" + url.trim();

        return url.trim();
    }

    private static void showTappedUrl(Context context, String url) {
        if (context == null || TextUtils.isEmpty(url))
            return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            String readerToastUrlErrorIntent = context.getString(R.string.reader_toast_err_url_intent);
            ToastUtils.showToast(context, String.format(readerToastUrlErrorIntent, url), ToastUtils.Duration.LONG);
        }
    }
}
