package org.wordpress.android.ui.stats;

import android.annotation.SuppressLint;
import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.stats.exceptions.StatsError;
import org.wordpress.android.ui.stats.models.AuthorsModel;
import org.wordpress.android.ui.stats.models.ClicksModel;
import org.wordpress.android.ui.stats.models.CommentFollowersModel;
import org.wordpress.android.ui.stats.models.CommentsModel;
import org.wordpress.android.ui.stats.models.FollowersModel;
import org.wordpress.android.ui.stats.models.GeoviewsModel;
import org.wordpress.android.ui.stats.models.InsightsAllTimeModel;
import org.wordpress.android.ui.stats.models.InsightsPopularModel;
import org.wordpress.android.ui.stats.models.InsightsTodayModel;
import org.wordpress.android.ui.stats.models.PostModel;
import org.wordpress.android.ui.stats.models.PublicizeModel;
import org.wordpress.android.ui.stats.models.ReferrersModel;
import org.wordpress.android.ui.stats.models.SearchTermsModel;
import org.wordpress.android.ui.stats.models.TagsContainerModel;
import org.wordpress.android.ui.stats.models.TopPostsAndPagesModel;
import org.wordpress.android.ui.stats.models.VideoPlaysModel;
import org.wordpress.android.ui.stats.models.VisitsModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class StatsUtils {
    @SuppressLint("SimpleDateFormat")
    private static long toMs(String date, String pattern) {
        if (date == null) {
            return -1;
        }

        if (pattern == null) {
            AppLog.w(T.UTILS, "Trying to parse with a null pattern");
            return -1;
        }

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
        return toMs(date, StatsConstants.STATS_INPUT_DATE_FORMAT);
    }

    public static String msToString(long ms, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date(ms));
    }

    /**
     * Get the current date of the blog in the form of yyyy-MM-dd (EX: 2013-07-18) *
     */
    public static String getCurrentDateTZ(int localTableBlogID) {
        String timezone = StatsUtils.getBlogTimezone(WordPress.getBlog(localTableBlogID));
        if (timezone == null) {
            AppLog.w(T.UTILS, "Timezone is null. Returning the device time!!");
            return getCurrentDate();
        }

        return getCurrentDateTimeTZ(timezone, StatsConstants.STATS_INPUT_DATE_FORMAT);
    }

    /**
     * Get the current datetime of the blog *
     */
    public static String getCurrentDateTimeTZ(int localTableBlogID) {
        String timezone = StatsUtils.getBlogTimezone(WordPress.getBlog(localTableBlogID));
        if (timezone == null) {
            AppLog.w(T.UTILS, "Timezone is null. Returning the device time!!");
            return getCurrentDatetime();
        }
        String pattern = "yyyy-MM-dd HH:mm:ss"; // precision to seconds
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
        SimpleDateFormat sdf = new SimpleDateFormat(StatsConstants.STATS_INPUT_DATE_FORMAT);
        return sdf.format(new Date());
    }

    /**
     * Get the current date in the form of "yyyy-MM-dd HH:mm:ss"
     */
    private static String getCurrentDatetime() {
        String pattern = "yyyy-MM-dd HH:mm:ss"; // precision to seconds
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

        /*
        Convert the timezone to a form that is compatible with Java TimeZone class
        WordPress returns something like the following:
           UTC+0:30   ---->  0.5
           UTC+1      ---->  1.0
           UTC-0:30   ----> -1.0
        */

        AppLog.d(T.STATS, "Parsing the following Timezone received from WP: " + blogTimeZoneOption);
        String timezoneNormalized;
        if (blogTimeZoneOption.equals("0") || blogTimeZoneOption.equals("0.0")) {
            timezoneNormalized = "GMT";
        } else {
            String[] timezoneSplitted = org.apache.commons.lang.StringUtils.split(blogTimeZoneOption, ".");
            timezoneNormalized = timezoneSplitted[0];
            if(timezoneSplitted.length > 1 && timezoneSplitted[1].equals("5")){
                timezoneNormalized += ":30";
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

        AppLog.d(T.STATS, "Setting the following Timezone: " + timezoneNormalized);
        gmtDf.setTimeZone(TimeZone.getTimeZone(timezoneNormalized));
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
        return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }


    //Calculate the correct start/end date for the selected period
    public static String getPublishedEndpointPeriodDateParameters(StatsTimeframe timeframe, String date) {
        if (date == null) {
            AppLog.w(AppLog.T.STATS, "Can't calculate start and end period without a reference date");
            return null;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(StatsConstants.STATS_INPUT_DATE_FORMAT);
            Calendar c = Calendar.getInstance();
            c.setFirstDayOfWeek(Calendar.MONDAY);
            Date parsedDate = sdf.parse(date);
            c.setTime(parsedDate);


            final String after;
            final String before;
            switch (timeframe) {
                case DAY:
                    after = StatsUtils.msToString(c.getTimeInMillis(), StatsConstants.STATS_INPUT_DATE_FORMAT);
                    c.add(Calendar.DAY_OF_YEAR, +1);
                    before =  StatsUtils.msToString(c.getTimeInMillis(), StatsConstants.STATS_INPUT_DATE_FORMAT);
                    break;
                case WEEK:
                    c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    after = StatsUtils.msToString(c.getTimeInMillis(), StatsConstants.STATS_INPUT_DATE_FORMAT);
                    c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                    c.add(Calendar.DAY_OF_YEAR, +1);
                    before = StatsUtils.msToString(c.getTimeInMillis(), StatsConstants.STATS_INPUT_DATE_FORMAT);
                break;
                case MONTH:
                    //first day of the next month
                    c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
                    c.add(Calendar.DAY_OF_YEAR, +1);
                    before =  StatsUtils.msToString(c.getTimeInMillis(), StatsConstants.STATS_INPUT_DATE_FORMAT);

                    //last day of the prev month
                    c.setTime(parsedDate);
                    c.set(Calendar.DAY_OF_MONTH, c.getActualMinimum(Calendar.DAY_OF_MONTH));
                    after = StatsUtils.msToString(c.getTimeInMillis(), StatsConstants.STATS_INPUT_DATE_FORMAT);
                    break;
                case YEAR:
                    //first day of the next year
                    c.set(Calendar.MONTH, Calendar.DECEMBER);
                    c.set(Calendar.DAY_OF_MONTH, 31);
                    c.add(Calendar.DAY_OF_YEAR, +1);
                    before =  StatsUtils.msToString(c.getTimeInMillis(), StatsConstants.STATS_INPUT_DATE_FORMAT);

                    c.setTime(parsedDate);
                    c.set(Calendar.MONTH, Calendar.JANUARY);
                    c.set(Calendar.DAY_OF_MONTH, 1);
                    after = StatsUtils.msToString(c.getTimeInMillis(), StatsConstants.STATS_INPUT_DATE_FORMAT);
                    break;
                default:
                    AppLog.w(AppLog.T.STATS, "Can't calculate start and end period without a reference timeframe");
                    return null;
            }
            return "&after=" + after + "&before=" + before;
        } catch (ParseException e) {
            AppLog.e(AppLog.T.UTILS, e);
            return null;
        }
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
    public static String getBlogStatsUsername(int localTableBlogID) {
        Blog currentBlog = WordPress.getBlog(localTableBlogID);
        if (currentBlog == null) {
            return null;
        }
        String statsAuthenticatedUser = currentBlog.getDotcom_username();

        if (org.apache.commons.lang.StringUtils.isEmpty(statsAuthenticatedUser)) {
            // Let's try the global wpcom credentials
            statsAuthenticatedUser = AccountHelper.getDefaultAccount().getUserName();
        }

        if (org.apache.commons.lang.StringUtils.isEmpty(statsAuthenticatedUser)) {
            AppLog.e(AppLog.T.STATS, "WPCOM Credentials for the current blog are null!");
            return null;
        }

        return statsAuthenticatedUser;
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

    public static synchronized void logVolleyErrorDetails(final VolleyError volleyError) {
        if (volleyError == null) {
            AppLog.e(T.STATS, "Tried to log a VolleyError, but the error obj was null!");
            return;
        }
        if (volleyError.networkResponse != null) {
            NetworkResponse networkResponse = volleyError.networkResponse;
            AppLog.e(T.STATS, "Network status code: " + networkResponse.statusCode);
            if (networkResponse.data != null) {
                AppLog.e(T.STATS, "Network data: " + new String(networkResponse.data));
            }
        }
        AppLog.e(T.STATS, "Volley Error details: " + volleyError.getMessage(), volleyError);
    }

    public static synchronized Serializable parseResponse(StatsService.StatsEndpointsEnum endpointName, String blogID, JSONObject response)
            throws JSONException {
        Serializable model = null;
        switch (endpointName) {
            case VISITS:
                model = new VisitsModel(blogID, response);
                break;
            case TOP_POSTS:
                model = new TopPostsAndPagesModel(blogID, response);
                break;
            case REFERRERS:
                model = new ReferrersModel(blogID, response);
                break;
            case CLICKS:
                model = new ClicksModel(blogID, response);
                break;
            case GEO_VIEWS:
                model = new GeoviewsModel(blogID, response);
                break;
            case AUTHORS:
                model = new AuthorsModel(blogID, response);
                break;
            case VIDEO_PLAYS:
                model = new VideoPlaysModel(blogID, response);
                break;
            case COMMENTS:
                model = new CommentsModel(blogID, response);
                break;
            case FOLLOWERS_WPCOM:
                model = new FollowersModel(blogID, response);
                break;
            case FOLLOWERS_EMAIL:
                model = new FollowersModel(blogID, response);
                break;
            case COMMENT_FOLLOWERS:
                model = new CommentFollowersModel(blogID, response);
                break;
            case TAGS_AND_CATEGORIES:
                model = new TagsContainerModel(blogID, response);
                break;
            case PUBLICIZE:
                model = new PublicizeModel(blogID, response);
                break;
            case SEARCH_TERMS:
                model = new SearchTermsModel(blogID, response);
                break;
            case INSIGHTS_ALL_TIME:
                model = new InsightsAllTimeModel(blogID, response);
                break;
            case INSIGHTS_POPULAR:
                model = new InsightsPopularModel(blogID, response);
                break;
            case INSIGHTS_TODAY:
                model = new InsightsTodayModel(blogID, response);
                break;
        }
        return model;
    }

    public static void openPostInReaderOrInAppWebview(Context ctx, final PostModel post) {
        final String postType = post.getPostType();
        final String url = post.getUrl();
        final long blogID = Long.parseLong(post.getBlogID());
        final long itemID = Long.parseLong(post.getItemID());
        if (postType.equals("post") || postType.equals("page")) {
            // If the post/page has ID == 0 is the home page, and we need to load the blog preview,
            // otherwise 404 is returned if we try to show the post in the reader
            if (itemID == 0) {
                ReaderActivityLauncher.showReaderBlogPreview(
                        ctx,
                        blogID
                );
            } else {
                ReaderActivityLauncher.showReaderPostDetail(
                        ctx,
                        blogID,
                        itemID
                );
            }
        } else if (postType.equals("homepage")) {
            ReaderActivityLauncher.showReaderBlogPreview(
                    ctx,
                    blogID
            );
        } else {
            AppLog.d(AppLog.T.UTILS, "Opening the in-app browser: " + url);
            WPWebViewActivity.openURL(ctx, url);
        }
    }

    /*
     * This function rewrites a VolleyError into a simple Stats Error by getting the error message.
     * This is a FIX for https://github.com/wordpress-mobile/WordPress-Android/issues/2228 where
     * VolleyErrors cannot be serializable.
     */
    public static StatsError rewriteVolleyError(VolleyError volleyError, String defaultErrorString) {
        if (volleyError != null && volleyError.getMessage() != null) {
            return new StatsError(volleyError.getMessage());
        }

        if (defaultErrorString != null) {
            return new StatsError(defaultErrorString);
        }

        // Error string should be localized here, but don't want to pass a context
        return new StatsError("Stats couldn't be refreshed at this time");
    }
}
