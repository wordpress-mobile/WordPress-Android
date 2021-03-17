package org.wordpress.android.ui.stats;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.tracker.ReaderTracker;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.viewmodel.ResourceProvider;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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

    /**
     * Get the current date in the form of yyyy-MM-dd (EX: 2013-07-18) *
     */
    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(StatsConstants.STATS_INPUT_DATE_FORMAT, Locale.ROOT);
        return sdf.format(new Date());
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

    public static void openPostInReaderOrInAppWebview(Context ctx,
                                                      final long remoteBlogID,
                                                      final String remoteItemID,
                                                      @Nullable final String itemType,
                                                      final String itemURL,
                                                      final ReaderTracker readerTracker) {
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
                ReaderPost post = ReaderPostTable.getBlogPost(blogID, itemID, true);
                ReaderActivityLauncher.showReaderBlogPreview(
                        ctx,
                        blogID,
                        post != null ? post.isFollowedByCurrentUser : null,
                        ReaderTracker.SOURCE_STATS,
                        readerTracker
                );
            } else {
                ReaderActivityLauncher.showReaderPostDetail(
                        ctx,
                        blogID,
                        itemID
                );
            }
        } else if (itemType.equals(StatsConstants.ITEM_TYPE_HOME_PAGE)) {
            ReaderPost post = ReaderPostTable.getBlogPost(blogID, itemID, true);
            ReaderActivityLauncher.showReaderBlogPreview(
                    ctx,
                    blogID,
                    post != null ? post.isFollowedByCurrentUser : null,
                    ReaderTracker.SOURCE_STATS,
                    readerTracker
            );
        } else {
            // For now, itemType.ATTACHMENT falls down this path. No need to repeat unless we
            // want to handle attachments differently in the future.
            AppLog.d(AppLog.T.UTILS, "Opening the in-app browser: " + itemURL);
            WPWebViewActivity.openURL(ctx, itemURL);
        }
    }

    private static int roundUp(double num, double divisor) {
        double unrounded = num / divisor;
        // return (int) Math.ceil(unrounded);
        return (int) (unrounded + 0.5);
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
