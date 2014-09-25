package org.wordpress.android.ui.stats;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Layout;
import android.text.Spannable;
import android.text.style.URLSpan;
import android.view.MotionEvent;
import android.widget.TextView;

import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.WPLinkMovementMethod;

public class StatsWPLinkMovementMethod extends WPLinkMovementMethod {
    public static WPLinkMovementMethod getInstance() {
        if (mMovementMethod == null) {
            mMovementMethod = new StatsWPLinkMovementMethod();
        }
        return mMovementMethod;
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_UP) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            if (layout == null) {
                return super.onTouchEvent(widget, buffer, event);
            }
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            URLSpan[] link = buffer.getSpans(off, off, URLSpan.class);
            if (link.length != 0) {
                String url = link[0].getURL();
                AppLog.d(AppLog.T.UTILS, "Tapped on the Link: " + url);
                if (url.startsWith("https://wordpress.com/my-stats")
                        || url.startsWith("http://wordpress.com/my-stats")) {
                    // make sure to load the no-chrome version of Stats over https
                    url = UrlUtils.makeHttps(url);
                    if (url.contains("?")) {
                        // add the no chrome parameters if not available
                        if (!url.contains("?no-chrome") && !url.contains("&no-chrome")) {
                            url += "&no-chrome";
                        }
                    } else {
                        url += "?no-chrome";
                    }
                    AppLog.d(AppLog.T.UTILS, "Opening the Authenticated in-app browser : " + url);
                    // Let's try the global wpcom credentials
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(widget.getContext());
                    String statsAuthenticatedUser = settings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
                    String statsAuthenticatedPassword = WordPressDB.decryptPassword(
                            settings.getString(WordPress.WPCOM_PASSWORD_PREFERENCE, null)
                    );
                    if (org.apache.commons.lang.StringUtils.isEmpty(statsAuthenticatedPassword)
                            || org.apache.commons.lang.StringUtils.isEmpty(statsAuthenticatedUser)) {
                        // Still empty. Do not eat the event, but let's open the default Web Browser.
                        return super.onTouchEvent(widget, buffer, event);
                    }
                    WPWebViewActivity.openUrlByUsingWPCOMCredentials(widget.getContext(),
                            url, statsAuthenticatedUser, statsAuthenticatedPassword);
                    return true;
                } else if (url.startsWith("https") || url.startsWith("http")) {
                    AppLog.d(AppLog.T.UTILS, "Opening the in-app browser: " + url);
                    WPWebViewActivity.openURL(widget.getContext(), url);
                    return true;
                }
            }
        }
        return super.onTouchEvent(widget, buffer, event);
    }
}
