package org.wordpress.android.fluxc.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateTimeUtils {
    private static final ThreadLocal<DateFormat> ISO8601Format = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        }
    };

    /**
     * Converts an ISO 8601 date to a Java date
     */
    public static Date dateFromIso8601(String iso8601date) {
        try {
            iso8601date = iso8601date.replace("Z", "+0000");
            DateFormat formatter = ISO8601Format.get();
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            return formatter.parse(iso8601date);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Converts a Java date to ISO 8601, in UTC
     */
    public static String iso8601UTCFromDate(Date date) {
        if (date == null) {
            return "";
        }
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat formatter = ISO8601Format.get();
        formatter.setTimeZone(tz);

        String iso8601date = formatter.format(date);

        // Use the ISO8601 "Z" notation rather than the +0000 UTC offset to be consistent with the WP.COM API
        return iso8601date.replace("+0000", "Z");
    }
}