package org.wordpress.android.ui.posts.social.compose

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.compose.unit.Margin

@Composable
fun PostSocialMessageItem(
    message: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .clickable(enabled, onClick = onClick)
            .padding(horizontal = Margin.ExtraLarge.value, vertical = Margin.MediumLarge.value)
    ) {
        Text(
            text = stringResource(R.string.social_item_message_title),
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onSurface
                .copy(alpha = if (enabled) ContentAlpha.high else ContentAlpha.disabled),
        )
        Text(
            text = message,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface
                .copy(alpha = if (enabled) ContentAlpha.medium else ContentAlpha.disabled),
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PostSocialMessageItemPreview() {
    val messages = listOf(
        "5 Chicken Recipes that you have to try on the grill this summer",
        "Small message sample",
        "Message to be shared to the social network when I publish this post"
    )
    var messageId by remember { mutableIntStateOf(0) }

    val updateMessage = {
        messageId = (messageId + 1) % messages.size
    }

    AppThemeM2 {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            PostSocialMessageItem(
                message = messages[messageId],
                onClick = updateMessage,
                modifier = Modifier.fillMaxWidth()
            )

            Divider()

            PostSocialMessageItem(
                message = messages[0],
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
            )
        }
    }
}
