package org.wordpress.android.support.feature.aibot

import androidx.compose.runtime.Composable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun formatRelativeTime(date: Date): String {
    val now = Date()
    val diffMillis = now.time - date.time
    val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
    val diffHours = TimeUnit.MILLISECONDS.toHours(diffMillis)
    val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

    return when {
        diffMinutes < 1 -> "Just now"
        diffMinutes < 60 -> "$diffMinutes minute${if (diffMinutes == 1L) "" else "s"} ago"
        diffHours < 24 -> "$diffHours hour${if (diffHours == 1L) "" else "s"} ago"
        diffDays < 7 -> "$diffDays day${if (diffDays == 1L) "" else "s"} ago"
        diffDays < 30 -> "${diffDays / 7} week${if (diffDays / 7 == 1L) "" else "s"} ago"
        else -> {
            val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            formatter.format(date)
        }
    }
}

@Composable
fun formatRelativeTimePreview(date: Date): String {
    val now = Date()
    val diffMillis = now.time - date.time
    val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
    val diffHours = TimeUnit.MILLISECONDS.toHours(diffMillis)
    val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

    return when {
        diffMinutes < 1 -> "Just now"
        diffMinutes < 60 -> "$diffMinutes minute${if (diffMinutes == 1L) "" else "s"} ago"
        diffHours < 24 -> "$diffHours hour${if (diffHours == 1L) "" else "s"} ago"
        diffDays < 7 -> "$diffDays day${if (diffDays == 1L) "" else "s"} ago"
        diffDays < 30 -> "${diffDays / 7} week${if (diffDays / 7 == 1L) "" else "s"} ago"
        else -> {
            val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            formatter.format(date)
        }
    }
}
