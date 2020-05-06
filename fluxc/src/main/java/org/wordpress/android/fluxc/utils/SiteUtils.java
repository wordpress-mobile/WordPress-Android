package org.wordpress.android.fluxc.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.util.MapUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static org.apache.commons.lang3.StringUtils.split;

public class SiteUtils {
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
    public static @NonNull String getDateTimeForSite(@NonNull SiteModel site,
                                                     @NonNull SimpleDateFormat dateFormat,
                                                     @NonNull Date date) {
        String wpTimeZone = site.getTimezone();
        dateFormat.setTimeZone(getNormalizedTimezone(wpTimeZone));
        return dateFormat.format(date);
    }

    /**
     * Given a {@link SiteModel} timezone returns a standard Java timezone
     * @param wpTimeZone from SiteModel
     * @return
     */
    public static TimeZone getNormalizedTimezone(String wpTimeZone) {
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

        return TimeZone.getTimeZone(timezoneNormalized);
    }

    /**
     * Given a formatsMap returns a List<PostFormatModel> or null
     * @param formatsMap the map of post formats
     * @return List<PostFormatModel> or null
     */
    public static @Nullable List<PostFormatModel> getValidPostFormatsOrNull(@Nullable Map<?, ?> formatsMap) {
        if (formatsMap == null) return null;

        List<PostFormatModel> res = new ArrayList<>();
        for (Object key : formatsMap.keySet()) {
            if (!(key instanceof String)) continue;
            String skey = (String) key;
            String sValue = MapUtils.getMapStr(formatsMap, skey);

            if (sValue.isEmpty()) return null;

            PostFormatModel postFormat = new PostFormatModel();
            postFormat.setSlug(skey);
            postFormat.setDisplayName(sValue);
            res.add(postFormat);
        }

        return res.isEmpty() ? null : res;
    }
}
