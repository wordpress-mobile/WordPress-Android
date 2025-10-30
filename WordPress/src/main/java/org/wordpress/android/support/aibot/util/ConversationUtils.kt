package org.wordpress.android.support.aibot.util

import android.content.res.Resources
import androidx.compose.ui.text.AnnotatedString
import org.wordpress.android.R
import org.wordpress.android.support.aibot.model.BotConversation
import org.wordpress.android.support.aibot.model.BotMessage
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

@Suppress("MagicNumber", "LongMethod")
fun generateSampleBotConversations(): List<BotConversation> {
    val now = Date()
    return listOf(
        // Conversation 1: App Crashing on Launch
        BotConversation(
            id = 1234,
            createdAt = Date(now.time - 3_600_000), // 1 hour ago
            mostRecentMessageDate = Date(now.time - 120_000), // 2 minutes ago
            lastMessage = "Wonderful! I'm so glad that resolved the issue for you.",
            messages = listOf(
                BotMessage(
                    id = 1001,
                    rawText = "",
                    formattedText = AnnotatedString("Hi, I'm having trouble with the app. It keeps crashing " +
                            "when I try to open it after " +
                            "the latest update. Can you help?"),
                    date = Date(now.time - 3_600_000), // 1 hour ago
                    isWrittenByUser = true
                ),
                BotMessage(
                    id = 1002,
                    rawText = "",
                    formattedText = AnnotatedString("I'm sorry to hear you're experiencing crashes! I'd be " +
                            "happy to help you troubleshoot " +
                            "this issue. Let me ask a few questions to better understand what's happening. " +
                            "What device are you using and what Android version are you running?"),
                    date = Date(now.time - 3_540_000), // 59 minutes ago
                    isWrittenByUser = false
                ),
                BotMessage(
                    id = 1003,
                    rawText = "",
                    formattedText = AnnotatedString("I'm using a Pixel 8 Pro with Android 14. The app worked " +
                            "fine before the update yesterday."),
                    date = Date(now.time - 3_480_000), // 58 minutes ago
                    isWrittenByUser = true
                ),
                BotMessage(
                    id = 1004,
                    rawText = "",
                    formattedText = AnnotatedString("Thank you for that information! Android 14 on Pixel 8 Pro " +
                            "should work well with our " +
                            "latest update. Let's try a few troubleshooting steps:\n\n1. First, try force-closing " +
                            "the app and reopening it\n2. If that doesn't work, try restarting your phone\n" +
                            "3. As a last resort, you might need to clear app data or reinstall\n\nCan you try " +
                            "step 1 first and let me know if that helps?"),
                    date = Date(now.time - 3_420_000), // 57 minutes ago
                    isWrittenByUser = false
                ),
                BotMessage(
                    id = 1005,
                    rawText = "" +
                            "I tap the app icon. Should I try reinstalling?",
                    formattedText = AnnotatedString("I tried force-closing and restarting my phone, but it's " +
                            "still crashing immediately when " +
                            "I tap the app icon. Should I try reinstalling?"),
                    date = Date(now.time - 3_300_000), // 55 minutes ago
                    isWrittenByUser = true
                ),
            )
        ),

        // Conversation 2: Site Setup Assistance
        BotConversation(
            id = 1235,
            createdAt = Date(now.time - 7_800_000),
            mostRecentMessageDate = Date(now.time - 7_200_000), // 2 hours ago
            lastMessage = "Congratulations on your new site! I'd be happy to help you get started.",
            messages = listOf(
                BotMessage(
                    id = 2001,
                    rawText = "",
                    formattedText = AnnotatedString("I just created my WordPress site and need help getting " +
                            "started. Where should I begin?"),
                    date = Date(now.time - 7_800_000),
                    isWrittenByUser = true
                ),
                BotMessage(
                    id = 2002,
                    rawText = "",
                    formattedText = AnnotatedString("Congratulations on your new site! I'd be happy to help " +
                            "you get started. Here are the key " +
                            "first steps:\n\n1. Choose and customize a theme\n2. Create your first pages (Home, " +
                            "About, Contact)\n3. Set up your site navigation\n4. Add your first blog post\n\n" +
                            "Which of these would you like to tackle first?"),
                    date = Date(now.time - 7_200_000),
                    isWrittenByUser = false
                )
            )
        ),

        // Conversation 3: Theme Customization
        BotConversation(
            id = 1236,
            createdAt = Date(now.time - 87_000_000),
            mostRecentMessageDate = Date(now.time - 86_400_000), // 1 day ago
            lastMessage = "You can change the colors by going to Appearance → Customize → Colors.",
            messages = listOf(
                BotMessage(
                    id = 3001,
                    rawText = "",
                    formattedText = AnnotatedString("How can I change the colors on my site? I want to " +
                            "match my brand."),
                    date = Date(now.time - 87_000_000),
                    isWrittenByUser = true
                ),
                BotMessage(
                    id = 3002,
                    rawText = "",
                    formattedText = AnnotatedString("You can change the colors by going to Appearance → " +
                            "Customize → Colors in your dashboard. " +
                            "Most themes allow you to customize colors for backgrounds, text, links, and buttons. " +
                            "Would you like step-by-step instructions?"),
                    date = Date(now.time - 86_400_000),
                    isWrittenByUser = false
                )
            )
        ),
    )
}
