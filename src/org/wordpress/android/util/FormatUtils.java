package org.wordpress.android.util;

import java.text.NumberFormat;

/**
 * Created by nbradbury on 1/3/14.
 */
public class FormatUtils {

    /*
     * NumberFormat isn't synchronized, so a separate instance must be created for each thread
     * http://developer.android.com/reference/java/text/NumberFormat.html
     */
    private static final ThreadLocal<NumberFormat> IntegerFormat = new ThreadLocal<NumberFormat>() {
        @Override
        protected NumberFormat initialValue() {
            return NumberFormat.getIntegerInstance();
        }
    };

    /*
     * returns the passed integer formatted with thousands-separators based on the current locale
     */
    public static final String formatInt(int value) {
        return IntegerFormat.get().format(value).toString();
    }
}
