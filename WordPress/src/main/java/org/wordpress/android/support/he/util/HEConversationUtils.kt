package org.wordpress.android.support.he.util

import androidx.compose.ui.text.AnnotatedString
import org.wordpress.android.support.he.model.SupportConversation
import org.wordpress.android.support.he.model.SupportMessage
import java.util.Date

@Suppress("MagicNumber", "LongMethod")
fun generateSampleHESupportConversations(): List<SupportConversation> {
    val now = Date()
    val oneHourAgo = Date(now.time - 3600000)
    val twoDaysAgo = Date(now.time - 172800000)
    val oneWeekAgo = Date(now.time - 604800000)

    return listOf(
        SupportConversation(
            id = 1,
            title = "Login Issues with Two-Factor Authentication Not Working on Mobile App",
            description = "I'm having trouble logging into my account. The two-factor authentication code " +
                "doesn't seem to be working properly when I try to access my site from the mobile app.",
            lastMessageSentAt = oneHourAgo,
            messages = listOf(
                SupportMessage(
                    id = 1,
                    rawText = "",
                    formattedText = AnnotatedString("Hello! My website has been loading very slowly for " +
                            "the past few days."),
                    createdAt = Date(oneHourAgo.time - 1800000),
                    authorName = "You",
                    authorIsUser = true
                ),
                SupportMessage(
                    id = 2,
                    rawText = "",
                    formattedText = AnnotatedString("Hi there! I'd be happy to help you with that. " +
                            "Can you share your site URL?"),
                    createdAt = Date(oneHourAgo.time - 900000),
                    authorName = "Support Agent",
                    authorIsUser = false
                ),
                SupportMessage(
                    id = 3,
                    rawText = "",
                    formattedText = AnnotatedString("Sure, it's example.wordpress.com"),
                    createdAt = oneHourAgo,
                    authorName = "You",
                    authorIsUser = true
                )
            )
        ),
        SupportConversation(
            id = 2,
            title = "Website Performance Issues After Installing New Theme and Plugins",
            description = "After updating my theme and installing several new plugins for my e-commerce " +
                "store, I've noticed significant slowdowns and occasional timeout errors affecting customer " +
                "experience.",
            lastMessageSentAt = twoDaysAgo,
            messages = listOf(
                SupportMessage(
                    id = 4,
                    rawText = "",
                    formattedText = AnnotatedString("I'm trying to install a new plugin but getting an error."),
                    createdAt = Date(twoDaysAgo.time - 3600000),
                    authorName = "You",
                    authorIsUser = true
                ),
                SupportMessage(
                    id = 5,
                    rawText = "",
                    formattedText = AnnotatedString("I can help with that! What's the error message you're seeing?"),
                    createdAt = twoDaysAgo,
                    authorName = "Support Agent",
                    authorIsUser = false
                )
            )
        ),
        SupportConversation(
            id = 3,
            title = "Need Help Configuring Custom Domain DNS Settings and Email Forwarding",
            description = "I recently purchased a custom domain and need assistance with proper DNS " +
                "configuration, SSL certificate setup, and setting up professional email forwarding for my " +
                "business site.",
            lastMessageSentAt = oneWeekAgo,
            messages = listOf(
                SupportMessage(
                    id = 6,
                    rawText = "",
                    formattedText = AnnotatedString("I need help setting up my custom domain."),
                    createdAt = oneWeekAgo,
                    authorName = "You",
                    authorIsUser = true
                )
            )
        )
    )
}
