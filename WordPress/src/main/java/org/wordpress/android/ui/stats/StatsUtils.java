package org.wordpress.android.ui.stats;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.stats.exceptions.StatsError;
import org.wordpress.android.ui.stats.models.AuthorsModel;
import org.wordpress.android.ui.stats.models.BaseStatsModel;
import org.wordpress.android.ui.stats.models.ClicksModel;
import org.wordpress.android.ui.stats.models.CommentFollowersModel;
import org.wordpress.android.ui.stats.models.CommentsModel;
import org.wordpress.android.ui.stats.models.FollowersModel;
import org.wordpress.android.ui.stats.models.GeoviewsModel;
import org.wordpress.android.ui.stats.models.InsightsAllTimeModel;
import org.wordpress.android.ui.stats.models.InsightsLatestPostDetailsModel;
import org.wordpress.android.ui.stats.models.InsightsLatestPostModel;
import org.wordpress.android.ui.stats.models.InsightsPopularModel;
import org.wordpress.android.ui.stats.models.InsightsTodayModel;
import org.wordpress.android.ui.stats.models.PublicizeModel;
import org.wordpress.android.ui.stats.models.ReferrersModel;
import org.wordpress.android.ui.stats.models.SearchTermsModel;
import org.wordpress.android.ui.stats.models.StatsPostModel;
import org.wordpress.android.ui.stats.models.TagsContainerModel;
import org.wordpress.android.ui.stats.models.TopPostsAndPagesModel;
import org.wordpress.android.ui.stats.models.VideoPlaysModel;
import org.wordpress.android.ui.stats.models.VisitsModel;
import org.wordpress.android.ui.stats.service.StatsServiceLogic;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.viewmodel.ResourceProvider;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class StatsUtils {
    private static long toMs(String date, String pattern) {
        if (date == null || date.equals("null")) {
            AppLog.w(T.UTILS, "Trying to parse a 'null' Stats Date.");
            return -1;
        }

        if (pattern == null) {
            AppLog.w(T.UTILS, "Trying to parse a Stats date with a null pattern");
            return -1;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.ROOT);
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

    public static String msToLocalizedString(long ms, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        return sdf.format(new Date(ms));
    }

    /**
     * Get the current date of the blog in the form of yyyy-MM-dd (EX: 2013-07-18) *
     */
    public static String getCurrentDateTZ(SiteModel site) {
        String timezone = site.getTimezone();
        if (timezone == null) {
            AppLog.w(T.UTILS, "Timezone is null. Returning the device time!!");
            return getCurrentDate();
        }

        return getCurrentDateTimeTZ(timezone, StatsConstants.STATS_INPUT_DATE_FORMAT);
    }

    /**
     * Get the current datetime of the blog *
     */
    public static String getCurrentDateTimeTZ(SiteModel site) {
        String timezone = site.getTimezone();
        if (timezone == null) {
            AppLog.w(T.UTILS, "Timezone is null. Returning the device time!");
            return getCurrentDatetime();
        }
        String pattern = "yyyy-MM-dd HH:mm:ss"; // precision to seconds
        return getCurrentDateTimeTZ(timezone, pattern);
    }

    /**
     * Get the current datetime of the blog in Ms *
     */
    public static long getCurrentDateTimeMsTZ(SiteModel site) {
        String timezone = site.getTimezone();
        if (timezone == null) {
            AppLog.w(T.UTILS, "Timezone is null. Returning the device time!");
            return new Date().getTime();
        }
        String pattern = "yyyy-MM-dd HH:mm:ss"; // precision to seconds
        return toMs(getCurrentDateTimeTZ(timezone, pattern), pattern);
    }

    /**
     * Get the current date in the form of yyyy-MM-dd (EX: 2013-07-18) *
     */
    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(StatsConstants.STATS_INPUT_DATE_FORMAT, Locale.ROOT);
        return sdf.format(new Date());
    }

    /**
     * Get the current date in the form of "yyyy-MM-dd HH:mm:ss"
     */
    private static String getCurrentDatetime() {
        String pattern = "yyyy-MM-dd HH:mm:ss"; // precision to seconds
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.ROOT);
        return sdf.format(new Date());
    }

    private static String getCurrentDateTimeTZ(String blogTimeZoneOption, String pattern) {
        Date date = new Date();
        SimpleDateFormat gmtDf = new SimpleDateFormat(pattern, Locale.ROOT);

        if (blogTimeZoneOption == null) {
            AppLog.w(T.UTILS, "blogTimeZoneOption is null. getCurrentDateTZ() will return the device time!");
            return gmtDf.format(date);
        }

        /*
        Convert the timezone to a form that is compatible with Java TimeZone class
        WordPress returns something like the following:
           UTC+0:30 ----> 0.5
           UTC+1 ----> 1.0
           UTC-0:30 ----> -1.0
        */

        AppLog.v(T.STATS, "Parsing the following Timezone received from WP: " + blogTimeZoneOption);
        String timezoneNormalized;
        if (TextUtils.isEmpty(blogTimeZoneOption) || blogTimeZoneOption.equals("0")
            || blogTimeZoneOption.equals("0.0")) {
            timezoneNormalized = "GMT";
        } else {
            String[] timezoneSplitted = org.apache.commons.lang3.StringUtils.split(blogTimeZoneOption, ".");
            timezoneNormalized = timezoneSplitted[0];
            if (timezoneSplitted.length > 1 && timezoneSplitted[1].equals("5")) {
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

        AppLog.v(T.STATS, "Setting the following Timezone: " + timezoneNormalized);
        gmtDf.setTimeZone(TimeZone.getTimeZone(timezoneNormalized));
        return gmtDf.format(date);
    }

    public static String parseDateToLocalizedFormat(String timestamp, String fromFormat, String toFormat) {
        SimpleDateFormat from = new SimpleDateFormat(fromFormat, Locale.ROOT);
        SimpleDateFormat to = new SimpleDateFormat(toFormat, Locale.getDefault());
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
     *
     * @param date1    the oldest date in Ms
     * @param date2    the newest date in Ms
     * @param timeUnit the unit in which you want the diff
     * @return the diff value, in the provided unit
     */
    public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }


    // Calculate the correct start/end date for the selected period
    @SuppressLint("SimpleDateFormat") // not sure what this method might be used for, so supressing for now
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
                    after = StatsUtils.msToLocalizedString(c.getTimeInMillis(), StatsConstants.STATS_INPUT_DATE_FORMAT);
                    c.add(Calendar.DAY_OF_YEAR, +1);
                    before = StatsUtils.msToLocalizedString(c.getTimeInMillis(),
                            StatsConstants.STATS_INPUT_DATE_FORMAT);
                    break;
                case WEEK:
                    c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    after = StatsUtils.msToLocalizedString(c.getTimeInMillis(), StatsConstants.STATS_INPUT_DATE_FORMAT);
                    c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                    c.add(Calendar.DAY_OF_YEAR, +1);
                    before = StatsUtils.msToLocalizedString(c.getTimeInMillis(),
                            StatsConstants.STATS_INPUT_DATE_FORMAT);
                    break;
                case MONTH:
                    // first day of the next month
                    c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
                    c.add(Calendar.DAY_OF_YEAR, +1);
                    before = StatsUtils.msToLocalizedString(c.getTimeInMillis(),
                            StatsConstants.STATS_INPUT_DATE_FORMAT);

                    // last day of the prev month
                    c.setTime(parsedDate);
                    c.set(Calendar.DAY_OF_MONTH, c.getActualMinimum(Calendar.DAY_OF_MONTH));
                    after = StatsUtils.msToLocalizedString(c.getTimeInMillis(),
                            StatsConstants.STATS_INPUT_DATE_FORMAT);
                    break;
                case YEAR:
                    // first day of the next year
                    c.set(Calendar.MONTH, Calendar.DECEMBER);
                    c.set(Calendar.DAY_OF_MONTH, 31);
                    c.add(Calendar.DAY_OF_YEAR, +1);
                    before = StatsUtils.msToLocalizedString(c.getTimeInMillis(),
                            StatsConstants.STATS_INPUT_DATE_FORMAT);

                    c.setTime(parsedDate);
                    c.set(Calendar.MONTH, Calendar.JANUARY);
                    c.set(Calendar.DAY_OF_MONTH, 1);
                    after = StatsUtils.msToLocalizedString(c.getTimeInMillis(), StatsConstants.STATS_INPUT_DATE_FORMAT);
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
        AppLog.e(T.STATS, "Volley Error Message: " + volleyError.getMessage(), volleyError);
    }

    public static synchronized boolean isRESTDisabledError(final Serializable error) {
        if (error == null || !(error instanceof com.android.volley.AuthFailureError)) {
            return false;
        }
        com.android.volley.AuthFailureError volleyError = (com.android.volley.AuthFailureError) error;
        if (volleyError.networkResponse != null && volleyError.networkResponse.data != null) {
            String errorMessage = new String(volleyError.networkResponse.data).toLowerCase(Locale.ROOT);
            return errorMessage.contains("api calls") && errorMessage.contains("disabled");
        } else {
            AppLog.e(T.STATS, "Network response is null in Volley. Can't check if it is a Rest Disabled error.");
            return false;
        }
    }

    public static synchronized BaseStatsModel parseResponse(StatsServiceLogic.StatsEndpointsEnum endpointName,
                                                            long siteId,
                                                            JSONObject response) throws JSONException {
        BaseStatsModel model = null;
        switch (endpointName) {
            case VISITS:
                model = new VisitsModel(siteId, response);
                break;
            case TOP_POSTS:
                model = new TopPostsAndPagesModel(siteId, response);
                break;
            case REFERRERS:
                model = new ReferrersModel(siteId, response);
                break;
            case CLICKS:
                model = new ClicksModel(siteId, response);
                break;
            case GEO_VIEWS:
                model = new GeoviewsModel(siteId, response);
                break;
            case AUTHORS:
                model = new AuthorsModel(siteId, response);
                break;
            case VIDEO_PLAYS:
                model = new VideoPlaysModel(siteId, response);
                break;
            case COMMENTS:
                model = new CommentsModel(siteId, response);
                break;
            case FOLLOWERS_WPCOM:
                model = new FollowersModel(siteId, response);
                break;
            case FOLLOWERS_EMAIL:
                model = new FollowersModel(siteId, response);
                break;
            case COMMENT_FOLLOWERS:
                model = new CommentFollowersModel(siteId, response);
                break;
            case TAGS_AND_CATEGORIES:
                model = new TagsContainerModel(siteId, response);
                break;
            case PUBLICIZE:
                model = new PublicizeModel(siteId, response);
                break;
            case SEARCH_TERMS:
                model = new SearchTermsModel(siteId, response);
                break;
            case INSIGHTS_ALL_TIME:
                model = new InsightsAllTimeModel(siteId, response);
                break;
            case INSIGHTS_POPULAR:
                model = new InsightsPopularModel(siteId, response);
                break;
            case INSIGHTS_TODAY:
                model = new InsightsTodayModel(siteId, response);
                break;
            case INSIGHTS_LATEST_POST_SUMMARY:
                model = new InsightsLatestPostModel(siteId, response);
                break;
            case INSIGHTS_LATEST_POST_VIEWS:
                model = new InsightsLatestPostDetailsModel(siteId, response);
                break;
        }
        return model;
    }

    public static void openPostInReaderOrInAppWebview(Context ctx,
                                                      final long remoteBlogID,
                                                      final String remoteItemID,
                                                      @Nullable final String itemType,
                                                      final String itemURL) {
        final long blogID = remoteBlogID;
        final long itemID = Long.parseLong(remoteItemID);
        if (itemType == null) {
            // If we don't know the type of the item, open it with the browser.
            AppLog.d(AppLog.T.UTILS, "Type of the item is null. Opening it in the in-app browser: " + itemURL);
            WPWebViewActivity.openURL(ctx, itemURL);
        } else if (itemType.equals(StatsConstants.ITEM_TYPE_POST)) {
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
        } else if (itemType.equals(StatsConstants.ITEM_TYPE_HOME_PAGE)) {
            ReaderActivityLauncher.showReaderBlogPreview(
                    ctx,
                    blogID
            );
        } else {
            AppLog.d(AppLog.T.UTILS, "Opening the in-app browser: " + itemURL);
            WPWebViewActivity.openURL(ctx, itemURL);
        }
    }

    public static void openPostInReaderOrInAppWebview(Context ctx, final StatsPostModel post) {
        final String postType = post.getPostType();
        final String url = post.getUrl();
        final long blogID = post.getBlogID();
        final String itemID = post.getItemID();
        openPostInReaderOrInAppWebview(ctx, blogID, itemID, postType, url);
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


    private static int roundUp(double num, double divisor) {
        double unrounded = num / divisor;
        // return (int) Math.ceil(unrounded);
        return (int) (unrounded + 0.5);
    }

    public static String getSinceLabel(Context ctx, String dataSubscribed) {
        try {
            SimpleDateFormat from = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ROOT);
            Date date = from.parse(dataSubscribed);
            return getSinceLabel(new ResourceProvider(ctx), date);
        } catch (ParseException e) {
            AppLog.e(AppLog.T.STATS, e);
        }

        return "";
    }

    public static String getSinceLabel(ResourceProvider ctx, Date date) {
        Date currentDateTime = new Date();

        // See http://momentjs.com/docs/#/displaying/fromnow/
        long currentDifference = Math.abs(StatsUtils.getDateDiff(date, currentDateTime, TimeUnit.SECONDS));

        if (currentDifference <= 45) {
            return ctx.getString(R.string.stats_followers_seconds_ago);
        }
        if (currentDifference < 90) {
            return ctx.getString(R.string.stats_followers_a_minute_ago);
        }

        // 90 seconds to 45 minutes
        if (currentDifference <= 2700) {
            long minutes = StatsUtils.roundUp(currentDifference, 60);
            String followersMinutes = ctx.getString(R.string.stats_followers_minutes);
            return String.format(followersMinutes, minutes);
        }

        // 45 to 90 minutes
        if (currentDifference <= 5400) {
            return ctx.getString(R.string.stats_followers_an_hour_ago);
        }

        // 90 minutes to 22 hours
        if (currentDifference <= 79200) {
            long hours = StatsUtils.roundUp(currentDifference, 60 * 60);
            String followersHours = ctx.getString(R.string.stats_followers_hours);
            return String.format(followersHours, hours);
        }

        // 22 to 36 hours
        if (currentDifference <= 129600) {
            return ctx.getString(R.string.stats_followers_a_day);
        }

        // 36 hours to 25 days
        // 86400 secs in a day - 2160000 secs in 25 days
        if (currentDifference <= 2160000) {
            long days = StatsUtils.roundUp(currentDifference, 86400);
            String followersDays = ctx.getString(R.string.stats_followers_days);
            return String.format(followersDays, days);
        }

        // 25 to 45 days
        // 3888000 secs in 45 days
        if (currentDifference <= 3888000) {
            return ctx.getString(R.string.stats_followers_a_month);
        }

        // 45 to 345 days
        // 2678400 secs in a month - 29808000 secs in 345 days
        if (currentDifference <= 29808000) {
            long months = StatsUtils.roundUp(currentDifference, 2678400);
            String followersMonths = ctx.getString(R.string.stats_followers_months);
            return String.format(followersMonths, months);
        }

        // 345 to 547 days (1.5 years)
        if (currentDifference <= 47260800) {
            return ctx.getString(R.string.stats_followers_a_year);
        }

        // 548 days+
        // 31536000 secs in a year
        long years = StatsUtils.roundUp(currentDifference, 31536000);
        String followersYears = ctx.getString(R.string.stats_followers_years);
        return String.format(followersYears, years);
    }

    /**
     * Transform a 2 characters country code into a 2 characters emoji flag.
     * Emoji letter A starts at: 0x1F1E6 thus,
     * 0x1F1E6 + 5 = 0x1F1EB represents the letter F
     * 0x1F1E6 + 17 = 0x1F1F7 represents the letter R
     * <p>
     * FR: 0x1F1EB 0x1F1F7 is the french flag: 🇫🇷
     * More infos on https://apps.timwhitlock.info/emoji/tables/iso3166
     *
     * @param countryCode - iso3166 country code (2chars)
     * @return emoji string representing the flag
     */
    public static String countryCodeToEmoji(String countryCode) {
        if (TextUtils.isEmpty(countryCode) || countryCode.length() != 2) {
            return "";
        }
        int char1 = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6;
        int char2 = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6;
        return new String(Character.toChars(char1)) + new String(Character.toChars(char2));
    }
}
