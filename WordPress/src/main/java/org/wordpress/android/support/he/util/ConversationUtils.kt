package org.wordpress.android.support.he.util

import org.wordpress.android.support.he.model.SupportConversation
import org.wordpress.android.support.he.model.SupportMessage
import java.util.Date

@Suppress("MagicNumber", "LongMethod")
fun generateSampleSupportConversations(): List<SupportConversation> {
    val now = Date()
    val oneHourAgo = Date(now.time - 3600000)
    val twoDaysAgo = Date(now.time - 172800000)
    val oneWeekAgo = Date(now.time - 604800000)

    return listOf(
        SupportConversation(
            id = 1,
            title = "Issue with site loading",
            description = "My site is loading slowly",
            lastMessageSentAt = oneHourAgo,
            messages = listOf(
                SupportMessage(
                    id = 1,
                    text = "Hello! My website has been loading very slowly for the past few days.",
                    createdAt = Date(oneHourAgo.time - 1800000),
                    authorName = "You",
                    authorIsUser = true
                ),
                SupportMessage(
                    id = 2,
                    text = "Hi there! I'd be happy to help you with that. Can you share your site URL?",
                    createdAt = Date(oneHourAgo.time - 900000),
                    authorName = "Support Agent",
                    authorIsUser = false
                ),
                SupportMessage(
                    id = 3,
                    text = "Sure, it's example.wordpress.com",
                    createdAt = oneHourAgo,
                    authorName = "You",
                    authorIsUser = true
                )
            )
        ),
        SupportConversation(
            id = 2,
            title = "Plugin compatibility question",
            description = "Question about plugin compatibility",
            lastMessageSentAt = twoDaysAgo,
            messages = listOf(
                SupportMessage(
                    id = 4,
                    text = "I'm trying to install a new plugin but getting an error.",
                    createdAt = Date(twoDaysAgo.time - 3600000),
                    authorName = "You",
                    authorIsUser = true
                ),
                SupportMessage(
                    id = 5,
                    text = "I can help with that! What's the error message you're seeing?",
                    createdAt = twoDaysAgo,
                    authorName = "Support Agent",
                    authorIsUser = false
                )
            )
        ),
        SupportConversation(
            id = 3,
            title = "Custom domain setup",
            description = "Help setting up custom domain",
            lastMessageSentAt = oneWeekAgo,
            messages = listOf(
                SupportMessage(
                    id = 6,
                    text = "I need help setting up my custom domain.",
                    createdAt = oneWeekAgo,
                    authorName = "You",
                    authorIsUser = true
                )
            )
        )
    )
}
