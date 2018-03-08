package org.wordpress.android.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class FormatUtils {
    /*
     * NumberFormat isn't synchronized, so a separate instance must be created for each thread
     * http://developer.android.com/reference/java/text/NumberFormat.html
     */
    private static final ThreadLocal<NumberFormat> INTEGER_INSTANCE = new ThreadLocal<NumberFormat>() {
        @Override
        protected NumberFormat initialValue() {
            return NumberFormat.getIntegerInstance();
        }
    };

    private static final ThreadLocal<DecimalFormat> DECIMAL_INSTANCE = new ThreadLocal<DecimalFormat>() {
        @Override
        protected DecimalFormat initialValue() {
            return (DecimalFormat) DecimalFormat.getInstance();
        }
    };

    /*
     * returns the passed integer formatted with thousands-separators based on the current locale
     */
    public static final String formatInt(int value) {
        return INTEGER_INSTANCE.get().format(value).toString();
    }

    public static final String formatDecimal(int value) {
        return DECIMAL_INSTANCE.get().format(value).toString();
    }
}
