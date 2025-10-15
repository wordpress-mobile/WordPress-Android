package org.wordpress.android.support.aibot.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import org.wordpress.android.R
import org.wordpress.android.support.aibot.model.BotConversation
import org.wordpress.android.support.aibot.model.BotMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


@Composable
fun formatRelativeTime(date: Date): String {
    val context = LocalContext.current
    val now = Date()
    val diffMillis = now.time - date.time
    val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
    val diffHours = TimeUnit.MILLISECONDS.toHours(diffMillis)
    val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

    return when {
        diffMinutes < 1 -> context.getString(R.string.ai_bot_time_just_now)
        diffMinutes < 60 -> if (diffMinutes == 1L) {
            context.getString(R.string.ai_bot_time_minute_ago, diffMinutes)
        } else {
            context.getString(R.string.ai_bot_time_minutes_ago, diffMinutes)
        }
        diffHours < 24 -> if (diffHours == 1L) {
            context.getString(R.string.ai_bot_time_hour_ago, diffHours)
        } else {
            context.getString(R.string.ai_bot_time_hours_ago, diffHours)
        }
        diffDays < 7 -> if (diffDays == 1L) {
            context.getString(R.string.ai_bot_time_day_ago, diffDays)
        } else {
            context.getString(R.string.ai_bot_time_days_ago, diffDays)
        }
        diffDays < 30 -> {
            val weeks = diffDays / 7
            if (weeks == 1L) {
                context.getString(R.string.ai_bot_time_week_ago, weeks)
            } else {
                context.getString(R.string.ai_bot_time_weeks_ago, weeks)
            }
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
                    text = "Hi, I'm having trouble with the app. It keeps crashing when I try to open it after " +
                            "the latest update. Can you help?",
                    date = Date(now.time - 3_600_000), // 1 hour ago
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = true
                ),
                BotMessage(
                    id = 1002,
                    text = "I'm sorry to hear you're experiencing crashes! I'd be happy to help you troubleshoot " +
                            "this issue. Let me ask a few questions to better understand what's happening. " +
                            "What device are you using and what Android version are you running?",
                    date = Date(now.time - 3_540_000), // 59 minutes ago
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = false
                ),
                BotMessage(
                    id = 1003,
                    text = "I'm using a Pixel 8 Pro with Android 14. The app worked fine before the update yesterday.",
                    date = Date(now.time - 3_480_000), // 58 minutes ago
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = true
                ),
                BotMessage(
                    id = 1004,
                    text = "Thank you for that information! Android 14 on Pixel 8 Pro should work well with our " +
                            "latest update. Let's try a few troubleshooting steps:\n\n1. First, try force-closing " +
                            "the app and reopening it\n2. If that doesn't work, try restarting your phone\n" +
                            "3. As a last resort, you might need to clear app data or reinstall\n\nCan you try " +
                            "step 1 first and let me know if that helps?",
                    date = Date(now.time - 3_420_000), // 57 minutes ago
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = false
                ),
                BotMessage(
                    id = 1005,
                    text = "I tried force-closing and restarting my phone, but it's still crashing immediately when " +
                            "I tap the app icon. Should I try reinstalling?",
                    date = Date(now.time - 3_300_000), // 55 minutes ago
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = true
                ),
                BotMessage(
                    id = 1006,
                    text = "Yes, let's try reinstalling the app. This will often resolve issues caused by corrupted " +
                            "app data during updates. Here's what to do:\n\n1. Long press the app icon and tap " +
                            "'Uninstall'\n2. Go to the Play Store and reinstall the app\n" +
                            "3. Sign back into your account\n\nYour data should be preserved if you're signed " +
                            "into your account. Give this a try and let me know how it goes!",
                    date = Date(now.time - 3_240_000), // 54 minutes ago
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = false
                ),
                BotMessage(
                    id = 1007,
                    text = "That worked! The app is opening normally now. Thank you so much for your help!",
                    date = Date(now.time - 180_000), // 3 minutes ago
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = true
                ),
                BotMessage(
                    id = 1008,
                    text = "Wonderful! I'm so glad that resolved the issue for you. The reinstall process often " +
                            "fixes problems that occur during app updates. If you run into any other issues, please " +
                            "don't hesitate to reach out. Is there anything else I can help you with today?",
                    date = Date(now.time - 120_000), // 2 minutes ago
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = false
                )
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
                    text = "I just created my WordPress site and need help getting started. Where should I begin?",
                    date = Date(now.time - 7_800_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = true
                ),
                BotMessage(
                    id = 2002,
                    text = "Congratulations on your new site! I'd be happy to help you get started. Here are the key " +
                            "first steps:\n\n1. Choose and customize a theme\n2. Create your first pages (Home, " +
                            "About, Contact)\n3. Set up your site navigation\n4. Add your first blog post\n\n" +
                            "Which of these would you like to tackle first?",
                    date = Date(now.time - 7_200_000),
                    userWantsToTalkToHuman = false,
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
                    text = "How can I change the colors on my site? I want to match my brand.",
                    date = Date(now.time - 87_000_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = true
                ),
                BotMessage(
                    id = 3002,
                    text = "You can change the colors by going to Appearance → Customize → Colors in your dashboard. " +
                            "Most themes allow you to customize colors for backgrounds, text, links, and buttons. " +
                            "Would you like step-by-step instructions?",
                    date = Date(now.time - 86_400_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = false
                )
            )
        ),

        // Conversation 4: SEO Help
        BotConversation(
            id = 1237,
            createdAt = Date(now.time - 259_800_000),
            mostRecentMessageDate = Date(now.time - 259_200_000), // 3 days ago
            lastMessage = "To improve your SEO, consider installing an SEO plugin like Yoast.",
            messages = listOf(
                BotMessage(
                    id = 4001,
                    text = "My site isn't showing up in Google search results. What should I do?",
                    date = Date(now.time - 259_800_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = true
                ),
                BotMessage(
                    id = 4002,
                    text = "To improve your SEO, consider these steps:\n\n1. Install an SEO plugin like Yoast\n" +
                            "2. Submit your sitemap to Google Search Console\n" +
                            "3. Use descriptive titles and meta descriptions\n4. Create quality content regularly\n" +
                            "5. Build internal links between pages\n\n" +
                            "Would you like detailed guidance on any of these?",
                    date = Date(now.time - 259_200_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = false
                )
            )
        ),

        // Conversation 5: Performance Questions
        BotConversation(
            id = 1238,
            createdAt = Date(now.time - 605_400_000),
            mostRecentMessageDate = Date(now.time - 604_800_000), // 1 week ago
            lastMessage = "Your site is loading well, but here are some tips to optimize further.",
            messages = listOf(
                BotMessage(
                    id = 5001,
                    text = "My website seems to be loading slowly. What can I do to speed it up?",
                    date = Date(now.time - 605_400_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = true
                ),
                BotMessage(
                    id = 5002,
                    text = "Your site is loading well, but here are some tips to optimize further:\n\n" +
                            "1. Optimize images (compress before uploading)\n2. Use a caching plugin\n" +
                            "3. Enable lazy loading for images\n4. Minimize plugins\n" +
                            "5. Use a CDN for static assets\n\nLet me know which area you'd like to focus on first!",
                    date = Date(now.time - 604_800_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = false
                )
            )
        )
    )
}
