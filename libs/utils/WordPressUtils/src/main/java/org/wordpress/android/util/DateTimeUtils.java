package org.wordpress.android.util;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateTimeUtils {
    private DateTimeUtils() {
        throw new AssertionError();
    }

    // See http://drdobbs.com/java/184405382
    private static final ThreadLocal<DateFormat> ISO8601_FORMAT = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        }
    };

    public static String javaDateToTimeSpan(final Date date, Context context, long currentTime) {
        if (date == null) {
            return "";
        }

        long passedTime = date.getTime();

        // return "now" if less than a minute has elapsed
        long secondsSince = (currentTime - passedTime) / 1000;
        if (secondsSince < 60) {
            return context.getString(R.string.timespan_now);
        }

        long daysSince = secondsSince / (60 * 60 * 24);

        // less than a year old, let `DateUtils.getRelativeTimeSpanString` do the job
        if (daysSince < 365) {
            return DateUtils.getRelativeTimeSpanString(passedTime, currentTime,
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL).toString();
        }

        // date is older, so include year (ex: Jan 30, 2013)
        return DateUtils.formatDateTime(context, passedTime, DateUtils.FORMAT_ABBREV_ALL);
    }

    /**
     * Converts a date to a localized relative time span ("Now", "8 hr. ago", "Yesterday", "3 days ago", "Jul 10, 1940")
     * We're using a call to `DateUtils.getRelativeTimeSpanString` in most cases.
     */
    public static String javaDateToTimeSpan(final Date date, Context context) {
        return javaDateToTimeSpan(date, context, System.currentTimeMillis());
    }

    /**
     * Given an ISO 8601-formatted date as a String, returns a {@link Date}.
     */
    public static Date dateFromIso8601(final String strDate) {
        try {
            DateFormat formatter = ISO8601_FORMAT.get();
            return formatter.parse(strDate);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Given an ISO 8601-formatted date as a String, returns a {@link Date} in UTC.
     */
    public static Date dateUTCFromIso8601(String iso8601date) {
        try {
            iso8601date = iso8601date.replace("Z", "+0000").replace("+00:00", "+0000");
            DateFormat formatter = ISO8601_FORMAT.get();
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            return formatter.parse(iso8601date);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Given a {@link Date}, returns an ISO 8601-formatted String.
     */
    public static String iso8601FromDate(Date date) {
        if (date == null) {
            return "";
        }
        DateFormat formatter = ISO8601_FORMAT.get();
        return formatter.format(date);
    }

    /**
     * Given a {@link Date}, returns an ISO 8601-formatted String in UTC.
     */
    public static String iso8601UTCFromDate(Date date) {
        if (date == null) {
            return "";
        }
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat formatter = ISO8601_FORMAT.get();
        formatter.setTimeZone(tz);

        String iso8601date = formatter.format(date);

        // Use "+00:00" notation rather than "+0000" to be consistent with the WP.COM API
        return iso8601date.replace("+0000", "+00:00");
    }

    /**
     * Returns the current UTC date
     */
    public static Date nowUTC() {
        Date dateTimeNow = new Date();
        return localDateToUTC(dateTimeNow);
    }

    public static Date localDateToUTC(Date dtLocal) {
        if (dtLocal == null) {
            return null;
        }
        TimeZone tz = TimeZone.getDefault();
        int currentOffsetFromUTC = tz.getRawOffset() + (tz.inDaylightTime(dtLocal) ? tz.getDSTSavings() : 0);
        return new Date(dtLocal.getTime() - currentOffsetFromUTC);
    }

    // Routines to return a diff between two dates - always return a positive number

    public static int daysBetween(Date dt1, Date dt2) {
        long hrDiff = hoursBetween(dt1, dt2);
        if (hrDiff == 0) {
            return 0;
        }
        return (int) (hrDiff / 24);
    }

    public static int hoursBetween(Date dt1, Date dt2) {
        long minDiff = minutesBetween(dt1, dt2);
        if (minDiff == 0) {
            return 0;
        }
        return (int) (minDiff / 60);
    }

    public static int minutesBetween(Date dt1, Date dt2) {
        long msDiff = millisecondsBetween(dt1, dt2);
        if (msDiff == 0) {
            return 0;
        }
        return (int) (msDiff / 60000);
    }

    public static int secondsBetween(Date dt1, Date dt2) {
        long msDiff = millisecondsBetween(dt1, dt2);
        if (msDiff == 0) {
            return 0;
        }
        return (int) (msDiff / 1000);
    }

    public static long millisecondsBetween(Date dt1, Date dt2) {
        if (dt1 == null || dt2 == null) {
            return 0;
        }
        return Math.abs(dt1.getTime() - dt2.getTime());
    }

    public static boolean isSameYear(Date dt1, Date dt2) {
        if (dt1 == null || dt2 == null) {
            return false;
        }
        return dt1.getYear() == dt2.getYear();
    }

    public static boolean isSameMonthAndYear(Date dt1, Date dt2) {
        if (dt1 == null || dt2 == null) {
            return false;
        }
        return dt1.getYear() == dt2.getYear() && dt1.getMonth() == dt2.getMonth();
    }

    // Routines involving Unix timestamps (GMT assumed)

    /**
     * Given an ISO 8601-formatted date as a String, returns the corresponding UNIX timestamp.
     */
    public static long timestampFromIso8601(final String strDate) {
        return timestampFromIso8601Millis(strDate) / 1000;
    }

    /**
     * Given an ISO 8601-formatted date as a String, returns the corresponding timestamp in milliseconds.
     *
     * @return 0 if the parameter is null, empty or not a date.
     */
    public static long timestampFromIso8601Millis(final String strDate) {
        if (TextUtils.isEmpty(strDate)) {
            return 0;
        }
        Date date = dateFromIso8601(strDate);
        if (date == null) {
            return 0;
        }
        return date.getTime();
    }

    /**
     * Given a UNIX timestamp, returns the corresponding {@link Date}.
     */
    public static Date dateFromTimestamp(long timestamp) {
        return new java.util.Date(timestamp * 1000);
    }

    /**
     * Given a UNIX timestamp, returns an ISO 8601-formatted date as a String.
     */
    public static String iso8601FromTimestamp(long timestamp) {
        return iso8601FromDate(dateFromTimestamp(timestamp));
    }

    /**
     * Given a UNIX timestamp, returns an ISO 8601-formatted date in UTC as a String.
     */
    public static String iso8601UTCFromTimestamp(long timestamp) {
        return iso8601UTCFromDate(dateFromTimestamp(timestamp));
    }

    /**
     * Given a UNIX timestamp, returns a relative time span ("8h", "3d", etc.).
     */
    public static String timeSpanFromTimestamp(long timestamp, Context context) {
        Date dateGMT = dateFromTimestamp(timestamp);
        return javaDateToTimeSpan(dateGMT, context);
    }
}
