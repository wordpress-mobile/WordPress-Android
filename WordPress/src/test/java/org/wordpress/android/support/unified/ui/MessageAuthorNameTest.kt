package org.wordpress.android.support.unified.ui

import androidx.compose.ui.text.AnnotatedString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.support.unified.model.UnifiedMessage
import java.util.Date

class MessageAuthorNameTest {
    @Test
    fun `the user's own message is labelled with the account display name`() {
        // The endpoint labels pre-escalation user messages with the bare WP.com login (CMM-2455).
        val name = resolve(message(authorRole = "user", authorName = "alinclamba85d71154af"))

        assertThat(name).isEqualTo(DISPLAY_NAME)
    }

    @Test
    fun `the user's own message falls back to the API name when there is no display name`() {
        val name = resolve(
            message = message(authorRole = "user", authorName = "alinclamba85d71154af"),
            currentUserName = ""
        )

        assertThat(name).isEqualTo("alinclamba85d71154af")
    }

    @Test
    fun `the bot is labelled with the assistant name rather than the raw role`() {
        val name = resolve(message(authorRole = "bot", authorName = "bot"))

        assertThat(name).isEqualTo(ASSISTANT_NAME)
    }

    @Test
    fun `a bot author name is replaced even under another role`() {
        val name = resolve(message(authorRole = "assistant", authorName = "bot"))

        assertThat(name).isEqualTo(ASSISTANT_NAME)
    }

    @Test
    fun `a Happiness Engineer keeps the name the API returns`() {
        val name = resolve(message(authorRole = "support", authorName = "Some Happiness Engineer"))

        assertThat(name).isEqualTo("Some Happiness Engineer")
    }

    @Test
    fun `a Happiness Engineer is not mistaken for the user when the names match`() {
        val name = resolve(message(authorRole = "support", authorName = DISPLAY_NAME))

        assertThat(name).isEqualTo(DISPLAY_NAME)
    }

    private fun resolve(message: UnifiedMessage, currentUserName: String = DISPLAY_NAME) =
        resolveMessageAuthorName(
            message = message,
            currentUserName = currentUserName,
            assistantName = ASSISTANT_NAME
        )

    private fun message(authorRole: String, authorName: String) = UnifiedMessage(
        id = 1L,
        formattedText = AnnotatedString("Message"),
        authorRole = authorRole,
        authorName = authorName,
        createdAt = Date(),
        attachments = emptyList(),
    )

    companion object {
        private const val DISPLAY_NAME = "Adalberto Plaza"
        private const val ASSISTANT_NAME = "AI Assistant"
    }
}
