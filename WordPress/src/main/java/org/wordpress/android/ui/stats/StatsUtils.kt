package org.wordpress.android.ui.stats

import android.content.Context
import org.wordpress.android.R
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.reader.ReaderActivityLauncher
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.UTILS
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

object StatsUtils {
    /**
     * Get a diff between two dates
     *
     * @param date1    the oldest date in Ms
     * @param date2    the newest date in Ms
     * @param timeUnit the unit in which you want the diff
     * @return the diff value, in the provided unit
     */
    fun getDateDiff(date1: Date, date2: Date, timeUnit: TimeUnit): Long {
        val diffInMillies = date2.time - date1.time
        return timeUnit.convert(diffInMillies, MILLISECONDS)
    }

    fun openPostInReaderOrInAppWebview(
        ctx: Context?,
        remoteBlogID: Long,
        remoteItemID: String,
        itemType: String?,
        itemURL: String,
        readerTracker: ReaderTracker?
    ) {
        val itemID = remoteItemID.toLong()
        if (itemType == null) {
            // If we don't know the type of the item, open it with the browser.
            AppLog.d(UTILS, "Type of the item is null. Opening it in the in-app browser: $itemURL")
            WPWebViewActivity.openURL(ctx, itemURL)
        } else if (itemType == StatsConstants.ITEM_TYPE_POST) {
            // If the post/page has ID == 0 is the home page, and we need to load the blog preview,
            // otherwise 404 is returned if we try to show the post in the reader
            if (itemID == 0L) {
                val post = ReaderPostTable.getBlogPost(remoteBlogID, itemID, true)
                ReaderActivityLauncher.showReaderBlogPreview(
                        ctx,
                        remoteBlogID,
                        post?.isFollowedByCurrentUser,
                        ReaderTracker.SOURCE_STATS,
                        readerTracker
                )
            } else {
                ReaderActivityLauncher.showReaderPostDetail(
                        ctx,
                        remoteBlogID,
                        itemID
                )
            }
        } else if (itemType == StatsConstants.ITEM_TYPE_HOME_PAGE) {
            val post = ReaderPostTable.getBlogPost(remoteBlogID, itemID, true)
            ReaderActivityLauncher.showReaderBlogPreview(
                    ctx,
                    remoteBlogID,
                    post?.isFollowedByCurrentUser,
                    ReaderTracker.SOURCE_STATS,
                    readerTracker
            )
        } else {
            // For now, itemType.ATTACHMENT falls down this path. No need to repeat unless we
            // want to handle attachments differently in the future.
            AppLog.d(UTILS, "Opening the in-app browser: $itemURL")
            WPWebViewActivity.openURL(ctx, itemURL)
        }
    }

    private fun roundUp(num: Double, divisor: Double): Int {
        val unrounded = num / divisor
        // return (int) Math.ceil(unrounded);
        return (unrounded + 0.5).toInt()
    }

    fun getSinceLabel(ctx: ResourceProvider, date: Date): String {
        val currentDateTime = Date()

        // See http://momentjs.com/docs/#/displaying/fromnow/
        val currentDifference = Math.abs(getDateDiff(date, currentDateTime, SECONDS))
        if (currentDifference <= 45) {
            return ctx.getString(R.string.stats_followers_seconds_ago)
        }
        if (currentDifference < 90) {
            return ctx.getString(R.string.stats_followers_a_minute_ago)
        }

        // 90 seconds to 45 minutes
        if (currentDifference <= 2700) {
            val minutes = roundUp(currentDifference.toDouble(), 60.0).toLong()
            val followersMinutes = ctx.getString(R.string.stats_followers_minutes)
            return String.format(followersMinutes, minutes)
        }

        // 45 to 90 minutes
        if (currentDifference <= 5400) {
            return ctx.getString(R.string.stats_followers_an_hour_ago)
        }

        // 90 minutes to 22 hours
        if (currentDifference <= 79200) {
            val hours = roundUp(currentDifference.toDouble(), (60 * 60).toDouble()).toLong()
            val followersHours = ctx.getString(R.string.stats_followers_hours)
            return String.format(followersHours, hours)
        }

        // 22 to 36 hours
        if (currentDifference <= 129600) {
            return ctx.getString(R.string.stats_followers_a_day)
        }

        // 36 hours to 25 days
        // 86400 secs in a day - 2160000 secs in 25 days
        if (currentDifference <= 2160000) {
            val days = roundUp(currentDifference.toDouble(), 86400.0).toLong()
            val followersDays = ctx.getString(R.string.stats_followers_days)
            return String.format(followersDays, days)
        }

        // 25 to 45 days
        // 3888000 secs in 45 days
        if (currentDifference <= 3888000) {
            return ctx.getString(R.string.stats_followers_a_month)
        }

        // 45 to 345 days
        // 2678400 secs in a month - 29808000 secs in 345 days
        if (currentDifference <= 29808000) {
            val months = roundUp(currentDifference.toDouble(), 2678400.0).toLong()
            val followersMonths = ctx.getString(R.string.stats_followers_months)
            return String.format(followersMonths, months)
        }

        // 345 to 547 days (1.5 years)
        if (currentDifference <= 47260800) {
            return ctx.getString(R.string.stats_followers_a_year)
        }

        // 548 days+
        // 31536000 secs in a year
        val years = roundUp(currentDifference.toDouble(), 31536000.0).toLong()
        val followersYears = ctx.getString(R.string.stats_followers_years)
        return String.format(followersYears, years)
    }
}
