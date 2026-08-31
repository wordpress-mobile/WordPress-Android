package org.wordpress.android.support.unified.util

import android.content.res.Resources
import org.wordpress.android.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


@Suppress("MagicNumber")
fun formatRelativeTime(date: Date, res: Resources): String {
    val now = Date()
    val diffMillis = now.time - date.time
    val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
    val diffHours = TimeUnit.MILLISECONDS.toHours(diffMillis)
    val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

    return when {
        diffMinutes < 1 -> res.getString(R.string.ai_bot_time_just_now)
        diffMinutes < 60 -> res.getQuantityString(
            R.plurals.ai_bot_time_minutes_ago,
            diffMinutes.toInt(),
            diffMinutes
        )
        diffHours < 24 -> res.getQuantityString(
            R.plurals.ai_bot_time_hours_ago,
            diffHours.toInt(),
            diffHours
        )
        diffDays < 7 -> res.getQuantityString(
            R.plurals.ai_bot_time_days_ago,
            diffDays.toInt(),
            diffDays
        )
        diffDays < 30 -> {
            val weeks = diffDays / 7
            res.getQuantityString(
                R.plurals.ai_bot_time_weeks_ago,
                weeks.toInt(),
                weeks
            )
        }
        else -> {
            val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            formatter.format(date)
        }
    }
}
