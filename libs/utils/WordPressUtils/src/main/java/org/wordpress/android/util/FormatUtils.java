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

    /*
     * returns the passed long formatted has an human readable filesize. Ex: 10 GB
     * unitStrings is expected to be an array of all possible sizes from byte to TeraByte, in the current locale
     */
    public static final String formatFileSize(long size, final String[] unitStrings) {
        final double log1024 = Math.log10(1024);
        if (size <= 0) {
            return "0";
        }
        int digitGroups = (int) (Math.log10(size) / log1024);

        return String.format(unitStrings[digitGroups], DECIMAL_INSTANCE.get().format(size / Math.pow(1024, digitGroups)));
    }

    /*
     * returns the passed double percentage (0 to 1) formatted as an human readable percentage. Ex: 0.25 returns 25%
     */
    public static final String formatPercentage(double value) {
        NumberFormat percentFormat = NumberFormat.getPercentInstance();
        percentFormat.setMaximumFractionDigits(1);
        String result = percentFormat.format(value);
        return result;
    }
}
