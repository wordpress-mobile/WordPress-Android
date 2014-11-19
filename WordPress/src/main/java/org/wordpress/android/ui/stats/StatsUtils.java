package org.wordpress.android.ui.stats;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * A utility class to help with date parsing and saving summaries in stats
 */
public class StatsUtils {

    @SuppressLint("SimpleDateFormat")
    public static long toMs(String date, String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        try {
            return sdf.parse(date).getTime();
        } catch (ParseException e) {
            AppLog.e(T.UTILS, e);
        }
        return -1;
    }

    /**
     * Converts date in the form of 2013-07-18 to ms *
     */
    public static long toMs(String date) {
        return toMs(date, "yyyy-MM-dd");
    }

    public static String msToString(long ms, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date(ms));
    }

    /**
     * Get the current date of the blog in the form of yyyy-MM-dd (EX: 2013-07-18) *
     */
    public static String getCurrentDateTZ(int localTableBlogID) {
        String pattern = "yyyy-MM-dd";
        String timezone = StatsUtils.getBlogTimezone(WordPress.getBlog(localTableBlogID));
        if (timezone == null) {
            AppLog.w(T.UTILS, "Timezone is null. Returning the device time!!");
            return getCurrentDate();
        }

        return getCurrentDateTimeTZ(timezone, pattern);
    }

    /**
     * Get the current datetime of the blog in Ms *
     */
    public static long getCurrentDateTimeMsTZ(int localTableBlogID) {
        String timezone = StatsUtils.getBlogTimezone(WordPress.getBlog(localTableBlogID));
        if (timezone == null) {
            AppLog.w(T.UTILS, "Timezone is null. Returning the device time!!");
            return new Date().getTime();
        }
        String pattern = "yyyy-MM-dd HH:mm:ss"; // precision to seconds
        return toMs(getCurrentDateTimeTZ(timezone, pattern), pattern);
    }

    /**
     * Get the current date in the form of yyyy-MM-dd (EX: 2013-07-18) *
     */
    private static String getCurrentDate() {
        String pattern = "yyyy-MM-dd";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(new Date());
    }

    private static String getBlogTimezone(Blog blog) {
        if (blog == null) {
            AppLog.w(T.UTILS, "Blog object is null!! Can't read timezone opt then.");
            return null;
        }

        JSONObject jsonOptions = blog.getBlogOptionsJSONObject();
        String timezone = null;
        if (jsonOptions != null && jsonOptions.has("time_zone")) {
            try {
                timezone = jsonOptions.getJSONObject("time_zone").getString("value");
            } catch (JSONException e) {
                AppLog.e(T.UTILS, "Cannot load time_zone from options: " + jsonOptions, e);
            }
        } else {
            AppLog.w(T.UTILS, "Blog options are null, or doesn't contain time_zone");
        }
        return timezone;
    }

    private static String getCurrentDateTimeTZ(String blogTimeZoneOption, String pattern) {
        Date date = new Date();
        SimpleDateFormat gmtDf = new SimpleDateFormat(pattern);

        if (blogTimeZoneOption == null) {
            AppLog.w(T.UTILS, "blogTimeZoneOption is null. getCurrentDateTZ() will return the device time!");
            return gmtDf.format(date);
        }

        if (blogTimeZoneOption.equals("0")) {
            gmtDf.setTimeZone(TimeZone.getTimeZone("GMT"));
        } else if (blogTimeZoneOption.startsWith("-")) {
            gmtDf.setTimeZone(TimeZone.getTimeZone("GMT" + blogTimeZoneOption));
        } else {
            if (blogTimeZoneOption.startsWith("+")) {
                gmtDf.setTimeZone(TimeZone.getTimeZone("GMT" + blogTimeZoneOption));
            } else {
                gmtDf.setTimeZone(TimeZone.getTimeZone("GMT+" + blogTimeZoneOption));
            }
        }
        return gmtDf.format(date);
    }

    public static String parseDate(String timestamp, String fromFormat, String toFormat) {
        SimpleDateFormat from = new SimpleDateFormat(fromFormat);
        SimpleDateFormat to = new SimpleDateFormat(toFormat);
        try {
            Date date = from.parse(timestamp);
            return to.format(date);
        } catch (ParseException e) {
            AppLog.e(T.STATS, e);
        }
        return "";
    }

    /**
     * Get a diff between two dates
     * @param date1 the oldest date in Ms
     * @param date2 the newest date in Ms
     * @param timeUnit the unit in which you want the diff
     * @return the diff value, in the provided unit
     */
    public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS);
    }

    public static int getSmallestWidthDP() {
        return WordPress.getContext().getResources().getInteger(R.integer.smallest_width_dp);
    }

    /**
     * Return the credentials for the  blog or null if not available.
     *
     * 1. Read the credentials at blog level (Jetpack connected with a wpcom account != main account)
     * 2. If credentials are empty read the global wpcom credentials
     * 3. Check that credentials are not empty before launching the activity
     *
     */
    public static StatsCredentials getBlogStatsCredentials(int localTableBlogID) {
        Blog currentBlog = WordPress.getBlog(localTableBlogID);
        if (currentBlog == null) {
            return null;
        }
        String statsAuthenticatedUser = currentBlog.getDotcom_username();
        String statsAuthenticatedPassword = currentBlog.getDotcom_password();

        if (org.apache.commons.lang.StringUtils.isEmpty(statsAuthenticatedPassword)
                || org.apache.commons.lang.StringUtils.isEmpty(statsAuthenticatedUser)) {
            // Let's try the global wpcom credentials
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
            statsAuthenticatedUser = settings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
            statsAuthenticatedPassword = WordPressDB.decryptPassword(
                    settings.getString(WordPress.WPCOM_PASSWORD_PREFERENCE, null)
            );
        }

        if (org.apache.commons.lang.StringUtils.isEmpty(statsAuthenticatedPassword)
                || org.apache.commons.lang.StringUtils.isEmpty(statsAuthenticatedUser)) {
            AppLog.e(AppLog.T.STATS, "WPCOM Credentials for the current blog are null!");
            return null;
        }

        return new StatsCredentials(statsAuthenticatedUser, statsAuthenticatedPassword);
    }

    public static class StatsCredentials {
        private String mUsername, mPassword;

        public StatsCredentials(String username, String password) {
            this.mUsername = username;
            this.mPassword = password;
        }

        public String getUsername() {
            return mUsername;
        }

        public String getPassword() {
            return mPassword;
        }
    }

    /**
     * Return the remote blogId as stored on the wpcom backend.
     * <p>
     * blogId is always available for dotcom blogs. It could be null on Jetpack blogs
     * with blogOptions still empty or when the option 'jetpack_client_id' is not available in blogOptions.
     * </p>
     * @return String  blogId or null
     */
    public static String getBlogId(int localTableBlogID) {
        Blog currentBlog = WordPress.getBlog(localTableBlogID);
        if (currentBlog == null) {
            return null;
        }
        if (currentBlog.isDotcomFlag()) {
            return String.valueOf(currentBlog.getRemoteBlogId());
        } else {
            return currentBlog.getApi_blogid();
        }
    }
}
