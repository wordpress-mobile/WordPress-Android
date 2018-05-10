package org.wordpress.android.fluxc.utils;

import org.wordpress.android.fluxc.model.SiteModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static org.apache.commons.lang3.StringUtils.split;

public class SiteUtils {
    /**
     * Given a {@link SiteModel} and a {@link String} compatible with {@link SimpleDateFormat},
     * returns a formatted date that accounts for the site's timezone setting.
     *
     * Imported from WordPress-Android with some modifications.
     */
    public static String getCurrentDateTimeForSite(SiteModel site, String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.ROOT);
        return getCurrentDateTimeForSite(site, dateFormat);
    }

    /**
     * Given a {@link SiteModel} and a {@link SimpleDateFormat},
     * returns a formatted date that accounts for the site's timezone setting.
     *
     * Imported from WordPress-Android with some modifications.
     */
    public static String getCurrentDateTimeForSite(SiteModel site, SimpleDateFormat dateFormat) {
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
        return dateFormat.format(new Date());
    }
}
