package org.wordpress.android.ui.stats;

import android.annotation.SuppressLint;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.RemoteException;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.datasets.StatsBarChartDataTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.StatsBarChartData;
import org.wordpress.android.models.StatsSummary;
import org.wordpress.android.models.StatsVideoSummary;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

/**
 * A utility class to help with date parsing and saving summaries in stats
 */
public class StatsUtils {
    private static final String STAT_SUMMARY = "StatSummary_";
    private static final String STAT_VIDEO_SUMMARY = "StatVideoSummary_";
    private static final long ONE_DAY = 24 * 60 * 60 * 1000;

    /**
     * Converts date in the form of 2013-07-18 to ms *
     */
    @SuppressLint("SimpleDateFormat")
    public static long toMs(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return sdf.parse(date).getTime();
        } catch (ParseException e) {
            AppLog.e(T.UTILS, e);
        }
        return -1;
    }

    public static String msToString(long ms, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date(ms));
    }

    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date());
    }

    public static String getYesterdaysDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date(getCurrentDateMs() - ONE_DAY));
    }

    public static long getCurrentDateMs() {
        return toMs(getCurrentDate());
    }

    public static String getBlogTimezone(Blog blog) {
        if (blog == null) {
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

    public static String getCurrentDateTZ(String blogTimeZoneOption) {
        Date date = new Date();
        SimpleDateFormat gmtDf = new SimpleDateFormat("yyyy-MM-dd");

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

    public static String getYesterdaysDateTZ(String blogTimeZoneOption) {
        String todayDateTZ = getCurrentDateTZ(blogTimeZoneOption);
        long yesterdayMillis = StatsUtils.toMs(todayDateTZ);
        SimpleDateFormat gmtDf = new SimpleDateFormat("yyyy-MM-dd");
        return gmtDf.format(new Date(yesterdayMillis - StatsUtils.ONE_DAY));
    }

    public static long getCurrentDateMsTZ(String blogTimeZoneOption) {
        return toMs(getCurrentDateTZ(blogTimeZoneOption));
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

    public static void saveSummary(String blogId, JSONObject stat) {
        try {
            JSONObject statsObject = stat.getJSONObject("stats");
            String day = stat.getString("day");
            statsObject.put("day", day);
            FileOutputStream fos = WordPress.getContext().openFileOutput(STAT_SUMMARY + blogId, Context.MODE_PRIVATE);
            fos.write(statsObject.toString().getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            AppLog.e(T.STATS, e);
        } catch (IOException e) {
            AppLog.e(T.STATS, e);
        } catch (JSONException e) {
            AppLog.e(T.STATS, e);
        }

        saveGraphData(blogId, stat);
    }

    private static void saveGraphData(String blogId, JSONObject stat) {
        try {
            JSONArray data = stat.getJSONObject("visits").getJSONArray("data");
            Uri uri = StatsContentProvider.STATS_BAR_CHART_DATA_URI;
            Context context = WordPress.getContext();

            int length = data.length();

            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

            if (length > 0) {
                ContentProviderOperation op = ContentProviderOperation.newDelete(uri).withSelection("blogId=? AND unit=?", new String[]{blogId, StatsBarChartUnit.DAY.name()}).build();
                operations.add(op);
            }

            for (int i = 0; i < length; i++) {
                StatsBarChartData item = new StatsBarChartData(blogId, StatsBarChartUnit.DAY, data.getJSONArray(i));
                ContentValues values = StatsBarChartDataTable.getContentValues(item);

                if (values != null) {
                    ContentProviderOperation op = ContentProviderOperation.newInsert(uri).withValues(values).build();
                    operations.add(op);
                }
            }

            ContentResolver resolver = context.getContentResolver();
            resolver.applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
            resolver.notifyChange(uri, null);
        } catch (JSONException e) {
            AppLog.e(T.STATS, e);
        } catch (RemoteException e) {
            AppLog.e(T.STATS, e);
        } catch (OperationApplicationException e) {
            AppLog.e(T.STATS, e);
        }
    }

    public static void deleteSummary(String blogId) {
        WordPress.getContext().deleteFile(STAT_SUMMARY + blogId);
    }

    public static StatsSummary getSummary(String blogId) {
        StatsSummary stat = null;
        try {
            FileInputStream fis = WordPress.getContext().openFileInput(STAT_SUMMARY + blogId);
            StringBuilder fileContent = new StringBuilder();

            byte[] buffer = new byte[1024];

            int bytesRead = fis.read(buffer);
            while (bytesRead != -1) {
                fileContent.append(new String(buffer, 0, bytesRead, "ISO-8859-1"));
                bytesRead = fis.read(buffer);
            }
            fis.close();

            Gson gson = new Gson();
            stat = gson.fromJson(fileContent.toString(), StatsSummary.class);

        } catch (FileNotFoundException e) {
            // stats haven't been downloaded yet
        } catch (IOException e) {
            AppLog.e(T.STATS, e);
        }
        return stat;
    }

    public static void saveVideoSummary(String blogId, JSONObject stat) {
        try {
            stat.put("date", getCurrentDate());
            FileOutputStream fos = WordPress.getContext().openFileOutput(STAT_VIDEO_SUMMARY + blogId, Context.MODE_PRIVATE);
            fos.write(stat.toString().getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            AppLog.e(T.STATS, e);
        } catch (IOException e) {
            AppLog.e(T.STATS, e);
        } catch (JSONException e) {
            AppLog.e(T.STATS, e);
        }
    }

    public static void deleteVideoSummary(String blogId) {
        WordPress.getContext().deleteFile(STAT_VIDEO_SUMMARY + blogId);
    }

    public static StatsVideoSummary getVideoSummary(String blogId) {
        StatsVideoSummary stat = null;
        try {
            FileInputStream fis = WordPress.getContext().openFileInput(STAT_VIDEO_SUMMARY + blogId);
            StringBuilder fileContent = new StringBuilder();

            byte[] buffer = new byte[1024];

            while (fis.read(buffer) != -1) {
                fileContent.append(new String(buffer));
            }

            JSONObject object = new JSONObject(fileContent.toString());

            String timeframe = object.getString("timeframe");
            int plays = object.getInt("plays");
            int impressions = object.getInt("impressions");
            int minutes = object.getInt("minutes");
            String bandwidth = object.getString("bandwidth");
            String date = object.getString("date");

            stat = new StatsVideoSummary(timeframe, plays, impressions, minutes, bandwidth, date);

        } catch (FileNotFoundException e) {
            AppLog.e(T.STATS, e);
        } catch (IOException e) {
            AppLog.e(T.STATS, e);
        } catch (JSONException e) {
            AppLog.e(T.STATS, e);
        }
        return stat;
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
