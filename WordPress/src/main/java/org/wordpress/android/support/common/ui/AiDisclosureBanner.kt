package org.wordpress.android.support.common.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.wordpress.android.R

/**
 * The fixed "You're chatting with AI from WordPress.com Support." line shown at the top of the AI
 * support chat, outside the scrolling messages so it stays visible as the conversation grows.
 *
 * Required by the EU AI Act (Art. 50): the wording comes from the app, never from the bot, and it
 * applies only while the AI is answering. Once a conversation is escalated to a Happiness Engineer
 * and becomes a ticket, the disclosure is not shown any more.
 *
 * The shadow is what separates it from the conversation: bot bubbles are painted in the same
 * surfaceVariant as this banner, so a message scrolling underneath would otherwise merge into it.
 */
@Composable
fun AiDisclosureBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shadowElevation = 3.dp,
    ) {
        Text(
            text = stringResource(
                R.string.ai_support_disclosure,
                stringResource(R.string.ai_support_disclosure_product_name)
            ),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}
