package org.wordpress.android.support.unified.model

import androidx.compose.ui.text.AnnotatedString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Date

class UnifiedMessageTest {
    @Test
    fun `the user role maps to a user message`() {
        val message = message(authorRole = "user", authorName = "alinclamba85d71154af")

        assertThat(message.isUser).isTrue()
        assertThat(message.isBot).isFalse()
    }

    @Test
    fun `the user role is matched case insensitively`() {
        val message = message(authorRole = "User", authorName = "adalpari")

        assertThat(message.isUser).isTrue()
        assertThat(message.isSupport).isFalse()
    }

    @Test
    fun `the bot role maps to a bot message`() {
        assertThat(message(authorRole = "bot", authorName = "bot").isBot).isTrue()
    }

    @Test
    fun `the bot role is matched case insensitively`() {
        assertThat(message(authorRole = "Bot", authorName = "Bot").isBot).isTrue()
    }

    @Test
    fun `a bot author name marks the message as a bot message`() {
        assertThat(message(authorRole = "assistant", authorName = "bot").isBot).isTrue()
    }

    @Test
    fun `a Happiness Engineer message is neither a user nor a bot message`() {
        val message = message(authorRole = "support", authorName = "Some Happiness Engineer")

        assertThat(message.isUser).isFalse()
        assertThat(message.isBot).isFalse()
        assertThat(message.isSupport).isTrue()
    }

    @Test
    fun `neither the user nor the bot counts as support`() {
        assertThat(message(authorRole = "user", authorName = "adalpari").isSupport).isFalse()
        assertThat(message(authorRole = "bot", authorName = "bot").isSupport).isFalse()
        assertThat(message(authorRole = "Bot", authorName = "Bot").isSupport).isFalse()
        assertThat(message(authorRole = "assistant", authorName = "bot").isSupport).isFalse()
    }

    private fun message(authorRole: String, authorName: String) = UnifiedMessage(
        id = 1L,
        formattedText = AnnotatedString("Message"),
        authorRole = authorRole,
        authorName = authorName,
        createdAt = Date(),
        attachments = emptyList(),
    )
}
