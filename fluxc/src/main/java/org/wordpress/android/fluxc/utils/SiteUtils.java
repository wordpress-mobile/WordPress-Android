package org.wordpress.android.fluxc.utils;

import android.support.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.utils.StatsGranularity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static org.apache.commons.lang3.StringUtils.split;

public class SiteUtils {
    private static final String DATE_FORMAT_DEFAULT = "yyyy-MM-dd";


    /**
     * Given a {@link SiteModel} and a {@link String} compatible with {@link SimpleDateFormat} and a {@param dateString}
     * returns a formatted date that accounts for the site's timezone setting.
     *
     */
    public static @NonNull String getDateTimeForSite(@NonNull SiteModel site,
                                                     @NonNull String pattern,
                                                     @NonNull String dateString) {
        Date date = StringUtils.isEmpty(dateString) ? new Date() : getDateFromString(pattern, dateString);
        return getDateTimeForSite(site, pattern, date);
    }


    /**
     * Given a {@link SiteModel} and a {@link String} compatible with {@link SimpleDateFormat},
     * returns a formatted date that accounts for the site's timezone setting.
     * <p>
     * Imported from WordPress-Android with some modifications.
     */
    public static @NonNull String getCurrentDateTimeForSite(@NonNull SiteModel site, @NonNull String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.ROOT);
        return getCurrentDateTimeForSite(site, dateFormat);
    }

    /**
     * Given a {@link SiteModel}, {@link String} and a {@link Date} compatible with {@link SimpleDateFormat},
     * returns a formatted date that accounts for the site's timezone setting.
     */
    public static @NonNull String getDateTimeForSite(@NonNull SiteModel site,
                                                     @NonNull String pattern,
                                                     @NonNull Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.ROOT);
        return getDateTimeForSite(site, dateFormat, date);
    }

    /**
     * Given a {@link SiteModel} and a {@link SimpleDateFormat},
     * returns a formatted date that accounts for the site's timezone setting.
     * <p>
     * Imported from WordPress-Android with some modifications.
     */
    public static @NonNull String getCurrentDateTimeForSite(@NonNull SiteModel site,
                                                            @NonNull SimpleDateFormat dateFormat) {
        Date date = new Date();
        return getDateTimeForSite(site, dateFormat, date);
    }


    /**
     * Given a {@link SiteModel}, {@link SimpleDateFormat} and a {@link Date},
     * returns a formatted date that accounts for the site's timezone setting.
     */
    private static @NonNull String getDateTimeForSite(@NonNull SiteModel site,
                                                      @NonNull SimpleDateFormat dateFormat,
                                                      @NonNull Date date) {
        String wpTimeZone = site.getTimezone();

        /*
        Convert the timezone to a form that is compatible with Java TimeZone class
        WordPress returns something like the following:
           UTC+0:30 ----> 0.5
           UTC+1 ----> 1.0
           UTC-0:30 ----> -1.0
        */

        String timezoneNormalized;
        if (wpTimeZone == null || wpTimeZone.isEmpty() || wpTimeZone.equals("0") || wpTimeZone.equals("0.0")) {
            timezoneNormalized = "GMT";
        } else {
            String[] timezoneSplit = split(wpTimeZone, ".");
            timezoneNormalized = timezoneSplit[0];
            if (timezoneSplit.length > 1) {
                switch (timezoneSplit[1]) {
                    case "5":
                        timezoneNormalized += ":30";
                        break;
                    case "75":
                        timezoneNormalized += ":45";
                        break;
                    case "25":
                        // Not used by any timezones as of current writing, but you never know
                        timezoneNormalized += ":15";
                        break;
                }
            }
            if (timezoneNormalized.startsWith("-")) {
                timezoneNormalized = "GMT" + timezoneNormalized;
            } else {
                if (timezoneNormalized.startsWith("+")) {
                    timezoneNormalized = "GMT" + timezoneNormalized;
                } else {
                    timezoneNormalized = "GMT+" + timezoneNormalized;
                }
            }
        }

        dateFormat.setTimeZone(TimeZone.getTimeZone(timezoneNormalized));
        return dateFormat.format(date);
    }



    /**
     * Given a {@param d1} start date, {@param d2} end date and the {@param granularity} granularity,
     * returns a quantity value.
     * If the start date or end date is empty, returns {@param defaultValue}
     */
    public static long getQuantityByGranularity(String d1,
                                                String d2,
                                                StatsGranularity granularity,
                                                long defaultValue) {
        if (StringUtils.isEmpty(d1) || StringUtils.isEmpty(d2)) return defaultValue;

        Date startDate = getDateFromString(DATE_FORMAT_DEFAULT, d1);
        Date endDate = getDateFromString(DATE_FORMAT_DEFAULT, d2);

        Calendar startDateCalendar = getStartDateCalendar(startDate);
        Calendar endDateCalendar = getEndDateCalendar(endDate);

        switch (granularity) {
            case WEEKS: return getQuantityInWeeks(startDateCalendar, endDateCalendar);
            case MONTHS: return getQuantityInMonths(startDateCalendar, endDateCalendar);
            case YEARS: return getQuantityInYears(startDateCalendar, endDateCalendar);
            default: return getQuantityInDays(startDateCalendar, endDateCalendar);
        }
    }


    /**
     * returns a {@link Date} instance
     * based on {@param pattern} and {@param dateString}
     */
    private static Date getDateFromString(String pattern,
                                          String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.ROOT);
        try {
            return dateFormat.parse(dateString);
        } catch (ParseException e) {
            return new Date();
        }
    }



    /**
     * Given a {@link SimpleDateFormat} instance and the {@link String} start date string,
     * returns a {@link Calendar} instance.
     * The start date time is set to 00:00:00
     */
    private static Calendar getStartDateCalendar(@NonNull Date startDate) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(startDate);
        cal1.set(Calendar.HOUR_OF_DAY, 0);
        cal1.set(Calendar.MINUTE, 0);
        cal1.set(Calendar.SECOND, 0);
        cal1.set(Calendar.MILLISECOND, 0);

        return cal1;
    }



    /**
     * Given a {@link SimpleDateFormat} instance and the {@link String} end date string,
     * returns a {@link Calendar} instance.
     * The end date time is set to 23:59:59
     */
    private static Calendar getEndDateCalendar(Date endDate) {
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(endDate);
        cal2.set(Calendar.HOUR_OF_DAY, 23);
        cal2.set(Calendar.MINUTE, 59);
        cal2.set(Calendar.SECOND, 59);
        cal2.set(Calendar.MILLISECOND, 59);
        return cal2;
    }



    /**
     * Given a {@link Calendar} instance for startDate and endDate,
     * returns a quantity that is calculated based on [StatsGranularity.DAYS]
     */
    private static long getQuantityInDays(@NonNull Calendar c1,
                                          @NonNull Calendar c2) {
        long millis1 = c1.getTimeInMillis();
        long millis2 = c2.getTimeInMillis();

        long diff = Math.abs(millis2 - millis1);
        return (long) Math.ceil(diff / (double) (24 * 60 * 60 * 1000));
    }

    /**
     * Given a {@link Calendar} instance for startDate and endDate,
     * returns a quantity that is calculated based on [StatsGranularity.WEEKS]
     */
    private static long getQuantityInWeeks(@NonNull Calendar c1,
                                           @NonNull Calendar c2) {
        /*
         * start date: if day of week is greater than 1: set to 1
         * end date: if day of week is less than 7: set to 7
         * */
        if (c1.get(Calendar.DAY_OF_WEEK) > 1) c1.set(Calendar.DAY_OF_WEEK, 1);
        if (c2.get(Calendar.DAY_OF_WEEK) < 1) c2.set(Calendar.DAY_OF_WEEK, 7);

        double diffInDays = getQuantityInDays(c1, c2);
        return (long) Math.ceil(diffInDays / 7);
    }


    /**
     * Given a {@link Calendar} instance for startDate and endDate,
     * returns a quantity that is calculated based on [StatsGranularity.MONTHS]
     */
    private static long getQuantityInMonths(@NonNull Calendar c1,
                                            @NonNull Calendar c2) {
        long diff = 0;
        if (c2.after(c1)) {
            while (c2.after(c1)) {
                if (c2.after(c1)) {
                    diff++;
                }
                c1.add(Calendar.MONTH, 1);
            }
        }
        return Math.abs(diff);
    }


    /**
     * Given a {@link Calendar} instance for startDate and endDate,
     * returns a quantity that is calculated based on [StatsGranularity.YEARS]
     */
    private static long getQuantityInYears(@NonNull Calendar c1,
                                           @NonNull Calendar c2) {
        int diffInYears = Math.abs(c2.get(Calendar.YEAR) - c1.get(Calendar.YEAR));
        if (diffInYears == 0) diffInYears++;
        return diffInYears;
    }
}
